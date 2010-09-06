; Albert Cardona 2010
; List how many of the connectors originated at a given Displayable id
; have targets onto any of the Displayable listed by id.

(ns my.connectors
  (:import [ini.trakem2.display Display Treeline]))

(defn find-connector-targets [connector]
  "Return the set of unique Treeline targets of the connector."
  (reduce #(into %1 %2) #{} (.getTargets connector Treeline)))

(defn proportion-from-ids
  ([origin-id col-target-ids]
    (proportion-from-ids origin-id col-target-ids (.. Display getFront getLayerSet)))
  ([origin-id col-target-ids layerset]
    (let [origin (.findById layerset origin-id)
           targets (reduce
                     #(conj %1 (.findById layerset %2))
                     #{}
                     col-target-ids)]
      (proportion origin targets))))

(defn proportion
  [origin targets]
  (println origin)
  (let [outgoing (first (.findConnectors origin))
        distrib (reduce
                  (fn [m c]
                    (let [n-inter (count (clojure.set/intersection (find-connector-targets c) targets))]
                      (if-let [sum (m n-inter)]
                        (assoc m n-inter (inc sum))
                        (assoc m n-inter 1))))
              (sorted-map)
              outgoing)
        total-post (reduce #(+ %1 (.getTargetCount %2)) 0 outgoing)
        total-post-reconstructed (reduce #(+ %1 (count (find-connector-targets %2))) 0 outgoing)
        total-post-from-target (reduce + (map (fn [[k v]] (* k v)) distrib))]
    (println origin)
    (println targets)
    (println (count outgoing))
    (println "Origins at which any of" (map #(.getId %) targets) "participate:\n" distrib)
    (println "Proportion:" (reduce #(+ %1 (val %2)) 0 (dissoc distrib 0)) "/" (count outgoing))
    (println "Proportion of total postsynaptic partners of" (.getId origin) ":" total-post-from-target "/" total-post-reconstructed " -- total:" total-post)))



; Origin: lch5-1 #71887
; Six primary targets:
;  - intersegmental from posterior #83486  -- 9
;  - drunken 1 #79954 -- 11
;  - basket 1 #77737  -- 10
;  - ipsilateral #81056 -- 11
;  - exit posteriorly only, no cell body #81955 -- 12
;  - cross A-P no cell body #81196 -- 9
;
;(proportion 71887 [83486 79954 77737 81056 81955 81196])

(defn by-selection
  "Pick the active tree as the reference,
  and all other selected trees as the targets."
  []
  (if-let [front (Display/getFront)]
    (if-let [[active & others] (.. front getSelection getSelected)]
      (proportion-from-ids (.getId active) (map #(.getId %) others))
      (ij.IJ/log "No object selected in the front display!"))
    (ij.IJ/log "No displays are open!")))

(defn by-id
  [tl-id]
  (let [tls (reduce
              #(assoc %1 (.getId %2) %2)
              {}
              (filter #(not (nil? (.getRoot %))) (.. ini.trakem2.ControlWindow getActive getRootLayerSet (getZDisplayables Treeline))))]
    (doseq [k (keys tls)]
      (if (= k tl-id) (println "found" k)))
    (proportion
       ;FAILS?(tl-id tls)
       (val (first (filter #(= tl-id (key %)) tls)))  ; so in the map, the equality of the number as key fails
       (vals tls))))

(defn by-id-2
  [tl-id]
  (let [layerset (.. ini.trakem2.ControlWindow getActive getRootLayerSet)
         tls (map #(.getId %)
                 (filter #(not (nil? (.getRoot %))) (.getZDisplayables layerset Treeline)))]
    (proportion-from-ids tl-id tls layerset)))


;(by-id-2 71887) ; lch5-1 left
;(by-id-2 75408) ; lch5-1 right
;(proportion-from-ids 71887 [83486 79954 77737 81056 81955 81196] (.. ini.trakem2.ControlWindow getActive getRootLayerSet))