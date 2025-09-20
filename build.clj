(ns _ (:require ["$LY2K_PACKAGES_DIR/make/0.3.0/main" :as m]))

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
