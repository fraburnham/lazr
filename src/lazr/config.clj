(ns lazr.config)

;; build a config map in here based on merging config files with defaults
;; this will have to be called so the overriding config can be passed
(defn ->config
  []
  {:correction {:cutoff :third-cut}})
