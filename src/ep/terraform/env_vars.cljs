(ns ep.terraform.env-vars
  (:require [cljs.reader :as reader]
            [cljs.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [expound.alpha :as expound]
            [fs]
            [get-stdin]
            [process]))

(s/def :tf/sensitive boolean?)
(s/def :tf/type string?)
(s/def :tf/value string?)

(s/def :tf/output-entry
  (s/keys :req-un [:tf/sensitive :tf/type :tf/value]))

(s/def :tf/output-key keyword?)

(s/def :tf/output-vars
  (s/map-of keyword? :tf/output-entry :conform-keys true))

(s/def :tf/mappings
  (s/map-of keyword? string? :conform-keys true))

(s/def :tf/env-vars
  (s/map-of string? string? :conform-keys true))

(s/fdef environment-var-reducer
  :args (s/cat :mappings :tf/mappings
               :env-vars :tf/env-vars
               :output-key :tf/output-key
               :output-value :tf/output-entry)
  :ret :tf/env-vars)

(defn environment-var-reducer
  "Given a mapping from tf output keys to environment variable
  names, reduce to a map of environment variables to their values.

  Discards all the key/values not in the mapping."
  [mappings env-vars output-key output-value]
  (if-let [mapped-key (get mappings output-key)]
    (assoc env-vars (str/upper-case mapped-key) (:value output-value))
    env-vars))

(s/fdef environment-vars-as-source
  :args (s/cat :env-vars (s/nilable :tf/env-vars))
  :ret string?)

(defn environment-vars-as-source
  "Given a mapping from tf output keys to environment variable
  names, reduce to a map of environment variables to their values.

  Discards all the key/values not in the mapping."
  [env-vars]
  (->> env-vars
       (sort-by key)
       (reduce
        (fn [acc [k v]]
          (conj acc (str k "=" v)))
        [])
       (str/join "\n")))

(s/fdef read-file-sync
  :args (s/cat :path string?)
  :ret any?)

(defn read-file-sync
  [path]
  (fs/readFileSync path "utf8"))

(s/fdef parse-edn-map
  :args (s/cat :edn-file-path string?)
  :ret map?)

(defn parse-edn-map
  "Reads and parses the contents from the given relative path to an edn file"
  [edn-file-path]
  (-> (read-file-sync edn-file-path)
      (reader/read-string)))

(s/fdef parse-json-map
  :args (s/cat :json-file-path string?)
  :ret map?)

(defn parse-json-map
  "Reads and parses the contents from the given relative path to an edn file"
  [json-file-path]
  (-> (read-file-sync json-file-path)
      (js/JSON.parse)
      (js->clj :keywordize-keys true)))

(s/def ::edn string?)
(s/def ::json string?)

(s/def ::parse-env-var-options (s/or :edn (s/keys :req-un [::edn])
                                     :json (s/keys :req-un [::json])))

(s/fdef parse-env-var-file-map
  :args (s/cat :options ::parse-env-var-options))

(defn parse-env-var-file-map
  "Gets the options passed to the script and figures out what file to parse"
  [options]
  (cond
    (:json options) (parse-json-map (:json options))
    (:edn options) (parse-edn-map (:edn options))))

(defn file-exists-sync
  [path]
  (fs/existsSync path))

(defn assert-tf-output [tf-output env-vars-input-map]
  (doseq [[k v] env-vars-input-map]
    (let [tf-output-var-val (get-in tf-output [k :value])]
      (assert (not (nil? tf-output-var-val)) (str "Didn't find key " k " on terraform output for " v))
      (assert (not (str/blank? tf-output-var-val)) (str "Empty value for key " k " on terraform output for " v)))))

(defn usage-msg
  [options-summary]
  (->> ["Print environment variables for Terraform json output."
        ""
        "The script reads Terrafom json output on stdin, usually the result"
        "of \"terraform output -json\", and produces a list of environment"
        "variables, according to the input mapping file."
        ""
        "It exits (1) in case the json input does not match the mapping or"
        "some variable is empty."
        ""
        "Usage: terraform output -json | terraform-env-vars --edn|--json <path>"
        ""
        "Options:"
        options-summary]
       (str/join \newline)))

(defn error-msg
  [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  "Validate create-publish-pipeline command line arguments.

  Enhances the class tools.cli map with an :exit-message and an :ok? key
  indicating if we should exit with 0 or now (in case of --help it will
  be true for instance)."
  [args]

  (let [cli-options [["-e" "--edn FILE" "EDN file that contains the map of environment variables."
                      :validate [file-exists-sync "The given file doesn't exist"]]
                     ["-j" "--json FILE" "JSON file that contains the map of environment variables."
                      :validate [file-exists-sync "The given file doesn't exist"]]
                     ["-h" "--help"]]
        {:keys [options errors summary] :as opts} (cli/parse-opts args cli-options :strict true)]

    (merge opts
           (cond
             (:help options) {:exit-message (usage-msg summary) :ok? true}

             errors {:exit-message (error-msg errors)}

             (or (:edn options) (:json options)) {:ok? true}

             :else {:exit-message (usage-msg summary)}))))

(defn exit
  [status msg]
  (println msg)
  (process/exit status))

(defn -main [& args]
  (set! s/*explain-out* (expound/custom-printer {:show-valid-values? true
                                                 :print-specs? false
                                                 :theme :figwheel-theme}))
  (let [{:keys [exit-message ok? options]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (-> (get-stdin)
          (.then
           (fn [stdin]
             (let [env-vars-input-map (parse-env-var-file-map options)
                   tf-output (js->clj (js/JSON.parse stdin) :keywordize-keys true)
                   env-vars-output-map (->> tf-output
                                            (s/assert* :tf/output-vars)
                                            (reduce-kv (partial environment-var-reducer env-vars-input-map)
                                                       {}))]

               ;; Ensures that all the vars on input-map are in tf-output
               (assert-tf-output tf-output env-vars-input-map)

               (print (environment-vars-as-source env-vars-output-map)))))
          (.catch #(js/console.error %))))))
