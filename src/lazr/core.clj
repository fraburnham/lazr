(ns lazr.core
  (:gen-class)
  (:require [lazr.command :as cmd]
            [lazr.greyscale :as grey]
            [lazr.photo-cube :as photo-cube]
            [lazr.scale :as scale]))

(defn -main
  [& args]
  (let [commands (as-> {:greyscale (grey/->Command)
                        :scale (scale/->Command)
                        :photo-cube (photo-cube/->Command)} *
                   (assoc * :help (cmd/->HelpCommand "lazr [COMMAND] [OPTIONS]" "Work with rasters, vectors and gcode for laser engravers" {} *)))]
    (cmd/dispatch commands args)))
