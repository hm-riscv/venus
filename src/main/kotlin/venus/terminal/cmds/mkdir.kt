package venus.terminal.cmds

import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal

var mkdir = Command(
        name = "mkdir",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            if (args.size < 1) {
                return "mkdir: mkdir takes in a folder name."
            }
            val result = t.vfs.mkdir(args[0])
            if (result == "") {
                t.vfs.save()
            }
            return result
        },
        tab = ::fileTabComplete,
        help = """This command makes a folder in the current directory or path.
            |Usage: mkdir [new folder name]
        """.trimMargin()
)