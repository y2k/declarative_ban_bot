(ns main
  (:require [effects-promise :as e]
            [handler.report :as report]
            [handler.join :as join]
            [moderator :as m]))

;; Infrastructure

(defn- telegram-bot-request? [request env]
  (let [url (URL. request.url)]
    (and (= "/telegram-bot" url.pathname)
         (= (.get request.headers "x-telegram-bot-api-secret-token") env.TG_SECRET_TOKEN))))

(defn main [_ request env]
  (fn [handlers]
    (if (telegram-bot-request? request env)
      (->
       (.json request)
       (.then
        (fn [json]
          ;; (eprintln (JSON.stringify json))
          ((e/batch
            [(report/handle json)
             (join/handle env json)])
           handlers)))
       (.catch console.error)
       (.then (fn [] (Response. "OK"))))
      (Promise/resolve (Response. "Not found" {:status 404})))))

(defn- create_effect_handlers [env]
  {:fetch (fn [{url :url decoder :decoder props :props}]
            (->
             (.replace url "~TG_TOKEN~" env.TG_TOKEN)
             (fetch props)
             (.then (fn [x] (if (= "json" decoder) (.json x) (.text x))))
             (.catch (fn [err]
                       (eprintln "fetch error:" err)
                       (throw err)
                       nil))))})

(export-default
 {:fetch (fn [request env]
           ((main {:now (Date.now)} request env) (create_effect_handlers env)))})
