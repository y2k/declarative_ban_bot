(ns handler.join
  (:require [effects-promise :as e]
            [telegram :as tg]))

(defn- handle_captcha_response [env update]
  (if-let [chat_id update?.message?.chat?.id
           user_id update?.message?.from?.id
           _ (= chat_id user_id)]
    (if (= "4" update?.message?.text)
      (e/batch [(tg/send_message :sendMessage
                                 {:chat_id user_id
                                  :text "Вы прошли капчу."})
                (tg/send_message :approveChatJoinRequest
                                 {:chat_id env.TG_APPROVE_CHAT
                                  :user_id user_id})])
      (tg/send_message :sendMessage
                       {:chat_id user_id
                        :text "Ответ неправильный. Можете написать поддержке в @xofftop"}))
    (e/pure nil)))

(defn- handle_join_request [env update]
  (if-let [chat_id update?.chat_join_request?.chat?.id
           user_id update?.chat_join_request?.from?.id
           user_name (or update?.chat_join_request?.from?.username "_")]
    (e/batch
     [(tg/send_message :sendMessage
                       {:parse_mode :MarkdownV2
                        :chat_id user_id
                        :text "Что бы остаться в @android\\_declarative, *решите капчу*:\n\n```clojure\n(+ 2 2)```\n\nУ вас 5 минут\\."})
      (tg/send_message :sendMessage
                       {:parse_mode :MarkdownV2
                        :chat_id "@android_declarative_ban_log"
                        :text (str "[Запрос на вступление от " user_name " " user_id "](tg://user?id=" user_id ")")})])
    (handle_captcha_response env update)))

(defn handle [env update]
  (if-let [_ (or update?.message?.new_chat_member update?.message?.left_chat_member)
           chat_id update?.message?.chat?.id
           message_id update?.message?.message_id]
    (tg/send_message :deleteMessage
                     {:chat_id chat_id :message_id message_id})
    (handle_join_request env update)))
