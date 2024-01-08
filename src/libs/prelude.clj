(export-default
 {:fetch
  (fn [url decoder props]
    (fn [world]
      (.perform world :fetch {:url url :decoder decoder :props props} world)))

  :database
  (fn [sql args]
    (fn [world]
      (.perform world :database {:sql sql :args args} world)))

  :batch
  (fn [args]
    (fn [world]
      (.perform world :batch {:children args} world)))

  :base64_to_string
  (fn [str]
    (.toString (Buffer.from str "base64")))

  :pure (fn [x] (fn [_] x))

  :create_world
  (fn []
    {:perform (fn [name args]
                (console/error "Effect not handled:" (str "[" name "]") args))})

  :run_io
  (fn [w f] (f w))

  :string_to_hex
  (fn [str]
    (-> (TextEncoder.) (.encode str) Buffer/from (.toString "base64")))

  :attach_log_handler
  (fn [world keyf f]
    (assoc world :perform (fn [key args w2]
                            (console/info "[LOG]" key args)
                            (.perform world key args w2))))

  :attach_effect_handler
  (fn [world keyf f]
    (assoc world :perform (fn [key args w2]
                            (if (= key keyf)
                              (f globalThis args w2)
                              (.perform world key args w2)))))})