package venus

/* ktlint-disable no-wildcard-imports */

import venus.api.venuspackage
import venus.terminal.Terminal
import venus.terminal.cmds.vdb
import venus.vfs.*
import venusbackend.assembler.*
import venusbackend.linker.LinkedProgram
import venusbackend.linker.Linker
import venusbackend.linker.ProgramAndLibraries
import venusbackend.numbers.QuadWord
import venusbackend.plus
import venusbackend.riscv.*
import venusbackend.riscv.insts.dsl.types.Instruction
import venusbackend.riscv.insts.floating.Decimal
import venusbackend.simulator.*
import venusbackend.simulator.plugins.*
import venusbackend.simulator.Tracer.Companion.wordAddressed
import venusbackend.simulator.cache.BlockReplacementPolicy
import venusbackend.simulator.cache.CacheError
import venusbackend.simulator.cache.CacheHandler
import venusbackend.simulator.cache.PlacementPolicy

// using these imports only for type checks and fake DOM
import kotlin.dom.addClass
import kotlin.dom.removeClass
import kotlin.dom.hasClass
import org.w3c.dom.*
import kotlin.js.JSON
import kotlin.js.Json
import kotlin.js.json

external class Timeout
external fun setInterval(
    callback: dynamic,
    delay: Int = definedExternally,
    vararg args: Any?
): Timeout
external fun setTimeout(
    callback: dynamic,
    delay: Int = definedExternally,
    vararg args: Any?
): Timeout
external fun clearTimeout(handle: Timeout = definedExternally): Unit
external val document: Document

/* ktlint-enable no-wildcard-imports */

/**
 * The "driver" singleton which can be called from Javascript for all functionality.
 */
@JsName("Driver") object Driver {
    @JsName("VFS") var VFS = VirtualFileSystem("/")
    var active_abs_file_name: String? = null
    var active_abs_file_path: String? = null

    fun set_active_afpath(path: String?) {
        active_abs_file_path = path
        document.getElementById("activeFileInEditor")!!.innerHTML = path.toString()
        activeFileinEditor = path ?: ""
    }

    val mainCache: CacheHandler = CacheHandler(1)
    var cache: CacheHandler = mainCache
    var cacheLevels: ArrayList<CacheHandler> = arrayListOf(mainCache)
    @JsName("simSettings") val simSettings = SimulatorSettings()
    var sim: Simulator = Simulator(LinkedProgram(), VFS, settings = simSettings)
    val simState64 = SimulatorState64()
    val temp = QuadWord()

    var tr: Tracer = Tracer(sim)

    private var timer: Timeout? = null
    val LS = LocalStorage()
    var useLS = false
    private var saveInterval: Timeout? = null
    var p = ""
    private var ready = false
    @JsName("FReginputAsFloat") var FReginputAsFloat = true
    @JsName("ScriptManager") var ScriptManager = venus.api.ScriptManager
    @JsName("debug") var debug = false

    @JsName("terminal") var terminal = Terminal(VFS)

    @JsName("activeFileinEditor") var activeFileinEditor: String = ""

    @JsName("driver_complete_loading") var driver_complete_loading: Boolean = false

    var doCallingConventionCheck: Boolean = false

    private var eCallReceiver: ((String)->(String))? = null


    init {
        /* This code right here is so that you can add custom kotlin code even after venus has been loaded! */
        js("window.eval_in_venus_env = function (s) {return eval(s);}")
        js("load_update_message(\"Initializing Venus: Init\");")
        simState64.getReg(0)
        Linter.lint("")
        console.log("Loading driver...")
        mainCache.attach(false)

        useLS = LS.get("venus") == "true"
        Renderer.renderButton(document.getElementById("sv") as HTMLButtonElement, useLS)

        setTimeout(Driver::initTimeout, 5)

        console.log("Finished loading driver!")
    }

    fun initTimeout() {
        js("load_update_message(\"Initializing Venus: Local Storage\");")
        loadAll(useLS)
        js("load_update_message(\"Initializing Venus: Renderer\");")
        Renderer.loadSimulator(sim)
        Renderer.renderAssembleButtons()
        saveInterval = setInterval(Driver::saveIntervalFn, 10000)
        Driver.ready = true
        initFinish()
    }

    fun initFinish() {
        if (Driver.ready) {
            if (!::fileExplorerCurrentLocation.isInitialized) {
                fileExplorerCurrentLocation = VFS.sentinel
                openVFObjectfromObj(VFS.sentinel)
            }
            js("""window.onbeforeunload = function(){
    driver.saveAll();
}""")
            js("window.driver_load_done();")
        } else {
            setInterval(Driver::initFinish, 100)
        }
    }

    @JsName("lint") fun lint(text: String): Array<LintError> = Linter.lint(text)

    /**
     * Run when the user clicks the "Simulator" tab.
     *
     * Assembles the text in the editor, and then renders the simulator.
     */
    @JsName("openSimulator") fun openSimulator() {
        openGenericMainTab("simulator")
    }

    @JsName("noAssemble") fun noAssemble() {
        Renderer.renderSimButtons()
    }

    fun getDefaultArgs(): String {
        return (document.getElementById("ArgsList") as HTMLInputElement).value
    }

    data class InstructionInfo(val pc: Int, val mcode: Int, val basicCode: String, val line: Int, val sourceFile: String)
    @JsName("getCurrentInstruction") fun getCurrentInstruction(): InstructionInfo{
//            val pcloc = (sim.getMaxPC().toInt() - MemorySegments.TEXT_BEGIN)
        val pcloc = sim.getPC().toInt()
        var mcode = MachineCode(0)
        try {
            mcode = sim.getNextInstruction()
            return InstructionInfo(pcloc, mcode[InstructionField.ENTIRE].toInt(), Instruction[mcode].disasm(mcode), 0, "");
        } catch (e: SimulatorError) {
            return InstructionInfo(0, 0, "error", 0, "error")
        }
    }

    @JsName("registerECallReceiver")
    fun registerECallReceiver(receiverFunction: (String) -> String): Boolean {
        sim.registerECallReceiver(::eCallHook)
        eCallReceiver = receiverFunction

        return true
    }

    fun eCallHook(id: Int): Boolean {
        val params = json(
            "a0" to id,
            "a1" to sim.getReg(11),
            "a2" to sim.getReg(12),
            "a3" to sim.getReg(13),
            "a4" to sim.getReg(14),
            "a5" to sim.getReg(15),
            "a6" to sim.getReg(16),
            "a7" to sim.getReg(17)
        )

        val result = eCallReceiver?.invoke(JSON.stringify(json("id" to id, "params" to params)))

        if (result != null) {
            val jsonobj: Json = JSON.parse(result)

            val a0: Any? = jsonobj.get("a0")
            val a1: Any? = jsonobj.get("a1")

            if (a0 is Int) sim.setReg(10, a0)
            if (a1 is Int) sim.setReg(11, a1)
            return true
        }
        return false
    }

