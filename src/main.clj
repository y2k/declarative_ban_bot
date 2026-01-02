(ns main
  (:require [effects-ex :as fx]
            [effects-promise :as e]
            [handler.report :as report]
            [moderator :as m]))

;; Infrastructure

(defn fetch_export [request env handlers]
  (->
   (.json request)
   (.then
    (fn [json]
      ;; (eprintln (JSON.stringify json))
      (if (not= (.get request.headers "x-telegram-bot-api-secret-token") env.TG_SECRET_TOKEN)
        (FIXME "Telegram secret token is not valid")
        nil)
      (let [cofx {:now (Date.now)}]
        ((report/handle cofx json) handlers))))
   (.catch console.error)
   (.then (fn [] (Response. "OK")))))

(defn- create_effect_handlers []
  {:fetch (fn [{url :url decoder :decoder props :props}]
            (->
             (.replace url "~TG_TOKEN~" env.TG_TOKEN)
             (fetch props)
             (.then (fn [x] (if (= "json" decoder) (.json x) (.text x))))))
    ;; :database (fn [{sql :sql args :args}]
    ;;             (->
    ;;              (.prepare env.DB sql)
    ;;              (.bind (spread args))
    ;;              (.run)))
    ;; :dispatch (fn [[key data]]
    ;;             ((handle_event cofx key data) w))
   })

(export-default
 {:fetch (fn [request env]
           (fetch_export request env (create_effect_handlers)))})
