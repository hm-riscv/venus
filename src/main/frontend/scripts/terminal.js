var util = util || {};
util.toArray = function(list) {
  return Array.prototype.slice.call(list || [], 0);
};

var Terminal = Terminal || function(cmdLineContainer, outputContainer) {
  window.URL = window.URL || window.webkitURL;
  window.requestFileSystem = window.requestFileSystem || window.webkitRequestFileSystem;

  var cmdLine_ = document.querySelector(cmdLineContainer);
  var output_ = document.querySelector(outputContainer);
  
  var fs_ = null;
  var cwd_ = null;
  var history_ = [];
  var histpos_ = 0;
  var histtemp_ = 0;

  cmdLine_.addEventListener('click', inputTextClick_, false);
  cmdLine_.addEventListener('keydown', historyHandler_, false);
  cmdLine_.addEventListener('keydown', processNewCommand_, false);

  //
  function inputTextClick_(e) {
    this.value = this.value;
  }

  //
  function historyHandler_(e) {
    if (history_.length) {
      if (e.keyCode == 38 || e.keyCode == 40) {
        if (history_[histpos_]) {
          history_[histpos_] = this.value;
        } else {
          histtemp_ = this.value;
        }
      }

      if (e.keyCode == 38) { // up
        histpos_--;
        if (histpos_ < 0) {
          histpos_ = 0;
        }
      } else if (e.keyCode == 40) { // down
        histpos_++;
        if (histpos_ > history_.length) {
          histpos_ = history_.length;
        }
      }

      if (e.keyCode == 38 || e.keyCode == 40) {
        this.value = history_[histpos_] ? history_[histpos_] : histtemp_;
        this.value = this.value; // Sets cursor to end of input.
          e.preventDefault();
      }
    }
  }

  //
  function processNewCommand_(e) {

    if (e.keyCode == 9) { // tab
        try {
            if (this.value) {
                var data = driver.terminal.tab(this.value);
                if (typeof(data) === "string") {
                    if (data.startsWith("An error occurred!")) {
                        e.preventDefault();
                    }
                } else {
                    whattodo = data[1];
                    prefix = data[0];
                    if (Array.isArray(whattodo)) {
                        if (whattodo.length === 0) {
                            // Does nothing atm.
                            e.preventDefault();
                        } else if (whattodo.length === 1) {
                            this.value += whattodo[0].replace(RegExp("^" + prefix), '');
                            e.preventDefault();
                        } else {
                            // Duplicate current input and append to output section.
                            var line = this.parentNode.parentNode.cloneNode(true);
                            line.removeAttribute('id');
                            try {
                                line.children[0].removeAttribute('id');
                            } catch (e) {
                            }
                            line.classList.add('line');
                            var input = line.querySelector('input.cmdline');
                            input.autofocus = false;
                            input.readOnly = true;
                            output_.appendChild(line);
                            output('<div class="ls-files">' + whattodo.sort().join('<br>') + '</div>');
                            common = "";
                            smallest_len = Number.MAX_SAFE_INTEGER;
                            for (i = 0; i < whattodo.length; i++) {
                                var l = whattodo[i].length;
                                if (l < smallest_len) {
                                    smallest_len = l;
                                }
                            }
                            for (i = 0; i < smallest_len; i++) {
                                var c = whattodo[0][i];
                                var shouldBread = false;
                                for (j = 1; j < whattodo.length; j++) {
                                    if (c !== whattodo[j][i]) {
                                        shouldBread = true;
                                        break;
                                    }
                                }
                                if (shouldBread) {
                                    break;
                                }
                                common += c;
                            }
                            this.value += common.replace(RegExp("^" + prefix), '');
                            e.preventDefault();
                        }
                    } else {
                        // Duplicate current input and append to output section.
                        var line = this.parentNode.parentNode.cloneNode(true);
                        line.removeAttribute('id');
                        try {
                            line.children[0].removeAttribute('id');
                        } catch (e) {
                        }
                        line.classList.add('line');
                        var input = line.querySelector('input.cmdline');
                        input.autofocus = false;
                        input.readOnly = true;
                        output_.appendChild(line);
                        output(whattodo);
                        e.preventDefault();
                    }
                }
            }
        } catch (e) {
            console.error(e);
        }
        document.getElementById("container").scrollTo(0, getDocHeight_());
      e.preventDefault();
      // Implement tab suggest.
    } else if (e.keyCode == 13) { // enter
      // Save shell history.
        try {
        if (this.value) {
          history_[history_.length] = this.value;
          histpos_ = history_.length;
        }

        // Duplicate current input and append to output section.
        var line = this.parentNode.parentNode.cloneNode(true);
        line.removeAttribute('id');
        try {
            line.children[0].removeAttribute('id');
        } catch (e) {}
        line.classList.add('line');
        var input = line.querySelector('input.cmdline');
        input.autofocus = false;
        input.readOnly = true;
        output_.appendChild(line);
        document.getElementById("input-line").style.display = "none";

        _CMDS = ['clear', 'clock', 'date', 'exit', 'help', 'uname'];

        if (this.value && this.value.trim()) {
          var args = this.value.split(' ').filter(function(val, i) {
            return val;
          });
          var cmd = args[0].toLowerCase();
            //args = args.splice(1); // Remove cmd from arg list.
        }
        if (cmd === "sudo" && args.length >= 2) {
            cmd = args[1].toLowerCase()
        }
        var noline = false;
          switch (cmd) {
              case 'exit':
                  output_.innerHTML = '';
                  this.value = '';
                  window.term.init();
                  driver.VFS.reset();
                  setDir();
                  terminal_showline(line, this);
                  return;
              case 'clear':
                  output_.innerHTML = '';
                  this.value = '';
                  terminal_showline(line, this);
                  return;
              case 'clock':
                  var appendDiv = jQuery($('.clock-container')[0].outerHTML);
                  appendDiv.attr('style', 'display:inline-block');
                  output_.appendChild(appendDiv[0]);
                  break;
              case 'date':
                  output( (new Date()).toString() );
                  break;
              case 'help':
                  if (args.splice(1).length > 0) {
                      output(driver.terminal.processInput(this.value || ""));
                  } else {
                      output('<div class="ls-files">' + _CMDS.concat(driver.terminal.getCommands()).sort().join('<br>') + '</div>');
                  }
                  break;
              case 'uname':
                  output(navigator.appVersion);
                  break;
              default:
                  if (cmd) {
                      var out = driver.terminal.processInput(this.value || "");
                      if (out.startsWith("VDIRECTIVE:RUNNING...")) {
                          output(out.replace(RegExp("^VDIRECTIVE:RUNNING\\.\\.\\."), ""));
                          document.getElementById("term_stop_btn").style.display = "block";
                          noline = true;
                          window.setTimeout(getSimOutData, 100, output, line, this)
                      } else if (out.startsWith("VDIRECTIVE:EXEFN...")) {
                          output(out.replace(RegExp("^VDIRECTIVE:EXEFN\\.\\.\\."), ""));
                          noline = true;
                          window.VENUSFNOUTPUT = "";
                          window.VENUSFNDONE = false;
                          window.setTimeout(checkFnExeOut, 100, output, line, this);
                      } else {
                          output(out);
                      }
                  }
          };
            setDir();
        } catch (e) {
        output("UNKNOWN INTERNAL ERROR:" + e.toString())
      }

      document.getElementById("container").scrollTo(0, getDocHeight_());
        if (!noline) {
            terminal_showline(line, this)
        }
    }
  }

  function setDir() {
      $('#currentprompt').html('[user@venus] ' + driver.VFS.path() + '# ');
  }

  //
  function formatColumns_(entries) {
    var maxName = entries[0].name;
    util.toArray(entries).forEach(function(entry, i) {
      if (entry.name.length > maxName.length) {
        maxName = entry.name;
      }
    });

    var height = entries.length <= 3 ?
        'height: ' + (entries.length * 15) + 'px;' : '';

    // 12px monospace font yields ~7px screen width.
    var colWidth = maxName.length * 7;

    return ['<div class="ls-files" style="-webkit-column-width:',
            colWidth, 'px;', height, '">'];
  }

  //
  function output(html, justappendtolast) {
      html = html.replace(/\n/g, "<br/>");
      var c = output_.children;
      var child = c[c.length - 1];
      if (justappendtolast && child !== undefined) {
          child.innerHTML += html
      } else {
          output_.insertAdjacentHTML('beforeEnd', "<p style=\"white-space: pre-wrap;\">" + html + '</p>');
      }
      document.getElementById("container").scrollTo(0, getDocHeight_());
  }

  // Cross-browser impl to get document's height.
  function getDocHeight_() {
    var d = document.getElementById("container");
    return Math.max(
        d.scrollHeight,
        d.offsetHeight,
        d.clientHeight
    );
    var d = document;
    return Math.max(
        Math.max(d.body.scrollHeight, d.documentElement.scrollHeight),
        Math.max(d.body.offsetHeight, d.documentElement.offsetHeight),
        Math.max(d.body.clientHeight, d.documentElement.clientHeight)
    );
  }

  //
  return {
    init: function() {
        output('<img align="left" src="images/favicon.png" width="100" height="100" style="padding: 0px 10px 20px 0px"><h2 style="letter-spacing: 4px">Venus Web Terminal</h2><p>' + new Date() + '</p><p>Enter "help" for more information.</p>');
    },
    reset: function() {
        output_.innerHTML = '';
        this.value = '';
        window.term.init();
        setDir();
    },
    output: output,
    getDocHeight_: getDocHeight_
  }
};

