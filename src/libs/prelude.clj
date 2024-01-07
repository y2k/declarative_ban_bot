(export-default
 {:fetch
  (fn [url decoder props]
    (fn [world]
      (.perform world :fetch {:url url :decoder decoder :props props})))

  :pure (fn  [x] (fn [_] x))

  :create_world
  (fn []
    {:perform (fn [name args]
                (console/error "Effect not handled:" (str "[" name "]") args))})

  :attach_effect_handler
  (fn [world keyf f]
    (assoc world :perform (fn [key args]
                            (if (= key keyf)
                              (f globalThis args)
                              (.perform world key args)))))})