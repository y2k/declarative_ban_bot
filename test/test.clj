(__unsafe_insert_js "import { promises as fs } from 'fs';")
(__unsafe_insert_js "import m from './main.js';")

(m/fetch
 {:json
  (fn []
    (->
     (.readFile fs "../test/samples/sample1.json")
     (.then JSON.parse)))}
 {:TG_TOKEN process.env.TG_TOKEN})
