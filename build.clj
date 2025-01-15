(ns _ (:require ["vendor/make/0.1.0/main" :as b]))

(b/generate
 [(b/module
   {:lang "js"
    :src-dir "src"
    :target-dir ".github/bin/src"
    :items ["effects" "main" "moderator"]})
  (b/module
   {:lang "js"
    :src-dir "test"
    :target-dir ".github/bin/test"
    :items ["test" "test_spam"]})
  (b/vendor
   {:lang "js"
    :target-dir ".github/bin/vendor"
    :items [{:name "effects" :version "0.1.0"}]})])
