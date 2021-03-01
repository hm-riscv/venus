package venus.vfs

import org.w3c.xhr.XMLHttpRequest
import venus.Driver
import kotlin.browser.window

val COMPATABLE_VERSION = "1.0.1"
val MESSAGE_TTL = 30

data class VFSMountedDriveHandler(var url: String, var key: String, var message_ttl: Int = MESSAGE_TTL) {
    var connected = false
    init {
//        login()
//        ping()
//        ping()
        if (url.endsWith("/")) {
            url = url.removeSuffix("/")
        }
//        connect()
    }

    fun connect() {
        if (!ping()) {
            throw IllegalStateException("Could not connect to $url")
        }
        compatable()
        verifyKey()
        connected = true
    }

    fun disconect() {
        connected = false
        key = ""
    }

    data class VFSMDHSave(val url: String, val key: String, val message_ttl: Int)
    fun save(): VFSMDHSave {
        return VFSMDHSave(url, key, message_ttl)
    }

    fun make_request(type: String, endPoint: String, data: String = ""): String? {
        val isSpecialEndpoint = endPoint in listOf("ping", "version", "showkey", "v1/auth")
        if (!connected && !isSpecialEndpoint) {
            connect()
        }
        val encdata = if (!isSpecialEndpoint) {
            try {
                fernetEncode(key, data)
            } catch (e: IllegalStateException) {
                disconect()
                throw e
            }
        } else {
            data
        }
        val xhttp = XMLHttpRequest()
        xhttp.open(type, "$url/$endPoint", async = false)
        xhttp.setRequestHeader("Content-Type", "application/json;charset=UTF-8")
        if (js("""window.DEBUG""") == true) {
            console.log("Sending request to $endPoint!")
        }
        try {
            xhttp.send(encdata)
        } catch (e: Throwable) {
//            return null
            console.error(e)
            connected = false
            throw IllegalStateException("Failed to send the request! Is the mount server still running?")
        }
        if (js("""window.DEBUG""") == true) {
            console.log("Got response!")
        }
//        if (xhttp.status == 401.toShort()) {
//            console.log("Failed to make request to $endPoint due to an auth error, logging back in...")
//            login()
//            return make_request(type, endPoint, data)
//        }
        if (xhttp.status == 0.toShort()) {
            throw IllegalStateException("Server responded with ${xhttp.status} - ${xhttp.statusText}! This may be due to strict enforcement of HTTPS.")
        }
        if (xhttp.status != 200.toShort()) {
            throw IllegalStateException("Server responded with ${xhttp.status} - ${xhttp.statusText} for request $endPoint. Data: $data")
        }
        return if (!isSpecialEndpoint) {
            val encres = xhttp.responseText.let { JSON.parse<CMDEncRes>(it) } ?: CMDEncRes(success = false)
            if (!encres.success) {
                val res = window.prompt("Failed to decrypt the message from the server! Server Message: ${encres.data}\nIf the key has changed, please enter it here. If not, just leave this blank.")
                if (res == null) {
                    throw IllegalStateException("Failed to decrypt the message from the server! Server Message: ${encres.data}")
                } else {
                    key = res
                    verifyKey(new_key = true)
                    make_request(type, endPoint, data)
                }
            } else {
                try {
                    fernetDecode(key, encres.data)
                } catch (e: IllegalStateException) {
                    disconect()
                    throw e
                }
            }
        } else {
            xhttp.responseText
        }
    }

//    fun login() {
//        val res = make_request("POST", "login", VENUS_AUTH_TOKEN)
//        this.token = JSON.parse<LoginToken>(res)
//    }
//
//    fun logout() {
//        if (this.token != null) {
//            make_request("POST", "logout", this.token!!.token)
//            this.token = null
//        }
//    }

