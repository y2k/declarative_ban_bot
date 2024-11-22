(defn fetch [url decoder props]
  (fn [w] ((:fetch w) {:url url :decoder decoder :props props})))

(defn database [sql args]
  (fn [w] ((:database w) {:sql sql :args args})))

(defn dispatch [key data]
  (fn [w] ((:dispatch w) [key data])))
