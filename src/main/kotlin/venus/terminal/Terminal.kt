package venus.terminal

import venus.vfs.VFSDummy
import venus.vfs.VFSType
import venusbackend.assembler.Lexer
import venus.vfs.VirtualFileSystem

class Terminal(var vfs: VirtualFileSystem) {

    @JsName("processInput") fun processInput(input: String): String {
        try {
            val args = this.extractArgs(input)
            var sudo = if (args[0].toLowerCase() === "sudo") {
                args.removeAt(0)
                true
            } else {
                false
            }
            val prog = if (args.size > 0) {
                args[0]
            } else {
                ""
            }
            try {
                val cmd = Command[args.removeAt(0)]
                return cmd.execute(args, this, sudo)
            } catch (e: CommandNotFoundError) {
                val obj = vfs.getObjectFromPath(prog) ?: VFSDummy()
                if (obj.type in listOf(VFSType.File, VFSType.LinkedProgram, VFSType.Program)) {
                    val r = Command[venus.terminal.cmds.run.name]
                    args.add(0, prog)
                    return r.execute(args, this, sudo)
                }
                return e.message ?: ": command not found"
            }
        } catch (e: Throwable) {
            console.error(e)
            return "Unknown error occurred: " + e.toString()
        }
    }

    fun extractArgs(input: String): ArrayList<String> {
        return Lexer.lex(input) as ArrayList<String>
    }

    @JsName("getCommands") fun getCommands() {
        js("var cmds = []")
        var ktcmds = Command.getCommands()
        for (c in ktcmds) {
            js("cmds.push(c)")
        }
        return js("cmds")
    }

    private val externalCommands = listOf("clear", "clock", "date", "exit", "help", "uname", "sudo")

    @JsName("tab") fun tab(lineinput: String): Any? {
        val ktcmds = Command.getCommands().union(externalCommands)
        val args = this.extractArgs(lineinput)
        try {
            if (args.isNotEmpty()) {
                val sudo = if (args[0] === "sudo") {
                    args.removeAt(0)
                    true
                } else {
                    false
                }
                if (args.isEmpty()) {
                    return js("[];")
                }
                if (args.size == 1) {
                    val possibleCommands = ArrayList<String>()
                    val prefix = args[0]
                    for (c in ktcmds) {
                        if (c.startsWith(prefix)) {
                            possibleCommands.add(c)
                        }
                    }
                    if (possibleCommands.isEmpty()) {
                        val posFiles = Command.fileTabComplete(args, t = this, sudo = sudo)[1]!! as ArrayList<*>
                        val pfix = VirtualFileSystem.getPath(prefix).lastOrNull() ?: prefix
                        return listTojsList(listOf(pfix, listTojsList(posFiles)))
                    }
                    return listTojsList(listOf(prefix, listTojsList(possibleCommands)))
                } else {
                    val cmd = Command[args.removeAt(0)]
                    val options = cmd.tab(args, this, sudo)
                    return listTojsList(listOf(options[0], listTojsList(options[1] as List<Any?>)))
                }
            }
        } catch (e: Throwable) {
            console.error(e)
            return "An error occurred! $e"
        }
        return "Something bad happened!"
    }

    fun listTojsList(l: List<Any?>): Any? {
        js("var list = [];")
        for (i in l) {
            js("list.push(i);")
        }
        return js("list;")
    }
}