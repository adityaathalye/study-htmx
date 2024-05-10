(ns study-htmx.web-one-app
  (:require
   [clojure.string :as s]
   [io.pedestal.interceptor :as interceptor]
   [study-htmx.response :as shr]
   [study-htmx.templates :as sht]))

(def contacts-db
  ["foo" "bar" "foobarbaz" "quxx" "moofoo" "bazquxx" "barmoobarbaz"])

(defn search-contacts
  [query-str contacts]
  (filter (fn [contact]
            (s/includes? contact query-str))
          contacts))

(defn contacts-list
  [contacts]
  [:ul (for [c contacts] [:li c])])

(def search-contacts-handler
  (interceptor/interceptor
   {:name ::search-contacts
    :enter (fn [context]
             (let [q (-> context :request :query-params :q)
                   contacts (if (s/blank? q)
                              contacts-db
                              (search-contacts q contacts-db))]
               (->> contacts
                    contacts-list
                    sht/layout
                    shr/ok
                    (assoc context :response))))}))

(comment
  (search-contacts "foo" contacts-db)

  'a)
