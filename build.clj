(ns build (:require ["$LY2K_PACKAGES_DIR/make/0.5.0/make" :as m]))

(def- prelude ".github/bin/src/prelude.js")
(def- out-src ".github/bin/src")
(def- out-test ".github/bin/test")

(def- effects-promise
  {:target "dep"
   :name "effects-promise"
   :version "0.3.0"
   :compile_target "js"
   :prelude-path prelude})

(m/build
 {:rules
  [;; Dependencies
   (assoc effects-promise :out-dir out-src)
   (assoc effects-promise :out-dir out-test)
   ;; Sources
   {:target "js" :root "src" :prelude-path prelude :out-dir out-src}
   {:target "js" :root "src" :prelude-path prelude :out-dir out-test}
   {:target "js" :root "test" :prelude-path prelude :out-dir out-test}]})
