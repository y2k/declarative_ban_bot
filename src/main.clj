(ns main
  (:require [effects-promise :as e]
            [handler.report :as report]
            [handler.join :as join]
            [moderator :as m]))

;; Infrastructure

(defn main [cofx request env]
  (fn [handlers]
    (->
     (.json request)
     (.then
      (fn [json]
        ;; (eprintln (JSON.stringify json))
        (if (not= (.get request.headers "x-telegram-bot-api-secret-token") env.TG_SECRET_TOKEN)
          (FIXME "Telegram secret token is not valid")
          nil)
        (let [io (e/batch [(report/handle cofx json)
                           (join/handle json)])]
          (io handlers))))
     (.catch console.error)
     (.then (fn [] (Response. "OK"))))))

(defn- create_effect_handlers [env]
  {:fetch (fn [{url :url decoder :decoder props :props}]
            (->
             (.replace url "~TG_TOKEN~" env.TG_TOKEN)
             (fetch props)
             (.then (fn [x] (if (= "json" decoder) (.json x) (.text x))))
             (.catch (fn [err]
                       (console.error "fetch error:" err)
                       (throw err)
                       nil))))})

(export-default
 {:fetch (fn [request env]
           ((main {:now (Date.now)} request env) (create_effect_handlers env)))})
