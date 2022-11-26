(ns lazr.core
  (:require [lazr.greyscale :as grey]))

(def help)
(def commands
  {:help {:description "Get top level command list and descriptions"
          :fn help}
   :greyscale {:description "Convert rasters to greyscales suitable for engraving"
               :fn grey/command}})

(defn help
  [& _]
  (println "Usage: lazr [command] ...")
  (println "For more information about a command: lazr [command] help")
  (println)
  (doseq [command (keys commands)]
    (println "\t" (name command) "\t" (get-in commands [command :description]))))

(defn command->fn
  [command]
  (get-in commands [command :fn]))

(defn -main
  [& args]
  (let [command (keyword (first args))]
    (apply (command->fn command) (rest args))))
