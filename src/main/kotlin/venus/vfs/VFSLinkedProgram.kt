package venus.vfs

import venusbackend.linker.LinkedProgram

class VFSLinkedProgram(override var label: String, override var parent: VFSObject, prog: LinkedProgram = LinkedProgram(), override var mountedHandler: VFSMountedDriveHandler? = null) : VFSObject {
    override val type = VFSType.LinkedProgram
    override var contents = HashMap<String, Any>()
    override var permissions = VFSPermissions()
    companion object {
        val innerProgram = "innerprogram"
        fun inflate(jsonContainer: JsonContainer, parent: VFSObject): VFSObject {
            val file = VFSLinkedProgram(jsonContainer.label, parent)
            // file.setLinkedProgram(jsonContainer.innerobj as LinkedProgram)
            return file
        }
    }
    init {
        contents[innerProgram] = prog
    }
    fun getLinkedProgram(): LinkedProgram {
        return contents[innerProgram] as LinkedProgram
    }
    fun setLinkedProgram(p: LinkedProgram) {
        contents[innerProgram] = p
    }

    override fun stringify(): JsonContainer {
        val me = JsonContainer()
        me.label = this.label
        me.permissions = this.permissions
        me.type = this.type.toString()
        me.innerobj = this.contents[VFSProgram.innerProgram] as Any
        return me
    }
}