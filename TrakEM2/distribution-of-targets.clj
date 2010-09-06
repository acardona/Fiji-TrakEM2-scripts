; Albert Cardona 2010-09-04 at Brighton, UK
; For a given tree, find all its downstream target trees.

(ns my.targets
  (:import [ini.trakem2 ControlWindow]
           [ini.trakem2.display Connector Display3D Treeline]
           [javax.vecmath Color3f]
           [java.awt Color]))

(defn- is-treeline [x] (= (class x) Treeline))

(defn- treelines [col] (filter is-treeline col))

(defn find-connector-targets
  "Return the set of unique Treeline targets of the connector."
  [connector]
  (treelines (reduce #(into %1 %2) #{} (.getTargets connector Treeline))))

(defn- freq-distrib
  [freq]
  ; sm is the sorted map of number of synapses vs number of treelines that make that many synapses
  ; m is the sorted map of number of synapses vs the vector of treelines that make that many synapses
  ; ve is the value of the entry of treeline id vs number of occurrences of that id in freq.
  ; n is the number of treelines that make ve synapses, so far.
  (let [distrib (reduce
                  (fn [m [k v]]
                    (let [n (m v)]
                      (assoc m v (if n
                                   (conj n (.getId k))
                                   [(.getId k)]))))
                  {}
                  freq)]
    [(reduce (fn [m [k v]]
               (assoc m k (count v)))
             (sorted-map)
             distrib)
     (sort distrib)]))

(defn target-distribution
  "For a given treeline, find out the distribution of synapses onto target treelines.
  I.e. how many synapses each target receives."
  [tl-id layerset]
  (let [origin (.findById layerset tl-id)
        outgoing (set (first (.findConnectors origin)))]
    (freq-distrib (frequencies (apply concat (map find-connector-targets outgoing))))))

(defn find-connector-origins [connector]
  "Return the set of unique Treline origins of the connector. Should be just one."
  (into #{} (treelines (.getOrigins connector Treeline))))

(defn source-distribution
  "For a given treeline, find out the distribution of synaptic contacts from its upstream treelines."
  [tl-id layerset]
  (let [origin (.findById layerset tl-id)
        incomming (set (second (.findConnectors origin)))]
    (freq-distrib (frequencies (apply concat (map find-connector-origins incomming))))))

; Same as above:
(defn source-distribution-2
  "For a given treeline, find out the distribution of synaptic contacts from its upstream treelines."
  [tl-id layerset]
    (->> (.findById layerset tl-id)
      (.findConnectors)
      (second)
      (map find-connector-origins)
      (apply concat)
      (frequencies)
      (freq-distrib)))
; the above is like a sequence monad!

(defn show [id]
  (let [p (ini.trakem2.ControlWindow/getActive)
        d (.findById (.getRootLayerSet p) id)]
    (Display3D/show (.findProjectThing p d))))

(defn show-in-color [ids color]
  (let [p (ini.trakem2.ControlWindow/getActive)
        layerset (.getRootLayerSet p)
        ds (map #(.findById layerset %) ids)
        d3d (Display3D/get layerset)
        cs (map #(let [c (.call (.createMesh d3d (.findProjectThing p %) % 1))]
                   (.setColor c (Color3f. color))
                   c)
                ds)]
    (.addContent d3d cs)))

(defn show-preferred
  "Show in 3D the preferred treelines. selection is a set of the preferred to show."
  [distrib selection color]
  (doseq [[n tls] (filter #(contains? selection (key %)) distrib)]
    (println (str "Showing: synapses=" n " ids: " tls))
    (show-in-color tls color)))

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
  ;(println (target-distribution 71887 layerset)) ; lch5-1 left
  ;(println (target-distribution 75408 layerset)) ; lch5-1 right
  ;
  ;(println (source-distribution 87269 layerset))
  ;(println (map (fn [col] (vec (map #(.getId %) col)))
  ;              (find-connectors (.findById layerset 75408) (.findById layerset 87269))))

  ;(show-preferred (second (target-distribution 71887 layerset)) (set (range 5 15)) Color/yellow)
  ;(show-preferred (second (target-distribution 75408 layerset)) (set (range 5 15)) Color/red)
  (println (map (fn [col] (vec (map #(.getId %) col)))
                (find-connectors (.findById layerset 71887) (.findById layerset 87269))))
)
