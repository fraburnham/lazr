(ns lazr.greyscale
  (:require [clojure.math :as math])
  (:import java.awt.image.BufferedImage
           ;;java.awt.image.DataBuffer
           java.awt.image.IndexColorModel))

;; TODO
;; (defn ->grey
;;   [image]
;;     (let [step (/ 256 num-greys)
;;         values (byte-array (range 0 255 step))
;;         color-model (IndexColorModel. 8 num-greys values values values)
;;         new-image (BufferedImage. (.getWidth image)
;;                                   (.getHeight image)
;;                                   BufferedImage/TYPE_BYTE_INDEXED
;;                                   color-model)
;;         in-rast (.getData image)
;;         in (.getDataBuffer in-rast)
;;         out-rast (.getData new-image)
;;         out (.getDataBuffer out-rast) ;; Is there a better way? Like getting a writable raster?
;;         scale (make-scaler [0 255] 0 [0 (bit-shift-left 1 (.getPixelSize (.getColorModel image)))])]
;;     (println data-elements (.getTransferType in-rast)  (type->key (.getType image)) (.getPixelSize (.getColorModel image)))
;;     ;; It's an alpha channel. Need to detect that and work around it. Works fine for 16bit grey?
;;     ;; No it doesn't work for 16bit grey. The scaler is fucked. 
;;     #_(dotimes [i (.getSize out)]
;;         (let [value (->> (map (fn [j]
;;                               ;;(println i (* i data-elements))
;;                               ;(.getElem in (+ j (* i data-elements)))
;;                                 (bit-shift-left (.getElem in (+ j (* i data-elements))) (* 8 j)))
;;                               (range data-elements))
;;                          (apply +)
;;                          (scale)
;;                          (#(/ % step))
;;                          (int))]
;;         ;(println value)
;;           (.setElem out i value)))
;;     (.setData new-image out-rast)
;;     new-image)
;;   (let [new-image (BufferedImage. (.getWidth image)
;;                                   (.getHeight image)
;;                                   BufferedImage/TYPE_BYTE_GRAY)]
;;     ;; I think there is some fuckery due to numDataElements
;;     ;; there is definitely some fuckery here due to numDataElements
;;     ;; likely some real conversion needs to happen to make this greyscale
;;     ;; OR is the issue the size of the data? pixelBits was 24
;;     ;; (.getPixelSize (.getColorModel image))
;;     (.setData new-image (.getData image))
;;     new-image))

(defn make-scaler
  [[out-min out-max] offset [in-min in-max]]
  (fn [val]
    (+ (/ (* (- val in-min)
             (- out-max out-min))
          (- in-max in-min))
       out-min
       offset)))

(defn- bytes-max
  [b]
  (math/pow 2.0 (* 8 b)))

;; (def transfer-type->key
;;   DataBuffer/TYPE_BYTE :byte
;;   DataBuffer/TYPE_DOUBLE :double
;;   DataBuffer/TYPE_FLOAT :float
;;   DataBuffer/TYPE_INT :int
;;   DataBuffer/TYPE_SHORT :short
;;   DataBuffer/TYPE_UNDEFINED :undefined
;;   DataBuffer/TYPE_USHORT :ushort)

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

;; (def type-has-alpha
;;   {:3-byte-abgr true
;;    :4-byte-abgr true
;;    :4-byte-abgr-pre true
;;    :int-argb true
;;    :int-argb-pre true})

(defn ->indexed-grey
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

