; Albert Cardona 2010
; Grab all connectors and obtain statistics

(ns my.connectors
  (:import [ini.trakem2.display Display Treeline Connector]))

(let [cs (.. Display getFront getLayerSet (getZDisplayables Connector))]
  (println "Connectors: " (count cs))
  (let [post (reduce
                (fn [m c]
                  (let [i (.getTargetCount c)
                        sum (m i)]
                    (if sum
                      (assoc m i (inc sum))
                      (assoc m i 1))))
                {}
                cs)]
    (println "Postsynaptic partners:" (into (sorted-map) post))
    (println "Total postsynaptic partners:" (reduce (fn [sum [k v]] (+ sum (* k v))) 0 post))))
