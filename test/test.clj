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
    (fn [log_json]
      (->
       (fs/readFile path "utf-8")
       (.then
        (fn [event_json]
          (let [event (JSON/parse event_json)
                fx_log []
                log (.reverse (JSON/parse (or log_json "[]")))]
            (defn- test_eff_handler [name args]
              (if (= :fork name)
                (args {:perform test_eff_handler})
                (if (> log.length 0)
                  (let [x (.pop log)
                        expected (JSON/stringify [x.key x.data])
                        actual (JSON/stringify (rec_parse [name args]))]
                    (if (= expected actual)
                      (Promise/resolve [name args])
                      (FIXME "Log: " (.replace path "/input/" "/output/") "\n" expected "\n<>\n" actual "\n")))
                  (do
                    (.push fx_log {:key name :data args})
                    (Promise/resolve [name args])))))
            (->
             (app/handle_event event.key event.data)
             (e/run_effect {:perform test_eff_handler})
             (.then (fn []
                      (if (= log_json null)
                        (fs/writeFile (.replace path "/input/" "/output/") (JSON/stringify (rec_parse fx_log) null 4))
                        (if (= log.length 0) null
                            (FIXME "Log not consumed: " path "\n" (JSON/stringify (.toReversed log) null 2)))))))))))))))

(let [path "../test/commands/input/"]
  (->
   (fs/readdir path)
   (.then
    (fn [files]
      (->
       files
       (.filter (fn [name] (.test (RegExp. "\\.json$") name)))
       (.map (fn [name] (assert (str path name)))))))))