    fun compatable() {
        val res = this.getVersion()
        if (res == null) {
            throw IllegalStateException("Failed to get version from the file server ($url)!")
        } else if (res != COMPATABLE_VERSION) {
            throw IllegalStateException("This version of Venus's drive handler ($COMPATABLE_VERSION) is not compatible with the mounted file server ($res)!")
        }
    }

    data class verKey(val msg: String, val send: String, val response: String)
    fun verifyKey(new_key: Boolean = false) {
        val rsp = make_request("GET", "showkey")
        val res = rsp?.let { JSON.parse<verKey>(it) }
                ?: throw IllegalStateException("Failed to receive a valid response for showkey! (Received $rsp)")
        var save_on_complete = new_key
        if (key == "") {
            while (key == "") {
                key = window.prompt(res.msg) ?: run {
                    disconect()
                    throw IllegalStateException("Failed to enter a valid key!")
                }
            }
            save_on_complete = true
        }
        val senddata = try {
            fernetEncode(key, res.send)
        } catch (e: Throwable) {
            disconect()
            console.log(e)
            throw IllegalStateException("You have entered an invalid key!")
        }
        val authrsp = make_request("POST", "v1/auth", data = JSON.stringify(verKey(msg = "verify", send = senddata, response = "")))
        val authres = authrsp?.let { JSON.parse<verKey>(it) } ?: run {
            disconect()
                throw IllegalStateException("Auth service on the mount server failed to correctly respond!")
            }
        if (authres.msg != "") {
            disconect()
            throw IllegalStateException("Auth server responded with an error: ${authres.msg}")
        }
        if (fernetDecode(key, authres.response) != res.response) {
            disconect()
            throw IllegalStateException("The key you supplied is incorrect!")
        }
        console.log("Key verified!")
        if (save_on_complete) {
            // Hack to save new key
            Driver.VFS.save()
        }
    }

    fun getVersion(): String? {
        val rsp = make_request("GET", "version")
        val res = rsp?.let { JSON.parse<pingRes>(it) }
        return res?.data
    }

    data class pingRes(val data: String)
    fun ping(): Boolean {
        console.log("Sending ping...")
        val rsp = make_request("GET", "ping")
        val res = rsp?.let { JSON.parse<pingRes>(it) } ?: pingRes("Server failed to respond!")
        console.log("Server responded with: ${res.data}!")
        return rsp != null
    }

    fun validate_connection(): String {
        try {
            if (!this.ping()) {
                return "The mount server responded with an invalid response!"
            }
        } catch (e: IllegalStateException) {
            return "cd: $e"
        }
        return ""
    }

    data class CMDEncRes(val success: Boolean, val data: String = "")

    data class CMDlsReq(val data: String)
    data class CMDlsRes(val success: Boolean, val data: Array<String> = arrayOf<String>())
    fun CMDls(dir: String = ""): Array<String>? {
        val rsp = make_request("POST", "api/fs/ls/names", data = JSON.stringify(CMDlsReq(dir)))
        val res = rsp?.let { JSON.parse<CMDlsRes>(it) } ?: CMDlsRes(success = false)
        if (!res.success) {
            return null
        }
        return res.data
    }

    data class CMDfileinfoReq(val name: String, val path: String)
    data class CMDfileinfoRes(val success: Boolean, val name: String = "", val type: String = "", val data: String = "")
    fun CMDfileinfo(path: String, name: String): Map<String, String> {
        val rsp = make_request("POST", "api/fs/file/info", data = JSON.stringify(CMDfileinfoReq(name = name, path = path)))
        val res = rsp?.let { JSON.parse<CMDfileinfoRes>(it) } ?: CMDfileinfoRes(success = false)
        return mapOf(Pair("name", res.name), Pair("type", res.type))
    }

    data class CMDfilereadReq(val path: String)
    data class CMDfilereadRes(val success: Boolean, val data: String = "FAILED TO READ FILE")
    fun CMDfileread(path: String): String {
        val rsp = make_request("POST", "api/fs/file/read", data = JSON.stringify(CMDfilereadReq(path = path)))
        val res = rsp?.let { JSON.parse<CMDfilereadRes>(it) } ?: CMDfilereadRes(success = false)
        var data = res.data
        var rd = ""
        js("rd = atob(data);")
        return rd
    }

