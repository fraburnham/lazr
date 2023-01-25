(ns lazr.svg.handler.path
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
  [paths transformer path]
  (swap! paths conj (transformer path)))

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
    (append-path! paths transformer (merge {:type :g1} (::initial-point @state))))

  (curvetoCubicAbs [_ x1 y1 x2 y2 x y]
    (set-positioning! paths state ::absolute)
    (append-path! paths transformer {:type :g5 :i x1 :j y1 :p x2 :q y2 :x x :y y}))

  (curvetoCubicRel [_ x1 y1 x2 y2 x y]
    (set-positioning! paths state ::relative)
    (append-path! paths transformer {:type :g5 :i x1 :j y1 :p x2 :q y2 :x x :y y}))

  (curvetoCubicSmoothAbs [_ x2 y2 x y]
    ;; what makes this different from `curvetoCubic`?
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
    (append-path! paths transformer {:type :g1 :x x :y y}))

  (linetoHorizontalAbs [_ x]
    (set-positioning! paths state ::absolute)
    (append-path! paths transformer {:type :g1 :x x}))

  (linetoHorizontalRel [_ x]
    (set-positioning! paths state ::relative)
    (append-path! paths transformer {:type :g1 :x x}))

  (linetoRel [_ x y]
    (set-positioning! paths state ::relative)
    (append-path! paths transformer {:type :g1 :x x :y y}))

  (linetoVerticalAbs [_ y]
    (set-positioning! paths state ::absolute)
    (append-path! paths transformer {:type :g1 :y y}))

  (linetoVerticalRel [_ y]
    (set-positioning! paths state ::relative)
    (append-path! paths transformer {:type :g1 :y y}))

  (movetoAbs [_ x y]
    (set-positioning! paths state ::absolute)
    (append-path! paths transformer {:type :g0 :x x :y y})
    (swap! state assoc ::initial-point {:x x :y y}))

  (movetoRel [_ x y]
    (set-positioning! paths state ::relative)
    (append-path! paths transformer {:type :g0 :x x :y y})
    (let [old (or (::initial-point @state) {:x 0 :y 0})] ; keep track of the absolute position we moved to
      (swap! state assoc ::initial-point {:x (+ (:x old) x) :y (+ (:y old) y)})))

  (startPath [_]
    (swap! state assoc ::start-path true)))

(defn update-transformer
  [handler transformer]
  (assoc handler :transformer transformer))
