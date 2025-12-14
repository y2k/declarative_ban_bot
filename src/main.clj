(ns main
  (:require [effects :as e]
            [effects-ex :as fx]
            [effects-promise :as ep]
            [moderator :as m]))

(defn- handle_message [cofx update]
  (fn [_] nil))

;; Infrastructure

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

(defn- fetch_export [request env]
  (->
   (.json request)
   (.then
    (fn [json]
      ;; (eprintln (JSON.stringify json))
      (if (not= (.get request.headers "x-telegram-bot-api-secret-token") env.TG_SECRET_TOKEN)
        (FIXME "Telegram secret token is not valid")
        nil)
      (let [cofx {:now (Date.now)}
            w (create_effect_handlers)]
        ((handle_message cofx json) w))))
   (.catch console.error)
   (.then (fn [] (Response. "OK")))))

(export-default
 {:fetch fetch_export})
