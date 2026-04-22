(ns build (:require [make :as m]))

(deps {:make "0.5.0"})

(m/build-simple
 {:out ".github/bin"
  :target "js"
  :deps [["effects-promise" "0.3.0"]]})
