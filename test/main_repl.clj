(ns main-repl
  (:require [main :as m]
            ["fs" :as fs]
            ["path" :as path]))

(defn load-txt-files []
  (let [dir "../test/resources/sample"
        files (fs/readdirSync dir)]
    (->> files
         (filter (fn [f] (clojure.string/ends-with? f ".txt")))
         (map (fn [f] (path/join dir f))))))

(let [files (load-txt-files)]
  (println
   (load-txt-files)))

;; (->
;;  (m.default.fetch
;;   {:json (fn [] (Promise/resolve {}))
;;    :headers {:get (fn [] "TG_SECRET_TOKEN")}}
;;   {:TG_SECRET_TOKEN "TG_SECRET_TOKEN"})
;;  (.then (fn [r] (.text r)))
;;  (.then (fn [r] (FIXME __LOC__ " | " r))))
