(ns my.network
  (:import [ini.trakem2.display Display Tree Node]
           [java.awt Color]
           [clojure.lang IPersistentVector])
  (:use [clojure.set :only (difference)]))

(set! *warn-on-reflection* true)

(defn ^:static up-to-same-degree
  "Return the list of nodes from nd1 (included)
  up its parent chain until reaching a node that has deg2 (not included)."
  ^IPersistentVector
  [^Node nd1
   ^long deg1
   ^long deg2]
  (loop [node nd1
          deg deg1
          top []]
    (if (<= deg deg2)
      top
      (recur (.getParent node)
              (dec deg)
              (conj top node)))))

(defn ^:static find-path
  "Return the list of nodes from nodeA to nodeB, both included."
  ^IPersistentVector
  [^Node nodeA
   ^long degA
   ^Node nodeB
   ^long degB]
   (let [topA (up-to-same-degree nodeA degA degB)
         topB (up-to-same-degree nodeB degB degA)
         ^Node nodeA (if (empty? topA) nodeA (.getParent ^Node (last topA)))
         ^Node nodeB (if (empty? topB) nodeB (.getParent ^Node (last topB)))
         [topA topB] (loop [^Node ndA nodeA
                             ^Node ndB nodeB
                             tpA topA
                             tpB topB]
                       (if (= ndA ndB)
                         [tpA tpB]
                         (recur (.getParent ndA)
                                 (.getParent ndB)
                                 (conj tpA ndA)
                                 (conj tpB ndB))))
          topA (conj topA (if (empty? topA)
                            nodeA
                            (.getParent ^Node (first topA))))]
        (into topA (reverse topB))))

(defn find-all-to-all-paths
  "Return a lazy sequence of vectors, each containing a possible path between two nodes in the tree."
  ([^Tree tree]
    (find-all-to-all-paths
      (into [] (.. tree getRoot getSubtreeNodes))
      (into {} (.computeAllDegrees tree))))
  ([nodes degrees]
    (for [i (range (count nodes))
          j (range (inc i) (count nodes))]
      (let [^Node nodeA (nodes i)
            ^Node nodeB (nodes j)]
        (find-path nodeA (degrees nodeA) nodeB (degrees nodeB))))))

