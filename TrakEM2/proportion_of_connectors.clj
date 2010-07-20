; Albert Cardona 2010
; List how many of the connectors originated at a given Displayable id
; have targets onto any of the Displayable listed by id.

(ns my.connectors
  (:import [ini.trakem2.display Display Treeline]))

(defn find-connector-targets [connector]
  "Return the set of unique Treeline targets of the connector."
  (reduce #(into %1 %2) #{} (.getTargets connector Treeline)))

(defn proportion
  [origin-id col-target-ids]
  (let [layerset (.. Display getFront getLayerSet)
         origin (.findById layerset origin-id)
         targets (reduce
                   #(conj %1 (.findById layerset %2))
                   #{}
                   col-target-ids)
         outgoing (first (.findConnectors origin))
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
    (println "Origins at which any of" col-target-ids "participate:\n" distrib)
    (println "Proportion:" (reduce #(+ %1 (val %2)) 0 (dissoc distrib 0)) "/" (count outgoing))
    (println "Proportion of total postsynaptic partners of" origin-id ":" total-post-from-target "/" total-post-reconstructed " -- total:" total-post)))



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

; Pick the active tree as the reference,
; and all other selected trees as the targets
(if-let [front (Display/getFront)]
  (if-let [[active & others] (.. front getSelection getSelected)]
    (proportion (.getId active) (map #(.getId %) others))
    (IJ/log "No object selected in the front display!"))
  (IJ/log "No displays are open!"))
