#!/usr/bin/env run_clj2js

(ns _ (:require [js.fs.promises :as fs]))

(defn- rec_parse [x]
  (cond
    (= null x) x
    (Array.isArray x) (.map x rec_parse)
    (= (type x) "string") (if (or (.startsWith x "[") (.startsWith x "{")) (rec_parse (JSON.parse x)) x)
    (= (type x) "object") (and x (-> (Object.entries x)
                                     (.reduce (fn [a x] (assoc a (get x 0) (rec_parse (get x 1)))) {})))
    :else x))

(->
 (fs/readFile ".github/bin/db_result.json" "utf-8")
 (.then (fn [data]
          (println (JSON.stringify (rec_parse data) null 2)))))
