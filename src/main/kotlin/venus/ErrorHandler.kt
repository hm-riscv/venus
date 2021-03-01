package venus

import venusbackend.simulator.SimulatorError
import kotlin.browser.window

/**
 * Created by thaum on 7/27/2018.
 */

internal fun handleError(where: String, error: Throwable, h: Boolean = false) {
    var handled = h || if (error is SimulatorError) {
        error.handled ?: false
    } else {
        false
    }
    if (!(error is SimulatorError && (error.infe != null))) {
//        Renderer.clearConsole()
        console.error(error)
    } else {
        handled = true
    }
    try {
        /*Here, I will attempt to get the local storage to display in the error so if someone send sme the error, I can more easily track it.*/
        val olduseLS = Driver.useLS
        Driver.useLS = false
        val oldlsm = Driver.LS.lsm

        Driver.LS.lsm = LocalStorageManager("venus_error")
        Driver.LS.remove("venus_error")
        Driver.LS.set("venus", "true")
        Driver.saveAll(true)
        val t = window.localStorage.getItem("venus_error")
        window.localStorage.removeItem("venus_error")

        Driver.LS.lsm = oldlsm
        Driver.useLS = olduseLS
        Renderer.displayError("\n--------------------\n")
        if (handled) {
            Renderer.displayError("[ERROR] An error has occurred!\n\n")
        } else {
            Renderer.displayError("[ERROR] An uncaught error has occurred! Here are the details that may help solve this issue.\n\n")
        }
        Renderer.displayError("Error:\n`" + error.toString())
        Renderer.displayError("\n\nID:\n'" + where + "'!\n\n")
        if (!handled) {
            Renderer.displayError("`\n\nData:\n" + t)
        }
    } catch (t: Throwable) {
        Renderer.displayError("An error occurred when trying to handle the error! Please tell me what you did since I do not fully know how you caused this error and could not generate a trace for me to figure that out. All I know is that the error was here: '" + where + "' and was:\n" + error.toString())
    }
}