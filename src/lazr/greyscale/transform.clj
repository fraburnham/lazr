(ns lazr.greyscale.transform
  (:require [clojure.math :as math])
  (:import java.awt.Color
           java.awt.image.BufferedImage
           java.awt.image.IndexColorModel))

(def type->key
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

(defn ->indexed
  [image num-greys]
  (let [type (type->key (.getType image))]
    (when-not (or (= type :ushort-gray)
                  (= type :byte-gray))
      (throw (ex-info "Cannot convert to indexed grey image. Must be :ushort-gray or :byte-gray" {:type type}))))
  (let [step (/ 256 num-greys)
        values (byte-array (map #(math/floor (+ % (/ step 2))) (range 0 255 step))) ; values are in the middle of their step
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
