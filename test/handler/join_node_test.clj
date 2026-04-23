(ns handler.join-node-test
  (:require [handler.join :as join]
            ["node:test" :as t]
            ["node:assert/strict" :as assert]))

(defn- collect_fetches [effect]
  (let [calls (atom [])]
    (->
     (effect {:fetch (fn [request]
                       (swap! calls (fn [xs] (conj xs request)))
                       (Promise/resolve nil))})
     (.then (fn [_] (deref calls))))))

(t/test "handle approves correct captcha response"
        (fn []
          (->
           (join/handle
            {:TG_APPROVE_CHAT -1001234567890}
            {:message {:chat {:id 42}
                       :from {:id 42}
                       :text "4"}})
           (collect_fetches)
           (.then
            (fn [calls]
              (assert/deepStrictEqual
               calls
               [{:url "https://api.telegram.org/bot~TG_TOKEN~/sendMessage"
                 :decoder :json
                 :props {:method "POST"
                         :headers {"Content-Type" "application/json"}
                         :body "{\"chat_id\":42,\"text\":\"Вы прошли капчу.\"}"}}
                {:url "https://api.telegram.org/bot~TG_TOKEN~/approveChatJoinRequest"
                 :decoder :json
                 :props {:method "POST"
                         :headers {"Content-Type" "application/json"}
                         :body "{\"chat_id\":-1001234567890,\"user_id\":42}"}}]))))))
