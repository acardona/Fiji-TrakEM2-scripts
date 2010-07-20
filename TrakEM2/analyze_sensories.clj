; Albert Cardona 2010
; Find sensories and which sensory is connecting onto which other sensory
(ns my.sensories
  (:import [ini.trakem2.display Display Treeline]))

(defn find-nodes [pt type]
  (.findChildrenOfTypeR pt type))

(defn find-connector-targets [connector]
  "Return the set of unique Treeline targets of the connector."
  (reduce #(into %1 %2) #{} (.getTargets connector Treeline)))

(defn find-targets-within
  "Get all connectors for t, and find out if any of its targets
   are part of the collection of potential targets.
   Returns an empty list if none, or the list of unique targets that are part of potential-targets."
  [t potential-targets]
  (clojure.set/intersection
    potential-targets
    (reduce
      #(clojure.set/union %1 (find-connector-targets %2))
      #{}
      (first (.findConnectors t)))))

(defn count-synapses
  "Count how many synapses t1 makes onto t2"
  [t1 t2]
  (reduce
    (fn [sum c]
      (if (contains? (find-connector-targets c) t2)
        (inc sum)
        sum))
    0
    (first (.findConnectors t1))))


(let [project (.. Display getFront getProject)
      n-root (.getRootProjectThing project)
      layerset (.getRootLayerSet project)
      n-neuropile (first (find-nodes n-root "neuropile"))
      n-sensories (reduce concat (map
      				   #(seq (find-nodes % "neuron"))
      				   (find-nodes n-neuropile "sensory")))
      ; A map of ProjectThing "neuron" vs. child Treeline instance
      sensories (reduce 
      	          (fn [m entry]
                    (assoc m (first entry) (second entry)))
      		    {}
      		    (map
      		      (fn [pt] [pt (.getObject (first (find-nodes pt "treeline")))])
      		      n-sensories))
      t-sensories (set (vals sensories))]
  (println "Sensories:" (count t-sensories))
  ;(println (map #(.getTitle %) (keys t-sensories)))
  ; For each sensory treeline, find out:
  ; 1 - If it synapses onto any other sensory
  ; 2 - Who are the sensories it contacts
  (doseq [[k v]
   (reduce
    (fn [m [pt t]]
      ;(println pt "-->" t)
      (assoc m pt (find-targets-within t t-sensories)))
    {}
    sensories)]
   (if (not (empty? v))
     (let [t1 (.getObject (first (.findChildrenOfType k "treeline")))]
       (println
         (.getTitle k) (str \# (.getId t1))
         " ==> "
         (map #(str (.getTitle (.getParent (.findProjectThing (.getProject k) %))) " #" (.getId %) " [" (count-synapses t1 %) "] || ") v)))))
  
  )
      
