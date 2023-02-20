(ns lazr.gcode.core
  (:require [clojure.set :refer [rename-keys]]
            [clojure.string :as string]))

;; TODO: spec for config!

(defn compress-number-string
  [s]
  (let [chunks (string/split s #"\.")
        integer-chunk (first chunks)
        decimal-chunk (second chunks)
        compressed (reduce
                    (fn [r v]
                      (if (empty? r)
                        (if (= \0 v)
                          r
                          (cons v r))
                        (cons v r)))
                    '()
                    (reverse decimal-chunk))]
    (str integer-chunk
         (if (empty? compressed)
           ""
           (str "." (string/join compressed))))))

(defn args
  ([args-map]
   (args args-map {}))
  ([{:keys [x y f s i j p q]} state]
   (let [format (comp compress-number-string format)]
     (str
      (when (number? i) (format " i%.4f" (float i)))
      (when (number? j) (format " j%.4f" (float j)))
      (when (number? p) (format " p%.4f" (float p)))
      (when (number? q) (format " q%.4f" (float q)))
      ;; are there cases where I can omit x or y?
      (when (number? x) (format " x%.4f" (float x)))
      (when (number? y) (format " y%.4f" (float y)))
      ;; can skip f,s if they haven't changed
      (when (and (number? f) (not= (:f state) f)) (format " f%d" f))
      (when (and (number? s) (not= (:s state) s)) (format " s%d" s))))))

(def ->renames
  {:g0 #(rename-keys % {:travel-speed :f})
   :g1 #(rename-keys % {:laser-speed :f :laser-intensity :s})
   :g5 #(rename-keys % {:laser-speed :f :laser-intensity :s})
   :m3 #(rename-keys % {:laser-intensity :s})
   :m4 #(rename-keys % {:laser-intensity :s})})

(def ->gcode
  {:g0 (comp (partial format "g0%s") args)
   :g1 (comp (partial format "g1%s") args)
   :g5 (comp (partial format "g5%s") args)
   :g90 (constantly "g90")
   :g91 (constantly "g91")
   :m3 (comp (partial format "m3%s") args)
   :m4 (comp (partial format "m4%s") args)})

(defn m4
  [{:keys [s]}]
  (format "m4 s%d" s))

(defn g0
  [argv]
  (str "g0" (args argv)))

(defn g1
  [argv]
  (str "g1" (args argv)))

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
   (fn [[gcode state] path]
     (let [rename (get ->renames (:type path))
           generate (get ->gcode (:type path))
           gmap (rename (merge path config))]
       [(str gcode "\n"
             (generate gmap state))
        (-> (assoc state :f (:f gmap))
            (assoc :s (:s gmap)))]))
   ["" {}]
   paths))
