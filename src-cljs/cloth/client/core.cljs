(ns cloth.client.core
  (:use [jayq.core :only [$ document-ready append-to]]
        [cloth.client.utils :only [log]])
  (:require [cloth.client.state :as state]
            [cloth.client.utils :as utils]))

(def codemirror-opts
  {:lineNumbers true
   :theme "solarized light"
   :matchBrackets true
   :extraKeys {"Tab" "indentAuto"
               "Ctrl-E" utils/eval-all
               "Ctrl-S" utils/save-file
               "Ctrl-Alt-X" utils/eval-sexp}})

(set! (.-delCharAfter CodeMirror/commands) utils/delCharAfterFixed)

(document-ready
 (fn []

   (reset! state/editor (CodeMirror/fromTextArea (first ($ :#codeArea))
                                                 (clj->js codemirror-opts)))

   (reset! state/socket (js/WebSocket. (str "ws://" window.location.host "/socket/" js/file)))

   (.on @state/editor "cursorActivity" (fn [inst]
                                         (when-not @state/prog-update
                                           (utils/rpc :code-change {:code (.getValue inst)
                                                                    :head (js->clj (.getCursor inst "head"))
                                                                    :anchor (js->clj (.getCursor inst "anchor"))}))))


   (set! (.-onmessage @state/socket)
         (fn [msg]
           (let [msg-data (cljs.reader/read-string (.-data msg))
                 op       (:op msg-data)
                 f        (utils/lookup-fn cloth.client.rpc (name op))]
             (if f
               (do
                 (log "Dispatching" op)
                 (f (:args msg-data)))
               (log (str "Unknown RPC function: " op))))))

   (.focus @state/editor)))
