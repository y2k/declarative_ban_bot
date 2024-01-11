(defn fetch [url decoder props]
  (fn [world]
    (.perform world :fetch {:url url :decoder decoder :props props} world)))

(defn database [sql args]
  (fn [world]
    (.perform world :database {:sql sql :args args} world)))

(defn batch [args]
  (fn [world]
    (.perform world :batch {:children args} world)))

(defn base64_to_string [str]
  (.toString (Buffer.from str "base64")))

(defn pure [x] (fn [_] x))

(defn create_world []
  {:perform (fn [name args]
              (console/error "Effect not handled:" (str "[" name "]") args))})

(defn run_io [w f] (f w))

(defn string_to_hex [str]
  (-> (TextEncoder.) (.encode str) Buffer/from (.toString "base64")))

(defn attach_log_handler [world keyf f]
  (assoc world :perform (fn [key args w2]
                          (console/info "[LOG]" key args)
                          (.perform world key args w2))))

(defn attach_effect_handler [world keyf f]
  (assoc world :perform (fn [key args w2]
                          (if (= key keyf)
                            (f globalThis args w2)
                            (.perform world key args w2)))))
