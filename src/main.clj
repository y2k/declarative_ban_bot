
(defn ^{:in fetch_received} fetch_json_received [request]
  (.json! request))

(defn ^{:in fetch_json_received} response_sended [json]
  (fetch!
   (str "https://api.telegram.org/bot" (read_env! "TG_TOKEN") "/sendMessage")
   {:method "POST"
    :headers {"Content-Type" "application/json"}
    :body
    (JSON/stringify
     {:chat_id json.message.chat.id
      :link_preview_options {:url "https://youtu.be/fvwTZc-dxsM?si=kWJbOxk2UyLrvZdu"}
      :text "https://youtu.be/dQw4w9WgXcQ?si=V_MIImS5ydLbZaSu"})}))

(export-default {:fetch (fn [request env init_context]
                          (->
                           (runtime/consume_event (merge env init_context) :fetch_received request)
                           (.catch console.error)
                           (.then (fn [] (Response. "OK")))))})
