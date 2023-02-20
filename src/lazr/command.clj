(ns lazr.command
  (:require [clojure.spec.alpha :as spec])
  (:import org.apache.commons.cli.Option
           org.apache.commons.cli.Options
           org.apache.commons.cli.DefaultParser
           org.apache.commons.cli.HelpFormatter))

(defprotocol Command
  ;; TODO: would be nice to have fdef on these fns, too
  (run [this options args] "Handle the command")
  (help [this] "Return :lazr.command/help for this command")
  (options [this] "Return a list of :lazr.command/option for this command"))

(spec/def ::args (spec/and coll? #(every? string? %)))
(spec/def ::opt (spec/and keyword?
                          #(= (count (name %)) 1)))
(spec/def ::long-opt (spec/and keyword?
                               #(> (count (name %)) 1)))
(spec/def ::has-arg boolean?)
(spec/def ::description (spec/and string?
                                  not-empty))
(spec/def ::parser fn?)

(spec/def ::command-name keyword?)
(spec/def ::command #(satisfies? Command %))
(spec/def ::commands (spec/and map?
                               #(every? (fn [[k v]]
                                          (and (spec/valid? ::command-name k)
                                               (spec/valid? ::command v)))
                                        %)))
(spec/def ::option (spec/keys :req [::description]
                              :opt [::long-opt ::has-arg ::parser]))
(spec/def ::options (spec/and map?
                              #(every? (fn [[opt option]]
                                        (and (spec/valid? ::option option)
                                             (spec/valid? ::opt opt)))
                                       %)))
(spec/def ::help (spec/keys :req [::description]
                            :opt [::commands]))

(defn- parse-options
  [options cli-opts]
  (reduce
   (fn [r opt]
     (let [opt-key (keyword (.getOpt opt))]
       (assoc r opt-key (if-let [parser (get-in options [opt-key ::parser])]
                          (parser (.getValue opt))
                          (.getValue opt)))))
   {}
   (.getOptions cli-opts)))

(defn- ->commons-options
  [options]
  (reduce
   (fn [r [opt option]]
     (.addOption r (cond-> (Option/builder (name opt))
                     (::long-opt option) (.longOpt (name (::long-opt option)))
                     (::description option) (.desc (::description option))
                     (::has-arg option) (.hasArg)
                     :always (.build))))
   (Options.)
   options))

(defn help-er
  [command command-string]
  (let [cmd-help (help command)]
    (println (::description cmd-help) "\n")
    (.printHelp (HelpFormatter.) command-string (->commons-options (options command)))
    (when-let [subcommands (::commands cmd-help)]
      (println "\nCommands:")
      (doseq [[n cmd] subcommands]
        (println "\t" (name n) "\t" (::description (help cmd)))))))

(spec/fdef help-er
  :args (spec/cat :help #(spec/valid? ::help %)
                  :options #(spec/valid? ::options %))
  :ret nil?)

(defn dispatch
  [commands args]
  (if-let [raw-command (first args)]
    (if-let [command ((keyword raw-command) commands)]
      (let [cli-opts (.parse (DefaultParser.) (->commons-options (options command)) (into-array String (rest args)) true)]
        (run command (parse-options (options command) cli-opts) (.getArgList cli-opts)))
      (throw (ex-info "Failed to get command" {:args args :commands commands :attempted (keyword (first args)) ::type ::failed-to-get-command})))
    (throw (ex-info "No command" {:args args :commands commands ::type ::no-command}))))

(defrecord HelpCommand [command-string description options commands]
  Command
  (run [this _ _]
    (help-er this command-string))
  (help [_] {::commands commands
             ::description description})
  (options [_] options))

;; TODO: positional args parsing
;; TODO: add key-fn to make handling the name easier downstream