    data class CMDfilewriteReq(val path: String, val data: String)
    data class CMDfilewriteRes(val success: Boolean, val data: String = "FAILED TO WRITE TO FILE")
    fun CMDfilewrite(path: String, data: String): String {
        val d = ""
        js("d = btoa(data);")
        val rsp = make_request("POST", "api/fs/file/write", data = JSON.stringify(CMDfilewriteReq(path = path, data = d)))
        val res = rsp?.let { JSON.parse<CMDfilewriteRes>(it) } ?: CMDfilewriteRes(success = false)
        if (!res.success) {
            window.alert("Failed to write to file: ${res.data}")
        }
        return res.data
    }

    data class CMDmkdirReq(val path: String)
    data class CMDmkdirRes(val success: Boolean, val data: String = "FAILED TO GET RESPONSE")
    fun CMDmkdir(path: String): String {
        val rsp = make_request("POST", "api/fs/mkdir", data = JSON.stringify(CMDmkdirReq(path = path)))
        val res = rsp?.let { JSON.parse<CMDmkdirRes>(it) } ?: CMDmkdirRes(success = false)
        if (!res.success) {
            window.alert("Failed to write make folder: ${res.data}")
        }
        return res.data
    }

    data class CMDtouchReq(val path: String)
    data class CMDtouchRes(val success: Boolean, val data: String = "FAILED TO GET RESPONSE")
    fun CMDtouch(path: String): String {
        val rsp = make_request("POST", "api/fs/touch", data = JSON.stringify(CMDtouchReq(path = path)))
        val res = rsp?.let { JSON.parse<CMDtouchRes>(it) } ?: CMDtouchRes(success = false)
        if (!res.success) {
            window.alert("Failed to write make file: ${res.data}")
        }
        return res.data
    }

    data class CMDrmReq(val path: String)
    data class CMDrmRes(val success: Boolean, val data: String = "FAILED TO GET RESPONSE")
    fun CMDrm(path: String): String {
        val rsp = make_request("POST", "api/fs/rm", data = JSON.stringify(CMDrmReq(path = path)))
        val res = rsp?.let { JSON.parse<CMDrmRes>(it) } ?: CMDrmRes(success = false)
        if (!res.success) {
            window.alert("Failed to remove file or directory: ${res.data}")
        }
        return res.data
    }

    data class CMDnameRes(val success: Boolean, val data: String)
    fun CMDname(): String {
        val rsp = make_request("GET", "api/fs/name")
        val res = rsp?.let { JSON.parse<CMDnameRes>(it) } ?: CMDnameRes(false, "Could not get a response from the server!")
        return res.data
    }

    fun fernetEncode(key: String, data: String): String {
        var encoded = ""
        try {
            js("""
            var secret = new fernet.Secret(key);
            var token = new fernet.Token({
              secret: secret,
            });
            encoded = token.encode(data)
        """)
        } catch (err: Exception) {
            throw IllegalStateException("Fernet encryption failed! ${err.message}")
        }
        return encoded
    }

    fun fernetDecode(key: String, data: String, ttl: Int = message_ttl): String {
        var raw = ""
        var error: String? = null
        js("""
        try {
            var secret = new fernet.Secret(key);
            var token = new fernet.Token({
              secret: secret,
              token: data,
              ttl: ttl
            });
            raw = token.decode(data)
        } catch (err) {
            error = err.message
        }
    """)
        if (error != null) {
            if (error == "Invalid Token: TTL") {
                throw IllegalStateException("A message you received has expired! Aborting...")
            } else {
                throw IllegalStateException("Unknown error when reading message: $error! Aborting...")
            }
        }
        return raw
    }
}

data class LoginToken(var token: String, var expiration: String)
