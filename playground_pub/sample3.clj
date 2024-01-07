
(defn database! [env sql arg]
  (->
   env.SPECTATOR_DB
   (.prepare sql)
   (.bind arg)
   (.all)))

(defn fetch! [_ url decoder props]
  (->
   (js/fetch url props)
   (.then (fn [x]
            (if (= :json decoder) (.json x) (.text x))))))

(defn now! [_]
  (.now js/Date))

(defn main [run_app env]
  (run_app
   scheduled
   {:now (fn [_] (.now js/Date))
    :db (fn [_ sql arg]
          (fn [_]
            (->
             env.SPECTATOR_DB
             (.prepare sql)
             (.bind arg)
             (.all))))
    :fetch (fn [_ url decoder props]
             (->
              (js/fetch url props)
              (.then (fn [x] (if (= :json decoder) (.json x) (.text x))))))}))

(defn scheduled! [env]
  (let [now (now!)
        subs (await! (database! env "SELECT FROM ... WHERE content->>'updated' > ? ORDER BY last_checked LIMIT 5" now))]
    (->
     subs
     (.map (fn [x] (fetch! x.url :json {})))
     (.append (database! env "UPDATE ... SET now = ? WHERE content->>'updated' > ? ORDER BY last_checked LIMIT 5" now now))
     (.all js/Promise))))

(defn scheduled-downloaded! [results]
  (let [results (get-success-downloades results)
        rss-items (get-rss-items)]
    ???))




