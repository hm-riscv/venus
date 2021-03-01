package venus.terminal.cmds

import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal
import venus.vfs.VFSLinkedProgram
import venus.vfs.VFSObject
import venus.vfs.VFSProgram
import venus.vfs.VFSType
import venusbackend.linker.LinkedProgram
import venusbackend.linker.Linker
import venusbackend.linker.ProgramAndLibraries
import venusbackend.riscv.Program

var link = Command(
        name = "link",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            if (args.size == 0) {
                return "link: Takes in names of programs which you want to link together. EG link out.l a.out b.out c.out"
            }
            if (args.size == 1) {
                return "link: Takes in a minimum of two args link [output] [input], ..."
            }
            val progs = ArrayList<Program>()
            val output = args.first()
            args.removeAt(0)
            for (program in args) {
                val obj = t.vfs.getObjectFromPath(program)
                if (obj === null) {
                    return "link: Could not find file at path to $program"
                }
                if (obj.type != VFSType.Program) {
                    return "link: The inputs must be programs! ($program)"
                }
                progs.add((obj as VFSProgram).getProgram())
            }
            val PandL = try {
                ProgramAndLibraries(progs, t.vfs)
            } catch (e: AssertionError) {
                return "link: An error occurred when getting the imports: $e"
            }
            val linkedProgram: LinkedProgram
            try {
                linkedProgram = Linker.link(PandL)
            } catch (e: Throwable) {
                return "link: An error occurred when running the linked program: $e"
            }
            if (!VFSObject.isValidName(output)) {
                return "link: The name of the output file is not valid! ($output)"
            }
            val obj = VFSLinkedProgram(output, t.vfs.currentLocation, linkedProgram)
            return if (t.vfs.currentLocation.addChild(obj)) "" else "link: Could not add linked program to the files!"
        },
        tab = ::fileTabComplete,
        help = "This command takes in names of programs which you want to link together." +
                "It only requires one program but takes in an arbitrary number of programs." +
                "\nEG link out.l a.out b.out c.out" +
                "\nUsage: link [output] [input program 1] {[input program 2] ... [input program n]}"
)