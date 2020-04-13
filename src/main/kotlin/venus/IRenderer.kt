package venus
/* ktlint-disable no-wildcard-imports */

import org.w3c.dom.*
import venus.vfs.*
import venusbackend.assembler.AssemblerError
import venusbackend.linker.LinkedProgram
import venusbackend.riscv.*
import venusbackend.riscv.insts.dsl.types.Instruction
import venusbackend.riscv.insts.floating.Decimal
import venusbackend.simulator.Diff
import venusbackend.simulator.Simulator
import venusbackend.simulator.SimulatorError
import venusbackend.simulator.cache.BlockState
import venusbackend.simulator.cache.ChangedBlockState
import venusbackend.simulator.diffs.*
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
interface IRenderer {
    // TODO: maybe need to declare in Renderer as const
    val MEMORY_CONTEXT: Int
    // TODO: Can I delete this?
    var pkgmsgTimeout: Int?
    /** a map from integers to the corresponding hex digits */
    val hexMap: List<Char>
    /** The register currently being highlighted */
    var activeRegister: HTMLElement?
    /** The instruction currently being highlighted */
    var activeInstruction: HTMLElement?
    /** The memory location currently centered */
    var activeMemoryAddress: Int
    /** The simulator being rendered */
    var sim: Simulator
    /* The way the information in the registers is displayed*/
    var displayType: String

    var mainTabs: ArrayList<String>

    @JsName("renderTab") fun renderTab(tab: String, tabs: List<String>) {
    }

    @JsName("addTab") fun addTab(tabName: String, tabList: ArrayList<String>): Boolean {
        return true;
    }

    @JsName("removeTab") fun removeTab(tabName: String, tabList: ArrayList<String>): Boolean  {
        return true;
    }

    
    /**
     * Shows the simulator tab and hides other tabs
     *
     * @param displaySim the simulator to show
     */
    fun renderSimulator(){
        
    }

    fun loadSimulator(displaySim: Simulator) {
        sim = displaySim
        setRunButtonSpinning(false)
        renderProgramListing()
        clearConsole()
        updateAll()
        renderSimButtons()
    }

    fun renderSimButtons() {
    }

    fun renderAssembleButtons() {
    }

    /** Shows the editor tab and hides other tabs */
    fun renderEditor() {
    }

    fun renderVenus() {
    }

    fun renderURLMaker() {
    }

    /**
     * Sets the tab to the desired visiblity.
     *
     * Also updates the highlighted tab at the top.
     *
     * @param tab the name of the tab (currently "editor" or "simulator")
     */
    @JsName("tabSetVisibility") private fun tabSetVisibility(tab: String, display: String) {
        
    }

    fun displayWarning(w: String) {
        
    }

    /** Display a given ERROR */
    fun displayError(thing: Any) {
        
    }

    fun stdout(thing: Any) {
        
    }

    fun stderr(thing: Any) {
        
    }

    /** Display a given [AssemblerError] */
    @Suppress("UNUSED_PARAMETER") fun displayAssemblerError(e: AssemblerError) {
//        if (e.line !== null) {
//            js("alert('[ERROR]\\n(Line: ' + e.line + ') ' + e.message)")
//        } else {
            js("alert('[ERROR]\\n' + e.message)")
//        }
    }

    /**
     * Renders the program listing under the debugger
     */
    private fun renderProgramListing() {
        
    }

    /**
     * Refresh all of the simulator tab's content
     *
     * @todo refactor this into a "reset" and "update" all function
     */
    fun updateAll() {
        
    }

    /**
     * Updates the view by applying each individual diff.
     *
     * @param diffs the list of diffs to apply
     */
    fun updateFromDiffs(diffs: List<Diff>) {
        
    }

    /**
     * Clears the current program listing.
     *
     * @todo find a less hacky way to do this?
     */
    fun clearProgramListing() {    
    }

    /**
     * Adds an instruction with the given index to the program listing.
     *
     * @param idx the pc of the instruction
     * @param mcode the machine code representation of the instruction
     * @param progLine the original assembly code
     */
    fun addToProgramListing(pcx: Int, mcode: MachineCode, progLine: String, invalidInst: Boolean = false) {
        
    }

