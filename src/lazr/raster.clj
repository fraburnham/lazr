(ns lazr.raster
  (:require [lazr.command :as cmd]
            [lazr.gcode.core :as gcode]
            [lazr.gcode.raster :as raster])
  (:import java.io.File
           javax.imageio.ImageIO))

(def str->int #(Integer/parseInt %)) ; this helper should live somewhere...

(def options
  {:p {:lazr.command/long-opt "pixels-per-mm"
       :lazr.command/description "Number of pixels per mm (often 10 for a line width of 0.1mm)"
       :lazr.command/has-arg true
       :lazr.command/parser str->int}
   :s {:lazr.command/long-opt "max-intensity"
       :lazr.command/description "Maximum laser intensity from 0 to 1000"
       :lazr.command/has-arg true
       :lazr.command/parser str->int}
   :f {:lazr.command/long-opt "speed"
       :lazr.command/description "Speed to travel while engraving"
       :lazr.command/has-arg true
       :lazr.command/parser str->int}
   :i {:lazr.command/long-opt "input"
       :lazr.command/description "Input file"
       :lazr.command/has-arg true}})

(defrecord Command []
  cmd/Command
  (run [_ {:keys [i s p f]} _]
    (println (gcode/encode {:laser-intensity s
                            :laser-speed f
                            :travel-speed f}
                           (raster/->gcode s p (ImageIO/read (File. i))))))

  (help [_] {:lazr.command/description "Convert a greyscale raster to gcode"})

  (options [_] options))