    @JsName("getInstructions") fun getIntructions(): Array<InstructionInfo> {
        val instructions: MutableList<InstructionInfo> = mutableListOf()
        for (i in 0 until sim.linkedProgram.prog.insts.size) {
            val programDebug = sim.linkedProgram.dbg[i]
            val (_, dbg) = programDebug
            val (_, line) = dbg
            val lineNo = dbg.lineNo
            val mc = sim.linkedProgram.prog.insts[i]
            val pc = sim.instOrderMapping[i]!!
            val basicCode = Instruction[mc].disasm(mc)
            val mcode = mc[InstructionField.ENTIRE].toInt()
            instructions.add(InstructionInfo(pc, mcode, basicCode, lineNo, dbg.prog.absPath))
        }

        return instructions.toTypedArray()
    }

    @JsName("assembleSimulator") fun assembleSimulator() {
        var text = getText()
        if (text == "") {
            js("codeMirror.refresh();codeMirror.save();")
            text = getText()
        }
        if (ready) {
            try {
                val editorFileName = active_abs_file_name ?: "editor.S"
                val fpath = active_abs_file_path ?: VFS.currentLocation.getPath() + "/$editorFileName"
                var remove = false
                if (VFS.getObjectFromPath(fpath) == null) {
                    VFS.addFile(fpath, text)
                    remove = true
                }
                val success = assemble(text, name = editorFileName, absPath = fpath)
                if (success != null) {
                    if (link(listOf(success))) {
                        val args = Lexer.lex(getDefaultArgs())
                        for (arg in args) {
                            sim.addArg(arg)
                        }
                        Renderer.loadSimulator(sim)
                        setCacheSettings()
                        Renderer.updateCache(Address(0, MemSize.WORD))
                    }
                }
                if (remove) {
                    VFS.remove(fpath)
                }
            } catch (e: Throwable) {
                Renderer.loadSimulator(Simulator(LinkedProgram(), VFS))
                handleError("Open Simulator", e)
            }
        } else {
            setTimeout(Driver::openSimulator, 100)
        }
    }

    @JsName("getRegister") fun getRegister(id: Int): Number {
        return sim.getReg(id)
    }

    @JsName("getFRegister") fun getFRegister(id: Int): Decimal {
        return sim.getFReg(id)
    }

    @JsName("setRegister") fun setRegister(id: Int, value: Number) {
        if (!currentlyRunning()) {
            try {
                sim.setRegNoUndo(id, value)
            } catch (e: NumberFormatException) {
                /* do nothing */
            }
        }
    }

    @JsName("setFRegister") fun setFRegister(id: Int, value: Number) {
        if (!currentlyRunning()) {
            try {
                sim.setFRegNoUndo(id, Decimal(value.toFloat(), value.toDouble(), FReginputAsFloat))
            } catch (e: NumberFormatException) {
                /* do nothing */
            }
        }
    }

    /**
     * DEPRECATED
     * no operation
     * should be irrelevant
     * looks for some url-args
     */
    @JsName("checkURLParams") fun checkURLParams() {
        /*
        var clearparams = true
        val currentURL = URL(window.location.href)

        var s = currentURL.searchParams.get("code")
        if (s != null) {
            s = parseString(s)
            js("codeMirror.save();")
            if (getText() != "") {
                if (getText() != s) {
                    val override = currentURL.searchParams.get("override")
                    val overrideb = override != null && override.toLowerCase() == "true"
                    val choice = if (overrideb) { true } else { window.confirm("You have some saved code already in venus! Do you want to override it with the code in your url?") }
                    if (choice) {
                        js("codeMirror.setValue(s);")
                    } else {
                        clearparams = false
                    }
                }
            } else {
                js("codeMirror.setValue(s);")
            }
        }

        s = currentURL.searchParams.get("tab")
        if (s != null) {
            s = parseString(s.toString())
            if (s in Renderer.mainTabs) {
                Renderer.renderTab(s, Renderer.mainTabs)
            } else {
                console.log("Unknown Tag!")
            }
        }

        s = currentURL.searchParams.get("target")
        if (s != null) {
            s = parseString(s)
            js("loadfromtarget(s);")
        }

        s = currentURL.searchParams.get("save")
        if (jsTypeOf(s) != undefined) {
            s = parseString(s.toString())
            if (s.toLowerCase() == "true") {
                persistentStorage(true)
                Renderer.renderButton(document.getElementById("sv") as HTMLButtonElement, true)
            }

            if (s.toLowerCase() == "false") {
                persistentStorage(false)
                Renderer.renderButton(document.getElementById("sv") as HTMLButtonElement, false)
            }
        }

        try {
            s = currentURL.searchParams.get("packages")
            if (s != null) {
                s = parseString(s.toString())
                var l = s.split(Regex(","))
                for (pack in l) {
                    ScriptManager.addPackage(pack)
                }
            }
        } catch (e: Exception) {
            console.warn("An error occurred when parsing the packages!")
            console.warn(e)
        }

        if (clearparams) {
            clearURLParams()
        }
         */
    }

    /**
     * DEPRECATED
     * No Operation
     * see fun checkURLParams
     */
    fun clearURLParams() {
        /*
        val location = window.location.origin + window.location.pathname
        js("window.history.replaceState({}, document.title, location)")
        */
    }

    fun parseString(s: String): String {
        val ps = s.replace("\\n", "\n")
                .replace("\\t", "\t")
        return ps
    }

    fun unparseString(s: String): String {
        val ps = s.replace("\n", "\\n")
                .replace("\t", "\\t")
        return ps
    }

    /**
     * Opens and renders the editor.
     */
    @JsName("openEditor") fun openEditor() {
        runEnd()
        openGenericMainTab("editor")
        js("codeMirror.refresh();")
    }

    @JsName("openVenus") fun openVenus() {
        openGenericMainTab("venus")
    }

    @JsName("openGenericMainTab") fun openGenericMainTab(name: String) {
        Renderer.renderTab(name, Renderer.mainTabs)
        if (name == "editor") {
            Renderer.renderAssembleButtons()
        }
        LS.set("defaultTab", name)
    }

    @JsName("openURLMaker") fun openURLMaker() {
        js("setUpURL();")
        Renderer.renderURLMaker()
    }

    /**
     * Gets the text from the textarea editor.
     */
    @JsName("getText") internal fun getText(): String {
        val editor = kotlin.browser.document.getElementById("asm-editor") as HTMLTextAreaElement
        return editor.value
    }

    /**
     * Assembles and links the program, sets the simulator
     *
     * @param text the assembly code.
     */
    internal fun assemble(text: String, name: String = "", absPath: String): Program? {
        val (prog, errors, warnings) = if (name != "") {
            Assembler.assemble(text, name, abspath = absPath)
        } else {
            Assembler.assemble(text, abspath = absPath)
        }
        if (errors.isNotEmpty()) {
            Renderer.displayAssemblerError(errors.first())
            return null
        }
        return prog
    }

    internal fun link(progs: List<Program>): Boolean {
        try {
            val PandL = ProgramAndLibraries(progs, VFS)
            val linked = Linker.link(PandL)
            loadSim(linked)
            return true
        } catch (e: AssemblerError) {
            Renderer.displayAssemblerError(e)
            return false
        }
    }

    fun loadSim(linked: LinkedProgram) {
        sim = Simulator(linked, VFS, simSettings)
        mainCache.reset()
        sim.state.cache = mainCache
        tr = Tracer(sim)
        if (doCallingConventionCheck) {
            this.enableCallingConvention(true)
        }
    }

