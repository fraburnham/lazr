(ns lazr.gcode.raster
  (:require [lazr.gcode.core :refer [g0 g1 m4]]))

(defn- pixel->laser
  [pixels-per-mm pixel]
  (/ pixel pixels-per-mm))

(defn- pixel->intensity
  [laser-intensity color-model rast w h]
  (int (* laser-intensity (/ (- 255 (.getRed color-model (byte-array [(.getSample rast w h 0)]))) 255))))

(defn ->gcode
  [image travel-speed laser-intensity pixels-per-mm]
  (let [height (.getHeight image)
        width (.getWidth image)
        w->x (partial pixel->laser pixels-per-mm)
        h->y (comp (partial pixel->laser pixels-per-mm) #(- (dec height) %))
        color-model (.getColorModel image)
        rast (.getRaster image)
        pixel->intensity (partial pixel->intensity laser-intensity color-model rast)]
    (loop [heights (reverse (range height)) ; heights in reverse because the laser has [0 0] at bottom left and png has it at top left
           gcode (str "g90\n"
                      (g0 {:x 0 :y 0 :f travel-speed}) "\n"
                      (m4 {:s 0}))]
      (if (empty? heights)
        (str gcode "\n"
             "m5\n"
             "g0 x0 y0\n")
        (let [h (first heights)]
          (recur
           (rest heights)
           (str gcode "\n"
                (loop [gcode (g0 {:x (w->x (if (even? h) width 0)) :y (h->y h) :f travel-speed})
                       ongoing-intensity nil
                       widths (if (even? h)
                                (reverse (range width))
                                (range width))]
                  (if (empty? widths)
                    (let [w (if (even? h) 0 (dec width))]
                      (str gcode "\n"
                           (g1 {:x (w->x w) :s (pixel->intensity w h)}))) ; needs the final intensity attached
                    (let [w (first widths)
                          intensity (pixel->intensity w h)
                          ongoing-intensity (or ongoing-intensity intensity)]
                      (recur
                       (cond
                         (not= ongoing-intensity intensity) (str gcode "\n" ; change in intensity
                                                                 (g1 {:x (w->x w) :s ongoing-intensity}))
                         :else gcode)
                       intensity
                       (rest widths))))))))))))

;; TODO: this needs refactor
;; specifically there are multiple places that determine which width or height to use (or which direction to go)
;; unify the mess!