    fun updateProgramListing(idx: Number, inst: Int, orig: String? = null): InstructionDiff {
        return InstructionDiff(0, 0, "")
    }

    /**
     * Gets the element with a given ID
     *
     * @param id the id of the desired element
     *
     * @returns the HTML element corresponding to the given ID
     * @throws ClassCastException if the element is not an [HTMLElement] or does not exist
     */
    fun getElement(id: String): HTMLElement {
        return HTMLDivElement as HTMLElement
    }

    /**
     * Updates the register with the given id and value.
     *
     * @param id the ID of the register (e.g., x13 has ID 13)
     * @param value the new value of the register
     * @param setActive whether the register should be set to the active register (i.e., highlighted for the user)
     */
    fun updateRegister(id: Int, value: Number, setActive: Boolean = false) {
    }

    /**
     * Updates the register with the given id and value.
     *
     * @param id the ID of the floating register (e.g., f13 has ID 13)
     * @param value the new value of the register
     * @param setActive whether the register should be set to the active register (i.e., highlighted for the user)
     */
    fun updateFRegister(id: Int, v: Decimal, setActive: Boolean = false) {
    }

    fun intToString(value: Int): String {
        var v = when (displayType) {
            "Hex" -> toHex(value)
            "Decimal" -> value.toString()
            "Unsigned" -> toUnsigned(value)
            "ASCII" -> toAscii(value)
            else -> toHex(value)
        }
        return v
    }

    /*@TODO make it so I can detect between if I am continuing or not so I do not have to be too wasteful.*/
    fun updateCache(a: Address) {
    }

    fun renderSetCacheLevel(i: Int) {
    }

    fun renderAddCacheLevel() {
    }

    fun renderRemoveCacheLevel() {
    }

    fun makeCacheBlocks() {
    }

    //TODO: Can I get rid of optional parameter
    fun updateCacheBlocks(b: ChangedBlockState = ChangedBlockState(0, BlockState.MISS)) {
        
    }

    fun updateAllCacheBlocks() {
    }

    /**
     * Updates the PC to the given value. It also highlights the to-be-executed instruction.
     *
     * @param pc the new PC
     * @todo abstract away instruction length
     */
    fun updatePC(pc: Number) {
    }

    /**
     * Prints the given thing to the console as a string.
     *
     * @param thing the thing to print
     */
    fun printConsole(thing: Any) {
    }

    /**
     * Clears the console
     */
    fun clearConsole() {
    }

    /**
     * Sets whether the run button is spinning.
     *
     * @param spinning whether the button should be spin
     */
    fun setRunButtonSpinning(spinning: Boolean) {
    }

    /**
     * Sets whether the name button is spinning.
     *
     * @param spinning whether the button should be spin
     */
    fun setNameButtonSpinning(name: String, spinning: Boolean) {
    }
    /**
     * Sets whether a button is disabled.
     *
     * @param id the id of the button to change
     * @param disabled whether or not to disable the button
     */
    private fun setButtonDisabled(id: String, disabled: Boolean) {
    }

    /**
     * Renders the control buttons to be enabled / disabled appropriately.
     */
    fun updateControlButtons() {
    }

    /**
     * Disables the step, undo and reset buttons.
     *
     * Used while running, see [Driver.runStart].
     */
    fun disableControlButtons() {
    }

    /**
     * Renders a change in breakpoint status
     *
     * @param idx the index to render
     * @param state whether or not there is a breakpoint
     */
    fun renderBreakpointAt(idx: Int, state: Boolean) {
    }

    /**
     * Number of rows to show around the current address
     */
   

    /** Show the memory sidebar tab */
    fun renderMemoryTab() {
    }

    /** Show the register sidebar tab */
    fun renderRegisterTab() {
    }

    fun renderCacheTab() {
    }

    fun renderSettingsTab() {
    }

    fun renderGeneralSettingsTab() {
    }

