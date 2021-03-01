package venus.vfs

/**
 * This class is meant to just be a dummy class to allow for VFS init.
 */
class VFSDummy(override var mountedHandler: VFSMountedDriveHandler? = null) : VFSObject {
    override val type = VFSType.Dummy
    override var label = "DUMMY"
    override var contents = HashMap<String, Any>()
    override var permissions = VFSPermissions()
    override lateinit var parent: VFSObject
}