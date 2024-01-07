(__unsafe_insert_js "import runtime from './prelude.js';")

(defn handle_message [json]
  (if-let [reply_to_message json?.message?.reply_to_message
           chat_id json?.message?.chat?.id
           _report (= "/report" json?.message?.text)]
    (runtime/fetch
     "https://api.telegram.org/bot~TG_TOKEN~/sendMessage"
     :json
     {:method "POST"
      :headers {"Content-Type" "application/json"}
      :body
      (JSON/stringify
       {:chat_id chat_id
        :link_preview_options {:url "https://youtu.be/fvwTZc-dxsM?si=kWJbOxk2UyLrvZdu"}
        :text "https://youtu.be/dQw4w9WgXcQ?si=V_MIImS5ydLbZaSu"})})
    (runtime/pure)))

(defn string_to_hex [str]
  (-> (TextEncoder.) (.encode str) Buffer/from (.toString "base64")))

(export-default
 {:fetch
  (fn [request env world]
    (->
     (.json request)
     (.then
      (fn [json]
        (console/log "[MESSAGE]" (string_to_hex json?.message?.reply_to_message?.text))
        (let [effect (handle_message json)]
          (->
           (runtime/create_world)
           (runtime/attach_effect_handler
            :fetch (fn [js args]
                     (->
                      (.replace args.url "~TG_TOKEN~" env.TG_TOKEN)
                      (js/fetch args.props)
                      (.then (fn [x] (if (= "json" args.decoder) (.json x) (.text x)))))))
           (merge world)
           effect))))
     (.catch console.error)
     (.then (fn [] (Response. "OK")))))})
