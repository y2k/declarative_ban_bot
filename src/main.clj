(ns main
  (:require [effect :as fx]
            [effect-fetch :as f]
            [handler.report :as report]
            [handler.join :as join]))

;; Infrastructure

(defn- telegram-bot-request? [request env]
  (let [url (URL. request.url)]
    (and (= "/telegram-bot" url.pathname)
         (= (.get request.headers "x-telegram-bot-api-secret-token") env.TG_SECRET_TOKEN))))

(defn- get-json [request]
  (fx/thunk
   (fn []
     (.json request))))

(defn main [_ request env]
  (if (telegram-bot-request? request env)
    (-> (get-json request)
        (fx/then
         (fn [json]
           ;; (eprintln (JSON.stringify json))
           (-> (fx/batch
                [(report/handle {:token env.TG_TOKEN} json)
                 (join/handle env json)])
               (fx/then (fn [] (fx/pure (Response. "OK")))))))
        (fx/recover
         (fn [err]
           (console.error err)
           (fx/pure (Response. "OK")))))
    (fx/pure (Response. "Not found" {:status 404}))))

(defn handle-fetch [request env ctx]
  (main {:now (Date.now)} request env))

(export-default
 {:fetch (fn [request env ctx]
           ((f/with-fetch js/fetch (handle-fetch request env ctx)) {}))})