    fun getMaxSteps(): Int {
        return (document.getElementById("tmaxsteps-val") as HTMLInputElement).value.toInt()
    }

    @JsName("updateMaxSteps") fun updateMaxSteps() {
        runEnd()
        simSettings.maxSteps = getMaxSteps()
    }

    fun exitcodecheck() {
        if (sim.exitcode != null) {
            val msg = "Exited with error code ${sim.exitcode}"
            if (sim.exitcode ?: 0 == 0) {
                Renderer.stdout(msg)
            } else {
                Renderer.displayWarning(msg)
            }
        }
    }

    @JsName("externalAssemble") fun externalAssemble(text: String, absPath: String = "", fileName: String = "main.s"): Any {
        var success = true
        var errs = ""
        val (prog, errors, warnings) = Assembler.assemble(text, abspath = absPath, name = fileName)
        if (errors.isNotEmpty()) {
            errs = errors.first().toString()
            success = false
        } else {
            try {
                val PandL = ProgramAndLibraries(listOf(prog), VFS)
                val linked = Linker.link(PandL)
                sim = Simulator(linked, VFS, simSettings)
                val args = Lexer.lex(getDefaultArgs())
                for (arg in args) {
                    sim.addArg(arg)
                }
                setCacheSettings()
            } catch (e: AssemblerError) {
                errs = e.toString()
                success = false
            }
        }

        return js("[success, errs, warnings]")
    }

    /**
     * Runs the simulator until it is done, or until the run button is pressed again.
     */
    @JsName("run") fun run() {
        if (currentlyRunning()) {
            runEnd()
        } else {
            try {
                Renderer.setRunButtonSpinning(true)
                timer = setTimeout(Driver::runStart, TIMEOUT_TIME, true)
                sim.step() // walk past breakpoint
            } catch (e: Throwable) {
                runEnd()
                handleError("RunStart", e, e is AlignmentError || e is StoreError || e is ExceededAllowedCyclesError)
            }
        }
    }

    @JsName("continue") fun continueRun() {
        try {
            if (!sim.isDone()) {
                sim.step()
            }
            while (true) {
                if (sim.isDone() || (sim.atBreakpoint())) {
                    exitcodecheck()
                    return
                }

                sim.step()
            }
        } catch (e: Throwable) {
            runEnd()
            handleError("RunStart", e, e is AlignmentError || e is StoreError || e is ExceededAllowedCyclesError)
        }
    }

    /**
     * Resets the simulator to its initial state
     */
    @JsName("reset") fun reset() {
        try {
            val args = sim.args
            val plugins = sim.plugins
            sim = Simulator(sim.linkedProgram, VFS, sim.settings, simulatorID = sim.simulatorID)
            tr.sim = sim
            for (arg in args) {
                sim.addArg(arg)
            }
            for ((id, p) in plugins) {
                sim.registerPlugin(id, p)
            }
            mainCache.reset()
            sim.state.setCacheHandler(mainCache)
            Renderer.loadSimulator(sim)
            setCacheSettings()
            Renderer.updateCache(Address(0, MemSize.WORD))
        } catch (e: Throwable) {
            Renderer.loadSimulator(Simulator(LinkedProgram(), VFS))
            handleError("Reset Simulator", e)
        }
    }

    @JsName("toggleBreakpoint") fun addBreakpoint(idx: Int) {
        val isBreakpoint = sim.toggleBreakpointAt(idx)
        Renderer.renderBreakpointAt(idx, isBreakpoint)
    }

    internal const val TIMEOUT_CYCLES = 100
    internal const val TIMEOUT_TIME = 10
    internal fun runStart(useBreakPoints: Boolean) {
        try {
            var cycles = 0
            while (cycles < TIMEOUT_CYCLES) {
                if (sim.isDone() || (sim.atBreakpoint() && useBreakPoints)) {
                    exitcodecheck()
                    runEnd()
                    Renderer.updateAll()
                    return
                }

                handleNotExitOver()
                sim.step()
                Renderer.updateCache(Address(0, MemSize.WORD))
                cycles++
            }

            timer = setTimeout(Driver::runStart, TIMEOUT_TIME, useBreakPoints)
        } catch (e: Throwable) {
            runEnd()
            handleError("RunStart", e, e is AlignmentError || e is StoreError || e is ExceededAllowedCyclesError)
        }
    }

    @JsName("runEnd") fun runEnd() {
        handleNotExitOver()
        sim.finishPlugins()
        Renderer.updatePC(sim.getPC())
        Renderer.updateAll()
        Renderer.setRunButtonSpinning(false)
        timer?.let(::clearTimeout)
        timer = null
    }

    /**
     * Runs the simulator for one step and renders any updates.
     */
    @JsName("step") fun step() {
        try {
            val diffs = sim.step()
            handleNotExitOver()
            exitcodecheck()
        } catch (e: Throwable) {
            handleError("step", e, e is AlignmentError || e is StoreError || e is ExceededAllowedCyclesError)
        }
    }

    /**
     * Runs the simulator for one step and renders any updates.
     */
    @JsName("isFinished") fun isFinished(): Boolean {
        if (sim.exitcode != null) {
            return true
        } else {
            return false
        }
    }

     /**
     * Runs the simulator for one step and renders any updates.
     */
    @JsName("handleError") fun handleErr(where: String, error: Throwable) {
        handleError(where, error, error is AlignmentError || error is StoreError || error is ExceededAllowedCyclesError)
    }

    @JsName("handleNotExitOver")
    private fun handleNotExitOver() {
        if (sim.settings.ecallOnlyExit &&
                (sim.getPC().toInt() >= sim.getMaxPC().toInt() || sim.getPC().toInt() < MemorySegments.TEXT_BEGIN)
        ) {
//            val pcloc = (sim.getMaxPC().toInt() - MemorySegments.TEXT_BEGIN)
            val pcloc = sim.getPC().toInt()
            sim.incMaxPC(4)
            var mcode = MachineCode(0)
            var progLine = ""
            try {
                mcode = sim.getNextInstruction()
                Renderer.addToProgramListing(pcloc, mcode, Instruction[mcode].disasm(mcode))
            } catch (e: SimulatorError) {
                val short0 = sim.loadHalfWord(sim.getPC())
                val short1 = sim.loadHalfWord(sim.getPC() + 2)
                Renderer.addToProgramListing(pcloc, MachineCode((short1 shl 16) or short0), "Invalid Instruction", true)
            }
        }
    }

    /**
     * Undo the last executed instruction and render any updates.
     */
    @JsName("undo") fun undo() {
        try {
            val diffs = sim.undo()
            Renderer.updateFromDiffs(diffs)
            Renderer.updateControlButtons()
        } catch (e: Throwable) {
            handleError("undo", e, e is AlignmentError || e is StoreError || e is ExceededAllowedCyclesError)
        }
    }

    /**
     * Change to memory tab.
     */
    @JsName("openMemoryTab") fun openMemoryTab() {
        Renderer.renderMemoryTab()
    }

    /**
     * Change to register tab.
     */
    @JsName("openRegisterTab") fun openRegisterTab() {
        Renderer.renderRegisterTab()
    }

    @JsName("openRegsTab") fun openRegsTab() {
        Renderer.renderRegsTab()
    }

    @JsName("openFRegsTab") fun openFRegsTab() {
        Renderer.renderFRegsTab()
    }

