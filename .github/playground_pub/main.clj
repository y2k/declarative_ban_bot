(defn fetch_json_received [request]
  (.json request))

(defn response_sended [env json]
  (fetch
   (str "https://api.telegram.org/bot" env.TG_TOKEN "/sendMessage")
   {:method "POST"
    :headers {"Content-Type" "application/json"}
    :body
    (JSON/stringify
     {:chat_id json.message.chat.id
      :link_preview_options {:url "https://youtu.be/fvwTZc-dxsM?si=kWJbOxk2UyLrvZdu"}
      :text "https://youtu.be/dQw4w9WgXcQ?si=V_MIImS5ydLbZaSu"})}))

(export-default {:fetch (fn [request env]
                          (->
                           (.json request)
                           (.then (fn [json] (response_sended env json)))
                           (.catch console.error)
                           (.then (fn [] (Response. "OK")))))})
