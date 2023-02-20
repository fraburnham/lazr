(ns lazr.box
  (:require [lazr.geometry :as geom]))

;; TODO: config spec!!
;; TODO: test cases!
;; TODO: constraints! (maybe I don't need constraints. Maybe the reason I did strange shit with the corners was because of even instead of odd numbers?)

;; ncviewer.com

;; TODO: Document!!
(defn- next-point
  ([config step]
   (next-point config step identity))
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
      (conj ret (next-point config step transformer)))
    [(transformer [0 0])]
    (range (* 2 (dec (/ width finger-width)))))))

;; TODO: make a public interface for `face` so that a single face can be generated on the command line
(defn- face
  [edge]
  (-> edge
      (#(into % (rest (mapv (comp (partial geom/translate (last %)) geom/rotate-90) edge))))
      (#(into % (rest (mapv (comp (partial geom/translate (last %)) geom/rotate-180) edge))))
      (#(into % (rest (mapv (comp (partial geom/translate (last %)) geom/rotate-270) edge))))))

(defn cube
  ([config]
   (cube config identity))
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

;; TODO: `column` a box with no caps and smooth edges where caps _would_ attach
;; TODO: `drawer` a box with no top cap and a smooth edge where the top cap _would_ attach
;; TODO: `box` a drawer with a removable lid (likely made of two bits, one to cover and one to keep it retained; slightly oversized for easy lifting)

;; TODO: refactor this to output the maps that encode' expects so that compression can be applied here
