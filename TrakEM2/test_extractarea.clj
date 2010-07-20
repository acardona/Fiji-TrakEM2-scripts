(ns my.test
  (:import
    [ini.trakem2.utils AreaUtils]
    [ij.plugin.filter ThresholdToSelection]
    [ij.process ImageProcessor]
    [ij ImagePlus IJ]
    [java.awt Rectangle]
    [java.awt.geom Area]
    [ij.gui ShapeRoi]))

(defn ij-way [#^ImageProcessor ip]
  (let [#^ImagePlus imp (ImagePlus. "" ip)
        tts (ThresholdToSelection.)]
    (doto ip
      (.setThreshold 1 255 ImageProcessor/NO_LUT_UPDATE))
    (doto tts
      (.setup "" imp)
      (.run ip))
    (.getRoi imp)))

(defn t2-way [#^ImageProcessor ip]
  (let [a1 (AreaUtils/extractArea ip (float 0))
        a2 (Area. (Rectangle. (.getWidth ip) (.getHeight ip)))]
    (doto a2
      (.subtract a1))
    (ShapeRoi. a2)))
    

(let [imp (IJ/getImage)]
   ;(.setRoi imp (ij-way (.getProcessor imp))))
   (.setRoi imp (t2-way (.getProcessor imp))))