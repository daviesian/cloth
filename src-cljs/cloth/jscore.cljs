(ns cloth.jscore
  (:use [jayq.core :only [$ document-ready append-to]])
  (:require [crate.core :as crate]))


(defn log [x]
  (.log js/console x))

(defn delCharAfterFixed [cm]
  (.deleteH cm 1 "char")
  (CodeMirror/signal cm "cursorActivity" cm))

(set! (.-delCharAfter CodeMirror/commands) delCharAfterFixed)

(def editor (atom nil))
(def socket (atom nil))
(def prog-update (atom false))

(defn eval-all [cm]
  (.selectAll CodeMirror/commands cm)
  (.send @socket (JSON/stringify (js-obj "op" "eval-all"))))

(defn eval-sexp [cm]
  (if-not (-> cm (.getTokenAt (.getCursor cm))
              .-state
              .-indentStack)
    (log "Not in a form")

    (do
      (while (-> cm (.getTokenAt (.getCursor cm))
                 .-state
                 .-indentStack)
        (.moveH cm -1 "char"))

      (let [start (.getCursor cm)]
        (.moveH cm 1 "char")
        (while (-> cm (.getTokenAt (.getCursor cm))
                   .-state
                   .-indentStack)
          (.moveH cm 1 "char"))
        (let [end (.getCursor cm)]

          (.setSelection cm start end)

          (.send @socket (JSON/stringify (js-obj "op" "eval-form"
                                                 "args" (js-obj "form" (.getSelection cm)))))
          )))))

(defn save-file []
  (.send @socket (JSON/stringify (js-obj "op" "save-file"))))

(defn replace-newlines [s]
  (.replace (.replace s #"\r" "") "#\n" "<br />"))

(defn separate-output []
  (.addClass (append-output "") "outputSeparator"))

(defn append-output [o]
  (let [new-div ($ (crate/html [:div {:class "outputBlock cm-s-solarized"} o]))]
    (append-to
     new-div
     :#outputPanel)

    (.scrollTop ($ :#outputPanel) (.-scrollHeight (first ($ :#outputPanel))))

    new-div))

(document-ready
 (fn []

   (reset! editor (CodeMirror/fromTextArea (first ($ :#codeArea))
                                           (js-obj "lineNumbers" true
                                                   "theme" "solarized dark"
                                                   "matchBrackets" true
                                                   "extraKeys" (js-obj "Tab" "indentAuto"
                                                                       "Ctrl-E" eval-all
                                                                       "Ctrl-S" save-file
                                                                       "Ctrl-Alt-X" eval-sexp))))

   (reset! socket (js/WebSocket. (str "ws://" window.location.host "/socket/" js/file)))

   (.on @editor "cursorActivity" (fn [inst]
                                   (when-not @prog-update
                                     (.send @socket
                                            (JSON/stringify
                                             (js-obj "op" "code-change"
                                                     "args" (js-obj "code" (.getValue inst)
                                                                    "head" (.getCursor inst "head")
                                                                    "anchor" (.getCursor inst "anchor"))))))))


   (set! (.-onmessage @socket)
         (fn [msg]
           (let [msg (JSON/parse (.-data msg))]
             (cond
              (= (.-op msg) "code-change") (do (reset! prog-update true)
                                               (.setValue @editor (.-code (.-args msg)))
                                               (.setSelection @editor (.-anchor (.-args msg)) (.-head (.-args msg)))
                                               (reset! prog-update false))
              (= (.-op msg) "eval-result") (do (separate-output)
                                               (when (not-empty (.-error msg))
                                                 (.addClass (append-output (replace-newlines (.-error msg))) "outputError"))
                                               (when (not-empty (.-output msg))
                                                 (append-output (replace-newlines (.-output msg))))
                                               (append-output (replace-newlines (.-ans msg))))
              (= (.-op msg) "message") (do (separate-output)
                                           (.addClass (append-output (replace-newlines (.-message msg))) "outputMessage"))))))


   (log (JSON/stringify (js-obj "A" 1)))))
