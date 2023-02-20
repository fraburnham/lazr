(ns lazr.metadata
  (:require [clojure.string :as str])
  (:import org.apache.commons.imaging.Imaging))

(defn read
  ; org.apache.commons.imaging.ImageParser and org.apache.commons.imaging.common.bytesource.ByteSource
  [parser byte-source]
  (reduce
   (fn [r e]
     (assoc r (keyword (str/replace (.getKeyword e) " " "-")) (.getText e)))
   {}
   (.getItems (.getMetadata parser byte-source nil))))

;; maybe this _is_ all dumb. Treat the dpi as whatever the user inputs? And don't ask for dpi ask for steps per mm. (need to start reading in an edn config file)
;; so the gcode generator will care about the image, steps/mm and desired output size.
;; not gcode generator, some final, pre-gcode step. Call it "prepare raster" (later there will probably be "prepare vector" for scaling vectors before gcoding)
