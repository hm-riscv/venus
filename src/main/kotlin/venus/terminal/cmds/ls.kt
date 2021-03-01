package venus.terminal.cmds

import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal

var ls = Command(
        name = "ls",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            val path = if (args.size > 0) {
                args[0]
            } else {
                null
            }
            return t.vfs.ls(path)
        },
        tab = ::fileTabComplete,
        help = "This command prints out the contents of the current folder." +
                "\nIt currently does not take in any arguments." +
                "\nUsage: ls"
)