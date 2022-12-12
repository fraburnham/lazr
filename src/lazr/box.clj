(ns lazr.box
  (:require [lazr.geometry :as geom]))

;; TODO: config spec!!
;; TODO: test cases!
;; TODO: constraints!

;; ncviewer.com

;; TODO: Document!!
(defn- line-segment
  ([config step]
   (line-segment config step identity))
  ([{:keys [finger-width finger-depth]} step transformer]
   (transformer
    [(* finger-width
        (int (/ step 2)))
     (* finger-depth
        (case (mod step 4)
          (0 3) 0
          (1 2) -1))])))

(defn- edge
  ([config]
   (edge config identity))
  ([{:keys [finger-width width] :as config} transformer]
   (reduce
    (fn [ret step]
      (conj ret (line-segment config step transformer)))
    [(transformer [0 0])]
    (range (* 2 (dec (/ width finger-width)))))))

(defn- face
  [edge]
  (-> edge
      (#(into % (rest (mapv (comp (partial geom/translate (last %)) geom/rotate-90) edge))))
      (#(into % (rest (mapv (comp (partial geom/translate (last %)) geom/rotate-180) edge))))
      (#(into % (rest (mapv (comp (partial geom/translate (last %)) geom/rotate-270) edge))))))

(defn box
  ([config]
   (box config identity))
  ([{:keys [finger-depth finger-width width] :as config} transformer]
   (let [cap-edge (-> (edge config transformer)
                      (conj (transformer [(- width finger-depth) (- finger-depth)])))
         wall-edge (->> (edge config (comp transformer (partial geom/translate [(* 2 finger-width) 0])))
                        (drop-last 3)
                        (into [[0 0]]))
         cap (mapv (partial geom/translate [0 finger-depth]) (face cap-edge))
         wall (mapv (partial geom/translate [0 finger-depth]) (face wall-edge))]
     [;; bottom row
      wall
      (mapv (partial geom/translate [(+ 1 width) 0]) wall)
      (mapv (partial geom/translate [(* 2 (+ 1 width)) 0]) wall)
      ;; top row
      (mapv (partial geom/translate [0 (+ 1 width)]) wall)
      (mapv (partial geom/translate [(+ 1 width) (+ 1 width)]) cap)
      (mapv (partial geom/translate [(* 2 (+ 1 width)) (+ 1 width)]) cap)])))
