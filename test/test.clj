(ns _ (:require ["../vendor/effects/main" :as e]
                ["../src/main" :as app]
                [js.fs.promises :as fs]))

(defn- rec_parse [x]
  (cond
    (= nil x) x
    (Array.isArray x) (.map x rec_parse)
    (= (type x) "object") (and x (-> (Object.entries x)
                                     (.reduce (fn [a x] (assoc a (get x 0) (rec_parse (get x 1)))) {})))
    (= (type x) "string") (if (.startsWith x "{") (rec_parse (JSON.parse x)) x)
    :else x))

(defn- assert [path]
  (->
   (fs/readFile (.replace path "/input/" "/output/") "utf-8")
   (.catch (fn [] nil))
   (.then
    (fn [output_json]
      (->
       (fs/readFile path "utf-8")
       (.then
        (fn [input_json]
          (let [event (JSON.parse input_json)
                actual_log []
                output_log (JSON.parse (or output_json "[]"))]
            (defn- test_eff_handler [name args]
              (.push actual_log {:key name :data args})
              (Promise.resolve [name args]))
            (->
             ((app/handle_event event.cofx event.key event.data) (Proxy. {}
                                                                         {:get (fn [target prop]
                                                                                 (fn [args]
                                                                                   (test_eff_handler prop args)))}))
            ;;  (e/run_effect {:perform test_eff_handler})
             (.then (fn []
                      (if (= output_json nil)
                        (fs/writeFile (.replace path "/input/" "/output/") (JSON.stringify (rec_parse actual_log) nil 4))
                        (let [expected (JSON.stringify (rec_parse output_log) nil 2)
                              actual (JSON.stringify (rec_parse actual_log) nil 2)]
                          (if (= expected actual)
                            nil
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
