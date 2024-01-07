(__unsafe_insert_js "import { promises as fs } from 'fs';")
(__unsafe_insert_js "import runtime from './prelude.js';")
(__unsafe_insert_js "import app from './main.js';")

(->
 (app/fetch
  {:json
   (fn []
     (->
      (fs/readFile "../test/samples/sample1.json")
      (.then JSON.parse)))}
  {:fixme 0}
  (->
   (runtime/create_world)
   (runtime/attach_effect_handler
    :fetch
    (fn [_ args]
      (console/info "[INFO]" "[fetch]" args))))))