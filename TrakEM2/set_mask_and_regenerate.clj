; Albert Cardona 2010
; For every image in every layer, if the image is missing a mask,
; set a mask that crops 6-pixels borders.
; Assumes 2048x2048 images.


(ns my.t2
  (:import
     (ini.trakem2 ControlWindow)
     (ij.process ByteProcessor)))


(let [mask (ByteProcessor. 2048 2048)]
  (doto mask
    (.setRoi 6 6 2036 2036)
    (.setValue 255)
    (.fill))
  (dorun
    (map
      (fn [patch]
        (when-not (.hasAlphaMask patch)
          (doto patch
            (.setAlphaMask mask)
            (.updateMipMaps))))
      (reduce
        #(into %1 %2)
        []
        (map
          #(.getDisplayables % Patch)
          (.. ControlWindow getActive getRootLayerSet getLayers))))))

