(ns join-test
  (:require [test-cloudflare-worker :as tu]
            ["node:test" :as t]))

(tu/before-after
 {:TG_TOKEN {:type "plain_text" :value "test-token"}
  :TG_SECRET_TOKEN {:type "plain_text" :value "test-secret"}
  :TG_APPROVE_CHAT {:type "plain_text" :value "-1001234567890"}})

(defn- telegram-request [text]
  (Request.
   "http://localhost/telegram-bot"
   {:method "POST"
    :headers {"Content-Type" "application/json"
              "x-telegram-bot-api-secret-token" "test-secret"}
    :body (JSON/stringify {:message {:chat {:id 42} :from {:id 42} :text text}})}))

(tu/test "handle approves correct captcha response"
         (telegram-request "4")
         "eyJlZmZlY3RzIjpbeyJ1cmwiOiJodHRwczovL2FwaS50ZWxlZ3JhbS5vcmcvYm90dGVzdC10b2tlbi9zZW5kTWVzc2FnZSIsInByb3BzIjp7Im1ldGhvZCI6IlBPU1QiLCJoZWFkZXJzIjp7IkNvbnRlbnQtVHlwZSI6ImFwcGxpY2F0aW9uL2pzb24ifSwiZGVjb2RlciI6Impzb24iLCJib2R5Ijoie1wiY2hhdF9pZFwiOjQyLFwidGV4dFwiOlwi0JLRiyDQv9GA0L7RiNC70Lgg0LrQsNC/0YfRgy4g0JTQvtCx0YDQviDQv9C+0LbQsNC70L7QstCw0YLRjCDQsiAtMTAwMTIzNDU2Nzg5MCFcIn0ifSwidHlwZSI6ImVmZmVjdHNfcHJvbWlzZS5mZXRjaDpmZXRjaCJ9LHsidXJsIjoiaHR0cHM6Ly9hcGkudGVsZWdyYW0ub3JnL2JvdHRlc3QtdG9rZW4vYXBwcm92ZUNoYXRKb2luUmVxdWVzdCIsInByb3BzIjp7Im1ldGhvZCI6IlBPU1QiLCJoZWFkZXJzIjp7IkNvbnRlbnQtVHlwZSI6ImFwcGxpY2F0aW9uL2pzb24ifSwiZGVjb2RlciI6Impzb24iLCJib2R5Ijoie1wiY2hhdF9pZFwiOlwiLTEwMDEyMzQ1Njc4OTBcIixcInVzZXJfaWRcIjo0Mn0ifSwidHlwZSI6ImVmZmVjdHNfcHJvbWlzZS5mZXRjaDpmZXRjaCJ9LHsidXJsIjoiaHR0cHM6Ly9hcGkudGVsZWdyYW0ub3JnL2JvdHRlc3QtdG9rZW4vc2VuZE1lc3NhZ2UiLCJwcm9wcyI6eyJtZXRob2QiOiJQT1NUIiwiaGVhZGVycyI6eyJDb250ZW50LVR5cGUiOiJhcHBsaWNhdGlvbi9qc29uIn0sImRlY29kZXIiOiJqc29uIiwiYm9keSI6IntcInBhcnNlX21vZGVcIjpcIk1hcmtkb3duVjJcIixcImNoYXRfaWRcIjpcIkBhbmRyb2lkX2RlY2xhcmF0aXZlX2Jhbl9sb2dcIixcInRleHRcIjpcItCa0LDQv9GH0LAg0L/RgNC+0LnQtNC10L3QsDogWzQyXSh0ZzovL3VzZXI/aWQ9NDIpXCJ9In0sInR5cGUiOiJlZmZlY3RzX3Byb21pc2UuZmV0Y2g6ZmV0Y2gifV0sInJlc3BvbnNlIjoiT0sifQ==")

(tu/test "handle logs wrong captcha response"
         (telegram-request "wrong")
         "eyJlZmZlY3RzIjpbeyJ1cmwiOiJodHRwczovL2FwaS50ZWxlZ3JhbS5vcmcvYm90dGVzdC10b2tlbi9zZW5kTWVzc2FnZSIsInByb3BzIjp7Im1ldGhvZCI6IlBPU1QiLCJoZWFkZXJzIjp7IkNvbnRlbnQtVHlwZSI6ImFwcGxpY2F0aW9uL2pzb24ifSwiZGVjb2RlciI6Impzb24iLCJib2R5Ijoie1wiY2hhdF9pZFwiOjQyLFwidGV4dFwiOlwi0J7RgtCy0LXRgiDQvdC10L/RgNCw0LLQuNC70YzQvdGL0LkuINCc0L7QttC10YLQtSDQvdCw0L/QuNGB0LDRgtGMINC/0L7QtNC00LXRgNC20LrQtSDQsiBAeG9mZnRvcFwifSJ9LCJ0eXBlIjoiZWZmZWN0c19wcm9taXNlLmZldGNoOmZldGNoIn0seyJ1cmwiOiJodHRwczovL2FwaS50ZWxlZ3JhbS5vcmcvYm90dGVzdC10b2tlbi9zZW5kTWVzc2FnZSIsInByb3BzIjp7Im1ldGhvZCI6IlBPU1QiLCJoZWFkZXJzIjp7IkNvbnRlbnQtVHlwZSI6ImFwcGxpY2F0aW9uL2pzb24ifSwiZGVjb2RlciI6Impzb24iLCJib2R5Ijoie1wiY2hhdF9pZFwiOlwiQGFuZHJvaWRfZGVjbGFyYXRpdmVfYmFuX2xvZ1wiLFwidGV4dFwiOlwi0JrQsNC/0YfQsCDQvdC1INC/0YDQvtC50LTQtdC90LA6IHVzZXIgNDJcXG50ZXh0OlxcbmQzSnZibWM9XCJ9In0sInR5cGUiOiJlZmZlY3RzX3Byb21pc2UuZmV0Y2g6ZmV0Y2gifV0sInJlc3BvbnNlIjoiT0sifQ==")
