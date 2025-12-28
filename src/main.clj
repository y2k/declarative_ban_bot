(ns main
  (:require [effects-ex :as fx]
            [effects-promise :as e]
            [moderator :as m]))

(defn- send_message [cmd args]
  (fx/fetch
   (str "https://api.telegram.org/bot~TG_TOKEN~/" cmd)
   :json
   {:method "POST"
    :headers {"Content-Type" "application/json"}
    :body (JSON.stringify args)}))

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
                                  :text (str "Ваша жалоба принята, администратор оповещен.")})]))))
      (e/pure nil))
    (e/pure nil)))

;; Infrastructure

(defn fetch_export [request env handlers]
  (->
   (.json request)
   (.then
    (fn [json]
      ;; (eprintln (JSON.stringify json))
      (if (not= (.get request.headers "x-telegram-bot-api-secret-token") env.TG_SECRET_TOKEN)
        (FIXME "Telegram secret token is not valid")
        nil)
      (let [cofx {:now (Date.now)}]
        ((handle_message cofx json) handlers))))
   (.catch console.error)
   (.then (fn [] (Response. "OK")))))

(defn- create_effect_handlers []
  {:fetch (fn [{url :url decoder :decoder props :props}]
            (->
             (.replace url "~TG_TOKEN~" env.TG_TOKEN)
             (fetch props)
             (.then (fn [x] (if (= "json" decoder) (.json x) (.text x))))))
    ;; :database (fn [{sql :sql args :args}]
    ;;             (->
    ;;              (.prepare env.DB sql)
    ;;              (.bind (spread args))
    ;;              (.run)))
    ;; :dispatch (fn [[key data]]
    ;;             ((handle_event cofx key data) w))
   })

(export-default
 {:fetch (fn [request env]
           (fetch_export request env (create_effect_handlers)))})
