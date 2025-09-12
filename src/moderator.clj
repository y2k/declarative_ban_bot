(def LIMIT_SPAM_OLD_SEC 900)
(def OLD_MESSAGE_ID_DIFF 5)

(defn is_too_old [now message_date message_id reply_message_id]
  (> (- message_id reply_message_id) OLD_MESSAGE_ID_DIFF))

(defn check_is_spam [message_in]
  (let [message (-> message_in
                    (.toLowerCase)
                    (.replaceAll "e" "е")
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
     (.includes message "банкомат")
     (.includes message "бесплатно")
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
     (.includes message "сотрудничеств")
     (.includes message "финанс")
     (.includes message "человек"))))
