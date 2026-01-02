(ns _ (:require ["$LY2K_PACKAGES_DIR/make/0.5.0/make" :as m]))

(m/build
 {:rules
  [;; Dependencies
   {:target "dep"
    :name "effects-promise"
    :version "0.3.0"
    :compile_target "js"
    :prelude-path ".github/bin/src/prelude.js"
    :out-dir ".github/bin/src"}
   {:target "dep"
    :name "effects-promise"
    :version "0.3.0"
    :compile_target "js"
    :prelude-path ".github/bin/src/prelude.js"
    :out-dir ".github/bin/test"}
   ;; Sources
   {:target "js"
    ;; :log true
    :root "src"
    :prelude-path ".github/bin/src/prelude.js"
    :out-dir ".github/bin/src"}
   {:target "js"
    :root "src"
    :prelude-path ".github/bin/src/prelude.js"
    :out-dir ".github/bin/test"}
   {:target "js"
    :root "test"
    :prelude-path ".github/bin/src/prelude.js"
    :out-dir ".github/bin/test"}]})
