(ns main-test
  (:require [test-cloudflare-worker :as tu]
            [utils :as st]
            ["fs" :as fs]
            ["path" :as path]))

(def- input-dir "../../test/resources/sample/in")

(defn- other-sample? [sample-file]
  (let [rel-path (path/relative input-dir sample-file)
        rel-dir (path/dirname rel-path)]
    (and (= rel-dir ".")
         (clojure.string/ends-with? sample-file ".json"))))

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
    (path/join st/output-dir (str basename ".json"))))

(tu/before-after
 {:TG_TOKEN {:type "plain_text" :value "test-token"}
  :TG_SECRET_TOKEN {:type "plain_text" :value "test-secret"}
  :TG_APPROVE_CHAT {:type "plain_text" :value "-1001234567890"}})

(let [config {:load-input (fn [sample-file] (JSON/parse (fs/readFileSync sample-file "utf8")))
              :expected-path expected-path
              :sample-name (fn [sample-file] (path/basename sample-file))
              :sample-subdir (fn [sample-file] (path/basename (path/dirname sample-file)))}]
  (.forEach (load-sample-files)
            (fn [sample-file] (st/register-sample-test config sample-file))))
