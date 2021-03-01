package venus.terminal

import venus.vfs.VFSObject
import venus.vfs.VirtualFileSystem

open class Command(
    val name: String,
    val execute: (parsedInput: MutableList<String>, t: Terminal, sudo: Boolean) -> String =
            { a, b, c -> throw NotImplementedError() },
    val tab: (parsedInput: MutableList<String>, t: Terminal, sudo: Boolean) -> MutableList<Any> =
            { a, b, c -> throw NotImplementedError() },
    val help: String = "Command does not have a help yet!"
) {
    companion object {
        private val allCommands = arrayListOf<Command>()

        fun getCommands(): ArrayList<String> {
            val cmds = ArrayList<String>()
            for (cmd in allCommands) {
                cmds.add(cmd.name)
            }
            return cmds
        }

        operator fun get(name: String) =
                allCommands.firstOrNull { it.name == name }
                        ?: throw CommandNotFoundError(name)

        @OptIn(ExperimentalStdlibApi::class)
        fun fileTabComplete(args: MutableList<String>, t: Terminal, sudo: Boolean): ArrayList<Any> {
            if (args.size >= 1) {
                val pfix = args[args.size - 1]
                val path = VirtualFileSystem.getPath(pfix)
                val prefix = try {
                    path.removeLast()
                } catch (e: NoSuchElementException) {
                    pfix
                }
                var paths = path.joinToString(separator = VFSObject.separator)
                if (pfix.startsWith("/")) {
                    paths = "/$paths"
                }
                return arrayListOf(prefix, t.vfs.filesFromPrefix(paths, prefix))
            }
            return arrayListOf("", ArrayList<String>())
        }
    }

    init {
        allCommands.add(this)
    }

    override fun toString() = name
}