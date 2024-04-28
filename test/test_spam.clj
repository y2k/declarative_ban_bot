(ns test (:require ["../vendor/packages/effects/effects" :as p]
                   ["../src/main" :as app]
                   [js.fs.promises :as fs]))

(defn- get_sha256_hash [str]
  (let [encoder (TextEncoder.)
        data (.encode encoder str)
        hash_promise (.digest crypto.subtle "SHA-256" data)]
    (.then
     hash_promise
     (fn [hash]
       (->
        (Array.from (Uint8Array. hash))
        (.map (fn [b] (-> b (.toString 16) (.padStart 2 "0"))))
        (.join ""))))))

(defn base64_to_string [str]
  (.toString (Buffer.from str "base64")))

(defn- test_item [template ban_text]
  (.then
   (get_sha256_hash ban_text)
   (fn [hash]
     (let [expected_path (str "../test/samples/output/" hash ".json")
           message (->
                    template
                    (.replace "1234567890" (JSON.stringify (base64_to_string ban_text)))
                    (JSON.parse))
           log (Array.)]
       (->
        (app/default.fetch
         {:headers {:get (fn [] "TG_SECRET_TOKEN")}
          :json (fn [] (Promise.resolve message))}
         {:TG_SECRET_TOKEN "TG_SECRET_TOKEN"
          :cofx {:now 1704388914000}}
         (->
          (p/attach_empty_effect_handler {})
          (p/attach_eff :batch (fn [args w] (.map args.children (fn [f] (f w)))))
          (p/attach_eff :database (fn [args] (.push log {:type :database :args args})))
          (p/attach_eff :fetch (fn [args] (.push log {:type :fetch :args args})))))
        (.then (fn []
                 (let [actual (JSON.stringify log null 2)]
                   (.then
                    (fs/readFile expected_path "utf-8")
                    (fn [expected]
                      (if (not= actual expected)
                        (throw (Error. (str "Actual <> Expected: " expected_path)))
                        null))
                    (fn [e]
                      (fs/writeFile expected_path actual)))))))))))

(->
 (Promise.all
  [(fs/readFile "../test/samples/sample.template.json" "utf-8")
   (fs/readFile "../test/samples/sample_texts.txt" "utf-8")])
 (.then (fn [[template ban_texts]]
          (->
           (.split ban_texts "\n")
           (.reduce
            (fn [promise ban_text]
              (.then promise (test_item template ban_text)))
            (Promise.resolve null))))))
