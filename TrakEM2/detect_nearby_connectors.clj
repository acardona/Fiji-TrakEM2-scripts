; 20100720 Albert Cardona
; Offers a dialog to choose a maximum distance, in calibrated units,
; and then pops up a table with all Connector pairs whose origins or targets
; lay at a distance smaller than max-distance from each other.


(ns my.detect_nearby_connectors
  (:import [ini.trakem2.display Display Connector Coordinate]
           [ini.trakem2.utils Utils]
           [ij IJ]
           [ij.gui GenericDialog]
           [javax.swing JFrame JScrollPane JTable JLabel]
           [javax.swing.table AbstractTableModel]
	   [java.awt.event MouseAdapter]
           [java.awt Dimension]))

(def get-origin
  (memoize #(.getOriginPoint % true)))

(def get-targets
  (memoize #(.getTargetPoints % true)))

(defn find-connector-neighbors
  "Returns a vector of maps, sorted by distance."
  [layer-set max-distance]
  (let [cs (vec (.getZDisplayables layer-set Connector))]
    (vec (sort
      (fn [h1 h2]
        (if (< (h1 :dist) (h2 :dist)) -1 1))
      (filter #(not (nil? %))
        (pmap
          (fn [[con1 con2]]
            (if (not (or (nil? (.getRoot con1))
                         (nil? (.getRoot con2))))
              (if-let [dist (some
                              #(if (< % max-distance) %)
                              (concat
                                [(.distance (get-origin con1) (get-origin con2))]
                                (for [t1 (get-targets con1)
                                      t2 (get-targets con2)]
                                  (.distance t1 t2))))]
                {:con1 con1 :con2 con2 :dist dist})))
          (for [i (range (count cs))
                j (range (inc i) (count cs))]
            [(cs i) (cs j)])))))))


(defn- make-model
  [nearby-cs]
  (proxy [AbstractTableModel] []
    (getColumnName [col]
      (case col
        0 ""
        1 "Connector 1"
        2 "Connector 2"
        3 "distance"))
    (getRowCount [] (count nearby-cs))
    (getColumnCount [] 4)
    (getValueAt [row col]
      (if-let [hit (get nearby-cs row)]
	(try
          (case col
            0 row
            1 (hit :con1)
            2 (hit :con2)
            3 (hit :dist))
	  (catch Exception e (.printStackTrace e)))))
    (isCellEditable [row col] false)
    (setValueAt [row col])))

(defn- make-row-listener
  "Takes a vector of hits, where each hit is a map, and returns a mouse listener for table rows, where each row shows a hit."
  [nearby-cs]
  (proxy [MouseAdapter] []
    (mousePressed [event]
      (if (= 2 (.getClickCount event))
        (let [table (.getSource event)
              row (.rowAtPoint table (.getPoint event))
              hit (get nearby-cs row)
              con1 (hit :con1)
              con2 (hit :con2)
              zds (set (.. con1 getLayerSet getZDisplayables))
              check (fn [con]
                      (let [b (contains? zds con)]
                        (if (not b)
                          (Utils/logAll (str "WARNING: can't find connector " con " anymore!")))
                        b))
              [b1 b2] (map check [con1 con2])
              show (fn [con]
                       (let [bounds (.getBoundingBox con)]
                         (.. Display getFront (center (Coordinate. (+ (.x bounds) (/ (.width bounds) 2))
                                                                   (+ (.y bounds) (/ (.height bounds) 2))
                                                                   (.getFirstLayer con)
                                                                   con)))))]
          (if (not (and b1 b2))
            (Utils/logAll "Cannot find any of the two connectors anymore.")
            ; else
            (if b1
              (show con1)
              (if b2
                (show con2)))))))))

(defn- make-table
  "Take a collection of connectors and display each in a row of a table."
  [nearby-cs max-distance unit]
  (doto (JFrame. (str "Nearby connectors " max-distance " " unit))
    (.add
      (doto (JScrollPane.
              (doto (JTable. (make-model nearby-cs))
                (.addMouseListener (make-row-listener nearby-cs))))
        (.setPreferredSize (Dimension. 500 500))))
    (.pack)
    (.setVisible true)))

(defn run
  []
  (if-let [layer-set (.. Display getFront getLayerSet)]
    (let [unit (.. layer-set getCalibration getUnit)
          gd (GenericDialog. "Detect nearby connectors")]
      (doto gd
        (.addNumericField "max distance"  100 0 10 unit)
        (.showDialog))
      (if (not (.wasCanceled gd))
        (let [max-distance (.getNextNumber gd)]
          (if (or (Double/isNaN max-distance)
                  (< max-distance 0))
            (IJ/log (str "Invalid number: " max-distance))
            ; else
            (let [nearby-cs (find-connector-neighbors layer-set max-distance)]
              (if (empty? nearby-cs)
                (IJ/log (str "No connectors found within " max-distance " " unit " of each other."))
                ; else
                (make-table nearby-cs max-distance unit)))))))))


(run)
