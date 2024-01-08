(__unsafe_insert_js "import p from './prelude.js';")

(defn is_spam [message_in]
  (let [message (-> message_in
                    (.replaceAll "o" "о")
                    (.replaceAll "x" "х"))]
    (or
     (.includes message "доход")
     (.includes message "оплата"))))

(defn handle_message [json]
  (if-let [reply_text json?.message?.reply_to_message?.text
           from json?.message?.from
           chat_id json?.message?.chat?.id
           reply_message_id json?.message?.reply_to_message?.message_id
           _report (= "/report" json?.message?.text)]
    (p/batch
     (concat
      [(p/database
        "INSERT INTO log (content) VALUES (?)"
        [(JSON/stringify {:from from :text (p/string_to_hex reply_text)})])]
      (if (is_spam reply_text)
        [(p/fetch
          "https://api.telegram.org/bot~TG_TOKEN~/deleteMessage"
          :json
          {:method "POST"
           :headers {"Content-Type" "application/json"}
           :body (JSON/stringify {:chat_id chat_id :message_id reply_message_id})})]
        [])))
    (p/pure)))

(export-default
 {:fetch
  (fn [request env world]
    (->
     (.json request)
     (.then
      (fn [json]
        (->
         (p/create_world)
         (p/attach_effect_handler
          :batch (fn [_ args w]
                   (-> (.map args.children (fn [f] (f w))) (Promise/all))))
         (p/attach_effect_handler
          :fetch (fn [js args]
                   (->
                    (.replace args.url "~TG_TOKEN~" env.TG_TOKEN)
                    (js/fetch args.props)
                    (.then (fn [x] (if (= "json" args.decoder) (.json x) (.text x)))))))
         (p/attach_effect_handler
          :database (fn [_ args]
                      (->
                       (.prepare env.DB args.sql)
                       (.bind (spread args.args))
                       (.run))))
         (p/attach_log_handler)
         (merge world)
         (p/run_io (handle_message json)))))
     (.catch console.error)
     (.then (fn [] (Response. "OK")))))})