    /**
     * Show the tracer settings tab
     */
    fun renderTracerSettingsTab() {
    }

    fun renderPackagesTab() {
    }

    fun renderRegsTab() {
    }

    fun renderFRegsTab() {
    }

    fun rendererAddPackage(pid: String, enabled: Boolean, removable: Boolean = true) {
    }

    fun rendererRemovePackage(pid: String) {
    }

    fun rendererUpdatePackage(pid: String, state: Boolean) {
    }

    fun pkgMsg(m: String) {
    }

    fun clearPkgMsg() {
    }

    /**
     * Update the [MEMORY_CONTEXT] words above and below the given address.
     *
     * Does not shift the memory display if it can be avoided
     *
     * @param addr the address to update around
     */
    fun updateMemory(addr: Int) {
    }

    /**
     * Determines if we need to move the memory display to show the address
     *
     * @param wordAddress the address we want to show
     * @return true if we need to move the display
     */
    private fun mustMoveMemoryDisplay(wordAddress: Int) = true

    /**
     * Renders a row of the memory.
     *
     * @param row the HTML element of the row to render
     * @param rowAddr the new address of that row
     */
    private fun renderMemoryRow(urow: HTMLElement, rowAddr: Int) {
    }

    private fun cleanTableRow(row: HTMLElement): HTMLElement {
        return row
    }

    

    /**
     * Convert a certain byte to hex
     *
     * @param b the byte to convert
     * @return a hex string for the byte
     *
     * @throws IndexOutOfBoundsException if b is not in -127..255
     */
    private fun byteToHex(b: Int): String {
        val leftNibble = hexMap[b ushr 4]
        val rightNibble = hexMap[b and 15]
        return "$leftNibble$rightNibble"
    }

    private fun byteToDec(b: Int): String = b.toByte().toString()

    private fun byteToUnsign(b: Int): String = b.toString()

    /**
     * Converts a value to a two's complement hex number.
     *
     * By two's complement, I mean that -1 becomes 0xFFFFFFFF not -0x1.
     *
     * @param value the value to convert
     * @return the hexadecimal string corresponding to that value
     * @todo move this?
     */
    fun toHex(value: Int, num_nibbles: Int = 8, add_prefix: Boolean = true): String {
        var remainder = value.toLong()
        var suffix = ""

        repeat(num_nibbles) {
            val hexDigit = hexMap[(remainder and 15).toInt()]
            suffix = hexDigit + suffix
            remainder = remainder ushr 4
        }

        if (add_prefix) {
            suffix = "0x" + suffix
        }

        return suffix
    }

    fun toHex(value: Number): String {
        return toHex(value.toInt())
    }

    private fun toUnsigned(value: Int): String =
            if (value >= 0) value.toString() else (value + 0x1_0000_0000L).toString()

    private fun toAscii(value: Int, num_nibbles: Int = 8): String {
        var s = ""
//        for (i in 0..3) {
//            val v = (value shr i * 8) and 0xFF
            val v = value
            s += when (v) {
                !in 0..255 -> toHex(v, num_nibbles = num_nibbles)
//                !in 32..126 -> "\uFFFD"
                !in 32..126 -> toHex(v, num_nibbles = num_nibbles)
                else -> "${v.toChar()}"
            }
//        }
        return s
    }

    /**
     * Sets the display type for all of the registers and memory
     * Rerenders after
     */
    fun updateRegMemDisplay() {
    }

    fun moveMemoryJump() {
    }

    private fun moveMemoryBy(rows: Int) {
    }

    fun moveMemoryUp() = moveMemoryBy(MEMORY_CONTEXT)
    fun moveMemoryDown() = moveMemoryBy(-MEMORY_CONTEXT)

    fun updateText() {
    }

    fun renderButton(e: HTMLButtonElement, b: Boolean) {
    }

    fun addObjectToDisplay(obj: VFSObject, special: String = "") {
    }

    fun addFilePWD(obj: VFSObject) {
    }

    fun clearObjectsFromDisplay() {
    }
}
