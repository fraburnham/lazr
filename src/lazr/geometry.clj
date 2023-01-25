(ns lazr.geometry)

(defn rotate-90
  [[x y]]
  [(- y) x])

(def rotate-180 (comp rotate-90 rotate-90))
(def rotate-270 (comp rotate-90 rotate-180))

;; TODO: make these take coords and finish out the transform handler
(defn translate
  [[offset-x offset-y] [x y]]
  [(+ x offset-x)
   (+ y offset-y)])

(defn mirror
  [{:keys [x? y?]} [x y]]
  [(if x? (- x) x)
   (if y? (- y) y)])

(defn scale
  [x-fac y-fac coords]
  (let [x-scaler #(when % (* x-fac %))
        y-scaler #(when % (* y-fac %))]
    (-> (update coords :x x-scaler)
        (update :y y-scaler))))
