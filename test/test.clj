(__unsafe_insert_js "import { promises as fs } from 'fs';")
(__unsafe_insert_js "import p from './prelude.js';")
(__unsafe_insert_js "import app from './main.js';")

(->
 (fs/readFile "../test/samples/sample.template.json" "utf-8")
 (.then
  (fn [template]
    (->
     (fs/readFile "../test/samples/ban_texts.txt" "utf-8")
     (.then
      (fn [ban_texts]
        (->
         (.split ban_texts "\n")
         (.reduce
          (fn [promise ban_text]
            ;; (console/info "=== [LOG] ===\n" (.replace template "1234567890" (JSON/stringify (p/base64_to_string ban_text))))
            (.then
             promise
             (fn []
               (let [message (JSON/parse (.replace template "1234567890" (JSON/stringify (p/base64_to_string ban_text))))
                     log (Array.)]
                 (->
                  {:json (fn [] (Promise/resolve message))}
                  (app/fetch
                   {}
                   (->
                    (p/create_world)
                    (p/attach_effect_handler :batch (fn [_ args w] (.map args.children (fn [f] (f w)))))
                    (p/attach_effect_handler :database (fn [_ args] (.push log {:type :database :args args})))
                    (p/attach_effect_handler :fetch (fn [_ args] (.push log {:type :fetch :args args})))))
                  (.then (fn [] (console/log "LOG:" (JSON/stringify log null 2)))))))))
          (Promise/resolve null)))))))))