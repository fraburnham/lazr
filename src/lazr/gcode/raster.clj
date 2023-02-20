(ns lazr.gcode.raster)

(defn- pixel->laser
  [pixels-per-mm pixel]
  (/ pixel pixels-per-mm))

(defn- pixel->intensity
  [laser-intensity color-model rast w h]
  (int (* laser-intensity (/ (- 255 (.getRed color-model (byte-array [(.getSample rast w h 0)]))) 255))))

;; Args should be a map w/ spec at this point
(defn ->gcode
  ([laser-intensity pixels-per-mm image]
   (->gcode laser-intensity pixels-per-mm image identity))
  ([laser-intensity pixels-per-mm image transformer]
   (let [height (.getHeight image)
         width (.getWidth image)
         x-transformer (fn [x] (:x (transformer {:x x :y 0})))
         y-transformer (fn [y] (:y (transformer {:x 0 :y y})))
         w->x (comp x-transformer (partial pixel->laser pixels-per-mm))
         h->y (comp y-transformer (partial pixel->laser pixels-per-mm) #(- (dec height) %))
         color-model (.getColorModel image)
         rast (.getRaster image)
         pixel->intensity (partial pixel->intensity laser-intensity color-model rast)]
     (loop [heights (reverse (range height)) ; heights in reverse because the laser has [0 0] at bottom left and png has it at top left
            gcodes [{:type :g90}
                    {:type :g0 :x 0 :y 0}
                    {:type :m4}]]
       (if (empty? heights)
         (-> (conj gcodes {:type :m5})
             (conj {:type :g0 :x 0 :y 0}))
         (let [h (first heights)]
           (recur
            (rest heights)
            (into gcodes
                  (loop [gcodes [{:type :g0 :x (w->x (if (even? h) width 0)) :y (h->y h)}]
                         ongoing-intensity nil
                         widths (if (even? h)
                                  (reverse (range width))
                                  (range width))]
                    (if (empty? widths)
                      (let [w (if (even? h) 0 (dec width))]
                        (conj gcodes
                              {:type :g1 :x (w->x w) :s (pixel->intensity w h)})) ; needs the final intensity attached
                      (let [w (first widths)
                            intensity (pixel->intensity w h)
                            ongoing-intensity (or ongoing-intensity intensity)]
                        (recur
                         (cond
                           (not= ongoing-intensity intensity) (conj gcodes ; change in intensity
                                                                   {:type :g1 :x (w->x w) :s ongoing-intensity})
                           :else gcodes)
                         intensity
                         (rest widths)))))))))))))

;; TODO: this needs refactor
;; specifically there are multiple places that determine which width or height to use (or which direction to go)
;; unify the mess!

