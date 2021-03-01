package venus.terminal.cmds

import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal
import venus.vfs.VirtualFileSystem

var mv = Command(
        name = "mv",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            if (args.size < 2) {
                return Command["mv"].help
            }
            var destLocation = args.removeAt(args.lastIndex)
            var sourceFiles = args
            var result = ""
            var d = t.vfs.getObjectFromPath(destLocation)
            if (d == null) {
                if (sourceFiles.size > 1) {
                    return Command["mv"].help
                }
                val pathlist = VirtualFileSystem.getPath(destLocation)
                val rn = pathlist.removeAt(pathlist.lastIndex)
                val newpath = VirtualFileSystem.makePath(pathlist)
                var d = t.vfs.getObjectFromPath(newpath)
                val source = sourceFiles.first()
                if (d == null && pathlist.size > 0) {
                    return "mv: rename $source to $destLocation: No such file or directory\n"
                }
                if (d == null) {
                    d = t.vfs.currentLocation
                }
                val f = t.vfs.getObjectFromPath(source) ?: return "mv: rename $source to $destLocation: No such file or directory\n"
                f.parent.removeChild(f.label)
                f.label = rn
                d.addChild(f)
                f.parent = d
                return ""
            }
            for (source in sourceFiles) {
                val f = t.vfs.getObjectFromPath(source)
                if (f == null) {
                    result += "mv: rename $source to $destLocation: No such file or directory\n"
                    continue
                }
                f.parent.removeChild(f.label)
                d.addChild(f)
                f.parent = d
            }
            return result
        },
        tab = ::fileTabComplete,
        help = """Moves a file/folder to a new location.
            |Usage: mv [source] [target]
            |       mv [source] ... [directory]
        """.trimMargin()
)