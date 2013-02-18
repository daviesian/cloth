(ns cloth.client.macros)

(defmacro defn-rpc [name [& params] & body]
  `(defn ^:export ~name [{:keys [~@params]} args#] ~@body))
