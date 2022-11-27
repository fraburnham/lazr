(ns lazr.scale
  (:require [lazr.command :as cmd])
  (:import java.awt.Image
           java.awt.image.BufferedImage
           java.io.File
           javax.imageio.ImageIO))

(defn scale
  [pixels-per-mm width height image]
  (let [width-pixels (* width pixels-per-mm)
        height-pixels (* height pixels-per-mm)
        out-buffer (BufferedImage. width-pixels height-pixels (.getType image))]
    (-> (.getGraphics out-buffer)
        (.drawImage (.getScaledInstance image width-pixels height-pixels Image/SCALE_SMOOTH) 0 0 nil))
    out-buffer))

(def str->int #(Integer/parseInt %))

(def options
  {:s {:lazr.command/long-opt "steps-per-mm"
       :lazr.command/description "Number of steps per mm (often 10 for a line width of 0.1mm)"
       :lazr.command/has-arg true
       :lazr.command/parser str->int}
   :x {:lazr.command/long-opt "width"
       :lazr.command/description "Width of the engraved image in mm"
       :lazr.command/has-arg true
       :lazr.command/parser str->int}
   :y {:lazr.command/long-opt "height"
       :lazr.command/description "Height of the engraved image in mm"
       :lazr.command/has-arg true
       :lazr.command/parser str->int}
   :i {:lazr.command/long-opt "input"
       :lazr.command/description "Input file"
       :lazr.command/has-arg true}
   :o {:lazr.command/long-opt "output"
       :lazr.command/description "Output file"
       :lazr.command/has-arg true}})

(defrecord Command []
  cmd/Command

  (run [this opts args]
    (try
      (cmd/dispatch {:help (cmd/->HelpCommand "lazr scale [OPTIONS]"
                                              (:lazr.command/description (cmd/help this))
                                              options
                                              nil)}
                    args)
      (catch clojure.lang.ExceptionInfo e
        (if (= :lazr.command/no-command (:lazr.command/type (ex-data e)))
          (let [in (:i opts)
                out (:o opts)
                pixels-per-mm (:s opts)
                height (:y opts)
                width (:x opts)]
            (as-> (ImageIO/read (File. in)) *
              (scale pixels-per-mm width height *)
              (ImageIO/write * "png" (File. out))))
          (throw e)))))

  (help [_] {:lazr.command/description "Scale rasters before converting to gcode"})

  (options [_] options))
