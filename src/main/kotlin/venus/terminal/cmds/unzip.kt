package venus.terminal.cmds

import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal
import venus.vfs.VFSFile
import venus.vfs.VFSFolder
import venus.vfs.VFSType
import venus.zip.Zip

var unzip = Command(
        name = "unzip",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            if (args.size != 1) {
                return "unzip: Takes one arg. The file to unzip."
            }
            val s = StringBuilder()
            var filename = args.removeAt(0)
            var file = t.vfs.getObjectFromPath(filename) ?: return "unzip: Could not find the file specified!"
            if (file.type != VFSType.File) {
                return "unzip: The specified file is not a file."
            }
            val z = Zip()
            z.loadZip(file as VFSFile, t.vfs, t.vfs.currentLocation as VFSFolder)
            return "VDIRECTIVE:EXEFN..."
        },
        tab = ::fileTabComplete,
        help = "This command will unzip the file specified into the current directory."
)