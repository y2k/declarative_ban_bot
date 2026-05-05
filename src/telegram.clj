(ns telegram
  (:require [effect-fetch :as f]))

(defn send_message [config cmd args]
  (f/fetch
   (str "https://api.telegram.org/bot" (:token config) "/" cmd)
   {:method "POST"
    :headers {"Content-Type" "application/json"}
    :decoder :json
    :body (JSON.stringify args)}))
