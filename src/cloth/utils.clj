(ns cloth.utils
  (:use [lamina.core]))

(defn forward-to-all-others [src dests msg]
  (doseq [d dests]
    (when (not= src d)
      (enqueue d msg))))