function terminal_showline(line, elm) {
    var input = document.getElementById("input-line");
    input.querySelector("input.cmdline").value = ''; // Clear/setup line for next input.
    input.style.display = "";
    document.getElementById("container").scrollTo(0, term.getDocHeight_());
}

function getSimOutData(output, line, elm) {
    output(driver.destructiveGetSimOut(), true);
    if (!driver.currentlyRunning()) {
        terminal_showline(line, elm);
        document.getElementById("term_stop_btn").style.display = "none";
        var sout = document.getElementById("console-output");
        if (sout) {
            sout.value = "";
        }
        return;
    }
    window.setTimeout(getSimOutData, 25, output, line, this);
}

function checkFnExeOut(output, line, elm) {
    var txt = window.VENUSFNOUTPUT;
    window.VENUSFNOUTPUT = "";
    output(txt, true);
    if (window.VENUSFNDONE === true) {
        terminal_showline(line, elm);
        return;
    }
    window.setTimeout(checkFnExeOut, 25, output, line, elm);
}

$(function() {

    // Set the command-line prompt to include the user's IP Address
    //$('.prompt').html('[' + codehelper_ip["IP"] + '@HTML5] # ');
    $('.prompt').html('[user@venus] /# ');

    // Initialize a new terminal object
    window.term = new Terminal('#input-line .cmdline', '#container output');
    term.init();

    var container = document.getElementById("container");
    var cmdline = document.querySelector('#input-line .cmdline');
    container.addEventListener('click', function(e) {
        cmdline.focus();
    }, false);

    // Update the clock every second
    setInterval(function() {
        function r(cls, deg) {
            $('.' + cls).attr('transform', 'rotate('+ deg +' 50 50)')
        }
        var d = new Date();
        r("sec", 6*d.getSeconds());
        r("min", 6*d.getMinutes());
        r("hour", 30*(d.getHours()%12) + d.getMinutes()/2)
    }, 1000);

});