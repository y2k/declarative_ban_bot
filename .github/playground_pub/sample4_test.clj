(__unsafe_insert_js "import { promises as fs } from 'fs';")
(__unsafe_insert_js "import m from './sample4.js';")

(defn runtime_create_world []
  {:perform (fn [name args] (console/error "Effect not handled:" (str "[" name "]") args))})

(defn runtime_attach_effect_handler [world keyf f]
  (assoc world :perform (fn [key args]
                          (if (= key keyf)
                            (f globalThis args)
                            (.perform world key args)))))

(->
 (m/fetch
  {:json
   (fn []
     (->
      (.readFile fs "../test/samples/sample1.json")
      (.then JSON.parse)))}
  process.env
  (->
   (runtime_create_world)
   (runtime_attach_effect_handler
    :fetch
    (fn [js args]
      (console/info "[INFO]" "[fetch]" args)))))
;;  (.then console.info)
 )
