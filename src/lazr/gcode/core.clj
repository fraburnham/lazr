(ns lazr.gcode.core
  (:require [clojure.set :refer [rename-keys]]))

;; TODO: spec for config!

(defn args
  [{:keys [x y f s i j p q]}]
  (str
   (when (number? i) (format " i%.4f" (float i)))
   (when (number? j) (format " j%.4f" (float j)))
   (when (number? p) (format " p%.4f" (float p)))
   (when (number? q) (format " q%.4f" (float q)))
   (when (number? x) (format " x%.4f" (float x)))
   (when (number? y) (format " y%.4f" (float y)))
   (when (number? f) (format " f%d" f))
   (when (number? s) (format " s%d" s))))

(def ->gcode
  {:g0 (comp (partial format "g0 %s") args #(rename-keys % {:travel-speed :f}))
   :g1 (comp (partial format "g1 %s") args #(rename-keys % {:laser-speed :f :laser-intensity :s}))
   :g5 (comp (partial format "g5 %s") args #(rename-keys % {:laser-speed :f :laser-intensity :s}))
   :g90 (constantly "g90")
   :g91 (constantly "g91")
   :m3 (comp (partial format "m3 %s") args #(rename-keys % {:laser-intensity :s}))
   :m4 (comp (partial format "m4 %s") args #(rename-keys % {:laser-intensity :s}))})

(defn m4
  [{:keys [s]}]
  (format "m4 s%d" s))

(defn g0
  [args]
  (str "g0" (args args)))

(defn g1
  [args]
  (str "g1" (args args)))

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

;; TODO: get rid of old uses of `encode` and replace with `encode'`
;;       make everything use the maps AND get a schema for the maps!
(defn encode'
  [config paths]
  (reduce
   (fn [gcode path]
     (let [f (get ->gcode (:type path))]
       (str gcode "\n"
            (f (merge path config)))))
   ""
   paths))
