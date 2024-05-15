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
  (filter (fn [[_id {:keys [fname lname email]
                     :or {fname "" lname "" email ""}}]]
            (or (s/includes? fname query-str)
                (s/includes? lname query-str)
                (s/includes? email query-str)))
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

(def new-contact-page-handler
  (interceptor/interceptor
   {:name ::new-contact-page
    :enter (fn [context]
             (->> (sht/contact-new {:show-error-set #{}})
                  sht/layout
                  shr/ok
                  (assoc context :response)))}))

(defn create-or-update-contact!
  [db id contact]
  (let [errors (reduce-kv (fn [errors k v]
                            (if (s/blank? v)
                              (conj errors k)
                              errors))
                          #{}
                          contact)]
    (if (not-empty errors)
      (assoc contact :show-error-set errors)
      (do (swap! db assoc id
                 (dissoc contact :show-error-set))
          (get @db id)))))

(defn new-contact!
  [db contact]
  (let [id (->> @db
                keys
                (map #(Integer/parseInt %))
                (apply max)
                inc
                str)]
    (create-or-update-contact! id db contact)))

(def new-contact-add-handler
  (interceptor/interceptor
   {:name ::new-contact-add
    :enter (fn [{:keys [request headers] :as context}]
             (let [new-contact-data (:form-params request)
                   maybe-new-contact (new-contact! contacts-db
                                                   new-contact-data)
                   response (if (not-empty (:show-error-set maybe-new-contact))
                              (->> (sht/contact-new maybe-new-contact)
                                   sht/layout
                                   shr/ok)
                              (shr/see-other "/contacts" headers))]
               (assoc context :response response)))}))

(def view-contact-handler
  (interceptor/interceptor
   {:name ::view-contact-page
    :enter (fn [{:keys [request] :as context}]
             (let [id (-> request :path-params :id)
                   contact (get @contacts-db id)]
               (->> contact
                    (sht/contact-view id)
                    sht/layout
                    shr/ok
                    (assoc context :response))))}))

(def edit-contact-page-handler
  (interceptor/interceptor
   {:name ::edit-contact-page
    :enter (fn [{:keys [request] :as context}]
             (let [id (-> request :path-params :id)
                   contact (get @contacts-db id)]
               (->> contact
                    (sht/contact-edit id)
                    sht/layout
                    shr/ok
                    (assoc context :response))))}))

(def edit-contact-handler
  (interceptor/interceptor
   {:name ::edit-contact-handler
    :enter (fn [{:keys [request headers] :as context}]
             (let [id (-> request :path-params :id)
                   new-contact-data (:form-params request)
                   maybe-new-contact (create-or-update-contact!
                                      contacts-db
                                      id
                                      new-contact-data)
                   response (if (not-empty (:show-error-set maybe-new-contact))
                              (->> maybe-new-contact
                                   (sht/contact-edit id)
                                   sht/layout
                                   shr/ok)
                              (shr/see-other "/contacts" headers))]
               (assoc context :response response)))}))

(def delete-contact-handler
  (interceptor/interceptor
   {:name ::delete-contact-handler
    :enter (fn [{:keys [request headers] :as context}]
             (let [id (-> request :path-params :id)]
               (swap! contacts-db dissoc id)
               (assoc context :response
                      (shr/see-other "/contacts" headers))))}))

(comment
  (search-contacts "foo" @contacts-db)

  'a)
