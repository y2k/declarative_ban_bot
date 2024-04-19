(ns _ (:require [effects :as e]
                [main :as app]
                [js.fs.promises :as fs]))

(defn- rec_parse [x]
  (cond
    (= null x) x
    (Array/isArray x) (.map x rec_parse)
    (= (typeof x) "object") (and x (-> (Object/entries x)
                                       (.reduce (fn [a x] (assoc a (get x 0) (rec_parse (get x 1)))) {})))
    (= (typeof x) "string") (if (.startsWith x "{") (rec_parse (JSON/parse x)) x)
    :else x))

(defn- assert [path]
  (->
   (fs/readFile (.replace path "/input/" "/output/") "utf-8")
   (.catch (fn [] null))
   (.then
    (fn [output_json]
      (->
       (fs/readFile path "utf-8")
       (.then
        (fn [input_json]
          (let [event (JSON/parse input_json)
                actual_log []
                output_log (JSON/parse (or output_json "[]"))]
            (defn- test_eff_handler [name args]
              (.push actual_log {:key name :data args})
              (Promise/resolve [name args]))
            (->
             (app/handle_event event.cofx event.key event.data)
             (e/run_effect {:perform test_eff_handler})
             (.then (fn []
                      (if (= output_json null)
                        (fs/writeFile (.replace path "/input/" "/output/") (JSON/stringify (rec_parse actual_log) null 4))
                        (let [expected (JSON/stringify (rec_parse output_log))
                              actual (JSON/stringify (rec_parse actual_log))]
                          (if (= expected actual)
                            null
                            (FIXME "Log: " (.replace path "/input/" "/output/") "\n" expected "\n<>\n" actual "\n")))))))))))))))

(let [path "../test/commands/input/"]
  (->
   (fs/readdir path)
   (.then
    (fn [files]
      (->
       files
       (.filter (fn [name] (.test (RegExp. "\\.json$") name)))
       (.map (fn [name] (assert (str path name)))))))))
