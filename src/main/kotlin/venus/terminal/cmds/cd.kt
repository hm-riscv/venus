package venus.terminal.cmds

import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal

var cd = Command(
        name = "cd",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            return t.vfs.cd(args.joinToString(" "))
        },
        tab = ::fileTabComplete,
        help = "cd takes in one argument (a path) and goes to the directory." +
                "\nUsage: cd [path]"
)