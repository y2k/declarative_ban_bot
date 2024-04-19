(ns app (:require [effects :as e]
                  [moderator :as m]))

(defn- string_to_hex [str]
  (-> (TextEncoder.) (.encode str) Buffer/from (.toString "base64")))

(defn- handle_message [update]
  (if-let [reply_text (or update?.message?.reply_to_message?.text update?.message?.reply_to_message?.caption)
           message_id update?.message?.message_id
           from update?.message?.from
           chat_id update?.message?.chat?.id
           chat_name update?.message?.chat?.username
           reply_from update?.message?.reply_to_message?.from
           reply_from_id reply_from?.id
           reply_message_id update?.message?.reply_to_message?.message_id
           message_date update?.message?.reply_to_message?.date
           _ (or (= "/spam" update?.message?.text) (= "/report" update?.message?.text))]
    (let [is_spam (m/check_is_spam reply_text)]
      (e/batch
       (concat
        [(e/database
          "INSERT INTO log (content) VALUES (?)"
          [(JSON/stringify {:from from :reply_from reply_from :text (string_to_hex reply_text)})])
         (send_message "deleteMessage"
                       {:chat_id chat_id :message_id message_id})
         (send_message "sendMessage"
                       {:chat_id 241854720
                        :disable_notification is_spam
                        :text (str "Бот вызван [spam: " is_spam "] https://t.me/" chat_name "/" reply_message_id)})]
        (cond
          (m/is_too_old (Date/now) message_date)
          [(send_message "sendMessage"
                         {:chat_id chat_id
                          :text (str "Сообщение старше " m/LIMIT_SPAM_OLD_SEC " секунд. Администратор уведомлен.")})]

          is_spam
          [(send_message "deleteMessage" {:chat_id chat_id :message_id reply_message_id})
           (send_message "restrictChatMember" {:chat_id chat_id :user_id reply_from_id :permissions {}})]

          :else [(send_message "sendMessage"
                               {:chat_id chat_id
                                :text (str "Сообщение не определено как спам. Администратор уведомлен.")})]))))
    (if-let [chat_id update?.message?.chat?.id
             _ (= "/healthcheck" update?.message?.text)]
      (send_message "sendMessage" {:chat_id chat_id :text "Bot is working"})
      (if-let [chat_id update?.message?.chat?.id
               text update?.message?.text
               _ (= "/find_ban" (get (.split text " ") 0))
               find_user (get (.split text " ") 1)]
        (->
         (e/database
          "SELECT content->>'reply_from' AS 'banned user', content->>'from' AS 'reporter', content->>'text' AS 'base64 msg' FROM log WHERE json_extract(content, '$.reply_from.username') = ? ORDER BY id DESC LIMIT 2;"
          [find_user])
         (e/next :find_user_completed (fn [r] [chat_id r])))
        (if-let [message_id update?.message?.reply_to_message?.message_id
                 chat_name (or update?.message?.chat?.username "_")]
          (send_message "sendMessage"
                        {:chat_id 241854720
                         :disable_notification true
                         :text (str "Неизвестный формат сообщения https://t.me/" chat_name "/" message_id)})
          (e/pure null))))))

(defn- handle_find_result [chat_id r]
  (send_message
   "sendMessage"
   {:parse_mode :MarkdownV2
    :chat_id chat_id
    :text
    (if (= 0 r.results.length)
      "Can't find ban records for this user"
      (->
       r.results
       (.map (fn [x] (str "```json\n" (JSON/stringify x null 2) "```")))
       (.join "\n/find_ban debug3bot")))}))

(defn handle_event [key data]
  ;; (println (JSON/stringify {:key key :data data} null 2))
  (case key
    :telegram (handle_message data)
    :find_user_completed (handle_find_result (spread data))
    (e/pure null)))

;; Infrastructure

(defn- send_message [cmd args]
  (e/fetch
   (str "https://api.telegram.org/bot~TG_TOKEN~/" cmd)
   :json
   {:method "POST"
    :headers {"Content-Type" "application/json"}
    :body (JSON/stringify args)}))

(defn- fetch_export [request env world]
  (->
   (.json request)
   (.then
    (fn [json]
      (if (not= (.get request.headers "x-telegram-bot-api-secret-token") env.TG_SECRET_TOKEN)
        (throw (Error. "Telegram secret token is not valid"))
        null)

      (let [w (->
               (e/attach_empty_effect_handler {})
              ;;  (e/attach_eff
              ;;   :next (fn [[fx f] w]x
              ;;           (.then (fx w)
              ;;                  (fn [r] ((f r) w)))))
               (e/attach_eff
                :batch (fn [args w]
                         (-> (.map args.children (fn [f] (f w))) (Promise/all))))
               (e/attach_eff
                :fetch (fn [{url :url decoder :decoder props :props}]
                         (->
                          (.replace url "~TG_TOKEN~" env.TG_TOKEN)
                          (fetch props)
                          (.then (fn [x] (if (= "json" decoder) (.json x) (.text x)))))))
               (e/attach_eff
                :database (fn [{sql :sql args :args}]
                            (->
                             (.prepare env.DB sql)
                             (.bind (spread args))
                             (.run))))
               (e/attach_eff
                :dispatch
                (fn [[key data]]
                  (e/run_effect (handle_event key data) w)))
              ;;  (e/attach_log_handler)
               (merge world))]
        (e/run_io w (handle_event :telegram json)))))

   (.catch console.error)
   (.then (fn [] (Response. "OK")))))

(export-default {:fetch fetch_export})
