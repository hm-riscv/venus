package venus.vfs

import venusbackend.riscv.Program

class VFSProgram(override var label: String, override var parent: VFSObject, prog: Program = Program(absPath = ""), override var mountedHandler: VFSMountedDriveHandler? = null) : VFSObject {
    override val type = VFSType.Program
    override var contents = HashMap<String, Any>()
    override var permissions = VFSPermissions()
    companion object {
        val innerProgram = "innerprogram"
        fun inflate(jsonContainer: JsonContainer, parent: VFSObject): VFSObject {
            val file = VFSProgram(jsonContainer.label, parent)
            // file.setProgram(jsonContainer.innerobj as Program)
            return file
        }
    }
    init {
        contents[innerProgram] = prog
    }
    fun getProgram(): Program {
        return contents[innerProgram] as Program
    }
    fun setProgram(p: Program) {
        contents[innerProgram] = p
    }

    override fun stringify(): JsonContainer {
        val me = JsonContainer()
        me.label = this.label
        me.permissions = this.permissions
        me.type = this.type.toString()
        me.innerobj = this.contents[innerProgram] as Any
        return me
    }
}