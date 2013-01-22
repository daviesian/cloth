(ns cloth.core
  (:use [clojure.pprint]
        [cloth.init]
        [cloth.server-state]
        [cloth.utils]
        [cloth.rpc-dispatch]
        [aleph.http]
        [lamina.core]
        [compojure.core]
        [compojure.route]
        [hiccup.core]
        [hiccup.page]
        [hiccup.form])
  (:require [clojure.data.json :as json]))

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





(defn websocket-handler [code-file ch handshake]
  (swap! clients (fn [old]
                   (assoc old code-file (conj (if-let [prev (get old code-file)]
                                                prev
                                                #{})
                                              ch))))

  (receive-all ch (fn [msg]
                    (dispatch 'cloth.rpc msg {:channel ch :code-file code-file :msg msg})))

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
