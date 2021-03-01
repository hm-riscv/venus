package venus.terminal.cmds

import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal
import venus.vfs.VFSDummy
import venus.vfs.VFSFile
import venus.vfs.VFSFolder
import venus.vfs.VFSType
import venus.zip.Zip

var zip = Command(
        name = "zip",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            if (args.size < 2) {
                return "zip: Takes at least two arguments, the zip file name and the file you want to add to the zip."
            }
            val s = StringBuilder()
            var output = args.removeAt(0)
            val z = Zip()
            for (fname in args) {
                val f = t.vfs.getObjectFromPath(fname) ?: VFSDummy()
                if (f.type == VFSType.File) {
                    z.addFile(fname, (f as VFSFile).readText())
                } else if (f.type == VFSType.Folder) {
                    z.addFolder((f as VFSFolder))
                } else {
                    s.append("'$fname' is not a path to a file! For now, this function only accepts files.\n")
                }
            }

            z.save(output)
            return s.toString()
        },
        tab = ::fileTabComplete,
        help = "This command creates a zip file."
)