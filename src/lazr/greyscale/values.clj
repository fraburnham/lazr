(ns lazr.greyscale.values)

(defn- filter-
  "Keep all kvp whose (> k pivot)"
  [hist pivot]
  (filter (fn [[k _]] (> k pivot)) hist))

(defn- sum-
  [hist]
  (reduce (fn [r [_ v]] (+ r v)) 0 hist))

(defn histogram
  [image]
  (let [data (.getDataBuffer (.getRaster image))]
    (reduce
     (fn [histogram i]
       (let [val (.getElem data i)]
         ;; need to skip working on indexed images for now?
         ;; I think there is an answer by pulling the values out of the color-space or color-model
         ;; but in the short term working with 8bit greyscale images is probably fine since the incoming
         ;; images can be considered as color
         (update histogram val (fn [c] (if (nil? c) 1 (inc c))))))
     {}
     (range (.getSize data)))))

(defn cut
  [max hist cut]
  (loop [values (reverse (sort (keys hist)))
         c 0]
    (let [val (first values)
          c (+ c (hist val))]
      (if (>= c cut)
        (- 1 (/ (second values) max))
        (recur (rest values) c)))))

(defn distribution
  [image]
  (let [hist (histogram image)
        filter- (partial filter- hist)
        cut (partial cut 255 hist)
        total-pixels (* (.getWidth image) (.getHeight image))
        get-pct #(* 100 (float (/ (sum- (filter- (* 255 %))) total-pixels)))]
    {:25 (get-pct 1/4)
     :33 (get-pct 1/3)
     :50 (get-pct 1/2)
     :66 (get-pct 2/3)
     :75 (get-pct 3/4)
     :90 (get-pct 9/10)
     :half-cut (cut (* total-pixels 1/2))
     :third-cut (cut (* total-pixels 2/3))
     :quarter-cut (cut (* total-pixels 3/4))}))



;; Now think about how to use these values to get to the desired values
;; Iterative upsquash until they're high enough?
;; Say 16 colors squashing top 1/2 into top 2/3 until they're done?
;; I have an idea, figure out where half (more than but not less than) of the colors are pct wise
;; then squash that fraction into the top 2/3

;; use (int (* num-greys (:third-cut (distribution image)))) for now, for the current plywood...
