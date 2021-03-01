package venus.terminal.cmds

import venus.Driver
import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal

var save = Command(
        name = "save",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            if (args.size != 1) {
                return "save: Takes in one argument [filename] which you want to save the editor to."
            }
            val txt: String
            try {
                js("codeMirror.save();")
                txt = Driver.getText()
            } catch (e: Throwable) {
                console.error(e)
                return "save: Could not save file!"
            }
            var result = t.vfs.write(args[0], txt)
            if (result != "") {
                result = t.vfs.touch(args[0])
                if (result == "") {
                    result = t.vfs.write(args[0], txt)
                }
            }
            if (result == "") {
                t.vfs.save()
                val obj = t.vfs.getObjectFromPath(args[0])!!
                Driver.saveVFObjectfromObj(obj, false)
            }
            return result
        },
        tab = ::fileTabComplete,
        help = """Saves the data in the editor to the specified file.
            |Usage: save [filename]
        """.trimMargin()
)