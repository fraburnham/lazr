(ns lazr.svg.handler.transform
  (:require [lazr.geometry :as geom])
  (:import [org.apache.batik.parser TransformListHandler]))

;; https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/transform#matrix
;; Not sure if this belongs in geometry or not. Gonna leave it here and hope it is more obvious in the future.
(defn- matrix-transform
  [a b c d e f {:keys [x y] :as coords}]
  (-> (update coords :x #(when % (+ (* a %) (* c y) e)))
      (update :y #(when % (+ (* b x) (* d %) f)))))

(defn- update-transformer!
  [existing-atom additional]
  (swap! existing-atom #(comp % additional)))

(defrecord handler [transformer]
  TransformListHandler

  (endTransformList [_])

  (matrix [_ a b c d e f]
    (update-transformer! transformer (partial matrix-transform a b c d e f)))

  (rotate [_ theta])

  (rotate [_ theta cx cy])

  (scale [_ sx]
    ;; https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/transform#scale
    (update-transformer! transformer (partial geom/scale sx sx)))

  (scale [_ sx sy]
    (update-transformer! transformer (partial geom/scale sx sy)))

  (skewX [_ skx])

  (skewY [_ sky])

  (startTransformList [_])

  (translate [_ tx])

  (translate [_ tx ty]))
