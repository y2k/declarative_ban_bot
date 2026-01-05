(ns moderator)

(def LIMIT_SPAM_OLD_SEC 900)
(def OLD_MESSAGE_ID_DIFF 5)

(defn is_too_old [message_id reply_message_id]
  (> (- message_id reply_message_id) OLD_MESSAGE_ID_DIFF))

(defn check_is_spam [message_in]
  (let [message (-> message_in
                    (.toLowerCase)
                    (.replaceAll "a" "а")
                    (.replaceAll "c" "с")
                    (.replaceAll "e" "е")
                    (.replaceAll "k" "к")
                    (.replaceAll "o" "о")
                    (.replaceAll "p" "р")
                    (.replaceAll "u" "и")
                    (.replaceAll "x" "х"))]
    (or
     (.test (RegExp "[^\\wа-яа-щ\\s\\.,;:\\-?\\x22\\x27()]") message)
     (.includes message "арбитраж")
     (.includes message "банкомат")
     (.includes message "бесплатно")
     (.includes message "график")
     (.includes message "деньги")
     (.includes message "долларов")
     (.includes message "доход")
     (.includes message "заработ")
     (.includes message "крипт")
     (.includes message "онлайн")
     (.includes message "оплата")
     (.includes message "партнер")
     (.includes message "предложени")
     (.includes message "прибыл")
     (.includes message "работе")
     (.includes message "руб")
     (.includes message "сотрудничеств")
     (.includes message "такси")
     (.includes message "финанс")
     (.includes message "человек"))))