    /**
     * Change to trace settings tab
     */
    @JsName("openTracerSettingsTab") fun openTracerSettingsTab() {
        Renderer.renderTracerSettingsTab()
    }

    @JsName("openCallingConventionSettingsTab") fun openCallingConventionSettingsTab() {
        Renderer.renderCallingConventionSettingsTab()
    }

    @JsName("openPackagesTab") fun openPackagesTab() {
        Renderer.renderPackagesTab()
    }

    @JsName("openCacheTab") fun openCacheTab() {
        Renderer.renderCacheTab()
    }

    @JsName("openSettingsTab") fun openSettingsTab() {
        Renderer.renderSettingsTab()
    }

    @JsName("openGeneralSettingsTab") fun openGeneralSettingsTab() {
        Renderer.renderGeneralSettingsTab()
    }

    @JsName("currentlyRunning") fun currentlyRunning(): Boolean = timer != null

    @JsName("destructiveGetSimOut") fun destrictiveGetSimOut(): String {
        val tmp = sim.stdout
        sim.stdout = ""
        return tmp
    }

    @JsName("openVenusTab") fun openVenusTab(tabid: String) {
        val tabs = listOf("venus-terminal", "venus-files", "venus-url", "venus-jvm")
        Renderer.renderTab(tabid, tabs)
        if (tabid == "venus-files") {
            refreshVFS()
        }
    }

    @JsName("enableCallingConvention") fun enableCallingConvention(enabled: Boolean) {
        doCallingConventionCheck = enabled
        if (doCallingConventionCheck) {
            sim.registerPlugin("CC", CallingConventionCheck())
        } else {
            sim.removePlugin("CC")
        }
    }

    /**
     * Save a register's value
     */
    @JsName("saveRegister") fun saveRegister(reg: HTMLInputElement, id: Int) {
        if (!currentlyRunning()) {
            try {
                val input = reg.value
                sim.setRegNoUndo(id, userStringToInt(input))
            } catch (e: NumberFormatException) {
                /* do nothing */
            }
        }
        Renderer.updateRegister(id, sim.getReg(id))
    }

    @JsName("saveFRegister") fun saveFRegister(freg: HTMLInputElement, id: Int) {
        if (!currentlyRunning()) {
            try {
                val input = freg.value
                val d = Decimal(f = userStringToFloat(input), d = userStringToDouble(input), isF = FReginputAsFloat)
                sim.setFRegNoUndo(id, d)
            } catch (e: NumberFormatException) {
                /* do nothing */
            }
        }
        Renderer.updateFRegister(id, sim.getFReg(id))
    }

    @JsName("updateRegMemDisplay") fun updateRegMemDisplay() {
        Renderer.updateRegMemDisplay()
    }

    @JsName("moveMemoryJump") fun moveMemoryJump() = Renderer.moveMemoryJump()

    @JsName("moveMemoryUp") fun moveMemoryUp() = Renderer.moveMemoryUp()

    @JsName("moveMemoryDown") fun moveMemoryDown() = Renderer.moveMemoryDown()

    @JsName("moveMemoryLocation") fun moveMemoryLocation(address: String) {
        try {
            val addr = userStringToInt(address)
            Renderer.updateMemory(addr)
        } catch (e: Throwable) {
            handleError("MoveMemLoc", e, true)
        }
    }

    fun getInstructionDump(): String {
        val sb = StringBuilder()
        for (i in 0 until sim.linkedProgram.prog.insts.size) {
            val mcode = sim.linkedProgram.prog.insts[i]
            val hexRepresentation = Renderer.toHex(mcode[InstructionField.ENTIRE].toInt())
            sb.append(hexRepresentation/*.removePrefix("0x")*/)
            sb.append("\n")
        }
        return sb.toString()
    }

    /**
     * DEPRECATED
     * Not implemented
     * This did copy instruction codes into a text area
     */
    @JsName("dump") fun dump() {
        /*
        try {
            Renderer.clearConsole()
            Renderer.printConsole(getInstructionDump())
            val ta = document.getElementById("console-output") as HTMLTextAreaElement
            ta.select()
            val success = document.execCommand("copy")
            if (success) {
                // window.alert("Successfully copied machine code to clipboard")
                console.log("Successfully copied machine code to clipboard")
            }
        } catch (e: Throwable) {
            handleError("dump", e)
        }
         */
    }

    @JsName("setOnlyEcallExit") fun setOnlyEcallExit(b: Boolean) {
        simSettings.ecallOnlyExit = b
    }

    @JsName("setAllowAccessBtnStackHeap") fun setAllowAccessBtnStackHeap(b: Boolean) {
        simSettings.allowAccessBtnStackHeap = b
    }

    @JsName("setSetRegsOnInit") fun setSetRegsOnInit(b: Boolean) {
        simSettings.setRegesOnInit = b
    }

    @JsName("verifyText") fun verifyText(input: HTMLInputElement) {
        try {
            if (!currentlyRunning()) {
                try {
                    var i = userStringToInt(input.value)
                    try {
                        MemorySegments.setTextBegin(i)
                        // val tabDisplay = document.getElementById("simulator-tab") as HTMLElement
                        // if (tabDisplay.classList.contains("is-active")) {
                            openSimulator()
                        // }
                    } catch (e: SimulatorError) {
                        console.warn(e.toString())
                    }
                } catch (e: NumberFormatException) {
                    /* do nothing */
                    console.warn("Unknown number format!")
                }
            } else {
                console.warn("Could not change text because the program is currently running!")
            }
            val ts = Renderer.intToString(MemorySegments.TEXT_BEGIN)
            input.value = ts
        } catch (e: Throwable) {
            handleError("Verify Text", e)
        }
    }

    @JsName("setMaxHistory") fun setMaxHistory(input: HTMLInputElement) {
        try {
            var i = userStringToInt(input.value)
            try {
                sim.setHistoryLimit(i)
            } catch (e: SimulatorError) {
                console.warn(e.toString())
            }
        } catch (e: NumberFormatException) {
            /* do nothing */
            console.warn("Unknown number format!")
        }
    }

    @JsName("setNumberOfCacheLevels") fun setNumberOfCacheLevels(i: Int) {
        if (i < 1) {
            (document.getElementById("setNumCacheLvls") as HTMLInputElement).value = cacheLevels.size.toString()
            handleError("Set Number of Cache Levels (LT0)", CacheError("You must set the number of caches to at least 1! If you do not want to use any cache, set this to 1 and then disable the cache."), true)
            return
        }
        (document.getElementById("setNumCacheLvls") as HTMLInputElement).value = i.toString()
        if (i == cacheLevels.size) {
            return
        }
        if (cacheLevels.size < i) {
            val lastCache = cacheLevels[cacheLevels.size - 1]
            while (cacheLevels.size < i) {
                val newCache = CacheHandler(cacheLevels.size + 1)
                cacheLevels[cacheLevels.size - 1].nextLevelCacheHandler = newCache
                cacheLevels.add(newCache)
                Renderer.renderAddCacheLevel()
            }
            lastCache.update()
        } else if (cacheLevels.size > i) {
            while (cacheLevels.size > i) {
                val prevCacheIndex = cacheLevels.size - 1
                val prevCache = cacheLevels[prevCacheIndex]
                cacheLevels.removeAt(prevCacheIndex)
                val lastCache = cacheLevels[cacheLevels.size - 1]
                lastCache.nextLevelCacheHandler = null
                if (cache.cacheLevel == prevCache.cacheLevel) {
                    cache = lastCache
                    Renderer.renderSetCacheLevel(cache.cacheLevel)
                }
                Renderer.renderRemoveCacheLevel()
            }
            setCacheSettings()
        }
    }

