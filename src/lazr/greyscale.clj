(ns lazr.greyscale
  (:require [lazr.greyscale.transform :as grey]
            [lazr.command :as cmd])
  (:import java.io.File
           javax.imageio.ImageIO))

(defn apply-command
  [command input-path output-path params]
  (as-> (ImageIO/read (File. input-path)) *
    (apply command (cons * params))
    (ImageIO/write * "png" (File. output-path))))

(def options
  {:i {:lazr.command/long-opt "input"
       :lazr.command/description "Input file"
       :lazr.command/has-arg true}
   :o {:lazr.command/long-opt "output"
       :lazr.command/description "Output file"
       :lazr.command/has-arg true}})

(defrecord SubCommand [description options fn]
  cmd/Command
  (run [_ opts _]
    ;; handle help case in here!
    ;; the dispatch and catch way
    (apply-command fn (:i opts) (:o opts) (vals (dissoc opts :i :o))))

  (help [_] {:lazr.command/description description})

  (options [_] options))

(def commands
  {:average (->SubCommand "Convert an image to grey based on the average of each pixel's R, G, and B values" options grey/->avg)
   :luminosity (->SubCommand "Convert an image to grey based on luminosity" options grey/->luminosity)
   :indexed (->SubCommand "Simplify a greyscale image by indexing"
                          (merge options {:c {:lazr.command/long-opt "num-greys"
                                              :lazr.command/description "Number of grey colors to use in output"
                                              :lazr.command/parser #(Integer/parseInt %)
                                              :lazr.command/has-arg true}})
                          grey/->indexed)})

(def desc "Convert rasters to greyscale for engraving")

(defrecord Command []
  cmd/Command
  (run [_ _ args]
    (let [help (cmd/->HelpCommand "lazr greyscale [COMMAND] [OPTIONS]" desc {} commands)]
      (try
        (cmd/dispatch (assoc commands :help help) args)
        (catch clojure.lang.ExceptionInfo _
          (cmd/run help nil nil)))))

  (help [_] {:lazr.command/description desc
             :lazr.command/commands commands})

  (options [_] nil))
