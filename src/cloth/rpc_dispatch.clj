(ns cloth.rpc-dispatch
  (:require [clojure.data.json :as json]))

(defn dispatch [ns msg context]
  (let [obj  (json/read-str msg)
        op   (get obj "op")
        args (assoc (get obj "args") "context" context)]

    (if-let [f (ns-resolve ns (symbol op))]
      (let [arglists (:arglists (meta f))
            arglist (first arglists)
            vals (map #(get args (name %)) arglist)]
        (if (> (count arglists) 1)
          (println "RPC Dispatch doesn't support overloaded functions.")
          (do (println "Dispatch op:" op)
              (apply f vals))))
      (println "No matching RPC function found."))))
