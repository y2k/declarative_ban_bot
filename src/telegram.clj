(ns telegram)

(defn- fetch [url decoder props]
  (fn [w] ((:fetch w) {:url url :decoder decoder :props props})))

(defn send_message [cmd args]
  (fetch
   (str "https://api.telegram.org/bot~TG_TOKEN~/" cmd)
   :json
   {:method "POST"
    :headers {"Content-Type" "application/json"}
    :body (JSON.stringify args)}))
