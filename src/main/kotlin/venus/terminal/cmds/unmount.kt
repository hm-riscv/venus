package venus.terminal.cmds

import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal
import venus.vfs.VFSDrive

var umount = Command(
        name = "umount",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            if (args.size < 1) {
                return Command["umount"].help
            }
            val dir = args[0]
            val m = t.vfs.getObjectFromPath(dir)
            if (m is VFSDrive && m.isMounted()) {
                m.parent.removeChild(m.name)
                t.vfs.save()
            } else {
                return "umount: $dir: Not a mounted dir"
            }
            return ""
        },
        tab = ::fileTabComplete,
        help = """Allows you to unmount external drives on the Venus web file system.
            |Usage: umount dir
            |dir is the dir you would like to unmount to.
        """.trimMargin()
)