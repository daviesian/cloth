(defproject cloth "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [compojure "1.1.5"]
                 [aleph "0.3.0-beta7"]
                 [hiccup "1.0.0"]
                 [org.clojure/data.json "0.2.0"]
                 [clojail "1.0.3"]
                 [jayq "2.2.0"]
                 [crate "0.2.4"]]

  :plugins [[lein-cljsbuild "0.3.0"]]
  :cljsbuild {:builds [{:source-paths ["src-cljs"]
                        :compiler     {:output-to "js-generated/main.js"
                                       :optimizations :whitespace
                                       :pretty-print true}}]}

  :main cloth.core)
