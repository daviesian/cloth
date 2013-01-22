(ns cloth.server-state
  (:use [clojail.core :only [sandbox]]
        [clojail.testers :only [secure-tester-without-def]]))

(def sb (sandbox secure-tester-without-def))

(def current-code (atom {}))

(def clients (atom {}))
