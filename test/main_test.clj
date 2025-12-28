(ns main-repl
  (:require [main :as m]
            [effects-promise :as e]
            ["fs" :as fs]
            ["path" :as path]))

(def- expected-dir "../test/resources/sample/out")

(defn- ensure-expected-dir! []
  (if (not (fs/existsSync expected-dir))
    (fs/mkdirSync expected-dir {:recursive true})))

(defn- load-txt-files []
  (let [dir "../test/resources/sample/in"
        files (fs/readdirSync dir)]
    (->> files
         (filter (fn [f] (clojure.string/ends-with? f ".txt")))
         (map (fn [f] (path/join dir f))))))

(defn- expected-path [sample-file]
  (let [basename (path/basename sample-file ".txt")]
    (path/join expected-dir (str basename ".expected.json"))))

(defn- fill-template [file-path]
  (let [content-base64 (fs/readFileSync file-path "utf8")
        content (JSON/stringify (str (Buffer.from content-base64 "base64")))
        template-path "../test/resources/sample/sample.template.json"
        template (fs/readFileSync template-path "utf8")]
    (clojure.string/replace template "__SPAM_TEXT__" content)))

(defn- run-test [sample-file]
  (let [log (atom [])
        input (JSON/parse (fill-template sample-file))]
    (->
     (m/fetch_export
      {:json (fn [] (Promise/resolve input))
       :headers {:get (fn [] :TGST_1234567890)}}
      {:TG_SECRET_TOKEN :TGST_1234567890}
      {:fetch (fn [p]
                (swap! log (fn [r] (conj r p)))
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
  (let [files (load-txt-files)
        results (atom {:passed 0 :failed 0 :missing 0})]
    (->
     (map
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
                   (println "  Expected:" (JSON/stringify expected))
                   (println "  Actual:  " (JSON/stringify actual)))))))))
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
  (ensure-expected-dir!)
  (let [out-path (expected-path sample-file)]
    (fs/writeFileSync out-path (JSON/stringify result nil 2) "utf8")
    (println "Saved golden:" out-path)))

(defn- update-golden []
  (let [files (load-txt-files)]
    (-> (Promise/all
         (map (fn [f]
                (-> (run-test f)
                    (.then (fn [result] (save-golden! f result)))))
              files))
        (.then (fn [_] (println "All golden files saved"))))))

;; (update-golden)
((run-all-tests) nil)
