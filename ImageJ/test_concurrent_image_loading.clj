; Albert Cardona 2010
; Test and find out the optimal number of images to concurrently load
; from the file system, for maximal speed opening a collection of images.

(ns my.test_concurrent_image_loading
  (:import [java.util.concurrent Executors]
  	    [ini.trakem2.io ImageSaver]
  	    [java.io File FilenameFilter]))


(defn test-concurrent-load
  "Generate an ExecutorService with num-threads
   and submit an equal number of jobs to it, where each job
   is loading an image found in the collection of paths.
   The threads load as many as num-loads images in total."
  [num-threads num-loads col-paths]
  (let [exec (Executors/newFixedThreadPool num-threads)
        loads (ref num-loads)]
    (loop [jobs []
            i 0]
      (if (< i num-threads)
        (do
          (recur
            (conj jobs #(loop [i (dosync (alter loads dec))]
                (if (> i 0)
                  (do
                    (if-let [img (ImageSaver/openJpegAlpha (get col-paths (int (rand (count col-paths)))))]
                      (.flush img))
                    (recur (dosync (alter loads dec)))))))
            (inc i)))
         (doseq [fu (.invokeAll exec jobs)]
            (.get fu))))
     (.shutdown exec)
     (println "done!")))


(defn get-paths
  [dir]
  (vec (map #(str dir %) (seq (.list (File. dir) (proxy [FilenameFilter] [] (accept [dir name] (.endsWith name ".jpg"))))))))

; level 0
(def dir "/data/lab/TEM/abd/trakem2.1242464784521.398974114.2102827473/trakem2.mipmaps/0/14/")

; level 2 (25%)
;(def dir "/data/lab/TEM/abd/trakem2.1242464784521.398974114.2102827473/trakem2.mipmaps/2/14/")

; level 4 (6.5%)
;(def dir "/data/lab/TEM/abd/trakem2.1242464784521.398974114.2102827473/trakem2.mipmaps/4/14/")

;(map println (get-paths dir))


;; Result: for all of them, 4 threads is faster

(println "15 threads:")
(time
  (test-concurrent-load 15 100 (get-paths dir)))

; --> 22.3 seconds

(println "4 threads:")
(time
  (test-concurrent-load 4 100 (get-paths dir)))

; --> 21.8 seconds

(println "2 threads:")
(time
  (test-concurrent-load 2 100 (get-paths dir)))

; --> 25.8 seconds

(println "1 threads:")
(time
  (test-concurrent-load 1 100 (get-paths dir)))

; --> 50.8 seconds


;
;(let [col-paths (get-paths dir)]
;  (if-let [img (ImageSaver/openJpegAlpha (get col-paths (int (rand (count col-paths)))))]
;    (do
;      (println img)
;      (.flush img))))
;)
