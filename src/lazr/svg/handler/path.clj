(ns lazr.svg.handler.path
  (:require [lazr.geometry :as geom])
  (:import [org.apache.batik.parser PathHandler]))

;; TODO: spec for state map
;; TODO: spec for paths!
;; TODO: `:type` should be a gcode key `:gcode/type` (assuming an import)
;;       and the spec for "types" of gcode should live in the gcode... code

(def positioning->gcode
  {::absolute :g90
   ::relative :g91})

;; If I make this a macro I have access to the paths and state by funky magic, right?
(defn- set-positioning!
  [paths state desired]
  (when (not= (::positioning @state) desired)
    (swap! paths conj {:type (get positioning->gcode desired)}))
  (swap! state assoc ::positioning desired))

(defn- append-path!
  [state paths transformer path]
  (swap! paths conj (transformer path))
  (swap! state assoc ::last path))

(defrecord handler [transformer paths state]
  PathHandler

  (arcAbs [_ rx ry x-axis-rotation large-arc-flag sweep-flag x y]
    ;; https://developer.mozilla.org/en-US/docs/Web/SVG/Tutorial/Paths#arcs
    ;; will need to consider the arc and sweep flags
    (set-positioning! paths state ::absolute)
    (println "# missing arcAbs"))

  (arcRel [_ rx ry x-axis-rotation large-arc-flag sweep-flag x y]
    (set-positioning! paths state ::relative)
    (println "# missing arcRel"))

  (closePath [_]
    ;; https://www.w3.org/TR/SVG/paths.html#PathDataClosePathCommand
    ;; `Z` or closepath closes a path by connecting back to the paths `initial-point`
    (set-positioning! paths state ::absolute)
    (append-path! state paths transformer (merge {:type :g1} (::initial-point @state))))

  (curvetoCubicAbs [_ x1 y1 x2 y2 x y]
    ;; https://developer.mozilla.org/en-US/docs/Web/SVG/Tutorial/Paths#curve_commands
    ;; The last set of coordinates here (x, y) specify where the line should end.
    ;; The other two are control points. (x1, y1) is the control point for the start of the curve,
    ;; and (x2, y2) is the control point for the end.
    (set-positioning! paths state ::absolute)
    (let [start (::last @state)]
      (doseq [p (geom/cubic-curve->points 5 [start {:x x1 :y y1} {:x x2 :y y2} {:x x :y y}])]
        (append-path! state paths transformer (assoc p :type :g1)))))

  (curvetoCubicRel [_ x1 y1 x2 y2 x y]
    (set-positioning! paths state ::relative)
    (let [start (::last @state)]
      (doseq [p (geom/cubic-curve->points 5 [start {:x x1 :y y1} {:x x2 :y y2} {:x x :y y}])]
        (append-path! state paths transformer (assoc p :type :g1)))))

  (curvetoCubicSmoothAbs [_ x2 y2 x y]
    ;; what makes this different from `curvetoCubic`?
    ;; https://svg-path-visualizer.netlify.app/bezier-curve/
    ;; The Smooth Curve command
    ;; There are also some special cases of Bézier Curves that have shortcut notation in SVG.
    ;; A common case is when you have multiple curves one after the other and you want it to smoothly transition between them.
    ;; To do so, you need to have the first control point of the next curve be the reflection of the second control point of the previous curve.
    ;; So as long as you specify one, you shouldn't need to specify the other one. That's what the S command is for (S for smooth).
    (set-positioning! paths state ::absolute)
    (println "# missing curvetoCubicSmoothAbs"))

  (curvetoCubicSmoothRel [_ x2 y2 x y]
    (set-positioning! paths state ::relative)
    (println "# missing curvetoCubicSmoothRel"))

  (curvetoQuadraticAbs [_ x1 y1 x y]
    ;; I think this is a cubic with (= x1 x2) and (= y1 y2)
    (set-positioning! paths state ::absolute)
    (println "# missing curvetoQuadraticAbs"))

  (curvetoQuadraticRel [_ x1 y1 x y]
    (set-positioning! paths state ::relative)
    (println "# missing curvetoQuadraticRel"))

  (curvetoQuadraticSmoothAbs [_ x y]
    (set-positioning! paths state ::absolute)
    (println "# missing curvetoQuadraticSmoothAbs"))

  (curvetoQuadraticSmoothRel [_ x y]
    (set-positioning! paths state ::relative)
    (println "# missing curvetoQuadraticSmoothRel"))

  (endPath [_])

  (linetoAbs [_ x y]
    (set-positioning! paths state ::absolute)
    (append-path! state paths transformer {:type :g1 :x x :y y}))

  (linetoHorizontalAbs [_ x]
    (set-positioning! paths state ::absolute)
    (append-path! state paths transformer {:type :g1 :x x}))

  (linetoHorizontalRel [_ x]
    (set-positioning! paths state ::relative)
    (append-path! state paths transformer {:type :g1 :x x}))

  (linetoRel [_ x y]
    (set-positioning! paths state ::relative)
    (append-path! state paths transformer {:type :g1 :x x :y y}))

  (linetoVerticalAbs [_ y]
    (set-positioning! paths state ::absolute)
    (append-path! state paths transformer {:type :g1 :y y}))

  (linetoVerticalRel [_ y]
    (set-positioning! paths state ::relative)
    (append-path! state paths transformer {:type :g1 :y y}))

  (movetoAbs [_ x y]
    (set-positioning! paths state ::absolute)
    (append-path! state paths transformer {:type :g0 :x x :y y})
    (swap! state assoc ::initial-point {:x x :y y}))

  (movetoRel [_ x y]
    (set-positioning! paths state ::relative)
    (append-path! state paths transformer {:type :g0 :x x :y y})
    (let [old (or (::initial-point @state) {:x 0 :y 0})] ; keep track of the absolute position we moved to
      (swap! state assoc ::initial-point {:x (+ (:x old) x) :y (+ (:y old) y)})))

  (startPath [_]
    (swap! state assoc ::start-path true)))

(defn update-transformer
  [handler transformer]
  (assoc handler :transformer transformer))
