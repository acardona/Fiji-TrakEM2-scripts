; Albert Cardona 2010
; Regenerate mipmaps for a all images within a range of layers

(ns my.t2
  (:import (ini.trakem2 ControlWindow)))

(def first-layer 34)
(def last-layer 458)

(dorun
  (pmap
    #(println "UPDATED" % (.get (.updateMipMaps %)))
    (reduce
      #(into %1 %2)
      []
      (map
        #(.getDisplayables % Patch)
        (.. ControlWindow getActive getRootLayerSet (getLayers first-layer last-layer))))))
