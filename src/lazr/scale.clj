(ns lazr.scale
  (:import java.awt.Image
           java.awt.image.BufferedImage
           java.io.File
           javax.imageio.ImageIO
           org.apache.commons.cli.Option
           org.apache.commons.cli.Options
           org.apache.commons.cli.DefaultParser
           org.apache.commons.cli.HelpFormatter))

(defn scale
  [pixels-per-mm width height image]
  (let [width-pixels (* width pixels-per-mm)
        height-pixels (* height pixels-per-mm)
        out-buffer (BufferedImage. width-pixels height-pixels (.getType image))]
    (-> (.getGraphics out-buffer)
        (.drawImage (.getScaledInstance image width-pixels height-pixels Image/SCALE_SMOOTH) 0 0 nil))
    out-buffer))

(defn command
  [& args]
  (let [options (as-> (Options.) *
                  (.addOption * (-> (Option/builder "s")
                                    (.longOpt "steps-per-mm")
                                    (.hasArg)
                                    (.required)
                                    (.desc "Number of steps per mm (often 10 for a line width of 0.1mm)")
                                    (.build)))
                  (.addOption * (-> (Option/builder "x")
                                    (.longOpt "width")
                                    (.hasArg)
                                    (.required)
                                    (.desc "Width of the engraved image in mm")
                                    (.build)))
                  (.addOption * (-> (Option/builder "y")
                                    (.longOpt "height")
                                    (.hasArg)
                                    (.required)
                                    (.desc "Height of the engraved image in mm")
                                    (.build)))
                  (.addOption * (-> (Option/builder "i")
                                    (.longOpt "input")
                                    (.hasArg)
                                    (.desc "Input file")
                                    (.required)
                                    (.build)))
                  (.addOption * (-> (Option/builder "o")
                                    (.longOpt "output")
                                    (.hasArg)
                                    (.desc "Output file (will be written as png)")
                                    (.required)
                                    (.build)))
                  ;; help is busted due to some require stuff.
                  (.addOption * "h" "help" false "Display option help"))
        cli-args (.parse (DefaultParser.) options (into-array String args))]
    (if (.hasOption cli-args "h")
      (.printHelp (HelpFormatter.) (str "lazr scale") options)
      (let [in (.getOptionValue cli-args "i")
            out (.getOptionValue cli-args "o")
            pixels-per-mm (Integer/parseInt (.getOptionValue cli-args "s"))
            height (Integer/parseInt (.getOptionValue cli-args "y"))
            width (Integer/parseInt (.getOptionValue cli-args "x"))]
        (as-> (ImageIO/read (File. in)) *
          (scale pixels-per-mm width height *)
          (ImageIO/write * "png" (File. out)))))))
