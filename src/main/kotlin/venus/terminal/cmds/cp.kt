package venus.terminal.cmds

import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal
import venus.vfs.VFSFile

var cp = Command(
        name = "cp",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            if (args.size < 1) {
                return Command["cp"].help
            }
            var result = ""
            var f = t.vfs.getObjectFromPath(args[0]) ?: return "cp: could not find the source file!"
            var d = t.vfs.getObjectFromPath(args[1]) ?: return "cp: could not find the destination folder!"
            if (f is VFSFile) {
                val text = f.readText()
                val new_f = VFSFile(f.label, d)
                new_f.permissions = f.permissions
                d.addChild(new_f, text)
            } else {
                result = "cp: Copy currently only works on files!"
            }
            return result
        },
        tab = ::fileTabComplete,
        help = """Copies a text/data file to a new location.
            |Usage: cp [src] [dst]
            |NOTE: This is a very dumb copy. It does not work on folders yet or many files!
        """.trimMargin()
)