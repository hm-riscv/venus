package venus.terminal.cmds

import venus.Driver
import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal
import venus.vfs.VFSType

var edit = Command(
        name = "edit",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            if (args.size != 1) {
                return "edit: Takes in one argument [filename]"
            }
            var obj = t.vfs.getObjectFromPath(args[0])
            if (obj === null) {
                val tout = t.vfs.touch(args[0])
                if (tout != "") {
                    return "edit: $tout"
                }
                obj = t.vfs.getObjectFromPath(args[0]) ?: return "edit: Failed to make the file!"
            }
            if (obj.type !== VFSType.File) {
                return "edit: Only files can be loaded into the editor."
            }
            try {
                Driver.editVFObjectfromObj(obj)
            } catch (e: Throwable) {
                return "edit: Could not load file to the editor!"
            }
            return ""
        },
        tab = ::fileTabComplete,
        help = "edit: Takes in one argument [filename] and will copy the contents to the editor tab and then go to the editor tab." +
                "\nUsage: edit [filename]"
)