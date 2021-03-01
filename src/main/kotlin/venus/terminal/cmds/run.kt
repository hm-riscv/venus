package venus.terminal.cmds

import venusbackend.assembler.Assembler
import venus.Driver
import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal
import venus.vfs.VFSFile
import venus.vfs.VFSLinkedProgram
import venus.vfs.VFSProgram
import venus.vfs.VFSType
import venusbackend.assembler.AssemblerError
import venusbackend.linker.LinkedProgram
import venusbackend.linker.Linker
import venusbackend.linker.ProgramAndLibraries
import venusbackend.riscv.Program
import venusbackend.simulator.Simulator

var run = Command(
        name = "run",
        // @TODO Fix how will intemperate files vs args.
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            if (args.size == 0) {
                return "run: Takes in names of programs which you want to link together and run."
            }
            val file = args.removeAt(0)
            val linkedprogs = ArrayList<LinkedProgram>()
            val progs = ArrayList<Program>()
            val files = ArrayList<VFSFile>()
//            for (file in fs) {
                val obj = t.vfs.getObjectFromPath(file)
                if (obj == null) {
                    return "run: Could not find file $file"
                } else {
                    when (obj.type) {
                        VFSType.File -> {
                            files.add(obj as VFSFile)
                        }
                        VFSType.Program -> {
                            progs.add((obj as VFSProgram).getProgram())
                        }
                        VFSType.LinkedProgram -> {
                            if (files.size + progs.size + linkedprogs.size > 0) {
                                return "run: You must either have no linked programs or just one linked program!"
                            }
                            linkedprogs.add((obj as VFSLinkedProgram).getLinkedProgram())
                        }
                        else -> {
                            return "run: Unsupported type: ${obj.type}"
                        }
                    }
                }
//            }
            if (files.size + progs.size + linkedprogs.size == 0) {
                return "run: Could not find any of the inputted files!"
            }

            // Assembly stage for any files
            var msg = ""
            for (file in files) {
                val (prog, errors, warnings) = Assembler.assemble(file.readText(), file.label, file.getPath())
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
                progs.add(prog)
            }

            // Linking Stage for any programs
            if (progs.size > 0) {
                val PandL = try {
                    ProgramAndLibraries(progs, t.vfs)
                } catch (e: AssemblerError) {
                    return "run: An error occurred when getting the imports: $e"
                }
                val linkedProgram: LinkedProgram
                try {
                    linkedProgram = Linker.link(PandL)
                } catch (e: Throwable) {
                    return "run: An error occurred when running the linked program: $e"
                }
                linkedprogs.add(linkedProgram)
            }

            // Getting the LinkedProgram which we want to simulate
            if (linkedprogs.size != 1) {
                return msg + "run: There must only be one linked program!"
            }
            val linkedProgram = linkedprogs[0]
            val sim: Simulator
            try {
                Driver.loadSim(linkedProgram)
                Driver.sim.addArg(args)
                Driver.runStart(false)
            } catch (e: Throwable) {
                return "run: An error occurred when running the programs execution: $e"
            }
            return "VDIRECTIVE:RUNNING..."
        },
        tab = ::fileTabComplete,
        help = """Runs the inputed linked program with given arguments.
            |Usage: run [program name] [argument]...
        """.trimMargin()
)