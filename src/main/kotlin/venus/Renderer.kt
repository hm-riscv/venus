package venus
/* ktlint-disable no-wildcard-imports */

import org.w3c.dom.*
import venus.vfs.*
import venusbackend.assembler.AssemblerError
import venusbackend.linker.LinkedProgram
import venusbackend.riscv.*
//import venusbackend.riscv.insts.dsl.types.Instruction
//import venusbackend.riscv.insts.floating.Decimal
import venusbackend.simulator.Diff
import venusbackend.simulator.Simulator
//import venusbackend.simulator.SimulatorError
//import venusbackend.simulator.cache.BlockState
import venusbackend.simulator.cache.ChangedBlockState
//import venusbackend.simulator.diffs.*
//import kotlin.browser.document
//import kotlin.browser.window
//import kotlin.dom.addClass
import kotlin.dom.removeClass

/* ktlint-enable no-wildcard-imports */

/**
 * This singleton is used to render different parts of the screen, it serves as an interface between the UI and the
 * internal simulator.
 *
 * @todo break this up into multiple objects
 */
internal object Renderer : IRenderer {
    override val MEMORY_CONTEXT = 6
    override var pkgmsgTimeout: Int? = 0
    override val hexMap: List<Char> = listOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
    'A', 'B', 'C', 'D', 'E', 'F')
    override var activeRegister: HTMLElement? = null
    override var activeInstruction: HTMLElement? = null
    override var activeMemoryAddress: Int = 0
    override var sim: Simulator = Simulator(LinkedProgram(), VirtualFileSystem("dummy"))
    override var displayType = "hex"
    override var mainTabs: ArrayList<String> = arrayListOf("simulator", "editor", "venus")
}
