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
      (->> (map (fn [path]
                  ;; add code in here to attach the extension so it can be used later for dispatch
                  (ImageIO/read (File. path))) args)
           (photo-cube/generate)
           (spit o))))

  (help [_] {:lazr.command/description (str "Given up to 6 images create a photo cube that is 110mm^3 with 100mm^2 images\n"
                                            "The fifth image will be on the top of the cube\n"
                                            "The sixth image will be on the bottom of the cube\n"
                                            "photo-cube -o cube.gcode 1.png 2.png 3.png 4.png 5.png 6.gcode")})

  (options [_] {:o {:lazr.command/long-opt "output"
                    :lazr.command/description "Output file"
                    :lazr.command/has-arg true}}))

;; TODO: make it possible to pass in png or svg or gcode for any of the *six* faces (with 5 being top and 6 being bottom)
;; this will allow logo insertion (as gcode for speed), so just start there. Any face can be png (go the normal way)
;; or gcode (just drop it in as-is; this might be quite hard as things stand... Some type detection?)
;; WAIT A MIN! The gcode doesn't _have_ to be in order. I can append the engravings at the end (ideally before the cutouts)
