package venus.vfs

import venus.vfs.VFSObject.Companion.isValidName

open class VFSFolder(var name: String, override var parent: VFSObject, override var mountedHandler: VFSMountedDriveHandler? = null) : VFSObject {
    override val type = VFSType.Folder
    override var label = name
    override var contents = HashMap<String, Any>()
    override var permissions = VFSPermissions()
    init {
        contents[".."] = parent
        contents["."] = this
    }
    fun addFile(name: String): Boolean {
        if (isValidName(name) && !contents.containsKey(name)) {
            contents[name] = VFSFile(name, this)
            return true
        }
        return false
    }
    fun addFolder(name: String): Boolean {
        if (isValidName(name) && !contents.containsKey(name)) {
            contents[name] = VFSFolder(name, this)
            return true
        }
        return false
    }

    override fun stringify(): JsonContainer {
        val me = JsonContainer()
        me.label = this.label
        me.permissions = this.permissions
        me.type = this.type.toString()
        for (item in this.contents.keys) {
            if (item !in listOf(".", "..")) {
                me.contents.add((this.contents[item] as VFSObject).stringify())
            }
        }
        return me
    }

    companion object {
        fun inflate(jsonContainer: JsonContainer, parent: VFSObject): VFSObject? {
            val folder = VFSFolder(jsonContainer.label, parent)
            for (i in 0 until js("jsonContainer.contents.length")) {
                val value = js("jsonContainer.contents[i]")
                var addchild = true
                val obj = when (value.type) {
                    VFSType.Folder.toString() -> {
                        inflate(value, folder)
                    }
                    VFSType.Program.toString() -> {
                        addchild = false
                        VFSProgram.inflate(value, folder)
                    }
                    VFSType.LinkedProgram.toString() -> {
                        addchild = false
                        VFSLinkedProgram.inflate(value, folder)
                    }
                    VFSType.File.toString() -> {
                        VFSFile.inflate(value, folder)
                    }
                    VFSType.Drive.toString() -> {
                        VFSDrive.inflate(value, folder)
                    }
                    else -> {
                        VFSDummy()
                    }
                }
                if (addchild && obj != null) {
                    folder.addChild(obj)
                }
            }
            return folder
        }
    }
}
