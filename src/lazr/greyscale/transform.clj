(ns lazr.greyscale.transform
  (:require [clojure.math :as math])
  (:import java.awt.Color
           java.awt.image.BufferedImage
           java.awt.image.IndexColorModel))

;; https://docs.oracle.com/javase/8/docs/api/java/awt/image/BufferedImage.html
(def image-type->key
  {BufferedImage/TYPE_3BYTE_BGR :3-byte-bgr
   BufferedImage/TYPE_4BYTE_ABGR :4-byte-abgr
   BufferedImage/TYPE_4BYTE_ABGR_PRE :4-byte-abgr-pre
   BufferedImage/TYPE_BYTE_BINARY :byte-binary
   BufferedImage/TYPE_BYTE_GRAY :byte-gray
   BufferedImage/TYPE_BYTE_INDEXED :byte-indexed
   BufferedImage/TYPE_CUSTOM :custom
   BufferedImage/TYPE_INT_ARGB :int-argb
   BufferedImage/TYPE_INT_ARGB_PRE :int-argb-pre
   BufferedImage/TYPE_INT_BGR :int-bgr
   BufferedImage/TYPE_INT_RGB :int-rgb
   BufferedImage/TYPE_USHORT_555_RGB :ushort-555-rgb
   BufferedImage/TYPE_USHORT_565_RGB :ushort-565-rgb
   BufferedImage/TYPE_USHORT_GRAY :ushort-gray})

(defn make-scaler
  [[out-min out-max] offset [in-min in-max]]
  (fn [val]
    (+ (/ (* (- val in-min)
             (- out-max out-min))
          (- in-max in-min))
       out-min
       offset)))

(defn ->weighted
  [weights image]
  (let [width (.getWidth image)
        height (.getHeight image)
        new-image (BufferedImage. width
                                  height
                                  BufferedImage/TYPE_BYTE_GRAY)
        rast (.getData new-image)
        data (.getDataBuffer rast)]
    (dotimes [x width]
      (dotimes [y height]
        (let [color (Color. (.getRGB image x y))
              r (* (.getRed color) (:r weights))
              g (* (.getGreen color) (:g weights))
              b (* (.getBlue color) (:b weights))
              value (->> (+ r g b)
                         (int))]
          (.setElem data (+ (* y width) x) value))))
    (.setData new-image rast)
    new-image))

(def ->avg (partial ->weighted {:r 1/3 :g 1/3 :b 1/3}))

(def ->luminosity (partial ->weighted {:r 0.299 :g 0.587 :b 0.114}))

(defn- indexed-greys
  "Shifts the number of white bias greys defined by the in ratio up to the out ratio portion of the output [0, max)
  (indexed-greys 5 3 1/2 10) ; I want 5 greys. Give the top 3 of them a color that is in the top 1/2 of [0, 10)
    => (0 2 5 6 8)"
  ([num-greys in out]
   (indexed-greys num-greys in out 256))
  ([num-greys in out max]
   (let [squash-range (* out max)
         expand-range (- max squash-range)
         expand-step-size (/ expand-range (- num-greys in))
         squash-step-size (/ squash-range in)]
     (byte-array
      (map
       int
       (concat
        (map #(+ % (/ expand-step-size 2))
             (range 0 expand-range expand-step-size))
        (map #(+ % (/ squash-step-size 2))
             (range expand-range max squash-step-size))))))))

;; TODO: long term I wonder if I could use a histogram to determine how to shift the values automagically...
;; TODO: spec for squash?
(defn ->indexed
  [image {:keys [c s u]}]
  (let [type (image-type->key (.getType image))]
    (when-not (or (= type :ushort-gray)
                  (= type :byte-gray))
      (throw (ex-info "Cannot convert to indexed grey image. Must be :ushort-gray or :byte-gray" {:type type}))))
  (let [num-greys c
        in (or s 1)
        out (or u (/ 1 num-greys))
        step (/ 256 num-greys)
        values (indexed-greys num-greys in out)
        color-model (IndexColorModel. 8 num-greys values values values)
        new-image (BufferedImage. (.getWidth image)
                                  (.getHeight image)
                                  BufferedImage/TYPE_BYTE_INDEXED
                                  color-model)
        rast (.getData image)
        data (.getDataBuffer rast)
        scale (make-scaler [0 255] 0 [0 (bit-shift-left 1 (.getPixelSize (.getColorModel image)))])]
    (dotimes [i (.getSize data)]
      (let [value (->> (.getElem data i)
                       (scale)
                       (#(/ % step))
                       (math/floor))]
        (.setElem data i value)))
    (.setData new-image rast)
    new-image))
