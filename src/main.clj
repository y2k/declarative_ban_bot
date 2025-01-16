(ns app (:require ["../vendor/effects/main" :as e]
                  ["./effects" :as fx]
                  [moderator :as m]))

;; BEGIN - Infrastructure

(defn- send_message [cmd args]
  (fx/fetch
   (str "https://api.telegram.org/bot~TG_TOKEN~/" cmd)
   :json
   {:method "POST"
    :headers {"Content-Type" "application/json"}
    :body (JSON.stringify args)}))

;; END - Infrastructure

(defn- string_to_hex [str]
  (-> (TextEncoder.) (.encode str) Buffer.from (.toString "base64")))

;; Message handler

(defn- handle_captcha_response [update]
  (if-let [chat_id update?.message?.chat?.id
           user_id update?.message?.from?.id
           _ (= chat_id user_id)]
    (do
      (println "CAPTCHA:" update?.message?.text)
      (send_message "sendMessage"
                    {:chat_id user_id
                     :text "Вы прошли капчу."}))
    (e/pure nil)))

(defn- handle_join_request [update]
  (if-let [chat_id update?.chat_join_request?.chat?.id
           user_id update?.chat_join_request?.from?.id
           user_name (or update?.chat_join_request?.from?.username "_")]
    (e/batch
     [(send_message "sendMessage"
                    {:parse_mode :MarkdownV2
                     :chat_id user_id
                     :text "Что бы остаться в @android\\_declarative, *решите капчу*:\n\n```clojure\n(+ 2 2)```\n\nУ вас 5 минут\\."})
      (send_message "sendMessage"
                    {:parse_mode :MarkdownV2
                     :chat_id "@android_declarative_ban_log"
                     :text (str "[Запрос на вступление от " user_name " " user_id "](tg://user?id=" user_id ")")})
      (send_message "approveChatJoinRequest"
                    {:chat_id chat_id
                     :user_id user_id})])
    (handle_captcha_response update)))

(defn- handle_service_message [update]
  (if-let [_ (or update?.message?.new_chat_member update?.message?.left_chat_member)
           chat_id update?.message?.chat?.id
           message_id update?.message?.message_id]
    (send_message "deleteMessage" {:chat_id chat_id :message_id message_id})
    (handle_join_request update)))

;; Command handler

(defn- handle_unknown_command [update]
  (if-let [reply_message_id update?.message?.reply_to_message?.message_id
           chat_name (or update?.message?.chat?.username "_")]
    (send_message "sendMessage"
                  {:chat_id 241854720
                   :disable_notification true
                   :text (str "Неизвестный формат сообщения https://t.me/" chat_name "/" reply_message_id)})
    (e/pure nil)))

(defn- handle_start_command [update]
  (if-let [chat_id update?.message?.chat?.id
           _ (= "/start" update?.message?.text)]
    (send_message "sendMessage" {:chat_id chat_id :text "Bot is working"})
    (handle_unknown_command update)))

(defn- handle_message [cofx update]
  (if-let [_ (.startsWith (or update?.message?.text "") "/")]
    (if-let [reply_text (or update?.message?.reply_to_message?.text update?.message?.reply_to_message?.caption)
             message_id update?.message?.message_id
             chat_id update?.message?.chat?.id
             chat_name update?.message?.chat?.username
             reply_from_id update?.message?.reply_to_message?.from?.id
             reply_message_id update?.message?.reply_to_message?.message_id
             message_date update?.message?.reply_to_message?.date
             command_text update?.message?.text
             _ (or (.startsWith command_text "/spam") (.startsWith command_text "/report"))]
      (let [is_spam (m/check_is_spam reply_text)
            notify_admin_fx (send_message "sendMessage"
                                          {:chat_id 241854720
                                           :disable_notification is_spam
                                           :text (str "Bot invoked\nUser: " reply_from_id "\nSpam: " is_spam "\nURL: https://t.me/" chat_name "/" reply_message_id)})]
        (e/batch
         (concat
          [(send_message "sendMessage"
                         {:chat_id "@android_declarative_ban_log"
                          :parse_mode :MarkdownV2
                          :text (str "Bot invoked\nSpam: `" is_spam "`\nURL: `https://t.me/" chat_name "/" reply_message_id "`")})
           (send_message "deleteMessage" {:chat_id chat_id :message_id message_id})]
          (cond
            (m/is_too_old cofx.now message_date message_id reply_message_id)
            [notify_admin_fx
             (send_message "sendMessage"
                           {:chat_id chat_id
                            :text (str "Репортить можно только " m/OLD_MESSAGE_ID_DIFF " последних сообщения. Администратор уведомлен.")})]

            is_spam
            [(send_message "deleteMessage" {:chat_id chat_id :message_id reply_message_id})
             (send_message "restrictChatMember" {:chat_id chat_id :user_id reply_from_id :permissions {}})]

            :else [notify_admin_fx
                   (send_message "sendMessage"
                                 {:chat_id chat_id
                                  :text (str "Сообщение не определено как спам. Администратор уведомлен.")})]))))
      (handle_start_command update))
    (handle_service_message update)))

(defn handle_event [cofx key data]
  ;; (println (JSON.stringify {:cofx cofx :key key :data data} nil 2))
  (case key
    :telegram (handle_message cofx data)
    (e/pure nil)))

;; Infrastructure

(defn- fetch_export [request env world]
  (->
   (.json request)
   (.then
    (fn [json]
      (println (JSON.stringify json))
      (if (not= (.get request.headers "x-telegram-bot-api-secret-token") env.TG_SECRET_TOKEN)
        (throw (Error. "Telegram secret token is not valid"))
        nil)
      (let [cofx (or env.cofx {:now (Date.now)})
            w (merge
               {:fetch (fn [{url :url decoder :decoder props :props}]
                         (->
                          (.replace url "~TG_TOKEN~" env.TG_TOKEN)
                          (fetch props)
                          (.then (fn [x] (if (= "json" decoder) (.json x) (.text x))))))
                :database (fn [{sql :sql args :args}]
                            (->
                             (.prepare env.DB sql)
                             (.bind (spread args))
                             (.run)))
                :dispatch (fn [[key data]]
                            ((handle_event cofx key data) w))}
               world)]
        ((handle_event cofx :telegram json) w))))
   (.catch console.error)
   (.then (fn [] (Response. "OK")))))

(export-default {:fetch fetch_export})
