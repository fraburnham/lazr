(ns lazr.geometry)

(defn rotate-90
  [[x y]]
  [(- y) x])

(def rotate-180 (comp rotate-90 rotate-90))
(def rotate-270 (comp rotate-90 rotate-180))

(defn translate
  [[offset-x offset-y] [x y]]
  [(+ x offset-x)
   (+ y offset-y)])
