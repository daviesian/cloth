(defn to-upper [s]
  (.toUpperCase s))

(defn my-reverse [[x & xs]]
  (if (nil? xs)
    [x]
    (concat (my-reverse xs) [x])))

(defn join-strs [coll]
  (let [head (first coll)
        tail (rest coll)]
    (if (empty? tail)
      head
      (str head (join-strs tail)))))

(defn separate [sep coll]
  (let [head (first coll)
        tail (rest coll)]
    (if (empty? tail)
      head
      (cons head 
            (cons sep 
                  (separate sep tail))))))
(join-strs 
 (separate " "
  (map to-upper 
       (my-reverse ["The" "quick" "brown" "fox" "jumped" "over" "the" "lazy" "dog"]))))

(map count ["The" "quick" "brown" "fox" "jumped" "over" "the" "lazy" "dog"])
(filter (fn [s] (> 4 (count s))) ["The" "quick" "brown" "fox" "jumped" "over" "the" "lazy" "dog"])
