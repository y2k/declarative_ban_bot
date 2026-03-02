(ns handler.report
  (:require [effects-promise :as e]
            [moderator :as m]
            [telegram :as tg]))

(def- log-channel-important "@android_declarative_events")
(def- log-channel "@android_declarative_ban_log")

(defn- report-command? [text]
  (and text
       (.startsWith text "/")
       (or (.startsWith text "/spam")
           (.startsWith text "/report"))))

(defn- build-url [chat-name message-id]
  (str "https://t.me/" chat-name "/" message-id))

(defn- log-to-channel- [channel reply-from-id chat-name reply-message-id reply-text]
  (tg/send_message :sendMessage
                   {:chat_id channel
                    :link_preview_options {:is_disabled true}
                    :text (str "user: " reply-from-id "\n"
                               "url: " (build-url chat-name reply-message-id) "\n"
                               "text:\n" (.toString (Buffer/from reply-text) "base64"))}))

(defn- notify [_ chat-name reply-message-id reply-from-id]
  (log-to-channel- log-channel reply-from-id chat-name reply-message-id ""))

(defn- notify-important [reply-from-id _ chat-name reply-message-id reply-text]
  (log-to-channel- log-channel-important reply-from-id chat-name reply-message-id reply-text))

(defn handle [update]
  (if-let [_ (report-command? update?.message?.text)
           reply-text (or update?.message?.reply_to_message?.text update?.message?.reply_to_message?.caption)
           message-id update?.message?.message_id
           chat-id update?.message?.chat?.id
           chat-name update?.message?.chat?.username
           reply-from-id update?.message?.reply_to_message?.from?.id
           reply-message-id update?.message?.reply_to_message?.message_id
           message-date update?.message?.reply_to_message?.date]
    (let [is-spam (m/check_is_spam reply-text)
          notify-important-fx (notify-important reply-from-id is-spam chat-name reply-message-id reply-text)
          action-effects (cond
                           (m/is_too_old message-id reply-message-id)
                           [notify-important-fx
                            (tg/send_message :sendMessage
                                             {:chat_id chat-id
                                              :text (str "Репортить можно только " m/OLD_MESSAGE_ID_DIFF " последних сообщения. Администратор уведомлен.")})]

                           is-spam
                           [(tg/send_message :deleteMessage {:chat_id chat-id :message_id reply-message-id})
                            (tg/send_message :restrictChatMember {:chat_id chat-id :user_id reply-from-id :permissions {}})]

                           :else
                           [notify-important-fx
                            (tg/send_message :sendMessage
                                             {:chat_id chat-id
                                              :text "Ваша жалоба принята, администратор оповещен."})])]
      (e/batch
       (concat [(notify is-spam chat-name reply-message-id reply-from-id)
                (tg/send_message :deleteMessage {:chat_id chat-id :message_id message-id})]
               action-effects)))
    (e/pure nil)))
