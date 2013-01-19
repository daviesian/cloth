(ns cloth.init
  (:import [org.apache.commons.io.output WriterOutputStream]
           [java.io PrintStream]))

;; First, we redirect the raw stdout of the server to this repl

(System/setOut (PrintStream. (WriterOutputStream. *out*)
                             true)) ;; Auto-flush the PrintStream


;; Next, we alter the root binding of *out* so that new threads
;; send their output to THIS repl rather than the original System/out.

(alter-var-root #'*out* (fn [_] *out*))
