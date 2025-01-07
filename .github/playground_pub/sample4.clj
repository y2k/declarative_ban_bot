(defn fetch_ [url decoder props]
  (fn [world]
    (.perform world :fetch {:url url :decoder decoder :props props})))

(defn pure [x] (fn [_] x))

(defn parse_command_url [] "https://youtu.be/P-beFdX0s2I?si=5_IzuZWwTwxjpwrB")
(defn parse_command_preview_url [] "https://www.youtube.com/watch?v=Mxoog-mI9IU")

(defn handle [json]
  (if-let [chat_id json?.message?.chat?.id
           text json?.message?.text
           url (parse_command_url text)
           preview_url (parse_command_preview_url text)]
    (fetch_ "https://api.telegram.org/bot~TG_TOKEN~/sendMessage"
            :json
            {:method "POST"
             :headers {"Content-Type" "application/json"}
             :body
             (JSON/stringify
              {:chat_id chat_id
               :link_preview_options {:url preview_url}
               :text url})})
    (pure nil)))

(defn runtime_create_world []
  {:perform (fn [name args] (console/error "Effect not handled:" (str "[" name "]") args))})

(defn runtime_attach_effect_handler [world keyf f]
  (assoc world :perform (fn [key args]
                          (if (= key keyf)
                            (f globalThis args)
                            (.perform world key args)))))

(export-default
 {:fetch
  (fn [request env world]
    (->
     (.json request)
     (.then
      (fn [json]
        (let [effect (handle json)]
          (->
           (runtime_create_world)
           (runtime_attach_effect_handler
            :fetch
            (fn [js args]
              (->
               (.replace args.url "~TG_TOKEN~" env.TG_TOKEN)
               (js/fetch args.props)
               (.then (fn [x] (if (= "json" args.decoder) (.json x) (.text x)))))))
           (merge world)
           effect))))
     (.catch console.error)
     (.then (fn [] (Response. "OK")))))})