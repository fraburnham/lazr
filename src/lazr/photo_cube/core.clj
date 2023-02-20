(ns lazr.photo-cube.core
  (:require [clojure.string :as str]
            [lazr.box :as box]
            [lazr.gcode.core :as gcode]
            [lazr.gcode.raster :as raster]
            [lazr.geometry :as geom]
            [lazr.greyscale.transform :as grey]
            [lazr.scale :as scale]))

(defn- index->offset
  [index]
  (let [base-x 5
        base-y 5
        panel-width 110 ; images are 100mm panels are 110mm (5mm finger joints on all sides)
        panel-padding 1
        x-calc (fn [index] (+ base-x (* index panel-width) (* index panel-padding)))]
    (if (< index 3)
      ;; now to get the spacing in place
      {:x (x-calc index) :y base-y}
      (let [index (- index 3)]
        {:x (x-calc index) :y (+ base-y panel-width panel-padding)}))))

(defn images->gcode
  [laser-intensity pixels-per-mm images]
  (let [->gcode (partial raster/->gcode laser-intensity pixels-per-mm)]
    (loop [gcode []
           images images
           index 0]
      (if (empty? images)
        gcode
        (recur
         (into gcode
               (->gcode (first images) (partial geom/translate (index->offset index))))
         (rest images)
         (inc index))))))

(defn- cube-cuts
  []
  (str/join
   "\n"
   (repeat
    5
    (gcode/encode
     {:laser-speed 500 :laser-intensity 750 :travel-speed 3500}
     (box/cube {:finger-depth 5 :finger-width 5 :width 110})))))

(defn- images->cube
  [images]
  (str (gcode/encode {:laser-speed 5000 :travel-speed 5000 :laser-intensity 900} (images->gcode 900 10 images))
       "\n"
       (cube-cuts)))

(defn- image-suitable?
  [image]
  (let [height (.getHeight image)
        width (.getWidth image)]
    (when-not (and (= height width)
                   (>= height 1000)
                   (>= width 1000))
      (throw (ex-info "Unsuitable image"
                      {:image image
                       :height height
                       :width width})))))

(defn- prepare-images
  [images]
  ;; need a gcode-stream? or something so that gcode file inputs are
  ;; ignored (maybe the best thing to do would be add metadata during args parsing...)
  (map
   (comp
    ;; 16 color indexed grey image auto color corrected
    (fn [image] (grey/->indexed-corrected image {:c 16}))
    ;; 8bit greyscale
    grey/->luminosity
    ;; 1000x1000
    (partial scale/scale 10 100 100))
   images))

(defn generate
  [images]
  (doseq [i images] (image-suitable? i))
  (-> (prepare-images images)
      (images->cube)))
