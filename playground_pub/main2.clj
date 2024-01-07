(reg_event
 "fetch_json_received"
 (fn [ctx message]
   (let [user_id message?.user_id
         _ (valid_command message?.text "/ls")]
     (->
      (Promise/all (ctx/query_db "SELECT * FROM ... WHERE user_id = ?" user_id)
                   (ctx/get_now))
      (.then (fn [items now]
               (ctx/fetch
                "https://api.telegram.org/bot.../sendMessage"
                {:body (str "For " now ", your items:" items)})))))))
