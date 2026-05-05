(ns other-samples-test
  (:require [effect :as fx]
            [test-cloudflare-worker :as tu]
            ["fs" :as fs]
            ["path" :as path]
            ["node:test" :as t]
            ["node:assert/strict" :as assert]))

(def- input-dir "../../test/resources/sample/in")
(def- output-dir "../../test/resources/sample/out")

(def- env-bindings
  {:TG_TOKEN {:type "plain_text" :value "test-token"}
   :TG_SECRET_TOKEN {:type "plain_text" :value "test-secret"}
   :TG_APPROVE_CHAT {:type "plain_text" :value "-1001234567890"}})

(defn- ensure-expected-dir! [subdir]
  (let [dir (path/join output-dir subdir)]
    (if (not (fs/existsSync dir))
      (fs/mkdirSync dir {:recursive true}))))

(defn- sample-file? [file-path]
  (clojure.string/ends-with? file-path ".json"))

(defn- other-sample? [sample-file]
  (let [rel-path (path/relative input-dir sample-file)
        rel-dir (path/dirname rel-path)]
    (and (= rel-dir ".")
         (sample-file? sample-file))))

(defn- load-sample-files []
  (let [entries (fs/readdirSync input-dir)]
    (->> entries
         (filter (fn [f]
                   (let [file-path (path/join input-dir f)
                         stat (fs/statSync file-path)]
                     (and (stat.isFile) (other-sample? file-path)))))
         (map (fn [f] (path/join input-dir f))))))

(defn- expected-path [sample-file]
  (let [basename (path/basename sample-file ".json")]
    (path/join output-dir (str basename ".json"))))

(defn- load-input [sample-file]
  (JSON/parse (fs/readFileSync sample-file "utf8")))

(defn- telegram-request [update]
  (Request.
   "http://localhost/telegram-bot"
   {:method "POST"
    :headers {"Content-Type" "application/json"
              "x-telegram-bot-api-secret-token" "test-secret"}
    :body (JSON/stringify update)}))

(defn- normalize-effect [effect]
  {:key :fetch
   :data {:url (.replace effect.url "test-token" "~TG_TOKEN~")
          :decoder effect.props.decoder
          :props {:method effect.props.method
                  :headers effect.props.headers
                  :body effect.props.body}}})

(defn- run-test [sample-file]
  (let [input (load-input sample-file)
        request (telegram-request input.data)]
    (-> (.text request)
        (.then (fn [body]
                 (.fetch (deref tu/worker)
                         request.url
                         {:method request.method
                          :headers request.headers
                          :body body})))
        (.then (fn [response] (.json response)))
        (.then (fn [body]
                 (map normalize-effect body.effects))))))

(defn- load-golden [sample-file]
  (let [golden-path (expected-path sample-file)]
    (if (fs/existsSync golden-path)
      (JSON/parse (fs/readFileSync golden-path "utf8"))
      nil)))

(defn- assert-sample [sample-file]
  (.then
   (run-test sample-file)
   (fn [actual]
     (let [expected (load-golden sample-file)]
       (if (nil? expected)
         (assert/fail (str "Missing golden: " sample-file))
         (assert/deepStrictEqual actual expected))))))

(defn- save-golden! [sample-file result]
  (let [subdir (path/basename (path/dirname sample-file))]
    (ensure-expected-dir! subdir))
  (let [out-path (expected-path sample-file)]
    (fs/writeFileSync out-path (JSON/stringify result nil 2) "utf8")
    (println "Saved golden:" out-path)))

(defn- update-golden []
  (let [files (load-sample-files)]
    (->
     (fn [f]
       (fn [_]
         (.then
          (run-test f)
          (fn [result] (save-golden! f result)))))
     (map files)
     (fx/batch)
     (fx/then (fn [_]
                (println "All golden files saved")
                (fx/pure nil))))))

(defn- with-worker [effect]
  (-> (tu/before env-bindings)
      (.then (fn [] (effect {})))
      (.finally (fn [] (tu/after)))))

(defn- register-sample-test [sample-file]
  (t/test (path/basename sample-file)
          (fn [] (assert-sample sample-file))))

(case (get process.argv 2)
  "update" (with-worker (update-golden))
  (do
    (t/before (fn [] (tu/before env-bindings)))
    (t/after (fn [] (tu/after)))
    (.forEach (load-sample-files) register-sample-test)))