    @JsName("setCacheEnabled") fun setCacheEnabled(enabled: Boolean) {
        cache.attach(enabled)
        Renderer.updateCache(Address(0, MemSize.WORD))
    }

    @JsName("updateCacheLevel") fun updateCacheLevel(e: HTMLSelectElement) {
        try {
            val level = e.value.removePrefix("L").toInt()
            updateCacheLvl(level)
        } catch (e: NumberFormatException) {
            handleError("Update Cache Level (NFE)", e, true)
        }
    }

    fun updateCacheLvl(level: Int) {
        if (level in 1..cacheLevels.size) {
            cache = cacheLevels[level - 1]
            Renderer.renderSetCacheLevel(level)
            setCacheSettings()
        } else {
            handleError("Update Cache Level (LVL)", CacheError("Cache level '" + level + "' does not exist in your current cache!"), true)
        }
    }

    @JsName("updateCacheBlockSize") fun updateCacheBlockSize(e: HTMLInputElement) {
        val v = e.value.toInt()
        try {
            cache.setCacheBlockSize(v)
        } catch (er: CacheError) {
            Renderer.clearConsole()
            Renderer.printConsole(er.toString())
        }
        e.value = cache.cacheBlockSize().toString()
        setCacheSettings()
    }

    @JsName("updateCacheNumberOfBlocks") fun updateCacheNumberOfBlocks(e: HTMLInputElement) {
        val v = e.value.toInt()
        try {
            cache.setNumberOfBlocks(v)
        } catch (er: CacheError) {
            Renderer.clearConsole()
            Renderer.printConsole(er.toString())
        }
        e.value = cache.numberOfBlocks().toString()
        setCacheSettings()
    }

    @JsName("updateCacheAssociativity") fun updateCacheAssociativity(e: HTMLInputElement) {
        val v = e.value.toInt()
        try {
            cache.setAssociativity(v)
        } catch (er: CacheError) {
            Renderer.clearConsole()
            Renderer.printConsole(er.toString())
        }
        e.value = cache.associativity().toString()
        setCacheSettings()
    }

    @JsName("updateCachePlacementPolicy") fun updateCachePlacementPolicy(e: HTMLSelectElement) {
        if (e.value == "N-Way Set Associative") {
            cache.setPlacementPol(PlacementPolicy.NWAY_SET_ASSOCIATIVE)
        } else if (e.value == "Fully Associative") {
            cache.setPlacementPol(PlacementPolicy.FULLY_ASSOCIATIVE)
        } else {
            cache.setPlacementPol(PlacementPolicy.DIRECT_MAPPING)
            e.value = "Direct Mapped"
        }
        setCacheSettings()
    }

    @JsName("updateCacheReplacementPolicy") fun updateCacheReplacementPolicy(e: HTMLSelectElement) {
        if (e.value == "Random") {
            cache.setBlockRepPolicy(BlockReplacementPolicy.RANDOM)
        } else {
            cache.setBlockRepPolicy(BlockReplacementPolicy.LRU)
            e.value = "LRU"
        }
        setCacheSettings()
    }

    @JsName("setCacheSeed") fun setCacheSeed(v: String) {
        cache.setCurrentSeed(v)
        setCacheSettings()
    }

    fun setCacheSettings() {
        val bs = cache.cacheBlockSize().toString()
        val nb = cache.numberOfBlocks().toString()
        val av = cache.associativity().toString()
        val avenabled = cache.canSetAssociativity()
        val at = cache.placementPol().toMyString()
        val rp = cache.blockRepPolicy().toMyString()
        val cs = cache.cacheSize().toString()
        val cseed = cache.seed
        val attached = cache.attached
        (document.getElementById("block-size-val") as HTMLInputElement).value = bs
        (document.getElementById("numblocks-val") as HTMLInputElement).value = nb
        val ave = (document.getElementById("associativity-val") as HTMLInputElement)
        ave.value = av
        ave.disabled = !avenabled
        (document.getElementById("associativity-type") as HTMLSelectElement).value = at
        (document.getElementById("replacementPolicy") as HTMLSelectElement).value = rp
        (document.getElementById("cache-size-val") as HTMLInputElement).value = cs
        (document.getElementById("cache-seed") as HTMLInputElement).value = cseed
        val attachedButton = (document.getElementById("cacheEnabled") as HTMLButtonElement)
        attachedButton.value = attached.toString()
        if (attached) {
            attachedButton.addClass("is-primary")
        } else {
            attachedButton.removeClass("is-primary")
        }
        Renderer.makeCacheBlocks()
        Renderer.updateCache(Address(0, MemSize.WORD))
    }

    @JsName("setAlignedAddressing") fun setAlignedAddressing(b: Boolean) {
        simSettings.alignedAddress = b
    }

    @JsName("setMutableText") fun setMutableText(b: Boolean) {
        simSettings.mutableText = b
    }

    @JsName("addPackage") fun addPackage(button: HTMLButtonElement) {
        if (!button.hasClass("is-loading")) {
            button.addClass("is-loading")
            js("window.venuspackage = {id:'LOADING!'}")
            val purlinput = document.getElementById("package-url-val") as HTMLInputElement
            val url = purlinput.value
            ScriptManager.addPackage(url)
            setTimeout(Driver::packageLoaded, 100, button)
        } else {
            console.log("Cannot add a new package until the previous package has finished!")
        }
    }

    @JsName("togglePackage") fun togglePackage(packageID: String) {
        setTimeout(ScriptManager::togglePackage, TIMEOUT_TIME, packageID)
    }

    @JsName("removePackage") fun removePackage(packageID: String) {
        setTimeout(ScriptManager::removePackage, TIMEOUT_TIME, packageID)
    }

    fun packageLoaded(b: HTMLButtonElement) {
        if (venuspackage == undefined) {
            b.removeClass("is-loading")
            return
        }
        setTimeout(Driver::packageLoaded, 100, b)
    }

    @JsName("trace") fun trace() {
        if (trTimer != null) {
            Renderer.setNameButtonSpinning("simulator-trace", false)
            trTimer?.let(::clearTimeout)
            trTimer = null
            tr.traceFullReset()
            sim.reset()
            Renderer.updateControlButtons()
            return
        }
        Renderer.setNameButtonSpinning("simulator-trace", true)
        Renderer.clearConsole()
        loadTraceSettings()
        trTimer = setTimeout(Driver::traceSt, TIMEOUT_TIME)
    }

