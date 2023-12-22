(defn default_handle_request [env json next]
  (fetch
   (str "https://api.telegram.org/bot" env.TG_TOKEN "/sendMessage")
   {:method "POST"
    :headers {"Content-Type" "application/json"}
    :body
    (.stringify
     JSON
     {:chat_id json.message.chat.id
      :link_preview_options {:url "https://youtu.be/fvwTZc-dxsM?si=kWJbOxk2UyLrvZdu"}
      :text "https://youtu.be/dQw4w9WgXcQ?si=V_MIImS5ydLbZaSu"})}))

(defn fetch_handler [request env]
  (->
   (.json request)
   (.then
    (fn [json]
      (->>
       (.warn console "[LOG] Message not handled:\n" json) (fn [])
       (default_handle_request env json))))
   (.catch console.error)
   (.then (fn [] (Response. "OK")))))

(defn json_to_string [x] (.stringify JSON x null 2))
(export-default {:fetch fetch_handler})
