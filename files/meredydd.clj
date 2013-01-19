




(defmacro prexpr [x]
  `(println "Expression was" ~(pr-str x)))




(prexpr (hello world))

