(ns lazr.svg.core
  (:require [clojure.data.xml :as xml]
            [clojure.string :as string]
            [lazr.svg.handler.path :as path]
            [lazr.svg.handler.transform :as transform])
  (:import [org.apache.batik.parser PathParser TransformListParser]))

(defprotocol DepthTraversal
  (walk [this transformer path-parser] "Traverse down the tree as appropriate, accumulating path and transformer data"))

(extend-protocol DepthTraversal
  clojure.data.xml.Element
  (walk [el transformer path-parser]
    (let [transform-handler (transform/->handler (atom transformer))
          transform-parser (doto (TransformListParser.)
                             (.setTransformListHandler transform-handler))
          path-handler (.getPathHandler path-parser)]
      (when (contains? (set (keys (:attrs el))) :transform)
        (.parse transform-parser (get-in el [:attrs :transform])))

      (when (= (:tag el) :path)
        (.setPathHandler path-parser (path/update-transformer path-handler @(:transformer transform-handler)))
        (.parse path-parser (get-in el [:attrs :d])))

      (walk (:content el) @(:transformer transform-handler) path-parser)))

  clojure.lang.LazySeq
  (walk [sq transformer path-parser]
    (doseq [s sq]
      (walk s transformer path-parser)))

  java.lang.String
  (walk [_ _ _]))

(defn transcode
  [svg-stream]
  (let [svg (xml/parse svg-stream)
        path-handler (path/->handler nil (atom []) (atom {}))
        path-parser (doto (PathParser.)
                      (.setPathHandler path-handler))
        height (-> (get-in svg [:attrs :viewBox])
                   (string/split #" ")
                   (last)
                   (Integer/parseInt))
        fix-origin (fn [coords] (let [invert #(when % (- height %))]
                                  (-> (update coords :y invert)
                                      (update :j invert)
                                      (update :q invert))))]
    (walk svg fix-origin path-parser)
    @(:paths path-handler)))
