(ns lazr.gcode.core)

;; TODO: spec for config!

(defn m4
  [{:keys [s]}]
  (format "m4 s%d" s))

(defn gN-args
  [{:keys [x y f s]}]
  (str
   (when (number? x) (format " x%.4f" (float x)))
   (when (number? y) (format " y%.4f" (float y)))
   (when (number? f) (format " f%d" f))
   (when (number? s) (format " s%d" s))))

(defn g0
  [args]
  (str "g0" (gN-args args)))

(defn g1
  [args]
  (str "g1" (gN-args args)))

(defn- prepare-coords
  [paths]
  (if (number? (first paths))
    (let [[x y] paths]
      {:x x :y y})
    (map prepare-coords paths)))

(defn- construct-gcode
  [{:keys [laser-speed laser-intensity travel-speed] :as config} coords]
  (if (map? (first coords))
    (reduce
     (fn [gcode coord]
       (str gcode "\n"
            (g1 (merge coord {:f laser-speed :s laser-intensity}))))
     (g0 (merge (first coords) {:f travel-speed}))
     (rest coords))
    (map (partial construct-gcode config) coords)))

(defn encode
  [config paths]
  (let [gcode (->> (prepare-coords paths)
                   (construct-gcode config))]
    (str ; append \n to end of str (posix file requirement)
     (reduce
      #(str %1 "\n" %2)
      (str "g90\n"                     ; absolute coordinates
           (m4 {:s (:laser-intensity config)}))
      gcode)
     "\n")))
