
var editor = null;
var socket = null;

function delCharAfterFixed(cm) {
    cm.deleteH(1, "char");
    CodeMirror.signal(cm, "cursorActivity", cm);
}

function evalAll(cm) {
    CodeMirror.commands.selectAll(cm);
    socket.send(JSON.stringify({op: "eval-all"}));
}
function saveFile(cm) {
    socket.send(JSON.stringify({op: "save-file"}));
}
function evalSexp(cm)
{
    if (cm.getTokenAt(cm.getCursor()).state.indentStack == null)
    {
        console.log("Not in a form");
        return;
    }

    while(cm.getTokenAt(cm.getCursor()).state.indentStack != null)
        cm.moveH(-1,"char");

    var start = cm.getCursor();

    cm.moveH(1, "char");

    while(cm.getTokenAt(cm.getCursor()).state.indentStack != null)
        cm.moveH(1,"char");

    var end  = cm.getCursor();

    cm.setSelection(start, end);

    socket.send(JSON.stringify({op: "eval-form", args: {form: cm.getSelection()}}));
}


CodeMirror.commands["delCharAfter"] = delCharAfterFixed;

function separateOutput()
{
    appendOutput("").addClass("outputSeparator");
}

function appendOutput(output)
{
    var newDiv = $('<div/>', {
        class: "outputBlock cm-s-solarized",
        html: output
    });

    newDiv.appendTo('#outputPanel');

    $('#outputPanel').scrollTop($('#outputPanel')[0].scrollHeight);

    return newDiv;
}

function replaceNewlines(str)
{
    return str.replace(/\r/g,"").replace(/\n/g, "<br/>");
}


$(function() {

    editor = CodeMirror.fromTextArea(document.getElementById("codeArea"), {
        lineNumbers: true,
        theme: "solarized light",
        matchBrackets: true,
        extraKeys: {"Tab": "indentAuto",
                    "Ctrl-E": evalAll,
                    "Ctrl-S": saveFile,
                    "Ctrl-Alt-X": evalSexp}
    });

    socket = new WebSocket("ws://" + window.location.host + "/socket/" + file);

    var progUpdate = false;
    editor.on("cursorActivity", function(inst) {
        if (!progUpdate)
            socket.send(JSON.stringify(
                {op: "code-change",
                 args: { code: inst.getValue(),
                         head: inst.getCursor("head"),
                         anchor: inst.getCursor("anchor")
                       }
                }));
    });

    socket.onmessage = function(msg) {
        msg = JSON.parse(msg.data);
        switch (msg.op)
        {
        case "code-change":

            progUpdate = true;
            editor.setValue(msg.args.code);
            editor.setSelection(msg.args.anchor, msg.args.head);
            progUpdate = false;
            break;
        case "eval-result":
            separateOutput();
            if (msg.error)
                appendOutput(replaceNewlines(msg.error)).addClass("outputError");
            if (msg.output)
                appendOutput(replaceNewlines(msg.output));
            appendOutput(replaceNewlines(msg.ans));
            break;
        case "message":
            separateOutput();
            appendOutput(replaceNewlines(msg.message)).addClass("outputMessage");
        }
    };


});
