(ns utils
  (:require [test-cloudflare-worker :as tu]
            ["fs" :as fs]
            ["path" :as path]
            ["node:test" :as t]
            ["node:assert/strict" :as assert]))

(def output-dir "../../test/resources/sample/out")

(defn register-sample-test [config sample]
  (t/test ((:sample-name config) sample)
          (fn [] (assert-sample config sample))))

(defn- assert-sample [config sample]
  (.then
   (run-test config sample)
   (fn [actual]
     (let [expected (load-golden config sample)]
       (if (nil? expected)
         (save-golden! config sample actual)
         (assert/deepStrictEqual actual expected))))))

(defn- load-golden [config sample]
  (let [golden-path ((:expected-path config) sample)]
    (if (fs/existsSync golden-path)
      (JSON/parse (fs/readFileSync golden-path "utf8"))
      nil)))

(defn- run-test [config sample]
  (let [input ((:load-input config) sample)
        request (telegram-request input.data)]
    (-> (.text request)
        (.then (fn [body]
                 (.fetch (deref tu/worker)
                         request.url
                         {:method request.method
                          :headers request.headers
                          :body body})))
        (.then (fn [response] (.json response)))
        (.then (fn [body] body.effects)))))

(defn- telegram-request [update]
  (Request. "http://localhost/telegram-bot"
            {:method "POST"
             :headers {"Content-Type" "application/json"
                       "x-telegram-bot-api-secret-token" "test-secret"}
             :body (JSON/stringify update)}))

(defn- save-golden! [config sample result]
  (let [out-path ((:expected-path config) sample)]
    (fs/writeFileSync out-path (JSON/stringify result nil 2) "utf8")
    (println "Saved golden:" out-path)))
