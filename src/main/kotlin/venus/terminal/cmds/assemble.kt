package venus.terminal.cmds

import venusbackend.assembler.Assembler
import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal
import venus.vfs.VFSFile
import venus.vfs.VFSObject
import venus.vfs.VFSProgram
import venus.vfs.VFSType

var assemble = Command(
        name = "assemble",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            if (args.size !in listOf(1, 2)) {
                return Command["assemble"].help
            }
            val filein = args[0]
            val programout = if (args.size == 2) args[1] else "a.out"
            val file = t.vfs.getObjectFromPath(filein)
            if (file === null) {
                return "venusbackend.assembler: Could not find file $filein"
            }
            if (file.type != VFSType.File) {
                return "assemble: $filein: Is a directory"
            }
//            if (!file.contents.containsKey(VFSFile.innerTxt)) {
//                return "assemble: $filein: COULD NOT FIND FILE CONTENTS!"
//            }
            if (!VFSObject.isValidName(programout)) {
                return "assemble: $programout: Invalid name"
            }
            var msg = ""
            val (prog, errors, warnings) = Assembler.assemble((file as VFSFile).readText(), programout, file.getPath())
            if (errors.isNotEmpty()) {
                msg += "assemble: Could not assemble file! Here are the errors:"
                for (error in errors) {
                    msg += "\n" + error.toString()
                }
                return msg
            }
            if (warnings.isNotEmpty()) {
                msg += "assemble: Assembled file with a few warnings:"
                for (warning in warnings) {
                    msg += "\n" + warning.toString()
                }
            }
            val p = VFSProgram(programout, file.parent, prog)
            file.parent.addChild(p)
            return msg
        },
        tab = ::fileTabComplete,
        help = """assemble: takes in two arguments: [text in] {[program out], a.out}
            |Returns a.out if no second argument exists""".trimMargin()
)