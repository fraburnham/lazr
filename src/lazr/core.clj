(ns lazr.core
  (:gen-class)
  (:require [lazr.greyscale :as grey]
            [lazr.scale :as scale]))

(def commands
  {:help {:description "Get top level command list and descriptions"
          :fn   (fn [& _]
                  (println "Usage: lazr [command] ...")
                  (println "For more information about a command: lazr [command] help")
                  (println)
                  (doseq [command (keys commands)]
                    (println "\t" (name command) "\t" (get-in commands [command :description]))))}
   :greyscale {:description "Convert rasters to greyscales suitable for engraving"
               :fn grey/command}
   :scale {:description "Scale rasters to prepare for gcode generation"
           :fn scale/command}})

(defn command->fn
  [command]
  (get-in commands [command :fn]))

(defn -main
  [& args]
  (let [command (keyword (first args))]
    (apply (command->fn command) (rest args))))

