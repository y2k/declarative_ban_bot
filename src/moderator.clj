(def LIMIT_SPAM_OLD_SEC 600)

(defn is_too_old [now message_date]
  (> (- (/ now 1000) message_date) LIMIT_SPAM_OLD_SEC))

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
     (.includes message "банкомат")
     (.includes message "арбитраж")
     (.includes message "заработ")
     (.includes message "онлайн")
     (.includes message "бесплатно")
     (.includes message "крипт")
     (.includes message "доход")
     (.includes message "прибыл")
     (.includes message "оплата"))))
