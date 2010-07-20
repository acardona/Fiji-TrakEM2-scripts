; Albert Cardona 2010
; Output in simplified format (.sif) the network consisting
; of a given neuron and all its downstream and upstream targets
; The .sif file opens in a text window

(ns my.first_order_network
  (:import [ini.trakem2.display Display Treeline]
           [ij.text TextWindow]))

(defn find-connector-targets [connector]
  "Return the set of unique Treeline targets of the connector."
  (reduce #(into %1 %2) #{} (.getTargets connector Treeline)))


(defn first-order-network
  "Find out the instance for tl-id, its outgoing and incomming connectors,
   and then from those all treelines.
   Then, using the outgoing and incomming connectors,
   output the relationships, but filtering for only those treelines found."
  [tl-id]
  (let [tl (.. Display getFront getLayerSet (findById tl-id))
        [outgoing incomming] (.findConnectors tl)
        ts (into
             #{}
             (concat
               [tl]
               (reduce #(into %1 (find-connector-targets %2)) #{} outgoing)
               (reduce #(into %1 (into #{} (.getOrigins %2 Treeline))) #{} incomming)))]
    (println (count outgoing) (count incomming))
    (println "Total Treelines found:" (count ts))
    (println "tl in ts:" (contains? (into #{} ts) tl))
    (TextWindow. "Graph" 
      (with-out-str
        (do
          (doseq [c outgoing]
            (if-let [targets (find-connector-targets c)]
              (doseq [t targets]
                (println tl-id "pd" (.getId t)))))
          (doseq [c incomming]
            (let [origin (first (.getOrigins c Treeline)) ; there should only be one
                  targets (clojure.set/intersection (find-connector-targets c) ts)]
              (if origin (.. System out (println (str "origin: " (.getId origin)))))
              (if targets (.. System out (println (str "targets: " (count targets)))))
              (if (and (not (nil? origin)) (not (nil? targets)))
                (doseq [t targets]
                  (println (.getId origin) "pd" (.getId t))))))))
      500 500)))



; lch5-1 left is 71887
; (first-order-network 71887)

(if-let [front (Display/getFront)]
  (if-let [active (.getActive front)]
    (first_order_network (.getId active))
    (IJ/log "No object selected in the front display!"))
  (IJ/log "No displays are open!"))
