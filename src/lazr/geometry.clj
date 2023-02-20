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