(def lut
 [[67 0 185]
  [69 0 187]
  [71 0 189]
  [74 0 193]
  [77 0 196]
  [78 0 198]
  [81 0 201]
  [83 0 203]
  [86 0 207]
  [89 0 210]
  [90 0 212]
  [93 0 215]
  [95 0 217]
  [99 0 220]
  [102 0 220]
  [103 0 221]
  [106 0 222]
  [108 0 222]
  [111 0 223]
  [114 0 224]
  [115 0 225]
  [118 0 226]
  [120 0 226]
  [123 0 226]
  [124 0 225]
  [127 0 222]
  [130 0 220]
  [132 0 220]
  [135 0 218]
  [136 0 216]
  [139 0 214]
  [142 0 212]
  [144 0 212]
  [146 0 209]
  [148 0 207]
  [150 0 203]
  [152 0 200]
  [152 0 198]
  [154 0 194]
  [156 0 192]
  [158 0 189]
  [158 0 187]
  [160 0 183]
  [162 0 180]
  [163 0 178]
  [164 0 174]
  [164 0 172]
  [166 0 168]
  [167 0 165]
  [168 0 163]
  [170 0 159]
  [170 0 157]
  [171 0 153]
  [173 0 150]
  [174 0 148]
  [175 0 144]
  [175 0 142]
  [177 0 139]
  [178 0 135]
  [179 0 133]
  [181 0 130]
  [181 0 128]
  [182 0 124]
  [184 0 123]
  [185 0 119]
  [186 0 115]
  [186 0 113]
  [188 0 110]
  [189 0 108]
  [190 0 104]
  [192 0 101]
  [192 0 99]
  [193 0 95]
  [195 0 94]
  [196 1 90]
  [198 3 86]
  [198 3 84]
  [199 5 81]
  [201 7 79]
  [202 8 75]
  [202 8 73]
  [204 10 70]
  [205 12 66]
  [207 14 65]
  [208 16 61]
  [208 17 59]
  [209 19 55]
  [210 22 52]
  [212 23 50]
  [213 26 46]
  [213 27 44]
  [214 30 41]
  [215 33 37]
  [217 34 36]
  [218 37 32]
  [218 38 30]
  [220 41 26]
  [221 44 22]
  [223 45 21]
  [224 48 17]
  [224 49 15]
  [226 52 11]
  [227 53 9]
  [229 56 6]
  [230 59 4]
  [230 60 4]
  [231 63 3]
  [233 64 3]
  [234 67 2]
  [235 70 1]
  [235 71 1]
  [237 74 1]
  [238 75 0]
  [240 78 0]
  [241 81 0]
  [241 82 0]
  [243 85 0]
  [244 86 0]
  [246 89 0]
  [246 90 0]
  [247 93 0]
  [249 96 0]
  [250 97 0]
  [252 100 0]
  [252 101 0]
  [252 103 0]
  [252 105 0]
  [253 107 0]
  [253 109 0]
  [253 109 0]
  [253 111 0]
  [254 113 0]
  [254 115 0]
  [255 117 0]
  [255 117 0]
  [255 119 0]
  [255 121 0]
  [255 123 0]
  [255 125 0]
  [255 125 0]
  [255 127 0]
  [255 129 0]
  [255 131 0]
  [255 133 0]
  [255 133 0]
  [255 134 0]
  [255 136 0]
  [255 138 0]
  [255 140 0]
  [255 140 0]
  [255 141 0]
  [255 143 0]
  [255 145 0]
  [255 147 0]
  [255 147 0]
  [255 148 0]
  [255 150 0]
  [255 152 0]
  [255 152 0]
  [255 154 0]
  [255 155 0]
  [255 157 0]
  [255 159 0]
  [255 159 0]
  [255 161 0]
  [255 162 0]
  [255 164 0]
  [255 166 0]
  [255 166 0]
  [255 168 0]
  [255 169 0]
  [255 171 0]
  [255 173 0]
  [255 173 0]
  [255 175 0]
  [255 176 0]
  [255 178 0]
  [255 180 0]
  [255 180 0]
  [255 182 0]
  [255 184 0]
  [255 186 0]
  [255 188 0]
  [255 188 0]
  [255 190 0]
  [255 191 0]
  [255 193 0]
  [255 195 0]
  [255 195 0]
  [255 197 0]
  [255 199 0]
  [255 201 0]
  [255 203 0]
  [255 203 0]
  [255 205 0]
  [255 206 0]
  [255 208 0]
  [255 208 0]
  [255 210 0]
  [255 212 0]
  [255 213 0]
  [255 215 0]
  [255 215 0]
  [255 217 0]
  [255 219 0]
  [255 220 0]
  [255 222 0]
  [255 222 0]
  [255 224 0]
  [255 226 0]
  [255 228 0]
  [255 230 0]
  [255 230 0]
  [255 232 0]
  [255 234 1]
  [255 235 3]
  [255 237 7]
  [255 237 9]
  [255 239 14]
  [255 241 16]
  [255 242 20]
  [255 244 25]
  [255 244 27]
  [255 246 31]
  [255 248 34]
  [255 248 41]
  [255 249 49]
  [255 249 51]
  [255 250 59]
  [255 251 65]
  [255 252 73]
  [255 253 81]
  [255 253 83]
  [255 254 91]
  [255 255 97]
  [255 255 104]
  [255 255 106]
  [255 255 114]
  [255 255 122]
  [255 255 128]
  [255 255 135]
  [255 255 137]
  [255 255 145]
  [255 255 153]
  [255 255 159]
  [255 255 166]
  [255 255 168]
  [255 255 176]
  [255 255 184]
  [255 255 190]
  [255 255 198]
  [255 255 200]
  [255 255 208]
  [255 255 216]
  [255 255 222]
  [255 255 226]])

