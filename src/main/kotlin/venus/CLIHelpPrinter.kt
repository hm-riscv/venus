package venus

import venusbackend.cli.HelpPrinter

class CLIHelpPrinter(private val syntaxWidth: Int) : HelpPrinter {
    var str: StringBuilder = StringBuilder()
    override fun printText(text: String) {
        str.append(text)
        str.append("\n")
    }

    override fun printSeparator() {
        str.append("\n")
    }

    override fun printEntry(helpEntry: String, description: String) {
        if (helpEntry.length <= syntaxWidth) {
            str.append("  ${helpEntry.padEnd(syntaxWidth)}  $description\n")
        } else {
            str.append("  $helpEntry\n")
            str.append("  ${"".padEnd(syntaxWidth)}  $description\n")
        }
    }

    fun get_string(): String {
        return str.toString()
    }
}