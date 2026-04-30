(ns main-repl
  (:require [main :as m]
            [effects-promise :as e]
            ["fs" :as fs]
            ["path" :as path]))

(def- input-dir "../test/resources/sample/in")
(def- output-dir "../test/resources/sample/out")

(defn- ensure-expected-dir! [subdir]
  (let [dir (path/join output-dir subdir)]
    (if (not (fs/existsSync dir))
      (fs/mkdirSync dir {:recursive true}))))

(defn- load-sample-files []
  (let [subdirs (fs/readdirSync input-dir)]
    (-> (->> subdirs
             (filter (fn [d] (let [stat (fs/statSync (path/join input-dir d))]
                               (stat.isDirectory))))
             (map (fn [subdir]
                    (let [dir (path/join input-dir subdir)
                          files (fs/readdirSync dir)]
                      (->> files
                           (filter (fn [f] (or (clojure.string/ends-with? f ".txt")
                                               (clojure.string/ends-with? f ".json"))))
                           (map (fn [f] (path/join dir f))))))))
        (.flat))))

(defn- expected-path [sample-file]
  (let [ext (if (clojure.string/ends-with? sample-file ".json") ".json" ".txt")
        basename (path/basename sample-file ext)
        subdir (path/basename (path/dirname sample-file))]
    (path/join output-dir subdir (str basename ".json"))))

(defn- fill-template [file-path]
  (let [content-base64 (fs/readFileSync file-path "utf8")
        content (JSON/stringify (str (Buffer.from content-base64 "base64")))
        template-path "../test/resources/sample/sample.template.json"
        template (fs/readFileSync template-path "utf8")]
    (clojure.string/replace template "__SPAM_TEXT__" content)))

(defn- load-input [sample-file]
  (if (clojure.string/ends-with? sample-file ".json")
    (JSON/parse (fs/readFileSync sample-file "utf8"))
    (JSON/parse (fill-template sample-file))))

(defn- run-test [sample-file]
  (let [log (atom [])
        input (load-input sample-file)]
    (->
     {:fetch (fn [p]
               (swap! log (fn [r] (conj r {:key :fetch :data p})))
               (Promise/resolve nil))}
     ((m/main
       input.cofx
       {:json (fn [] (Promise/resolve input.data))
        :url "https://example.com/telegram-bot"
        :headers {:get (fn [] :TGST_1234567890)}}
       {:TG_SECRET_TOKEN :TGST_1234567890}))
     (.then (fn [_] (deref log))))))

(defn- load-golden [sample-file]
  (let [golden-path (expected-path sample-file)]
    (if (fs/existsSync golden-path)
      (JSON/parse (fs/readFileSync golden-path "utf8"))
      nil)))

(defn- compare-results [expected actual]
  (= (JSON/stringify expected) (JSON/stringify actual)))

(defn- run-all-tests []
  (let [files (load-sample-files)
        results (atom {:passed 0 :failed 0 :missing 0})]
    (->
     (map
      (fn [f]
        (e/thunk
         (.then
          (run-test f)
          (fn [actual]
            (let [expected (load-golden f)]
              (cond
                (nil? expected)
                (do
                  (swap! results (fn [r] (update r :missing (fn [x] (inc x)))))
                  (println "⚠ MISSING golden:" f))

                (compare-results expected actual)
                (do
                  (swap! results (fn [r] (update r :passed (fn [x] (inc x)))))
                  (println "✓ PASS:" f))

                :else
                (do
                  (swap! results (fn [r] (update r :failed (fn [x] (inc x)))))
                  (println "✗ FAIL:" f)
                  (println "  Expected:\n" (JSON/stringify expected nil 2))
                  (println "  Actual:\n" (JSON/stringify actual nil 2)))))))))
      files)
     (e/batch)
     (e/then (fn [_]
               (let [{passed :passed failed :failed missing :missing}
                     (deref results)]
                 (println "")
                 (println "Results:" passed "passed," failed "failed," missing "missing")
                 (if (> failed 0)
                   (FIXME (str failed " test(s) failed"))
                   (e/pure nil))))))))

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
     (e/batch)
     (e/then (fn [_]
               (println "All golden files saved")
               (e/pure nil))))))

(case (get process.argv 2)
  "update" ((update-golden) {})
  ((run-all-tests) {}))
