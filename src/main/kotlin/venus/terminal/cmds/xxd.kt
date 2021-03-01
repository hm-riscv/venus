package venus.terminal.cmds

import venus.Renderer
import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal
import venus.vfs.VFSFile

@ExperimentalStdlibApi
var xxd = Command(
        name = "xxd",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            if (args.size < 1) {
                return Command["xxd"].help
            }
            var result = ""
            val charchunk = 2
            val charsperline = 16
            var f = t.vfs.getObjectFromPath(args[0]) ?: return "xxd: could not find the file!"
            if (f is VFSFile) {
                val text = f.readText()
                var i = 0
                var curline = ""
                if (text.length > 0) {
                    result += Renderer.toHex(0, add_prefix = false) + ":"
                }
                for (chr: Char in text.toCharArray()) {
                    if (curline.length == charsperline) {
                        result += "  $curline\n"
                        result += Renderer.toHex(i, add_prefix = false) + ":"
                        curline = ""
                    }
                    if (curline.length % charchunk == 0) {
                        result += " "
                    }
                    val cv = chr.toInt()
                    result += Renderer.toHex(cv, 2, false)
                    curline += when (cv) {
                        !in 32..126 -> "."
                        else -> chr.toString()
                    }
                    i++
                }
                if (curline.length > 0) {
                    var charsLeft = charsperline - curline.length
                    if (charsLeft % 2 == 1) {
                        charsLeft--
                        result += "  "
                    }
                    charsLeft /= 2
                    for (j in 0 until charsLeft) {
                        result += "     "
                    }
                    result += "  $curline"
                }
            } else {
                result = "xxd: only works on files!"
            }
            return result
        },
        tab = ::fileTabComplete,
        help = """Takes a file and prints it out in hex.
            |Usage: xxd file
            |NOTE: This is a very dumb xxd and does not have full features.
        """.trimMargin()
)