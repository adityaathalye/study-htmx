(ns study-htmx.web-one-app
  (:require
   [clojure.string :as s]
   [io.pedestal.interceptor :as interceptor]
   [study-htmx.response :as shr]
   [study-htmx.templates :as sht]))

(defonce contacts-db
  (atom (into {}
              (map (fn [s n]
                     (let [n (inc n)]
                       [(str n)
                        {:fname (str s n)
                         :lname (str "bar" n)
                         :phone "9876543210"
                         :email (format "%s.%s@example.com" s n)}]))
                   ["foo" "bar" "foobarbaz" "quxx" "moofoo" "bazquxx" "barmoobarbaz"]
                   (range)))))

(defn search-contacts
  [query-str contacts]
  (filter (fn [[_id {:keys [fname lname]}]]
            (or (s/includes? fname query-str)
                (s/includes? lname query-str)))
          contacts))

(def search-contacts-handler
  (interceptor/interceptor
   {:name ::search-contacts
    :enter (fn [{:keys [request] :as context}]
             (let [q (-> request :query-params :q)
                   contacts (if (s/blank? q)
                              @contacts-db
                              (search-contacts q @contacts-db))]
               (->> (sht/contacts-page-body q contacts)
                    sht/layout
                    shr/ok
                    (assoc context :response))))}))

(comment
  (search-contacts "foo" contacts-db)

  'a)
