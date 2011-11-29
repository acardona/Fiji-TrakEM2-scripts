; Albert Cardona 2011-11-29
; Give a CSV file, load sets of 3 columns as x,y,r, where r is
; painted a colored band around the line defined by x,y.
; Look at the bottom for an example.


(ns my.plot
  (:import [java.io BufferedReader InputStreamReader FileInputStream FileOutputStream OutputStreamWriter]
           [java.awt Color Dimension Rectangle BasicStroke]
           [javax.swing JFrame JMenuItem]
           [java.awt.event ActionListener MouseAdapter]
           [org.jfree.chart ChartFactory ChartPanel JFreeChart]
           [org.jfree.chart.axis NumberAxis]
           [org.jfree.chart.plot XYPlot PlotOrientation]
           [org.jfree.chart.renderer.xy DeviationRenderer]
           [org.jfree.data.xy XYDataset YIntervalSeries YIntervalSeriesCollection]
           [org.jfree.ui RectangleInsets]
           [fiji.util.gui GenericDialogPlus]
           [ij Prefs]
           [ij.io SaveDialog]
           [org.apache.batik.dom GenericDOMImplementation]
           [org.apache.batik.svggen SVGGraphics2D]))

(defmacro read-lines
  [filename f]
  `(with-open [br# (BufferedReader.
                     (InputStreamReader.
                       (FileInputStream. ~filename)))]
    (~f (line-seq br#))))

(defn parse-cell
  [^String text
   default-value]
  (try
    (Double/parseDouble (.trim text))
    (catch NumberFormatException nfe
      (println "Using 0 for cell: " text)
      default-value)))

(defn parse-csv
  "Returns a CSV file as a list of lists of values.
   Defaults to the comma as a separator."
  [filename opts]
  (let [skip (or (:skip opts) 0) ; rows to skip from the top
        separator (or (:separator opts) ",")
        f (fn [lines]
            (reduce
              (fn [v nums]
                (conj v nums))
              []
              (map (fn [^String line]
                     (map (fn [^String cell] (parse-cell cell 0))
                          (seq (.split line separator))))
                   (drop skip lines))))]
    (read-lines filename f)))

(defn as-columns
  "Reslice."
  [rows]
  (map (fn [i]
         (map #(nth % i) rows))
       (range (count (first rows)))))

(defn ^YIntervalSeries as-y-interval-series
  [data spec]
  (let [xs (nth data (:x spec))
        ys (take (count xs) (nth data (:y spec))) ; ensure same size as xs
        rs (take (count xs) (nth data (:r spec))) ; idem
        y-series (YIntervalSeries. (or (:title spec) "Series"))]
    (loop [X xs
           Y ys
           R rs]
      (when (next X)
        (let [x (first X)
              y (first Y)
              r (first R)] 
          (.add y-series x y (- y r) (+ y r))
          (recur (rest X) (rest Y) (rest R)))))
    y-series))

(defn ^XYDataset create-dataset
  [filename & options]
  (let [opts (when options (apply assoc {} options))
        data (as-columns (parse-csv filename opts))
        series (or (:series opts) [{:x 0 :y 1 :r 2}])
        dataset (YIntervalSeriesCollection.)]
    (doseq [s series]
      (.addSeries dataset (as-y-interval-series data s)))
    dataset))

(defn create-chart
  [^XYDataset dataset
   series
   ^Color background ; background color of the plot
   ^String title
   ^String x-label
   ^String y-label]
  (let [chart (ChartFactory/createXYLineChart title x-label y-label dataset PlotOrientation/VERTICAL true true false)
        plot (.getPlot chart)
        renderer (DeviationRenderer. true false)]
    (.setBackgroundPaint chart Color/white)
    (.setInsets plot (RectangleInsets. (double 5) (double 5) (double 5) (double 20)))
    (.setBackgroundPaint plot background)
    (.setAxisOffset plot (RectangleInsets. (double 5) (double 5) (double 5) (double 5)))
    (.setDomainGridlinePaint plot ^java.awt.Paint Color/white) ; silly cast needed
    (.setRangeGridlinePaint plot Color/white)
    (.setRenderer plot renderer)
    (dotimes [i (count series)]
      (let [s (nth series i)]
        (.setSeriesStroke renderer (int i)
                          (BasicStroke. (float (or (:stroke s) 1)) 1 1))
        (.setSeriesPaint renderer (int i)
                         (apply #(Color. %1 %2 %3 %4) (or (:stroke-color s) [255 0 0 255])))
        (.setSeriesFillPaint renderer (int i)
                             (apply #(Color. %1 %2 %3 %4) (or (:color s) [255 0 0 255])))))
    (doto (.getRangeAxis plot)
      (.setAutoRangeIncludesZero false))
    chart))

(defn save-as-SVG
  [^JFreeChart chart
   ^Rectangle bounds]
  (let [sd (SaveDialog. "Save as SVG" nil ".svg")
        filename (.getFileName sd)]
    (when filename
      (let [path (str (.getDirectory sd)
                      (if (.endsWith filename ".svg") filename (str filename ".svg")))
            ^SVGGraphics2D svg (SVGGraphics2D.
                                 (.createDocument
                                   (GenericDOMImplementation/getDOMImplementation)
                                   nil "svg" nil))]
        (.draw chart svg bounds)
        (with-open [out (OutputStreamWriter.  (FileOutputStream. path) "UTF-8")]
          (.stream svg out true)
          (.flush out)
          (println "SVG file saved at" path))))))

(defn plot
  [filename & options]
  (let [opts (apply assoc {} options)
        cp (ChartPanel.
             (create-chart (apply create-dataset filename options)
                           (:series opts)
                           (apply #(Color. %1 %2 %3) (:background opts))
                           (:title opts)
                           (:x-label opts)
                           (:y-label opts)))]
    (.setPopupMenu cp
                   (doto (.getPopupMenu cp)
                     (.addSeparator)
                     (.add (doto (JMenuItem. "Save as SVG")
                             (.addActionListener
                               (reify ActionListener
                                 (actionPerformed [this event]
                                   (save-as-SVG (.getChart cp) (.getBounds cp)))))))))
    (doto (JFrame. "Plot")
      (.setContentPane
        (doto cp
          (.setPreferredSize (Dimension. 500 270))))
      (.pack)
      (.setVisible true))))

(comment
(plot "/home/albert/lab/Marta/crabspeed_data.csv"
      :skip 2 ; Lines to skip from the top of the CSV file
      :separation ","
      :title "Title"
      :x-label "X-axis"
      :y-label "Y-axis"
      :background [255 255 255]
      ; Colors are in RGBA
      :series [{:x 0 :y 1 :r 4 :title "First" :color [255 0 0 128] :stroke 1.0}
               {:x 7 :y 8 :r 11 :title "Second" :color [0 0 255 128] :stroke 1.0}
               {:x 14 :y 15 :r 18 :title "Third" :color [0 200 0 128] :stroke 1.0}])
)

(defn ^String make-key
  [k]
  (str "act.plot.deviation." (.substring (str k) 1)))

(defn ^String retrieve
  [k
   ^String default-value]
  (Prefs/get (make-key k)
             default-value))

(defn ^String store
  [k
   ^String value]
  (Prefs/set (make-key k) (str value))
  value)

(defn choose
  []
  (let [^GenericDialogPlus gd (GenericDialogPlus. "Plot CSV")]
    (doto gd
      (.addFileField "Choose *.csv file" (retrieve :filename "/home/albert/lab/Marta/crabspeed_data.csv") 50)
      (.addSlider "Skip lines" 1 20 (int (eval (read-string (retrieve :skip "2")))))
      (.addStringField "Separation" (retrieve :separation ","))
      (.addStringField "Title" (retrieve :title "Plot") 50)
      (.addStringField "X-axis label" (retrieve :x-label "X-axis") 50)
      (.addStringField "Y-axis label" (retrieve :y-label "Y-axis") 50)
      (.addStringField "Background color" (retrieve :background "[255 255 255]") 11)
      (.addMessage "Series (one per row):")
      (.addTextAreas (retrieve :series (str
                       "{:x 0 :y 1 :r 4 :title \"First\" :color [255 0 0 128] :stroke 1.0 :stroke-color [255 0 0 128]}\n"
                       "{:x 7 :y 8 :r 11 :title \"Second\" :color [0 0 255 128] :stroke 1.0 :stroke-color [0 0 255 128]}\n"
                       "{:x 14 :y 15 :r 18 :title \"Third\" :color [0 200 0 128] :stroke 1.0 :stroke-color [0 200 0 128]}"))
                     nil
                     5 90)
      (.showDialog))
    (when-not (.wasCanceled gd)
      (plot (store :filename (.getNextString gd))
            :skip (int (store :skip (.getNextNumber gd)))
            :separation (store :separation (.getNextString gd))
            :title (store :title (.getNextString gd))
            :x-label (store :x-label (.getNextString gd))
            :y-label (store :y-label (.getNextString gd))
            :background (eval (read-string (store :background (.getNextString gd))))
            :series (eval (read-string (str \[ (store :series (.getText (.getTextArea1 gd))) \])))))))

(choose)
