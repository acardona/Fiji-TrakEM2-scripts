(ns my.quantify_network
  (:import [ini.trakem2 Project]
           [ini.trakem2.display Connector Tree])
  (:use [clojure.contrib.monads :only (domonad sequence-m)]
        [clojure.set :only (union intersection)]))

(defn find-arbors
  "Return a collection of the subset of arbors that match the given string."
  [layerset s]
  (let [project (.getProject layerset)
        pattern (re-pattern s)]
    (domonad sequence-m
      [arbor (.getZDisplayables layerset Arbor true)
       :when (re-find pattern (.getMeaningfulTitle project arbor))]
      arbor)))
; Test
; (println (find-arbors (.. Project getProjects (get 0) getRootLayerSet) "three"))

(defmacro all-connectors
  [layerset]
  `(.getZDisplayables ~layerset Connector))

(defn trees
  [ts]
  (filter #(not= (class %) Connector) ts))

(defn get-targets
  [connector]
  (reduce #(into %1 %2) #{} (trees (.getTargets connector Tree))))

(defn get-origins
  [connector]
  (set (trees (.getOrigins connector Tree))))

(defn find-divergent
  "Return a map of Tree keys and sets of Tree targets.
  Keys are taken from sources, and targets are searched for in targets.
  Only when a given Tree has more than one Tree target is the pair included in the map,
  by definition of divergence."
  [sources targets]
  (let [utargets (into #{} targets)]
    (into {} (filter #(> (count (val %)) 1)
      (apply merge-with union
        (domonad sequence-m
          [tl sources
           :when (not= nil (.getRoot tl))
           cso (first (.findConnectors tl))]
          {tl (intersection utargets (get-targets cso))}))))))

(defn find-convergent
   "Return a map of Tree keys (the target) and sets of Tree origins.
   Keys are taken from targets, and sets of Tree origins from sources.
   Only when more than one Tree is connecting onto the same target,
   is the pair of Tree target vs. sets of Tree origins included in the map,
   by definition of convergence."
  [sources targets]
  (let [usources (into #{} sources)]
    (into {} (filter #(> (count (val %)) 1)
      (apply merge-with union
        (domonad sequence-m
          [tl targets
          :when (not= nil (.getRoot tl))
           csi (second (.findConnectors tl))]
        {tl (intersection usources (get-origins csi))}))))))


(let [layerset (.. Project getProjects (get 0) getRootLayerSet)
      origins (find-arbors layerset "three")
      targets (find-arbors layerset "trees")]
  ;(println origins targets)
  (println "convergent:" (find-divergent origins targets))
  (println "divergent:" (find-convergent origins targets))
  )
  