(defn betweenness-centrality
  "Return a map of each node vs. its centrality."
  ([^Tree tree]
    (betweenness-centrality (into [] (.. tree getRoot getSubtreeNodes))
                            (into {} (.computeAllDegrees tree))))
  ([nodes degrees]
    (apply merge-with +
      (map #(zipmap % (repeat 1))
        (find-all-to-all-paths nodes degrees)))))

(defn get-color [i highest]
  (let [[r g b] (lut (int (* 255 (/ i highest))))]
    (Color. (int r) (int g) (int b))))

(defn colorize-centrality
  "Set a heat color to each node based on its centrality."
  [^Tree tree]
  (let [bc (betweenness-centrality tree)
        highest (apply max (vals bc))
        get-color (memoize get-color)]
    (println "highest:" highest)
    (doseq [^Node nd (keys bc)]
      (.setColor nd (get-color (get bc nd) highest)))))


(defn run-colorize-centrality
  "Colorize the nodes of the selected tree with a heat LUT based on their centrality."
  []
  (let [tree (first (.. Display getSelected))]
    (if (nil? tree)
      (println "No tree selected!")
      (if (not (contains? (ancestors (class tree)) Tree))
        (println "The selected object is not a Tree!")
        (do
          (colorize-centrality tree)
          (Display/repaint))))))

;(time
;  (run-colorize-centrality))

; Now colorize not by centrality of node but by centrality of branch

(def etching-multiplier (int 2))

(defn branch-centrality
  [^Tree tree]
  (let [degrees (into {} (.computeAllDegrees tree))]
    (loop [nds (into {} (map
                           (fn [^Node nd] [nd (set (.getChildrenNodes nd))])
                           (.. tree getRoot getSubtreeNodes)))
            branch-nodes (into {} (filter #(> (count (val %)) 1) nds))
            step-vs-removed {} ; a map of step vs [remaining-branches, removed nodes] in that step
            bc (betweenness-centrality (vec (keys nds)) degrees) ; a map of node vs centrality value
            step (int 1)]
      (println "nodes:" (count nds) "step:" step "branch-nodes:" (count branch-nodes))
      (let [nodes-to-remove (reduce
                              (fn [s [nd bcv]]
                                (if (< bcv (* etching-multiplier (count nds)))
                                  (conj s nd)
                                  s))
                                #{}
                                bc)
              remaining-branch-nodes (into {} (filter #(> (count (last %)) 1)
                                                       (map (fn [[k v]] [k (difference v nodes-to-remove)])
                                                            branch-nodes)))
              step-vs-removed (assoc step-vs-removed step [(count remaining-branch-nodes) nodes-to-remove])
              nds (apply dissoc nds nodes-to-remove)]
          (if (== 0 (count nodes-to-remove))
            step-vs-removed
            ; Else
            (recur nds
                    remaining-branch-nodes
                    step-vs-removed
                    (betweenness-centrality (vec (keys nds)) degrees)
                    (inc step)))))))

(defn colorize-branch-centrality
  [^Tree tree]
  (let [svr (branch-centrality tree)
        highest (apply max (keys svr))
        get-color (memoize get-color)]
    (doseq [[step [nb nds]] svr]
      (println step nb ":" (count nds))
      (doseq [^Node nd nds]
        (.setColor nd (get-color step highest))))))

(defn run-branch-centrality
  []
  (let [tree (first (.. Display getSelected))]
    (if (nil? tree)
      (println "No tree selected!")
      (if (not (contains? (ancestors (class tree)) Tree))
        (println "The selected object is not a Tree!")
        (do
          (colorize-branch-centrality tree)
          (Display/repaint))))))

(time
  (run-branch-centrality))

