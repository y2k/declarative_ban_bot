(__unsafe_insert_js "import { promises as fs } from 'fs';")
(__unsafe_insert_js "import p from './prelude.js';")
(__unsafe_insert_js "import app from './main.js';")

(defn get_sha256_hash [str]
  (let [encoder (TextEncoder.)
        data (.encode encoder str)
        hash_promise (.digest crypto.subtle "SHA-256" data)]
    (.then
     hash_promise
     (fn [hash]
       (->
        (Array/from (Uint8Array. hash))
        (.map (fn [b] (-> b (.toString 16) (.padStart 2 "0"))))
        (.join ""))))))

(defn test_item [template ban_text]
  (.then
   (get_sha256_hash ban_text)
   (fn [hash]
     (let [expected_path (str "../test/samples/" hash ".json")
           message (->
                    template
                    (.replace "1704388913" (/ (Date/now) 1000))
                    (.replace "1234567890" (JSON/stringify (p/base64_to_string ban_text)))
                    (JSON/parse))
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
        (.then (fn []
                 (let [actual (JSON/stringify log null 2)]
                   (.then
                    (fs/readFile expected_path "utf-8")
                    (fn [expected]
                      (if (not= actual expected)
                        (throw (Error. (str "Actual <> Expected: " expected_path)))
                        null))
                    (fn [e]
                      (fs/writeFile expected_path actual)))))))))))

(->
 (fs/readFile "../test/samples/sample.template.json" "utf-8")
 (.then
  (fn [template]
    (->
     (fs/readFile "../test/samples/sample_texts.txt" "utf-8")
     (.then
      (fn [ban_texts]
        (->
         (.split ban_texts "\n")
         (.reduce
          (fn [promise ban_text]
            ;; (console/info "=== [LOG] ===\n" (.replace template "1234567890" (JSON/stringify (p/base64_to_string ban_text))))
            (.then promise (test_item template ban_text)))
          (Promise/resolve null)))))))))