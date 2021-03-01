package venus.vfs

import kotlin.browser.window

class VFSDrive(val n: String, override var parent: VFSObject, override var mountedHandler: VFSMountedDriveHandler? = null) : VFSFolder(n, parent) {
    override val type = VFSType.Drive
    init {
        if (parent is VFSDummy) {
            this.contents[".."] = this
        } else {
            this.contents[".."] = parent
        }
    }

    companion object {
        fun inflate(jsonContainer: JsonContainer, parent: VFSObject): VFSObject? {
            if (jsonContainer.innerobj != "") {
                val iobj = jsonContainer.innerobj
                var key = ""
                var url = ""
                var message_ttl = MESSAGE_TTL
                js("""
                    key = iobj["key"];
                    url = iobj["url"];
                    if ("message_ttl" in iobj) {
                        message_ttl = iobj["message_ttl"];
                    }
                """)
                val handler = VFSMountedDriveHandler(url, key, message_ttl = message_ttl)
                try {
                    handler.connect()
                } catch (e: IllegalStateException) {
                    val emsg = "Failed to mount drive `${jsonContainer.label}`: $e"
                    console.error(emsg)
                    window.alert(emsg)
                }
                val folder = VFSDrive(jsonContainer.label, parent, mountedHandler = handler)
                return folder
            }
            val folder = VFSDrive(jsonContainer.label, parent)
            for (i in 0 until js("jsonContainer.contents.length")) {
                val value = js("jsonContainer.contents[i]")
                var addchild = true
                val obj = when (value.type) {
                    VFSType.Folder.toString() -> {
                        VFSFolder.inflate(value, folder)
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
                        inflate(value, folder)
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

    override fun stringify(): JsonContainer {
        val me = JsonContainer()
        me.label = this.label
        me.permissions = this.permissions
        me.type = this.type.toString()
        me.innerobj = this.mountedHandler?.save() ?: ""
        if (this.mountedHandler == null) {
            for (item in this.contents.keys) {
                if (item !in listOf(".", "..")) {
                    me.contents.add((this.contents[item] as VFSObject).stringify())
                }
            }
        }
        return me
    }
}