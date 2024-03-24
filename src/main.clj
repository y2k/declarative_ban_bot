(ns app (:require [effects :as e]))

(defn string_to_hex [str]
  (-> (TextEncoder.) (.encode str) Buffer/from (.toString "base64")))

(def LIMIT_SPAM_OLD_SEC 600)

(defn- is_too_old [now message_date]
  (> (- (/ now 1000) message_date) LIMIT_SPAM_OLD_SEC))

(defn- check_is_spam [message_in]
  (let [message (-> message_in
                    (.toLowerCase)
                    (.replaceAll "k" "к")
                    (.replaceAll "c" "с")
                    (.replaceAll "a" "а")
                    (.replaceAll "u" "и")
                    (.replaceAll "p" "р")
                    (.replaceAll "o" "о")
                    (.replaceAll "x" "х"))]
    (or
     (.test (RegExp "[^\\wа-яа-щ\\s\\.,;:\\-?\\x22\\x27()]") message)
     (.includes message "арбитраж")
     (.includes message "заработ")
     (.includes message "онлайн")
     (.includes message "бесплатно")
     (.includes message "криптовалют")
     (.includes message "доход")
     (.includes message "прибыл")
     (.includes message "оплата"))))

(defn- handle_message [update]
  (if-let [reply_text update?.message?.reply_to_message?.text
           message_id update?.message?.message_id
           from update?.message?.from
           chat_id update?.message?.chat?.id
           chat_name update?.message?.chat?.username
           reply_from update?.message?.reply_to_message?.from
           reply_from_id reply_from?.id
           reply_message_id update?.message?.reply_to_message?.message_id
           message_date update?.message?.reply_to_message?.date
           _ (or (= "/spam" update?.message?.text) (= "/report" update?.message?.text))]
    (let [is_spam (check_is_spam reply_text)]
      (e/batch
       (concat
        [(e/database
          "INSERT INTO log (content) VALUES (?)"
          [(JSON/stringify {:from from :reply_from reply_from :text (string_to_hex reply_text)})])
         (execute_bot "deleteMessage"
                      {:chat_id chat_id :message_id message_id})
         (execute_bot "sendMessage"
                      {:chat_id 241854720
                       :disable_notification is_spam
                       :text (str "Бот вызван [spam: " is_spam "] https://t.me/" chat_name "/" reply_message_id)})]
        (cond
          (is_too_old (Date/now) message_date)
          [(execute_bot "sendMessage"
                        {:chat_id chat_id
                         :text (str "Сообщение старше " LIMIT_SPAM_OLD_SEC " секунд. Администратор уведомлен.")})]

          is_spam
          [(execute_bot "deleteMessage" {:chat_id chat_id :message_id reply_message_id})
           (execute_bot "restrictChatMember" {:chat_id chat_id :user_id reply_from_id :permissions {}})]

          :else [(execute_bot "sendMessage"
                              {:chat_id chat_id
                               :text (str "Сообщение не определено как спам. Администратор уведомлен.")})]))))
    (if-let [chat_id update?.message?.chat?.id
             _ (= "/healthcheck" update?.message?.text)]
      (execute_bot "sendMessage" {:chat_id chat_id :text "Bot is working"})
      (if-let [chat_id update?.message?.chat?.id
               text update?.message?.text
               _ (= "/find_ban" (get (.split text " ") 0))
               find_user (get (.split text " ") 1)]
        (e/broadcast
         :find_user_completed
         (e/database
          "SELECT content->>'reply_from' AS 'banned user', content->>'from' AS 'reporter', content->>'text' AS 'base64 msg' FROM log WHERE json_extract(content, '$.reply_from.username') = ? ORDER BY id DESC LIMIT 2;"
          [find_user])
         (fn [r] [chat_id r]))
        (e/pure null)))))

(defn- handle_find_result [chat_id r]
  (execute_bot
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

(defn- execute_bot [cmd args]
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
      ;; (println "[INPUT]" (JSON/stringify json null 2))
      (if (not= (.get request.headers "x-telegram-bot-api-secret-token") env.TG_SECRET_TOKEN)
        (throw (Error. "Telegram secret token is not valid"))
        null)

      (let [w (->
               (e/attach_empty_effect_handler {})
               (e/attach_eff
                :next (fn [_ [fx f] w]
                        (.then (fx w)
                               (fn [r] ((f r) w)))))
               (e/attach_eff
                :batch (fn [_ args w]
                         (-> (.map args.children (fn [f] (f w))) (Promise/all))))
               (e/attach_eff
                :fetch (fn [js args]
                         (->
                          (.replace args.url "~TG_TOKEN~" env.TG_TOKEN)
                          (js/fetch args.props)
                          (.then (fn [x] (if (= "json" args.decoder) (.json x) (.text x)))))))
               (e/attach_eff
                :database (fn [_ args]
                            (->
                             (.prepare env.DB args.sql)
                             (.bind (spread args.args))
                             (.run))))
               (e/attach_eff
                :dispatch
                (fn [_ [key data]]
                  (e/run_effect (handle_event key data) w)))
              ;;  (e/attach_log_handler)
               (merge world))]
        (e/run_io w (handle_event :telegram json)))))

   (.catch console.error)
   (.then (fn [] (Response. "OK")))))

(export-default {:fetch fetch_export})
