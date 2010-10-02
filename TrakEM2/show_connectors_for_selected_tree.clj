; Albert Cardona 2010
; Find all connectors for the selected treeline or areatree
; and show them as pre and post, in different colors, in the 3D viewer
; and also output their distribution of common targets

(ns t2.network
  (:import (ini.trakem2.display Display Connector Treeline Display3D AreaTree)
           (ij IJ)
           (java.awt Color)
           (customnode CustomTriangleMesh)
           (javax.vecmath Color3f)))
        

; c is a connector

(defn get-pre
  "Get all Connector instances for which tl is the origin"
  [tl cs]
  (reduce
    (fn [v c]
      (if (some #{tl} (.getOrigins c Treeline))
        (conj v c)
        v))
    []
    cs))

(defn get-post
  "Get all Connector instances for which tl is a target"
  [tl cs]
  (reduce
    (fn [v c]
      (if (some #{tl} (apply concat (.getTargets c Treeline)))
        (conj v c)
        v))
    []
    cs))

(defn TEST []
  (if-let [tl (.. Display getFront getActive)]
    (let [univ (.. Display3D (get (.getLayerSet tl)) getUniverse)
           cs (.. tl getLayerSet (getZDisplayables Connector))]
      (println "pre:\n" (get-pre tl cs))
      (println "post:\n" (get-post tl cs)))))

(defn get-pre-targets
  [coll-pre]
  (reduce
    (fn [m target]
      (assoc m target
               (if-let [num (m target)]
                 (inc num)
                 1)))
    {}
    (apply concat (map #(apply concat (.getTargets % Treeline)) coll-pre))))

(defn get-post-origins
  [coll-post]
  (reduce
    (fn [m origin]
      (assoc m origin
               (if-let [num (m origin)]
                 (inc num)
                 1)))
    {}
    (apply concat (map #(.getOrigins % Treeline) coll-post))))

(defn get-name [tl]
  (str
    (if-let [pt (.. tl getProject (getShortMeaningfulTitle tl))]
      pt
      tl)))

(defn analyze
  "Count number of pre- and post- synapses, and list who the pre/post are and how many contacts each makes with tl."
  [tl]
  (let [cs (.findConnectors tl)
        pre (get cs 0)
        post (get cs 1)
        pre-targets (get-pre-targets pre)
        post-origins (get-post-origins post)]
    (println "Connectors for" (get-name tl))
    (println "Found" (count pre) "pre -- origins")
    (println "Found" (count post) "post -- targets")
    (println "Found known pre targets:" (count pre-targets))
    (doseq [[target num] pre-targets]
      (println "  " (get-name target) " : " num))
    (println "Found known post origins:" (count post-origins))
    (doseq [[origin num] post-origins]
      (println "  " (get-name origin) " : " num))
    (println "Distribution of number of synapses per downstream partner:"
      (reduce
        (fn [m [tl num-contacts]]
          (if-let [sum (m num-contacts)]
            (assoc m num-contacts (inc sum))
            (assoc m num-contacts 1)))
        (sorted-map)
        pre-targets))
    [pre post pre-targets post-origins]))

; (analyze (.. Display getFront getActive))


(defn get-mesh
  "Returns a Tree.MeshData, with a list of Point3f vertices and a list of Color3f, one for each vertex."
  [displ scale]
  (cond
     (= Connector (class displ)) (.generateMesh displ scale 12)
     (= Treeline (class displ))  (.generateSkeleton displ scale 12)
     (= AreaTree (class displ))  (.generateMesh displ scale 1)))

(defn add-to-3d
  [univ displ color scale]
  (let [content (.createContent univ (CustomTriangleMesh. (.verts (get-mesh displ scale))
                                                          (Color3f. color) 0)
                                                          (str (.getTitle displ) " #" (.getId displ)))]
    (.setLocked content true)
    (.addContent univ content)))

(defn visualize
  "Run the analysis and also show in 3D"
  [tl]
  (let [[pre post pre-targets post-origins] (analyze tl)
         d3d (.. Display3D (get (.getLayerSet tl)))
         show (fn [coll color]
                (doseq [displ coll]
                  (add-to-3d (.getUniverse d3d) displ color 1)))]
    (show pre Color/red)
    (show post Color/blue)
    ;(show [tl] Color/yellow)
    ))
    

(if-let [front (Display/getFront)]
  (if-let [active (.getActive front)]
    (visualize active)
    (IJ/log "No object selected in the front display!"))
  (IJ/log "No displays are open!"))
