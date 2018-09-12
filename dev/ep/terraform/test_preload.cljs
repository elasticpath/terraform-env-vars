(ns ep.terraform.test-preload
  (:require [cljs.spec.alpha :as s]
            [cljs.test :as test]
            [expound.alpha :as expound]
            [pjstadig.print :as humane-print]
            [pjstadig.util :as humane-util]
            [pjstadig.humane-test-output]))

(s/check-asserts true)

(goog-define COLORS true)

(set! s/*explain-out* (expound/custom-printer {:theme (if ^boolean COLORS :figwheel-theme :none)}))

;; needed - see https://github.com/bensu/doo/issues/182
(.on js/process "unhandledRejection" #(throw %))

(defmethod test/report [:cljs.test/default :error] [m]
  (test/inc-report-counter! :error)
  (println "\nERROR in" (test/testing-vars-str m))
  (when (seq (:testing-contexts (test/get-current-env)))
    (println (test/testing-contexts-str)))
  (when-let [message (:message m)] (println message))
  (let [actual (:actual m)
        ex-data (ex-data actual)]
    (if (:cljs.spec.alpha/failure ex-data)
      (do (println "expected:" (pr-str (:expected m)))
          (print "  actual:\n")
          (println (.-message actual)))
      (test/print-comparison m))
    (println (.-stack actual))))

;; :( https://github.com/pjstadig/humane-test-output/pull/26
(def report #'humane-util/report-)

;; using a custom report with humane-test-output + expound
(defmethod test/report [:cljs.test/default :fail] [m]
  (let [actual (:actual m)
        ex-data (ex-data actual)]
    (if (:cljs.spec.alpha/failure ex-data)
      (do (test/inc-report-counter! :fail)
          (when (seq (:testing-contexts (test/get-current-env)))
            (println "--" (:testing-contexts (test/get-current-env))))
          (when-let [message (:message m)] (println message))
          (println "expected:" (pr-str (:expected m)))
          (print "  actual:\n")
          (println (.-message actual)))
      ;; humane does it all
      (report (humane-print/convert-event m)))))
