(ns cloth.core
  (:use [clojure.pprint]
        [cloth.init]
        [aleph.http]
        [lamina.core]
        [compojure.core]
        [compojure.route]
        [hiccup.core]
        [hiccup.page]
        [hiccup.form]
        [clojail.core :only [sandbox]]
        [clojail.testers :only [secure-tester-without-def blacklist-symbols blacklist-objects]])
  (:require [clojure.data.json :as json]))

(def sb (sandbox secure-tester-without-def))

(def current-code (atom {}))

(defn gen-file-page [file]
  (when-not (.exists (java.io.File. (str "files/" file)))
    (spit (str "files/" file) ""))
  (when-not (contains? @current-code file)
    (swap! current-code assoc file (slurp (str "files/" file))))
  (html5
   [:head
    (include-css "/cm/lib/codemirror.css"
                 "/cm/theme/solarized.css"
                 "/css/cloth.css")
    (include-js "/cm/lib/codemirror.js"
                "/cm/mode/clojure/clojure.js"
                "/cm/lib/util/matchbrackets.js"
                "https://ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js"
                "js/cloth.js")]
   [:body
    [:script (str "var file = \"" file "\";")]

    [:form
     (text-area "codeArea" (get @current-code file))]

    [:div {:id "outputPanel"}
     [:div {:id "outputFiller"}]]]))


(defn forward-to-all-others [src dests msg]
  (doseq [d dests]
    (when (not= src d)
      (enqueue d msg))))

(def clients (atom {}))




(defmulti op-handler (fn [ch code-file msg] (get msg "op")))

(defmethod op-handler :default [ch code-file msg]
  (println "Unknown operation:" (get msg "op")))

(defmethod op-handler "save-file" [ch code-file msg]
  (let [file-name (str "files/" code-file)
        content   (get @current-code code-file)]
    (spit file-name content)))

(defmethod op-handler "code-change" [ch code-file msg]
  (swap! current-code assoc code-file (get msg "code"))
  (forward-to-all-others ch (get @clients code-file) (json/write-str msg)))

(defmethod op-handler "eval-all" [ch code-file msg]
  (binding [*print-length* 20]
    (let [err (java.io.StringWriter.)
          out (java.io.StringWriter.)
          ast (try (read-string (str "(do " (get @current-code code-file) "\n)"))
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
      (forward-to-all-others nil (get @clients code-file) resp))))

(defn websocket-handler [code-file ch handshake]
  (swap! clients (fn [old]
                   (assoc old code-file (conj (if-let [prev (get old code-file)]
                                                prev
                                                #{})
                                              ch))))

  (receive-all ch (fn [msg]
                    (let [obj (json/read-str msg)]
                      (op-handler ch code-file obj))))

  (on-closed ch #(swap! clients (fn [old]
                                  (assoc old code-file (disj (get old code-file) ch))))))

(defroutes server-routes
  (GET "/" [] (#'gen-file-page "index.clj"))
  (GET "/:file" [file] (#'gen-file-page file))
  (GET "/socket/:file" [file] (wrap-aleph-handler (partial websocket-handler file)))
  (files "/cm/" {:root "codemirror-3.0"})
  (files "/js/" {:root "src-js"})
  (files "/css/" {:root "css"})
  (not-found "Page not found."))

(defn log-request [req]
  (println "REQ:" (:uri req))
  (flush))

(defn wrap-logger [aleph-handler]
  (fn [resp-ch req]
    (#'log-request req)
    (aleph-handler resp-ch req)))

(start-http-server (wrap-logger (wrap-ring-handler #'server-routes)) {:port 8080 :websocket true})
