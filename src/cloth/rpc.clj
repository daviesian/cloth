(ns cloth.rpc
  (:use [cloth.server-state]
        [cloth.utils])
  (:require [clojure.data.json :as json]))

(defn save-file [context]
  (let [file-name (str "files/" (context :code-file))
        content   (get @current-code (context :code-file))]
    (spit file-name content)))

(defn code-change [head code anchor context]
  (swap! current-code assoc (context :code-file) code)
  (forward-to-all-others (context :channel) (get @clients (context :code-file)) (json/write-str (context :msg))))

(defn eval-all [context]
  (binding [*print-length* 20]
    (let [err (java.io.StringWriter.)
          out (java.io.StringWriter.)
          ast (try (read-string (str "(do " (get @current-code (context :code-file)) "\n)"))
                   (catch Exception e
                     (binding [*out* err]
                       (println "Read error:" (.toString e)))))
          ans (try (print-str (sb ast {#'*out* out}))
                   (catch Exception e
                     (binding [*out* err]
                       (println "Eval error:" (.toString e)))))
          resp (json/write-str {"op" "eval-result"
                                "ans" ans
                                "output" (str out)
                                "error" (str err)})]
      (forward-to-all-others nil (get @clients (context :code-file)) resp))))
