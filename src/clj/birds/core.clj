(ns birds.core
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as str]
            [birds.validators :as valid]
            [birds.runner :as runner]
            [birds.simulate :as sim])
  (:gen-class))

(defn param-range [values] (sim/values-range values))
(defn normal-range [values] (apply range values))
(defn slurper [filename]
  (try
    (slurp filename)
    (catch Exception _)))

(defn get-settings
  [settings-file defaults]
  (->> [settings-file "config.edn" (io/resource "config.edn")]
       (map slurper)
       (remove nil?)
       first
       (edn/read-string {:readers {'birds/param-range param-range 'birds/range normal-range}})
       (merge defaults)))

(def cli-options
  ;; An option with a required argument
  [["-n" "--times NUMBER" "The number of tries to run per settings combination"
    :default 1
    :parse-fn #(Integer/parseInt %)]
   ["-t" "--ticks NUMBER" "The number of ticks per run"
    :default 10000
    :parse-fn #(Integer/parseInt %)]
   ["-w" "--width NUMBER" "The width of the area simulated"
    :default 1000
    :parse-fn #(Integer/parseInt %)]
   ["-H" "--height NUMBER" "The height of the area simulated"
    :default 1000
    :parse-fn #(Integer/parseInt %)]
   ["-l" "--log LEVEL" "The log level to be used"]
   ["-c" "--config FILENAME" "The config file to use"]
   ["-h" "--help"]])

(defn usage [summary]
  (str/join
   "\r\n"
   ["Simulate birds singing in a test area."
    ""
    "A CSV file is outputted with the number of birds that sang and the number that resang, along with the configuration for that specific run."
    ""
    summary]))

(defn format-errors [errors]
  (->> errors
       (map (fn [[k problems]]
              (->> problems
                   (map (partial str "-> "))
                   (into ["" (str (name k) ":")]))))
       flatten
       (into ["The following configuration options have invalid values:"])
       (str/join "\r\n")))

(defn exit
  ([message] (exit 0 message))
  ([status message]
   (when message (println message))
   (shutdown-agents)
   (System/exit status)))

(defn -main [& args]
  (let [{:keys [options arguments summary]} (parse-opts args cli-options)
        settings (get-settings (:config options) options)
        output-file (or (first arguments) "output.csv")
        errors (valid/validate-settings settings)]
    (cond
      (:help options) ; help => exit OK with usage summary
      (exit (usage summary))

      errors ; errors => exit with description of errors
      (exit 1 (format-errors errors))

      (= (-> settings (dissoc :config :log) keys set) #{:width :height :times :ticks})
      (exit (usage "Requires a valid config file to be provided"))

      :else
      (exit 0 (runner/run-simulations! settings output-file)))))
