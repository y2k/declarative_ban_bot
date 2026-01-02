(ns main-repl
  (:require [main :as m]
            [effects-promise :as e]
            ["fs" :as fs]
            ["path" :as path]))

(def- expected-dir "../test/resources/sample/out")

(defn- ensure-expected-dir! []
  (if (not (fs/existsSync expected-dir))
    (fs/mkdirSync expected-dir {:recursive true})))

(defn- load-sample-files []
  (let [dir "../test/resources/sample/in"
        files (fs/readdirSync dir)]
    (->> files
         (filter (fn [f] (or (clojure.string/ends-with? f ".txt")
                             (clojure.string/ends-with? f ".json"))))
         (map (fn [f] (path/join dir f))))))

(defn- expected-path [sample-file]
  (let [ext (if (clojure.string/ends-with? sample-file ".json") ".json" ".txt")
        basename (path/basename sample-file ext)]
    (path/join expected-dir (str basename ".json"))))

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
     (m/main
      input.cofx
      {:json (fn [] (Promise/resolve input.data))
       :headers {:get (fn [] :TGST_1234567890)}}
      {:TG_SECRET_TOKEN :TGST_1234567890}
      {:fetch (fn [p]
                (swap! log (fn [r] (conj r {:key :fetch :data p})))
                (Promise/resolve nil))})
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
     (fn [f]
       (fn [_]
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
     (map files)
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
  (ensure-expected-dir!)
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

;; ((update-golden) {})
((run-all-tests) {})
