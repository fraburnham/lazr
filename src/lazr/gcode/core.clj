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
      ;; can skip f,s,x,y if they haven't changed
      (when (and (number? x) (not= (:x state) x)) (format " x%.4f" (float x)))
      (when (and (number? y) (not= (:y state) y)) (format " y%.4f" (float y)))
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
   :m4 (comp (partial format "m4%s") args)
   :m5 (constantly "m5")})

(defn encode
  [config paths]
  (apply
   str
   (first
    (reduce
     (fn [[gcode state] path]
       (let [rename (get ->renames (:type path) identity)
             generate (get ->gcode (:type path) (fn [& _] (throw (ex-info "No gcode generator found for type" {:path path}))))
             gmap (merge (rename config) (rename path))]
         [(conj gcode "\n" (generate gmap state))
          (-> (assoc state :f (:f gmap))
              (assoc :s (:s gmap))
              (assoc :x (:x gmap))
              (assoc :y (:y gmap)))]))
     [[] {}]
     paths))))
