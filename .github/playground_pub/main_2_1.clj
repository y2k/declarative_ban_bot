

(defn ^{:in fetch_json_received} ??? [message]
  (let [user_id message.user_id
        _ (valid-command message.text "/ls")]
    (doa
     (let [[items now]
           (combine
            (query_db! "SELECT * FROM ... WHERE user_id = ?" user_id)
            (get_now!))]
       (fetch! "https://api.telegram.org/bot.../sendMessage"
                {:body (str "For " now ", your items:" items)})))))










(comment

  (bar)

  (binding [fetch! test_fetch]
    (bar))

  comment)

(defn ^:dynamic fetch! [a b] (+ a b))
(defn test_fetch [a b] (* a b))
(defn bar [] (fetch! 3 7))
