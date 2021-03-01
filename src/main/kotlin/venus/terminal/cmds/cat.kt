package venus.terminal.cmds

import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal

var cat = Command(
        name = "cat",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            return t.vfs.cat(args.joinToString(" "))
        },
        tab = ::fileTabComplete,
        help = "cat: takes in one argument (a file or path to a file) and prints out the contents of the file." +
                "\nEx cat foo.txt" +
                "\nUsage: cat [path to file]"
)