(ns lazr.photo-cube
  (:require [lazr.command :as cmd]
            [lazr.photo-cube.core :as photo-cube])
  (:import java.io.File
           javax.imageio.ImageIO))

(defrecord Command []
  cmd/Command

  (run [this {:keys [o]} args]
    (if (or (= (first args) "help")
            (not o))
      (cmd/help this)
      (->> (map (fn [path] (ImageIO/read (File. path))) args)
           (photo-cube/generate)
           (spit o))))

  (help [_] {:lazr.command/description (str "Given 5 images create a photo cube that is 110mm^3 with 100mm^2 images\n"
                                            "The fifth image will be on the top of the cube\n"
                                            "photo-cube -o cube.gcode 1.png 2.png 3.png 4.png 5.png")})

  (options [_] {:o {:lazr.command/long-opt "output"
                    :lazr.command/description "Output file"
                    :lazr.command/has-arg true}}))
