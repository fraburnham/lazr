(ns lazr.greyscale
  (:require [lazr.greyscale.transform :as grey])
  (:import java.io.File
           java.lang.String
           javax.imageio.ImageIO
           org.apache.commons.cli.Option
           org.apache.commons.cli.Options
           org.apache.commons.cli.DefaultParser
           org.apache.commons.cli.HelpFormatter))

(defn apply-command
  [command input-path output-path & params]
  (as-> (ImageIO/read (File. input-path)) *
    (apply command (cons * params))
    (ImageIO/write * "png" (File. output-path))))

(defn parse-args
  [subcommand options args]
  (let [cli-args (.parse (DefaultParser.) options (into-array String args))]
    (if (.hasOption cli-args "h")
      (.printHelp (HelpFormatter.) (str "lazr greyscale " (name subcommand)) options)
      {:input (.getOptionValue cli-args "i")
       :output (.getOptionValue cli-args "o")
       :cli-args cli-args})))

;; updating this in the fns below is causing mutation
;; it shouldn't be an issue since each call to lazr will be "fresh"
(def options (as-> (Options.) *
               (.addOption * (-> (Option/builder "i") (.longOpt "input") (.hasArg) (.desc "Input file") (.required) (.build)))
               (.addOption * (-> (Option/builder "o") (.longOpt "output") (.hasArg) (.desc "Output file (will be written as png)") (.required) (.build)))
               (.addOption * "h" "help" false "Display option help")))

(defn average
  [& args]
  (let [{:keys [input output]} (parse-args :average options args)]
    (apply-command grey/->avg input output)))

(defn luminosity
  [& args]
  (let [{:keys [input output]} (parse-args :luminosity options args)]
    (apply-command grey/->luminosity input output)))

(defn indexed
  [& args]
  (let [options (.addOption options (-> (Option/builder "c") (.longOpt "num-greys") (.hasArg) (.desc "Number of grey colors to use in output") (.required) (.build)))
        {:keys [cli-args input output]} (parse-args :indexed options args)
        num-greys (Integer/parseInt (.getOptionValue cli-args "c"))]
    (apply-command grey/->indexed input output num-greys)))

(defn command
  [& args]
  (let [subcommand->fn {:average average ; this neesd to be the same format as others for help consistency (do help last ugh)
                        :indexed indexed
                        :luminosity luminosity}
        subcommand (keyword (first args))]
    (apply (subcommand->fn subcommand) (rest args))))