    private fun loadTraceSettings() {
        tr.format = (document.getElementById("tregPattern") as HTMLTextAreaElement).value
        tr.base = (document.getElementById("tbase-val") as HTMLInputElement).value.toInt()
        tr.totCommands = (document.getElementById("ttot-cmds-val") as HTMLInputElement).value.toInt()
        tr.maxSteps = (document.getElementById("tmaxsteps-val") as HTMLInputElement).value.toInt()
        tr.instFirst = (document.getElementById("tinst-first") as HTMLButtonElement).value == "true"
        tr.twoStage = (document.getElementById("tTwoStage") as HTMLButtonElement).value == "true"
        wordAddressed = (document.getElementById("tPCWAddr") as HTMLButtonElement).value == "true"
    }

    var trTimer: Timeout? = null
    internal fun traceSt() {
        try {
            tr.traceStart()
            traceLoop()
        } catch (e: Throwable) {
            handleError("Trace tr Start", e, e is AlignmentError || e is StoreError || e is ExceededAllowedCyclesError)
            Renderer.setNameButtonSpinning("simulator-trace", false)
            trTimer?.let(::clearTimeout)
            trTimer = null
        }
    }

    internal fun traceLoop() {
        try {
            var cycles = 0
            while (cycles < TIMEOUT_CYCLES) {
                if (sim.isDone()) {
                    trTimer = setTimeout(Driver::runTrEnd, TIMEOUT_TIME)
                    return
                }
                try {
                    tr.traceStep()
                } catch (err: SimulatorError) {
                    trTimer = setTimeout(Driver::runTrEnd, TIMEOUT_TIME, err)
                    return
                }
                cycles++
            }
            trTimer = setTimeout(Driver::traceLoop, TIMEOUT_TIME)
        } catch (e: Throwable) {
            handleError("Trace tr Loop", e, e is AlignmentError || e is StoreError || e is ExceededAllowedCyclesError)
            Renderer.setNameButtonSpinning("simulator-trace", false)
            trTimer?.let(::clearTimeout)
            trTimer = null
        }
    }
    internal fun runTrEnd(err: SimulatorError? = null) {
        try {
            tr.traceEnd()
            if (err != null) {
                tr.traceAddError(err)
            }
            tr.traceStringStart()
            trTimer = setTimeout(Driver::traceStringLoop, TIMEOUT_TIME)
        } catch (e: Throwable) {
            handleError("Trace Tr End", e, e is AlignmentError || e is StoreError || e is ExceededAllowedCyclesError)
            Renderer.setNameButtonSpinning("simulator-trace", false)
            trTimer?.let(::clearTimeout)
            trTimer = null
        }
    }

    internal fun traceStringLoop() {
        try {
        var cycles = 0
        while (cycles < TIMEOUT_CYCLES) {
            if (!tr.traceStringStep()) {
                trTimer = setTimeout(Driver::traceStringEnd, TIMEOUT_TIME)
                return
            }
        }
            trTimer = setTimeout(Driver::traceStringLoop, TIMEOUT_TIME)
        } catch (e: Throwable) {
            handleError("Trace String Loop", e, e is AlignmentError || e is StoreError || e is ExceededAllowedCyclesError)
            Renderer.setNameButtonSpinning("simulator-trace", false)
            trTimer?.let(::clearTimeout)
            trTimer = null
        }
    }

    internal fun traceStringEnd() {
        try {
            tr.traceStringEnd()
            Renderer.clearConsole()
            Renderer.printConsole(tr.getString())
        } catch (e: Throwable) {
            handleError("Trace String End", e, e is AlignmentError || e is StoreError || e is ExceededAllowedCyclesError)
        }
        Renderer.setNameButtonSpinning("simulator-trace", false)
        trTimer?.let(::clearTimeout)
        trTimer = null
    }

    /*@JsName("trace") fun trace() {
        //@todo make it so trace is better
        Renderer.setNameButtonSpinning("simulator-trace", true)
        Renderer.clearConsole()
        this.loadTraceSettings()
        setTimeout(Driver::traceStart, TIMEOUT_TIME)
    }*/
    internal fun traceStart() {
        try {
            tr.trace()
            setTimeout(Driver::traceString, TIMEOUT_TIME)
        } catch (e: Throwable) {
            handleError("Trace Start", e, e is AlignmentError || e is StoreError || e is ExceededAllowedCyclesError)
            Renderer.setNameButtonSpinning("simulator-trace", false)
        }
    }
    internal fun traceString() {
        try {
            tr.traceString()
            Renderer.clearConsole()
            Renderer.printConsole(tr.getString())
        } catch (e: Throwable) {
            handleError("Trace to String", e)
        }
        Renderer.setNameButtonSpinning("simulator-trace", false)
    }

    @JsName("persistentStorage") fun persistentStorage(b: Boolean) {
        useLS = b
        if (useLS) {
            console.log("Persistent storage has been enabled!")
            LS.set("venus", "true")
            saveAll()
        } else {
            console.log("Persistent storage has been disabled!")
            LS.set("venus", "false")
            // this.LS.reset()
        }
    }

    @JsName("psReset") fun psReset() {
        LS.reset()
        console.log("Persistent storage has been reset!")
    }

    fun saveIntervalFn() {
        if (useLS) {
            blinkSave(true)
            setTimeout(Driver::blinkSave, 500, false)
            saveAll()
        }
    }

    fun blinkSave(b: Boolean) {
        val e = document.getElementById("sv") as HTMLButtonElement
        if (b) {
            e.style.color = "yellow"
        } else {
            e.style.color = ""
        }
    }

    fun saveAll(override: Boolean = false) {
        if (!(useLS or override)) {
            return
        }
        /*Trace settings*/
        loadTraceSettings()
        LS.set("trace_format", tr.format)
        LS.set("trace_base", tr.base.toString())
        LS.set("trace_totCommands", tr.totCommands.toString())
        LS.set("trace_maxSteps", tr.maxSteps.toString())
        LS.set("trace_instFirst", tr.instFirst.toString())
        LS.set("trace_wordAddressed", wordAddressed.toString())
        LS.set("trace_TwoStage", tr.twoStage.toString())

        /* History Limit */
        LS.set("history_limit", this.simSettings.max_histroy.toString())
        /*Text Begin*/
        LS.set("text_begin", MemorySegments.TEXT_BEGIN.toString())
        /*Other Settings*/
        LS.set("aligned_memory", simSettings.alignedAddress.toString())
        LS.set("mutable_text", simSettings.mutableText.toString())
        LS.set("ecall_exit_only", simSettings.ecallOnlyExit.toString())
        LS.set("set_regs_on_init", simSettings.setRegesOnInit.toString())
        LS.set("simargs", getDefaultArgs())
        LS.set("enableCallingConvention", this.doCallingConventionCheck.toString())

        /*Program*/
        js("codeMirror.save()")
        LS.set("prog", getText())

        /*Cache*/
        val numExtraCache = LS.safeget("cache_levels", "1").toInt()
        if (cacheLevels.size < numExtraCache) {
            for (i in (cacheLevels.size + 1)..numExtraCache) {
                LS.remove("cache_L" + i + "_associativity")
                LS.remove("cache_L" + i + "_cacheBlockSize")
                LS.remove("cache_L" + i + "_numberOfBlocks")
                LS.remove("cache_L" + i + "_placementPol")
                LS.remove("cache_L" + i + "_blockRepPolicy")
                LS.remove("cache_L" + i + "_seed")
                LS.remove("cache_L" + i + "_attach")
            }
        }
        LS.set("cache_levels", cacheLevels.size.toString())
        LS.set("cache_current_level", cache.cacheLevel.toString())
        for (i in cacheLevels.indices) {
            val curCache = cacheLevels[i]
            LS.set("cache_L" + (i + 1) + "_associativity", curCache.associativity().toString())
            LS.set("cache_L" + (i + 1) + "_cacheBlockSize", curCache.cacheBlockSize().toString())
            LS.set("cache_L" + (i + 1) + "_numberOfBlocks", curCache.numberOfBlocks().toString())
            LS.set("cache_L" + (i + 1) + "_placementPol", curCache.placementPol().toString())
            LS.set("cache_L" + (i + 1) + "_blockRepPolicy", curCache.blockRepPolicy().toString())
            LS.set("cache_L" + (i + 1) + "_seed", curCache.seed)
            LS.set("cache_L" + (i + 1) + "_attach", curCache.attached.toString())
        }

        LS.set("activeFileinEditor", activeFileinEditor)
        var s = if (active_abs_file_name == null) {
            "n"
        } else {
            "p$active_abs_file_name"
        }
        LS.set("active_abs_file_name", s)
        s = if (active_abs_file_path == null) {
            "n"
        } else {
            "p$active_abs_file_path"
        }
        LS.set("active_abs_file_path", s)

        if (::fileExplorerCurrentLocation.isInitialized) {
            LS.set("fileExplorerCurrentLocation", fileExplorerCurrentLocation.getPath())
        }
        LS.set("terminalCurrentLocation", terminal.vfs.currentLocation.getPath())
    }

