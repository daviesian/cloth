(ns cloth.client.rpc
  (:use [cloth.client.utils :only [log]])
  (:require [cloth.client.utils :as utils]
            [cloth.client.state :as state])
  (:require-macros [cloth.client.macros :as m]))

(m/defn-rpc code-change [code head anchor]
  (reset! state/prog-update true)
  (.setValue @state/editor code)
  (.setSelection @state/editor (clj->js anchor) (clj->js head))
  (reset! state/prog-update false))

(m/defn-rpc message [message]
  (utils/separate-output)
  (.addClass (utils/append-output (utils/replace-newlines message)) "outputMessage"))

(m/defn-rpc eval-result [ans output error]
  (utils/separate-output)
  (when (not-empty error)
    (.addClass (utils/append-output (utils/replace-newlines error)) "outputError"))
  (when (not-empty output)
    (utils/append-output (utils/replace-newlines output)))
  (utils/append-output (utils/replace-newlines ans)))
