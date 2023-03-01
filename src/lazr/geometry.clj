(ns lazr.geometry)

(defn rotate-90
  [{:keys [x y] :as gcode-map}]
  (-> (assoc gcode-map :x (- y))
      (assoc :y x)))

(def rotate-180 (comp rotate-90 rotate-90))
(def rotate-270 (comp rotate-90 rotate-180))

;; TODO: finish out the transform handler
(defn translate
  [{:keys [x y]} gcode-map]
  (-> (update gcode-map :x #(when (and % x) (+ x %)))
      (update :y #(when (and % y) (+ y %)))))

(defn scale
  [x-fac y-fac gcode-map]
  (let [x-scaler #(when % (* x-fac %))
        y-scaler #(when % (* y-fac %))]
    (-> (update gcode-map :x x-scaler)
        (update :y y-scaler))))

(defn- lerp
  "Linear interpolation between points a and b"
  [a b t]
  (let [interp #(+ (% a)
                   (* t
                      (- (% b) (% a))))]
    {:x (interp :x)
     :y (interp :y)}))

;; https://github.com/jonase/mlx/blob/master/clojurescript/bezier/src/bezier/core.cljs
(defn cubic-curve->points
  [num-segments [start c1 c2 end]]
  ;; there is probably a smart thing to do w/ num-segments
  ;; esp since some of them may be suuper tiny
  ;; each point is the start of it's line segment and the end of the one before it
  (map (fn [seg]
         (let [t (/ seg num-segments)
               p (lerp start c1 t)
               q (lerp c1 c2 t)
               r (lerp c2 end t)
               u (lerp p q t)
               v (lerp q r t)]
           (lerp u v t)))
       (range (inc num-segments))))
