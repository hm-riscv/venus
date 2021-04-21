package venus.vfs

import venus.Driver
import venusbackend.simulator.SimulatorSettings
import kotlin.browser.window

@JsName("VirtualFileSystem") class VirtualFileSystem(val defaultDriveName: String, val simSettings: SimulatorSettings = SimulatorSettings()) {
    var sentinel = VFSDrive(defaultDriveName, VFSDummy())
    var currentLocation: VFSObject = sentinel

    companion object {
        val LSName = "VFS_DATA"

        fun getPath(path: String): ArrayList<String> {
            return path.split(VFSObject.separator) as ArrayList
        }

        fun makePath(path: ArrayList<String>): String {
            return path.joinToString(VFSObject.separator)
        }
    }

    init {
        sentinel.parent = sentinel
    }

    fun makeFileInDir(path: String): VFSFile? {
        val obj = getObjectFromPath(path)
        if (obj == null) {
            val nobj = getObjectFromPath(path, true) as VFSObject
            return if (nobj.type != VFSType.File) {
                val name = nobj.label
                val parent = nobj.parent
                parent.removeChild(name)
                val newfile = VFSFile(name, parent)
                parent.addChild(newfile)
                newfile
            } else {
                nobj as VFSFile
            }
        } else {
            if (obj.type == VFSType.File) {
                return obj as VFSFile
            }
            return null
        }
    }

    @JsName("reset") fun reset() {
        this.currentLocation = sentinel
    }

    @JsName("mkdir") fun mkdir(dirName: String): String {
        val newdir = VFSFolder(dirName, currentLocation)
        return if (currentLocation.addChild(newdir)) {
            ""
        } else {
            "Could not make directory: $dirName"
        }
    }

    @JsName("cd") fun cd(dir: String): String {
        var tmp = chdir(dir, currentLocation)
        var msg = ""
        if (tmp is Pair<*, *>) {
            msg = tmp.second as String
            tmp = tmp.first!!
        }
        if (tmp is VFSObject) {
            if (tmp.type !in listOf(VFSType.Folder, VFSType.Drive)) {
                return "Can only go into folders and drives."
            }
            currentLocation = tmp
        } else {
            return tmp.toString()
        }
        return msg
    }

    fun chdir(dir: String, curloc: VFSObject): Any {
        val splitpath = getPath(dir)
        var templocation = if (dir.startsWith("/") || dir == "") {
            if (splitpath.size > 0) {
                splitpath.removeAt(0)
            }
            sentinel
        } else {
            curloc
        }
        val initial_location = templocation
        var mountCheck = true
        for (dir in splitpath) {
            if (templocation.isMounted() && mountCheck) {
                mountCheck = false
                val connected = templocation.mountedHandler!!.validate_connection()
                if (connected != "") {
                    if (initial_location.isMounted() && initial_location.mountedHandler!!.validate_connection() != "") {
                        return Pair(sentinel, "$connected")
                    }
                    return "$connected"
                }
            }
            if (dir == "") {
                continue
            }
            if (!templocation.containsChild(dir)) {
                return "$dir: No such file or directory"
            }
            templocation = templocation.getChild(dir) as VFSObject
            if (templocation.type in listOf(VFSType.File, VFSType.Program)) {
                return "$dir: Not a directory"
            }
        }
        return templocation
    }

    @JsName("touch") fun touch(filename: String): String {
        val newfile = VFSFile(filename, currentLocation)
        return if (currentLocation.addChild(newfile)) {
            ""
        } else {
            "Could not make file: $filename"
        }
    }

    @JsName("ls") fun ls(path: String? = null): String {
        var location = path?.let { getObjectFromPath(it) } ?: currentLocation
        var str = ""
        for (s in location.childrenNames()) {
            str += s + (if ((location.getChild(s) as VFSObject).type in listOf(VFSType.Folder, VFSType.Drive)) VFSObject.separator else "") + "\n"
        }
        return str
//        for (c in currentLocation.children() as MutableCollection<VFSObject>) {
//            str += c.label + (if (c.type in listOf(VFSType.Folder, VFSType.Drive)) VFSObject.separator else "") + "\n"
//        }
//        return str
    }

    @JsName("cat") fun cat(filedir: String): String {
        val splitpath = getPath(filedir)
        var templocation = if (filedir.startsWith("/")) {
            splitpath.removeAt(0)
            sentinel
        } else {
            currentLocation
        }
        for (obj in splitpath) {
            if (obj == "") {
                continue
            }
            if (!templocation.containsChild(obj)) {
                return "cat: $filedir: No such file or directory"
            }
            templocation = templocation.getChild(obj) as VFSObject
        }
        if (templocation.type != VFSType.File) {
            return "cat: $filedir: Is not a file!"
        }
//        if (!templocation.containsChild(VFSFile.innerTxt)) {
//            return "cat: $filedir: COULD NOT FIND FILE CONTENTS!"
//        }
        return (templocation as VFSFile).readText()
    }

    @JsName("path") fun path(): String {
        return currentLocation.getPath() + VFSObject.separator
    }
    // @FIXME There is a bug for going into a file
    @JsName("remove") fun remove(path: String): String {
        return rm(path, currentLocation)
    }

