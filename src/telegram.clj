(ns telegram
  (:require [effects-ex :as fx]))

(defn send_message [cmd args]
  (fx/fetch
   (str "https://api.telegram.org/bot~TG_TOKEN~/" cmd)
   :json
   {:method "POST"
    :headers {"Content-Type" "application/json"}
    :body (JSON.stringify args)}))
