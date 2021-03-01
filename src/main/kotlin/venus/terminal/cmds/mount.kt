package venus.terminal.cmds

/* ktlint-disable no-wildcard-imports */

import venus.CLIHelpPrinter
import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal
import venus.vfs.MESSAGE_TTL
import venus.vfs.VFSMountedDriveHandler
import venusbackend.cli.*

/* ktlint-enable no-wildcard-imports */

var mount = Command(
        name = "mount",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
//            if (args.size < 1) {
//                return Command["mount"].help
//            }
            val help_printer = CLIHelpPrinter(24)
            val cli = CommandLineInterface("mount", defaultHelpPrinter = help_printer)
            val url by cli.positionalArgument("url", "URL where the mount server is located.", "local", minArgs = 1)
            val dir by cli.positionalArgument("dir", "This is the dir you would like to mount to. By default, it will mount it in the current location with the name the drive gives it.", "vmfs", minArgs = 0)
            val key by cli.positionalArgument("key", "key is the validation key to ensure you are talking to the correct mount server.", "", minArgs = 0)
            val message_ttl by cli.flagValueArgument(listOf("-ttl", "--messageTTL"), "TTL", "The amount of time (in seconds) you want the mount server to still accept messages from a client. Set to less than or equal to 0 to disable ttl.", initialValue = MESSAGE_TTL) { it.toInt() }

//            var fixed_args = mutableListOf("mount")
//            fixed_args.addAll(args)
            try {
                cli.parse(args)
            } catch (e: Exception) {
                if (e is HelpPrintedException || e is CommandLineException) {
                    return help_printer.get_string()
                }
                return "mount: ${e.message}\n${help_printer.get_string()}"
            }

            var fixed_url = url
            if (fixed_url == "local") {
                fixed_url = "http://localhost:6161"
            }
            if (!(fixed_url.startsWith("http://") || url.startsWith("https://"))) {
                fixed_url = "http://" + fixed_url
            }
            try {
                val h = VFSMountedDriveHandler(fixed_url, key, message_ttl = message_ttl)
                h.connect()
                val res = t.vfs.mountDrive(dir, h)
                t.vfs.save()
                return res
            } catch (e: Throwable) {
                return "mount: $e"
            }
            return ""
        },
        tab = ::fileTabComplete,
        help = """Allows you to mount external drives to the Venus web file system.
            |Usage: mount device dir key
            |device is either a name of a device or url of a hosted drive.
            |dir is the dir you would like to mount to. By default, it will mount it in the current location with the name the drive gives it.
            |key is the validation key to ensure you are talking to the correct mount server.
            |You can use dir to name the mount point/nest it in another folder.
        """.trimMargin()
)