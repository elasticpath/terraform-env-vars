(ns ep.terraform.env-vars-test
  (:require [clojure.test :as test :refer-macros [deftest is testing]]
            [orchestra-cljs.spec.test :as orchestra]
            [ep.terraform.env-vars :as tenv]
            [fs]
            [goog.object :as gobj]))

(orchestra/instrument)

(deftest environment-var-reducer
  (is (= {}
         (reduce-kv (partial tenv/environment-var-reducer {}) {} {}))
      "should produce an empty map if mappings are empty")

  (is (= {"DBNAME" "event_store"}
         (reduce-kv (partial tenv/environment-var-reducer {:event_store_database_name "DBNAME"})
                    {}
                    {:event_store_database_name
                     {:sensitive false
                      :type      "string"
                      :value     "event_store"}}))
      "should produce the corrent env var map from the mappings"))

(deftest environment-vars-as-source
  (is (= "" (tenv/environment-vars-as-source {}))
      "should produce empty string if the input is an empty map")

  (is (= "" (tenv/environment-vars-as-source nil))
      "should produce empty string if the input is nil")

  (is (= "ENV=dev\nHOSTNAME=some-hostname-app.azurewebsites.net"
         (tenv/environment-vars-as-source {"HOSTNAME" "some-hostname-app.azurewebsites.net"
                                           "ENV"      "dev"}))
      "should produce the correct string"))

(deftest parse-edn-file
  (with-redefs [tenv/read-file-sync (constantly "{:terraform-var-name \"ENV_VARIABLE\"}")]
    (is (= {:terraform-var-name "ENV_VARIABLE"} (tenv/parse-env-var-file-map {:edn "a-edn-file"})) "should correctly parse an edn map")))

(deftest parse-json-file
  (with-redefs [tenv/read-file-sync (constantly "{\"terraform-var-name\": \"ENV_VARIABLE\"}")]
    (is (= {:terraform-var-name "ENV_VARIABLE"} (tenv/parse-env-var-file-map {:json "a-json-file"})) "should correctly parse a JSON file")))

(deftest cli-validation
  (is (some? (:exit-message (tenv/validate-args []))) "should return an exit message when no args")
  (is (some? (:exit-message (tenv/validate-args ["--whatever"]))) "should return an exit message with wrong args")

  (testing "option --edn-file"
    (with-redefs [tenv/file-exists-sync (constantly true)]
      (is (true? (:ok? (tenv/validate-args ["-e" "edn-file"]))) "should return :ok? with -e arg")
      (is (true? (:ok? (tenv/validate-args ["--edn" "edn-file"]))) "should return :ok? with --edn arg")))

  (with-redefs [tenv/file-exists-sync (constantly false)]
    (is (some? (:exit-message (tenv/validate-args ["-e" "edn-file"]))) "should return an exit message with no args when file doesn't exist")
    (is (some? (:exit-message (tenv/validate-args ["--edn" "edn-file"]))) "should return an exit message with no args when file doesn't exist"))

  (testing "option --json-file"
    (with-redefs [tenv/file-exists-sync (constantly true)]
      (is (true? (:ok? (tenv/validate-args ["-j" "json-file"]))) "should return :ok? with -j arg")
      (is (true? (:ok? (tenv/validate-args ["--json" "json-file"]))) "should return :ok? with --json arg"))

    (with-redefs [tenv/file-exists-sync (constantly false)]
      (is (some? (:exit-message (tenv/validate-args ["-j" "json-file"]))) "should return an exit message with no args when file doesn't exist")
      (is (some? (:exit-message (tenv/validate-args ["--json" "json-file"]))) "should return an exit message with no args when file doesn't exist"))))
