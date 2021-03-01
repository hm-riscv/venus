package venus.terminal.cmds

import venus.terminal.Command
import venus.terminal.Command.Companion.fileTabComplete
import venus.terminal.Terminal
import venus.vfs.VFSDummy
import venus.vfs.VFSFile
import venus.vfs.VFSType

var download = Command(
        name = "download",
        execute = fun(args: MutableList<String>, t: Terminal, sudo: Boolean): String {
            val s: StringBuilder = StringBuilder()
            for (fname: String in args) {
                val f = t.vfs.getObjectFromPath(fname) ?: VFSDummy()
                if (f.type == VFSType.File) {
                    downloadFile(fname, (f as VFSFile).readText())
                } else {
                    s.append("'$fname' is not a path to a file!")
                }
            }
            return s.toString()
        },
        tab = ::fileTabComplete
)

fun downloadFile(filename: String, text: String) {
    js("""
        var element = document.createElement('a');
        // Due to chars being 16 bytes, we cannot do standard encoding :'(
//        var encoded = encodeURIComponent(text);
        var encoded = "";
        for (var i = 0; i < text.length; i++) {
            var byte = (text.charCodeAt(i)).toString(16)
            if (byte.length == 1) {
                byte = "0" + byte;
            }
            encoded += "%" + byte;
        }
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encoded);
        element.setAttribute('download', filename);
        
        element.style.display = 'none';
        document.body.appendChild(element);
        
        element.click();
        
        document.body.removeChild(element);
    """)
}