(ns cloth.rpc
  (:use [cloth.server-state]
        [cloth.utils]))

(defn save-file [context]
  (let [file-name (str "files/" (context :code-file))
        content   (get @current-code (context :code-file))]
    (spit file-name content)
    (let [resp (pr-str {:op :message
                        :args {:message (str "File saved: " (context :code-file))} })]
      (forward-to-all-others nil (get @clients (context :code-file)) resp))))

(defn code-change [head code anchor context]
  (swap! current-code assoc (context :code-file) code)
  (forward-to-all-others (context :channel) (get @clients (context :code-file)) (context :msg)))


(defn eval-form [context form]
  (binding [*print-length* 20]
    (let [err (java.io.StringWriter.)
          out (java.io.StringWriter.)
          ast (try (read-string (str "(do " form "\n)"))
                   (catch Exception e
                     (binding [*out* err]
                       (println "Read error:" (.toString e)))))
          ans (try (print-str (sb ast {#'*out* out}))
                   (catch Exception e
                     (binding [*out* err]
                       (println "Eval error:" (.toString e)))))
          resp (pr-str {:op :eval-result
                        :args {:ans ans
                               :output (str out)
                               :error (str err)}})]
      (forward-to-all-others nil (get @clients (context :code-file)) resp))))

(defn eval-all [context]
  (eval-form context (get @current-code (context :code-file))))
