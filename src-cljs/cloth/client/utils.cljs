(ns cloth.client.utils
  (:use [jayq.core :only [$ append-to]])
  (:require [crate.core :as crate]
            [cloth.client.state :as state]))

(defn rpc
  ([f] (rpc f nil))
  ([f args]
     (.send @state/socket {:op f :args args})))

(defn log [& args]
  (.apply (aget js/console "log") js/console (clj->js args)))

(defn delCharAfterFixed [cm]
  (.deleteH cm 1 "char")
  (CodeMirror/signal cm "cursorActivity" cm))

(defn eval-all [cm]
  (.selectAll CodeMirror/commands cm)
  (rpc :eval-all))

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

          (rpc :eval-form {:form (.getSelection cm)}))))))

(defn save-file []
  (rpc :save-file))

(defn replace-newlines [s]
  (.replace (.replace s #"\r" "") "#\n" "<br />"))

(defn append-output [o]
  (let [new-div ($ (crate/html [:div {:class "outputBlock cm-s-solarized"} o]))]
    (append-to
     new-div
     :#outputPanel)

    (.scrollTop ($ :#outputPanel) (.-scrollHeight (first ($ :#outputPanel))))

    new-div))

(defn separate-output []
  (.addClass (append-output "") "outputSeparator"))

(defn lookup-fn [ns f]
  (aget ns (.replace f "-" "_")))
