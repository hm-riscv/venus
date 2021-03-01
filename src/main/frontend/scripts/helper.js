/*This will remove the venus save data*/
var url_string = window.location.href;
var url = new URL(url_string);
if (url.searchParams.get("clear") === "true") {
    var clear = window.confirm("Would you like to clear Venus's settings? (Warning: this action cannot be undone!)");

    if (clear) {
        console.log("Found clear message! Removing venus data from the localStorage...");
        localStorage.removeItem("venus");
        function removeURLParameter(url, parameter) {
            //prefer to use l.search if you have a location/link object
            var urlparts= url.split('?');
            if (urlparts.length>=2) {

                var prefix= encodeURIComponent(parameter)+'=';
                var pars= urlparts[1].split(/[&;]/g);

                //reverse iteration as may be destructive
                for (var i= pars.length; i-- > 0;) {
                    //idiom for string.startsWith
                    if (pars[i].lastIndexOf(prefix, 0) !== -1) {
                        pars.splice(i, 1);
                    }
                }

                url= urlparts[0] + (pars.length > 0 ? '?' + pars.join('&') : "");
                return url;
            } else {
                return url;
            }
        }
    }
    window.location.replace(removeURLParameter(window.location.href, "clear"))
}

document.addEventListener("keydown", function(e) {
    if ((window.navigator.platform.match("Mac") ? e.metaKey : e.ctrlKey)  && e.keyCode == 83) {
        if (document.getElementById("editor-tab").classList.contains("is-active")) {
            e.preventDefault();
            // Process the event here (such as click on submit button)
            codeMirror.save();
            downloadtrace('asm-editor', 'code.s', true);
        }
    }
}, false);

function vgetBaseLog(x, y) {
    return Math.log(y) / Math.log(x);
}
function vdecimalToHexString(number)
{
    if (number < 0)
    {
        number = 0xFFFFFFFF + number + 1;
    }

    return number.toString(16).toUpperCase().substring(0,8);
}
function vnumToBase(curNumBase, n, length, base, signextend) {
    var amount = Math.pow(2, length);
    length = vgetBaseLog(curNumBase, amount);
    var num = 0;
    if (signextend) {
        num = parseInt(parseInt(n, base).toString(10), 10);
        num = parseInt(vdecimalToHexString(num), 16).toString(curNumBase);
    } else {
        num = parseInt(n, base).toString(curNumBase);
    }
    if (length - num.length > 0) {
        num = "0".repeat(length - num.length) + num;
    }
    snum = "";
    if (curNumBase == 2) {
        for (var i = 0; i < length; i++) {
            if (i % 4 == 0 && i != 0) {
                snum += " ";
            }
            snum += num[i];
        }
    } else {
        snum = num;
    }
    return snum;
}
window._recent_save_ = false;
function downloadtrace(id, filename, custom_name) {
    if (!window._recent_save_ && driver.activeFileinEditor !== "") {
        driver.saveVFObject(driver.activeFileinEditor);
        window._recent_save_ = true;
        setTimeout(function () {
            window._recent_save_ = false;
        }, 500);
        return
    }
    var cusN = prompt("Please enter a name for the file. Leave blank for default. If you do not want to receive this prompt anymore, please open (edit) a file through the terminal or file explorer. Saving will then happen to last file which was 'edit'ed. If you save twice really fast, you can still bring up this prompt.");
    if (cusN != "") {
        if (cusN == null) {
            return;
        }
        if (cusN.split('.').pop() != filename.split('.').pop()) {
            cusN = cusN + "." + filename.split('.').pop();
        }
        filename = cusN;
    }
    var text = document.getElementById(id).value;
    var element = document.createElement('a');
    element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(text));
    element.setAttribute('download', filename);

    element.style.display = 'none';
    document.body.appendChild(element);

    element.click();

    document.body.removeChild(element);
}
function CopyToClipboard(containerid) {
    var copyText = document.getElementById(containerid);

    /* Select the text field */
    copyText.select();

    /* Copy the text inside the text field */
    document.execCommand("Copy");
    return
}
function validateBase(e) {
    if (e.value == "") {
        return;
    }
    if (e.value < 1) {
        e.value = 2;
    }
    if (e.value > 32) {
        e.value = 32;
    }
}
function toggleThis(e) {
    if (e.value == "true") {
        e.classList.remove("is-primary");
        e.value = "false";
    } else {
        e.classList.add("is-primary");
        e.value = "true";
    }
}

(function(window, document, undefined){
    'use strict';
    var start;
    var end;
    var delta;
    var button = document.getElementById("sv");
    var buttonfor = document.getElementById("forsv");
    var maxtime = 1000;
    window.svtmot = -1;
    window.svclr = false;

    var bmd = function(){
        start = new Date();
        clearTimeout(window.svtmot);
        window.svtmot = setTimeout(function(date){
            if (window.svtmot !== -1 && start === date) {
                button.style.backgroundColor = "red"
            }
        }, maxtime, start);
    }

    button.addEventListener("mousedown", bmd);

    buttonfor.addEventListener("mousedown", bmd);

    var bmu = function() {
        window.svtmot = -1;
        clearTimeout(window.svtmot);
        end = new Date();
        delta = end - start;
        if (delta > maxtime) {
            window.svclr = true;
            driver.psReset();
        }
        button.style.backgroundColor = "";
    }

    button.addEventListener("mouseup", bmu);

    buttonfor.addEventListener("mouseup", bmu);

})(window, document);

