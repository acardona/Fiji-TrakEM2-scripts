; Albert Cardona 2010
; Obtain general statistics on treelines and connectors
; that represent neuronal circuitry

(ns my.statistics
  (:import [ini.trakem2.display Display Connector Treeline]
           [ij.measure Calibration]
           [javax.vecmath Point3f]
           [java.awt.geom AffineTransform]))

;(defn as-point [node cal]
;  (let [#^Point3f p (.asPoint node)]
;    (set! (.x p) (* (.x p) (.pixelWidth cal)))
;    (set! (.y p) (* (.y p) (.pixelHeight cal)))
;    (set! (.z p) (* (.z p) (.pixelWidth cal))) ; not pixelDepth
;    p))


;(defn distance [nd1 nd2 cal]
;  (.distance (as-point nd1 cal) (as-point nd2 cal)))

(defn distance!
  "Takes two nodes and a function that transforms in place the Point3f representation of the node."
  [nd1 nd2 to-world]
  (.distance (to-world (.asPoint nd1)) (to-world (.asPoint nd2))))

(defn- transform! [tl #^Calibration cal]
  "Returns a function that can 2D-affine transform and calibrate in place a Point3f instance
   according to the AffineTransform of the Treeline, and works thread-unsafe."
  (let [#^floats fp (make-array Float/TYPE 2)
        #^AffineTransform aff (.getAffineTransform tl)
        fun (fn [#^Point3f p]
              (aset fp 0 (.x p))
              (aset fp 1 (.y p))
              (.transform aff fp 0 fp 0 1)
              (set! (.x p) (* (get fp 0) (.pixelWidth cal)))
              (set! (.y p) (* (get fp 1) (.pixelHeight cal)))
              (set! (.z p) (* (.z p) (.pixelWidth cal))) ; not pixelDepth
              p)]
    fun))

(defn measure-cable-length [tl #^Calibration cal]
  (let [to-world (transform! tl cal)]
    (reduce
      (fn [len node]
        (+ len (if-let [parent (.getParent node)]
                 (distance! node parent to-world)
                 0)))
      0
      (.. tl getRoot getSubtreeNodes))))

(defn measure-lower-bound-cable-length
  "Return the sum of the distances between end points and their parent branch points,
   between branch points and their parent branch points,
   and between the first branch point and the root."
  [tl #^Calibration cal]
  (let [to-world (transform! tl cal)]
    (reduce
      (fn [len node]
        (+ len (distance! node (.findPreviousBranchOrRootPoint node) to-world)))
      0
      (filter #(and (not (nil? (.getParent %)))
                    (not (= 1 (.getChildrenCount %))))
              (.. tl getRoot getSubtreeNodes)))))


(let [layerset (.. Display getFront getLayerSet)
       #^Calibration cal (.getCalibration layerset)
       cs (.getZDisplayables layerset Connector)
       ts (.getZDisplayables layerset Treeline)]
  (println "Number of arbors:" (count ts))
  (println "Number of presynaptic sites:" (count cs))
  (println "Number of postsynaptic sites:" (reduce #(+ %1 (.getTargetCount %2)) 0 cs))
  (println "Total cable length:" (reduce #(+ %1 (measure-cable-length %2 cal)) 0 ts))
  (println "Total lower-bound cable length:" (reduce #(+ %1 (measure-lower-bound-cable-length %2 cal)) 0 ts)))