    /*If b is true, will load stored values else load default values.*/
    fun loadAll(b: Boolean) {
        val t = Tracer(sim)
        /*Trace Settings*/
        var fmt = t.format
        var bs = t.base.toString()
        var totC = t.totCommands.toString()
        var ms = simSettings.maxSteps.toString()
        var instf = t.instFirst.toString()
        var tws = t.twoStage.toString()
        var wa = wordAddressed.toString()

        /* History Limit */
        var hlim = this.simSettings.max_histroy.toString()
        /*Text begin*/
        var txtStart = Renderer.intToString(MemorySegments.TEXT_BEGIN)
        /*Other Settings*/
        var am = simSettings.alignedAddress.toString()
        var mt = simSettings.mutableText.toString()
        var eeo = simSettings.ecallOnlyExit.toString()
        var sroi = simSettings.setRegesOnInit.toString()
        var simargs = ""
        var defaultTab = "venus"
        var docc = "false"

        /*Program*/
        js("codeMirror.save()")
        p = getText()
        if (useLS) {
            console.log("Using local storage!")
            /*Trace Settings*/
            fmt = LS.safeget("trace_format", fmt)
            bs = LS.safeget("trace_base", bs)
            totC = LS.safeget("trace_totCommands", totC)
            ms = LS.safeget("trace_maxSteps", ms)
            instf = LS.safeget("trace_instFirst", instf)
            tws = LS.safeget("trace_TwoStage", tws)
            wa = LS.safeget("trace_wordAddressed", wa)

            /* History Limit */
            hlim = LS.safeget("history_limit", hlim)

            /*Text Begin*/
            txtStart = LS.safeget("text_begin", txtStart)

            /*Other Settings*/
            am = LS.safeget("aligned_memory", am)
            mt = LS.safeget("mutable_text", mt)
            eeo = LS.safeget("ecall_exit_only", eeo)
            sroi = LS.safeget("set_regs_on_init", sroi)
            simargs = LS.safeget("simargs", simargs)
            defaultTab = LS.safeget("defaultTab", defaultTab)
            docc = LS.safeget("enableCallingConvention", docc)

            /*Program*/
            p = LS.safeget("prog", p)

            /*Cache*/
            try {
                setNumberOfCacheLevels(LS.safeget("cache_levels", cacheLevels.size.toString()).toInt())
                updateCacheLvl(LS.safeget("cache_current_level", cache.cacheLevel.toString()).toInt())
                for (i in cacheLevels.indices) {
                    val currentCache = cacheLevels[i]
                    currentCache.setCacheBlockSize(LS.safeget("cache_L" + (i + 1) + "_cacheBlockSize", currentCache.cacheBlockSize().toString()).toInt())
                    currentCache.setNumberOfBlocks(LS.safeget("cache_L" + (i + 1) + "_numberOfBlocks", currentCache.numberOfBlocks().toString()).toInt())
                    currentCache.setBlockRepPolicy(BlockReplacementPolicy.valueOf(LS.safeget("cache_L" + (i + 1) + "_blockRepPolicy", currentCache.blockRepPolicy().toString())))
                    currentCache.setPlacementPol(PlacementPolicy.valueOf(LS.safeget("cache_L" + (i + 1) + "_placementPol", currentCache.placementPol().toString())))
                    currentCache.setAssociativity(LS.safeget("cache_L" + (i + 1) + "_associativity", currentCache.associativity().toString()).toInt())
                    currentCache.attach(LS.safeget("cache_L" + (i + 1) + "_attach", currentCache.attached.toString()) == "true")
                    currentCache.setCurrentSeed(LS.safeget("cache_L" + (i + 1) + "_seed", currentCache.seed))
                }
            } catch (e: Throwable) {
                console.warn("An error occurred when loading the cache data!")
                console.warn(e)
            }
            try {
                VFS.load()
            } catch (e: Throwable) {
                console.warn("An error occurred when loading the VFS data!")
                console.warn(e)
            }

            var tmp = LS.safeget("active_abs_file_name", "")
            active_abs_file_name = if (tmp == "" || tmp == "n") {
                null
            } else {
                tmp.substring(1 until (tmp.length))
            }

            tmp = LS.safeget("active_abs_file_path", "")
            val ntmp = if (tmp == "" || tmp == "n") {
                null
            } else {
                tmp.substring(1 until (tmp.length))
            }
            set_active_afpath(ntmp)

            activeFileinEditor = LS.safeget("activeFileinEditor", "")

            tmp = LS.safeget("fileExplorerCurrentLocation", "")
            fileExplorerCurrentLocation = VFS.sentinel
            openVFObject(tmp)

            terminal.vfs.cd(LS.safeget("terminalCurrentLocation", ""))
            js("try {term.reset();} catch (e) {console.error(e);}")
        } else {
            console.log("Local Storage has been disabled!")
        }
        /*Trace Settings*/
        (document.getElementById("tregPattern") as HTMLTextAreaElement).value = fmt
        tr.format = fmt
        (document.getElementById("tbase-val") as HTMLInputElement).value = bs
        tr.base = bs.toInt()
        (document.getElementById("ttot-cmds-val") as HTMLInputElement).value = totC
        tr.totCommands = totC.toInt()
        (document.getElementById("tmaxsteps-val") as HTMLInputElement).value = ms
        tr.maxSteps = ms.toInt()
        simSettings.maxSteps = ms.toInt()
        Renderer.renderButton(document.getElementById("tinst-first") as HTMLButtonElement, instf == "true")
        tr.instFirst = instf == "true"
        Renderer.renderButton(document.getElementById("tPCWAddr") as HTMLButtonElement, wa == "true")
        wordAddressed = wa == "true"
        Renderer.renderButton(document.getElementById("tTwoStage") as HTMLButtonElement, tws == "true")
        tr.twoStage = tws == "true"

        /* History Limit */
        val hl = document.getElementById("max-history") as HTMLInputElement
        hl.value = hlim
        setMaxHistory(hl)

        /*Text Begin*/
        val ts = document.getElementById("text-start") as HTMLInputElement
        ts.value = txtStart
        verifyText(ts)

        /*Other Settings*/
        Renderer.renderButton(document.getElementById("alignAddr") as HTMLButtonElement, am == "true")
        simSettings.alignedAddress = am == "true"
        Renderer.renderButton(document.getElementById("mutableText") as HTMLButtonElement, mt == "true")
        simSettings.mutableText = mt == "true"
        Renderer.renderButton(document.getElementById("ecallExit") as HTMLButtonElement, eeo == "true")
        simSettings.ecallOnlyExit = eeo == "true"
        Renderer.renderButton(document.getElementById("setRegsOnInit") as HTMLButtonElement, sroi == "true")
        simSettings.setRegesOnInit = sroi == "true"
        (document.getElementById("ArgsList") as HTMLInputElement).value = simargs
        Renderer.renderButton(document.getElementById("enableCallingConvention") as HTMLButtonElement, docc == "true")
        enableCallingConvention(docc == "true")

        /*Program*/
        js("codeMirror.setValue(driver.p);")
        p = ""

        mainCache.update()
        setCacheSettings()

        ScriptManager.loadDefaults()
        ScriptManager.loadPackages()

        checkURLParams()

        fun checkToSetTab() {
            js("load_update_message(\"Initializing Venus: Waiting on packages to load...\");")
            if (!this.ScriptManager.packagesLoading()) {
                this.openGenericMainTab(defaultTab)
                this.driver_complete_loading = true
                return
            }
            setTimeout(fun () { checkToSetTab() }, 10)
        }
        setTimeout(fun () { checkToSetTab() }, 10)
        js("codeMirror.refresh();")
    }