var LocalStorageManager = class LocalStorageManager{
    constructor(name) {
        this.name = name;
        this.vls = {};
        this.getObj();
    }
    getObj() {
        var v = localStorage.getItem(this.name);
        if (v == null) {
            this.setup();
            return;
        }
        var jsn = JSON.parse(v);
        if (typeof jsn === 'object' && jsn[this.name] !== undefined) {
            this.vls = jsn;
        } else {
            this.setup();
        }
    }
    writeObj() {
        /*@todo handle if this obj is NOT the thing taking the storage, this will loop forever in rare cases.*/
        try {
            localStorage.setItem(this.name, JSON.stringify(this.vls));
        } catch (e) {
            console.log("Could not store the data in localStorage! Removing largest element and trying again...")
            this.removeLargest()
        }
    }
    setup() {
        this.vls[this.name] = 'false';
        this.writeObj();
    }
    get(key) {
        var v = this.vls[key];
        if (v === undefined) {
            v = 'undefined';
        }
        return v
    }
    set(key, value) {
        if (this.vls[key] === value) {
            return
        }
        this.vls[key] = value;
        this.writeObj();
    }
    remove(key) {
        delete this.vls[key];
        this.writeObj();
    }
    reset() {
        this.vls = {}
        this.setup()
    }
    removeLargest() {
        var largestKey = ""
        var ksize = 0
        var key = ""
        for (key of Object.keys(this.vls)) {
            var v = this.get(key)
            if (v.length >= ksize) {
                ksize = v.length
                largestKey = key
            }
        }
        var ktot = Object.keys(this.vls)
        if (ktot !== 0) {
            console.log("Removing largest key '" + largestKey + "' of size '" + ksize + "'")
            this.remove(largestKey)
        } else {
            console.log("Could not remove any more elements! If this was called by set, then you should clear your localStorage and try again.")
        }
    }
};

function loadStylesheet(url) {
    var urlelm = document.getElementById(url);
    if (urlelm) {
        urlelm.parentNode.removeChild(urlelm)
    }
    var css = document.createElement('link');
    css.setAttribute("rel", "stylesheet");
    css.setAttribute("href", url);
    css.setAttribute("id", url);
    document.getElementsByTagName("head")[0].appendChild(css);
}

function selectText(containerid) {
    if (document.selection) { // IE
        var range = document.body.createTextRange();
        range.moveToElementText(document.getElementById(containerid));
        range.select();
    } else if (window.getSelection) {
        var range = document.createRange();
        range.selectNode(document.getElementById(containerid));
        window.getSelection().removeAllRanges();
        window.getSelection().addRange(range);
    }
}

function setUpURL() {
    var u = generateURL();
    var a = document.getElementById('generatedurl');
    a.href = u;
    a.innerText = u;
}

function unparseString(s) {
    let ps = s.replace("\n", "\\n")
        .replace("\t", "\\t");
    return ps
}

function generateURL(){
    var location = window.location.origin + window.location.pathname + "?";
    var e = document.getElementById("urloptions-save");
    if (e && e.value === "true") {
        var e = document.getElementById("urloptions-save-choice");
        if (e && e.value === "true") {
            location += "save=true&"
        } else {
            location += "save=false&"
        }
    }
    var e = document.getElementById("urloptions-code");
    if (e && e.value === "true") {
        var e = document.getElementById("urloptions-code-override");
        if (e) {
            location += "override=" + e.value + "&";
        }
        codeMirror.save();
        location += "code=" + encodeURIComponent(unparseString(document.getElementById("asm-editor").value)) + "&";
    }

    return location;
}

function httpGetAsync(theUrl, callback)
{
    var xmlHttp = new XMLHttpRequest();
    xmlHttp.onreadystatechange = function() {
        if (xmlHttp.readyState == 4 && xmlHttp.status == 200)
            callback(xmlHttp.responseText);
    };
    xmlHttp.onerror = function () {
        console.log(xmlHttp);
    };
    xmlHttp.open("GET", theUrl, true); // true for asynchronous
    xmlHttp.send(null);
}

// (function() {
//     var cors_api_host = 'cors-anywhere.herokuapp.com';
//     var cors_api_url = 'https://' + cors_api_host + '/';
//     var slice = [].slice;
//     var origin = window.location.protocol + '//' + window.location.host;
//     var open = XMLHttpRequest.prototype.open;
//     XMLHttpRequest.prototype.open = function() {
//         var args = slice.call(arguments);
//         var targetOrigin = /^https?:\/\/([^\/]+)/i.exec(args[1]);
//         if (targetOrigin && targetOrigin[0].toLowerCase() !== origin &&
//             targetOrigin[1] !== cors_api_host) {
//             args[1] = cors_api_url + args[1];
//         }
//         return open.apply(this, args);
//     };
// })();

function loadfromtarget(t) {
    // httpGetAsync("https://cors-anywhere.herokuapp.com/" + t, lftcallback);
    httpGetAsync(t, lftcallback);
}

function lftcallback(resp) {
    codeMirror.setValue(resp);
}

// document.addEventListener ("keydown", function (zEvent) {
//     if (zEvent.ctrlKey  && zEvent.key === "c") {
//         if (driver.LS.get("defaultTab") === "venus") {
//             driver.runEnd();
//             var cl = document.getElementsByClassName("cmdline");
//             if (cl.length > 0) {
//                 cl[cl.length - 1].focus();
//             }
//         }
//     }
// });

// try {
//     var venusBodyTabView = document.getElementById("venus-body-tab-view");
//     // var venusBodyTabViewChild = document.getElementById("venus-body-tab-view-child");
//     var venusBodyTabViewChild = document.getElementById("container");
// } catch (e) {
//     console.error(e);
// }
// function modifyTerminalSize() {
//     venusBodyTabViewChild.style.width = (venusBodyTabView.clientWidth).toString() + "px";
// }
// modifyTerminalSize();
// window.onresize = modifyTerminalSize;