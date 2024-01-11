(__unsafe_insert_js "import p from './prelude.js';")

(def LIMIT_SPAM_OLD_SEC 300)

(defn is_spam [message_in message_date]
  (let [message (-> message_in
                    (.toLowerCase)
                    (.replaceAll "a" "а")
                    (.replaceAll "u" "и")
                    (.replaceAll "p" "р")
                    (.replaceAll "o" "о")
                    (.replaceAll "x" "х"))]
    (and
     (< (- (/ (Date/now) 1000) message_date) LIMIT_SPAM_OLD_SEC)
     (or
      (.includes message "криптовалют")
      (.includes message "доход")
      (.includes message "оплата")))))

(defn handle_message [update]
  (if-let [reply_text update?.message?.reply_to_message?.text
           message_id update?.message?.message_id
           from update?.message?.from
           chat_id update?.message?.chat?.id
           chat_name update?.message?.chat?.username
           reply_from_id update?.message?.reply_to_message?.from?.id
           reply_message_id update?.message?.reply_to_message?.message_id
           message_date update?.message?.reply_to_message?.date
           _report (= "/report" update?.message?.text)]
    (p/batch
     (concat
      [(p/database
        "INSERT INTO log (content) VALUES (?)"
        [(JSON/stringify {:from from :text (p/string_to_hex reply_text)})])
       (execute_bot "deleteMessage"
                    {:chat_id chat_id :message_id message_id})
       (execute_bot "sendMessage"
                    {:chat_id 241854720
                     :text (str "Бот вызван https://t.me/" chat_name "/" reply_message_id)})]
      (if (is_spam reply_text message_date)
        [(execute_bot "deleteMessage"
                      {:chat_id chat_id :message_id reply_message_id})
         (execute_bot "restrictChatMember"
                      {:chat_id chat_id :user_id reply_from_id :permissions {}})]
        [(execute_bot "sendMessage"
                      {:chat_id chat_id
                       :text (str "Сообщение не определено как спам или старше " LIMIT_SPAM_OLD_SEC " секунд. Администратор уведомлен.")})])))
    (p/pure)))

;; Infrastructure

(defn execute_bot [cmd args]
  (p/fetch
   (str "https://api.telegram.org/bot~TG_TOKEN~/" cmd)
   :json
   {:method "POST"
    :headers {"Content-Type" "application/json"}
    :body (JSON/stringify args)}))

(export-default
 {:fetch
  (fn [request env world]
    (->
     (.json request)
     (.then
      (fn [json]
        (if (not= (.get request.headers "x-telegram-bot-api-secret-token") env.TG_SECRET_TOKEN)
          (throw (Error. "Telegram secret token not valid"))
          null)
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
