(defn fetch [url decoder props]
  (fn [world]
    (.perform world :fetch {:url url :decoder decoder :props props} world)))

(defn database [sql args]
  (fn [world]
    (.perform world :database {:sql sql :args args} world)))

(defn batch [args]
  (fn [world]
    (.perform world :batch {:children args} world)))

(defn call [key data]
  (fn [env] (env/perform key data)))

(defn dispatch [key data] (call :dispatch [key data]))

(defn broadcast [key fx f]
  (then fx (fn [json] (dispatch key (f json)))))

(defn then [fx f]
  (fn [env]
    (let [pr (fx env)]
      (.then
       pr
       (fn [r] (let [r2 (f r)]
                 (r2 env)))))))

(defn next [fx f]
  (fn [world]
    (.perform world :next [fx f] world)))

(defn base64_to_string [str]
  (.toString (Buffer.from str "base64")))

(defn pure [x] (fn [_] x))

(defn create_world []
  {:perform (fn [name args]
              (console/error "Effect not handled:" (str "[" name "]") args))})

(defn run_io [w f] (f w))
(defn run_effect [f w] (f w))

(defn string_to_hex [str]
  (-> (TextEncoder.) (.encode str) Buffer/from (.toString "base64")))

(defn attach_log_handler [world]
  (assoc world :perform (fn [key args w2]
                          (console/info "[LOG]" (JSON/stringify [key args] null 2))
                          (.perform world key args w2))))

(defn attach_effect_handler [world keyf f]
  (assoc world :perform (fn [key args w2]
                          (if (= key keyf)
                            (f globalThis args w2)
                            (.perform world key args w2)))))
