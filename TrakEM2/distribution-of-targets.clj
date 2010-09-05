; Albert Cardona 2010-09-04 at Brighton, UK
; For a given tree, find all its downstream target trees.

(ns my.targets
  (:import [ini.trakem2 ControlWindow]
           [ini.trakem2.display Connector Treeline]))

(defn find-connector-targets [connector]
  "Return the set of unique Treeline targets of the connector."
  (reduce #(into %1 %2) #{} (.getTargets connector Treeline)))

(defn target-distribution
  "For a given treeline, find out the distribution of synapses onto target treelines.
  I.e. how many synapses each target receives."
  [tl-id layerset]
  (let [origin (.findById layerset tl-id)
        targets (filter #(not (nil? (.getRoot %))) (.getZDisplayables layerset Treeline))
        outgoing (first (.findConnectors origin))]
    (reduce
      (fn [sm e]
        (if-let [v (sm (val e))]
          (assoc sm (val e) (inc v))
          (assoc sm (val e) 1)))
      (sorted-map)
      (frequencies (apply concat (map find-connector-targets outgoing))))))

(let [layerset (.. ControlWindow getActive getRootLayerSet)]
  (println (target-distribution 71887 layerset))
  (println (target-distribution 75408 layerset)))