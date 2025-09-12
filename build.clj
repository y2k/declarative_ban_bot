;; (ns _ (:require ["vendor/make/0.1.0/main" :as b]))

;; (b/generate
;;  [(b/module
;;    {:lang "js"
;;     :src-dir "src"
;;     :target-dir ".github/bin/src"
;;     :items ["effects" "main" "moderator"]})
;;   (b/module
;;    {:lang "js"
;;     :src-dir "test"
;;     :target-dir ".github/bin/test"
;;     :items ["test" "test_spam"]})
;;   (b/vendor
;;    {:lang "js"
;;     :target-dir ".github/bin/vendor"
;;     :items [{:name "effects" :version "0.1.0"}]})])

(ns _ (:require [".github/vendor/make/0.3.0/main" :as m]))

(m/build
 {:deps {
        ;;  :android_app   "0.1.0"
        ;;  :chat_ui       "0.2.0"
         :effects       "0.1.0"
        ;;  :effects_tools "0.1.0"
        ;;  :random        "0.1.0"
         }
  :compile
  [{:target "js"
    :root "src"
    :namespace "fixme1"
    :out-dir ".github/bin/src"}
   {:target "js"
    :root "test"
    :namespace "fixme2"
    :out-dir ".github/bin/test"}]})