    lateinit var fileExplorerCurrentLocation: VFSObject

    // @JsName("deleteVFObject") fun deleteVFObject(name: String) {
    //     if (window.confirm("Are you sure you want to delete this file?")) {
    //         VFS.rm(name, fileExplorerCurrentLocation)
    //         refreshVFS()
    //     }
    // }

    @JsName("openVFObject") fun openVFObject(name: String) {
        var s = VFS.chdir(name, fileExplorerCurrentLocation)
        if (s is Pair<*, *>) {
            console.log(s.second)
            s = s.first!!
        }
        if (s is VFSObject && s.type in listOf(VFSType.Drive, VFSType.Folder)) {
            fileExplorerCurrentLocation = s
            openVFObjectfromObj(fileExplorerCurrentLocation)
        } else {
            console.log(s)
        }
    }

    fun openVFObjectfromObj(obj: VFSObject) {
        Renderer.clearObjectsFromDisplay()
        Renderer.addFilePWD(obj)
//        for ((key, value) in fileExplorerCurrentLocation.contents) {
//            if (key in listOf(".", "..")) {
//                Renderer.addObjectToDisplay(value as VFSObject, key)
//            } else {
//                Renderer.addObjectToDisplay(value as VFSObject)
//            }
//        }
        for (key in fileExplorerCurrentLocation.childrenNames()) {
            if (key in listOf(".", "..")) {
                Renderer.addObjectToDisplay(fileExplorerCurrentLocation.getChild(key) as VFSObject, key)
            } else {
                Renderer.addObjectToDisplay(fileExplorerCurrentLocation.getChild(key) as VFSObject)
            }
        }
    }

    @JsName("refreshVFS") fun refreshVFS() {
        openVFObject(".")
    }

    fun saveEditorIfModified(): Boolean {
        // Returns if there was an error.
        if (activeFileinEditor == "") {
            return true
        }
        val txt: String = try {
            js("codeMirror.save();")
            this.getText()
        } catch (e: Throwable) {
            console.error(e)
            return false
        }
        val s = VFS.getObjectFromPath(activeFileinEditor, location = fileExplorerCurrentLocation)
        if (s is VFSObject && s.type == VFSType.File) {
            if ((s as VFSFile).readText() != txt) {
                // if (window.confirm("Detected a modification in the opened file in the editor! Do you want to save it before opening the new file?")) {
                //     saveVFObjectfromObj(s)
                // }
            }
        } else {
            console.log(s)
            return false
        }
        return true
    }

    fun editVFObjectfromObj(obj: VFSObject) {
        if (activeFileinEditor == obj.getPath()) {
            this.openEditor()
            return
        }
        if (!saveEditorIfModified()) {
            // if (!window.confirm("Could not save active file! Do you still want to open the new file?")) {
            //     return
            // }
        }
        if (obj.type !== VFSType.File) {
            console.warn("Only files can be loaded into the editor.")
            return
        }
        try {
            val txt: String = (obj as VFSFile).readText()
            active_abs_file_name = obj.label
            set_active_afpath(obj.getPath())
            js("codeMirror.setValue(txt);")
            this.openEditor()
            js("codeMirror.refresh();")
            activeFileinEditor = obj.getPath()
        } catch (e: Throwable) {
            console.error(e)
            console.warn("Could not load file to the editor!")
        }
    }

    @JsName("editVFObject") fun editVFObject(name: String) {
        val s = VFS.getObjectFromPath(name, location = fileExplorerCurrentLocation)
        if (s is VFSObject) {
            editVFObjectfromObj(s)
        } else {
            console.log(s)
        }
    }

    fun saveVFObjectfromObj(obj: VFSObject, save: Boolean = true) {
        val name = obj.getPath()
        // if (activeFileinEditor != name && !window.confirm("Are you sure you want to save the file `$activeFileinEditor` to the file `$name`?")) {
        //     return
        // }
        val txt: String
        try {
            js("codeMirror.save();")
            txt = getText()
        } catch (e: Throwable) {
            console.error(e)
            console.warn("Could not save file!")
            return
        }
        if (save) {
            if (obj.type != VFSType.File) {
                console.warn("You can (currently) only save to files!")
                return
            }
            var file = obj as VFSFile
            file.setText(txt)
            this.VFS.save()
        }
        active_abs_file_name = obj.label
        set_active_afpath(obj.getPath())
        activeFileinEditor = obj.getPath()
    }

    @JsName("saveVFObject") fun saveVFObject(name: String) {
        val s = VFS.getObjectFromPath(name, location = fileExplorerCurrentLocation)
        if (s is VFSObject) {
            saveVFObjectfromObj(s)
        } else {
            console.log(s)
        }
    }

    @JsName("clearActiveFileInEditor") fun clearActiveFileInEditor() {
        active_abs_file_name = null
        set_active_afpath(null)
    }

    @JsName("vdbVFObject") fun vdbVFObject(name: String) {
        val s = VFS.getObjectFromPath(name, location = fileExplorerCurrentLocation)
        if (s is VFSObject) {
            val cur_loc = VFS.currentLocation
            VFS.currentLocation = fileExplorerCurrentLocation
            val res = vdb.execute(arrayListOf("${s.getPath()}"), terminal, false)
            VFS.currentLocation = cur_loc
            if (res != "") {
                js("alert('[ERROR]\\n' + res)")
            }
        } else {
            console.log(s)
        }
    }
}