    fun rm(path: String, curloc: VFSObject): String {
        val templocation = this.getObjectFromPath(path)
        if (templocation === null) {
            return "rm: cannot remove '$path': No such file or directory"
        }
        if (curloc.getPath().contains(templocation.getPath())) {
            return "rm: cannot remove '$path': Path is currently active"
        }
        val p = templocation.parent

        if (templocation.isMounted() && templocation.type == VFSType.Drive) {
            return "rm: cannot remove a mounted drive. Use `umount` instead."
        }

        return (if (p.removeChild(templocation.label)) "" else "rm: could not remove file")
    }

    @JsName("write") fun write(path: String, msg: String): String {
        val splitpath = getPath(path)
        var templocation = if (splitpath.size > 0 && splitpath[0] == sentinel.label) {
            splitpath.removeAt(0)
            sentinel
        } else {
            currentLocation
        }
        for (obj in splitpath) {
            if (obj == "") {
                continue
            }
            if (!templocation.containsChild(obj)) {
                return "write: cannot write to '$path': No such file or directory"
            }
            templocation = templocation.getChild(obj) as VFSObject
        }
        if (templocation.type != VFSType.File) {
            return "cat: $path: Is not a file!"
        }
        (templocation as VFSFile).setText(msg)
        return ""
    }

    @JsName("addFile") fun addFile(path: String, data: String, loc: VFSObject = currentLocation): String {
        val splitpath = getPath(path)
        if (splitpath.size == 0) {
            return "There was no file passed in!"
        }
        val fname = splitpath.removeAt(splitpath.size - 1)
        var curloc = loc
        for (p in splitpath) {
            curloc = if (curloc.containsChild(p)) {
                val next = curloc.getChild(p)!! as VFSObject
                if (next.type !in listOf(VFSType.Folder, VFSType.Drive)) {
                    return "Could not create folder due to a non folder existing in the path."
                }
                next
            } else {
                val fold = VFSFolder(p, curloc)
                curloc.addChild(fold)
                fold
            }
        }
        val f = VFSFile(fname, curloc)
        f.setText(data)
        curloc.addChild(f)
        return ""
    }

    fun mountDrive(name: String, handler: VFSMountedDriveHandler, loc: VFSObject = currentLocation): String {
        if (loc.containsChild(name)) {
            return "Object with same name exists in this folder!"
        }
        loc.addChild(VFSDrive(name, loc, mountedHandler = handler))
        return ""
    }

    fun getParentFromObject(obj: VFSObject): VFSObject? {
        return obj.parent
    }

    fun getObjectFromPath(path: String, make: Boolean = false, location: VFSObject? = null): VFSObject? {

        val splitpath = getPath(path)

        if (path == "" || path == "/") {
            return sentinel
        }

        if (path.startsWith("/")) {
            if (splitpath.size > 0) {
                splitpath.removeAt(0)
            } else {
                return sentinel
            }
        }

        var curloc = location ?: sentinel
        val fname = splitpath.removeAt(splitpath.size - 1)

        for (p in splitpath) {
            curloc = if (curloc.containsChild(p)) {
                val next = curloc.getChild(p)!! as VFSObject
                if (next.type !in listOf(VFSType.Folder, VFSType.Drive)) {
                    return null
                }
                next
            } else {
                if (p.matches("[a-z]:")) { // Drive Name
                    val drive = VFSDrive(p, curloc)
                    curloc.addChild(drive)
                    drive
                } else {
                    val fold = VFSFolder(p, curloc)
                    curloc.addChild(fold)
                    fold
                }
            }
        }
        val f = VFSFile(fname, curloc)
        var fpath = f.getPath()
        f.setText(fs.readFileSync(fpath, "utf-8"))
        curloc.addChild(f)
        return f
    }


    fun filesFromPrefix(path: String, prefix: String): ArrayList<String> {
        val location: VFSObject = getObjectFromPath(path) ?: this.currentLocation
        val fnames = ArrayList<String>()
        for (key: String in location.childrenNames()) {
            if (key.startsWith(prefix)) {
                val obj = location.getChild(key) as VFSObject
                var k = key
                if (obj.type in listOf(VFSType.Folder, VFSType.Drive)) {
                    k += "/"
                }
                fnames.add(k)
            }
        }
        return fnames
    }

    fun stringify(): String {
        return JSON.stringify(sentinel.stringify())
    }

    fun parse(vfsString: String) {
        val raw = JSON.parse<JsonContainer>(vfsString)
        val temp = VFSDrive.inflate(raw, VFSDummy())
        val newsent = temp as VFSDrive
        newsent.parent = newsent
        this.sentinel = newsent
        this.currentLocation = this.sentinel
    }

    fun load() {
        val vfsJSON = window.localStorage.getItem(LSName)
        if (vfsJSON != undefined) {
            this.parse(vfsJSON)
        }
    }

    fun save() {
        if (Driver.useLS) {
            val vfsJSON = this.stringify()
            window.localStorage.setItem(LSName, vfsJSON)
        }
    }
}