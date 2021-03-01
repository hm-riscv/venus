package venus.terminal.cmds

import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal

var rm = Command(
        name = "rm",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            var output = ""
            var fails = 0
            var attempts = 0
            for (arg in args) {
                attempts++
                val out = t.vfs.remove(arg)
                if (out != "") {
                    output += out + "\n"
                    fails++
                }
            }
            if (fails < attempts) {
                t.vfs.save()
            }
            return output
        },
        tab = ::fileTabComplete,
        help = """Remove (unlink) the FILE(s),
            |Usage: rm [OPTION]... [FILE]...
            |NOTE: Options and multiple file input is currently not implemented yet.
        """.trimMargin()
)