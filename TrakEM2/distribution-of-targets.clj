; Albert Cardona 2010-09-04 at Brighton, UK
; For a given tree, find all its downstream target trees.

(ns my.targets
  (:import [ini.trakem2 ControlWindow]
           [ini.trakem2.display Connector Treeline]))

(defn find-connector-targets
  "Return the set of unique Treeline targets of the connector."
  [connector]
  (reduce #(into %1 %2) #{} (.getTargets connector Treeline)))

(defn- freq-distrib
  [freq]
  ; sm is the sorted map of number of synapses vs number of treelines that make that many synapses
  ; m is the sorted map of number of synapses vs the vector of treelines that make that many synapses
  ; ve is the value of the entry of treeline id vs number of occurrences of that id in freq.
  ; n is the number of treelines that make ve synapses, so far.
  (reduce
    (fn [[sm m] e]
      (let [ve (val e)
            n (sm ve)]
        (if n
          [(assoc sm ve (inc n))
           (assoc m ve (conj (m ve) (.getId (key e))))]
          ; else, new entries
          [(assoc sm ve 1)
           (assoc m ve [(.getId (key e))])])))
    [(sorted-map) (sorted-map)]
    freq))

(defn target-distribution
  "For a given treeline, find out the distribution of synapses onto target treelines.
  I.e. how many synapses each target receives."
  [tl-id layerset]
  (let [origin (.findById layerset tl-id)
        targets (filter #(not (nil? (.getRoot %))) (.getZDisplayables layerset Treeline))
        outgoing (first (.findConnectors origin))]
    (freq-distrib (frequencies (apply concat (map find-connector-targets outgoing))))))

(defn find-connector-origins [connector]
  "Return the set of unique Treline origins of the connector. Should be just one."
  (into #{} (.getOrigins connector)))

(defn source-distribution
  "For a given treeline, find out the distribution of synaptic contacts from its upstream treelines."
  [tl-id layerset]
  (let [origin (.findById layerset tl-id)
        sources (filter #(not (nil? (.getRoot %))) (.getZDisplayables layerset Treeline))
        incomming (second (.findConnectors origin))]
    (freq-distrib (frequencies (apply concat (map find-connector-origins incomming))))))

(defn show [id]
  (let [p (ini.trakem2.ControlWindow/getActive)
        d (.findById (.getRootLayerSet p) id)]
    (ini.trakem2.display.Display3D/show (.findProjectThing p d))))

(defn find-connectors
  "From tl-a to tl-b, and from tl-b to tl-a."
  [tl-a tl-b]
  (let [[outgoing incomming] (.findConnectors tl-a)
         search (fn [cs f]
                  (reduce
                    (fn [found c]
                      (if (contains? (f c) tl-b)
                        (conj found c)
                        found))
                    []
                    cs))]
    [(search outgoing find-connector-targets)
     (search incomming find-connector-origins)]))


(let [layerset (.. ControlWindow getActive getRootLayerSet)]
  ;(println (target-distribution 71887 layerset))
  ;(println (target-distribution 75408 layerset))
  ;(println (source-distribution 87269 layerset))
  (println (find-connectors (.findById layerset 75408) (.findById layerset 87269)))
)

  ; There is an error somewhere: one way reports 7, the other way reports 6.
  ; find-connectors also reports 6, agreeing with source-distribution
  ; so perhaps target-distribution is wrong, but I can't see how it is wrong


  