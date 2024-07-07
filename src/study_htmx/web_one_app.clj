(ns study-htmx.web-one-app
  (:require
   [clojure.string :as s]
   [io.pedestal.interceptor :as interceptor]
   [study-htmx.response :as shr]
   [study-htmx.templates :as sht]
   [study-htmx.archiver :as archiver]
   [hiccup2.core :as h2c]))

(def ^{:private true} contacts-db-init
  (into {}
        (map (fn [s n]
               (let [n (inc n)]
                 [(str n)
                  {:fname (str s n)
                   :lname (str "bar" n)
                   :phone "9876543210"
                   :email (format "%s.%s@example.com" s n)}]))
             ["foo" "bar" "foobarbaz" "quxx" "moofoo" "bazquxx" "barmoobarbaz"]
             (range))))

(defonce contacts-db
  (atom contacts-db-init))

(comment
  (reset! contacts-db contacts-db-init))

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
    :enter (fn [context]
             (assoc context :archiver archiver/archiver))

    :leave (fn [{:keys [request] :as context}]
             (let [page-size 5
                   q (-> request :query-params :q)
                   page (-> request :query-params :page
                            ((fnil Integer/parseInt "1"))
                            (max 1))
                   contacts (if (s/blank? q)
                              @contacts-db
                              (search-contacts q @contacts-db))
                   contacts-paged (partition-all page-size contacts)
                   num-pages (count contacts-paged)
                   current-page (if (>= page num-pages)
                                  num-pages
                                  page)
                   page-of-contacts (nth contacts-paged (dec current-page)
                                         [])
                   page-of-contacts (into {} page-of-contacts)
                   response (if (= (get-in request [:headers "hx-trigger"])
                                   "search")
                              (str (h2c/html (sht/contact-rows page-of-contacts)))
                              (sht/layout
                               (sht/contacts-page-body q page-of-contacts
                                                       num-pages
                                                       current-page
                                                       @(:archiver context))))]
               (->> response
                    shr/ok
                    (assoc context :response))))}))

(defn count-contacts!
  []
  (format "(%s total contacts)"
          (count @contacts-db)))

(def count-contacts-handler
  {:name ::count-contacts
   :enter (fn [context]
            (->> (count-contacts!)
                 shr/ok
                 (assoc context :response)))})

(def new-contact-page-handler
  (interceptor/interceptor
   {:name ::new-contact-page
    :enter (fn [context]
             (->> (sht/contact-new {:show-error-set #{}})
                  sht/layout
                  shr/ok
                  (assoc context :response)))}))

(defn duplicate-email?
  [db email id]
  (some #(= email (:email %))
        (vals (dissoc @db id))))

(defn create-or-update-contact!
  [db id contact]
  (let [errors (reduce-kv (fn [errors k v]
                            (if (s/blank? v)
                              (conj errors k)
                              errors))
                          #{}
                          contact)
        errors (if (duplicate-email? db (:email contact) id)
                 (conj errors :email-duplicate)
                 errors)]
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
                (apply (partial max 0))
                inc
                str)]
    (create-or-update-contact! db id contact)))

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
               (->> (assoc contact :id id)
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
               (->> (assoc contact :id id)
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
                              (->> (assoc maybe-new-contact :id id)
                                   (sht/contact-edit id)
                                   sht/layout
                                   shr/ok)
                              (shr/see-other "/contacts" headers))]
               (assoc context :response response)))}))

(def delete-contact-handler
  (interceptor/interceptor
   {:name ::delete-contact-handler
    :enter (fn [{:keys [request headers] :as context}]
             (let [id (-> request :path-params :id)
                   delete-btn? (= (get-in request [:headers "HX-Trigger"])
                                  "delete-btn")]
               (swap! contacts-db dissoc id)
               (assoc context :response
                      (if delete-btn?
                        (shr/see-other "/contacts" headers)
                        (-> "" shr/ok)))))}))

(def bulk-delete-contacts
  (interceptor/interceptor
   {:name ::bulk-delete-contacts
    :leave (fn [{:keys [request] :as context}]
             (let [contact-ids (-> request :form-params :selected_contact_ids)
                   contact-ids (if (string? contact-ids)
                                 [contact-ids]
                                 contact-ids)]
               (apply swap! contacts-db
                      dissoc
                      contact-ids)
               ((:enter search-contacts-handler)
                (assoc-in context [:request :query-params :q] ""))))}))

(def validate-email-for-contact
  (interceptor/interceptor
   {:name ::validate-email-for-contact
    :enter (fn [{:keys [request] :as context}]
             (let [input-email (-> request :params :email)
                   contact-id (-> request :path-params :contact-id)
                   response (when (duplicate-email? contacts-db
                                                    input-email
                                                    contact-id)
                              "Sorry. Email already taken.")]
               (assoc context :response
                      (-> response shr/ok))))}))

(def archive-of-contacts
  (interceptor/interceptor
   {:name ::archive-of-contacts
    :enter (fn [context]
             (when (= @(:status @archiver/archiver) :done)
               (archiver/reset-archive! archiver/archiver))
             (when (= @(:status @archiver/archiver) :waiting)
               (archiver/do-archive! (into [] @contacts-db)
                                     (count @contacts-db)
                                     archiver/archiver))
             (assoc context :archiver archiver/archiver))
    :leave (fn [context]
             (assoc context
                    :response
                    (-> (sht/contacts-archive @(:archiver context))
                        h2c/html
                        str
                        shr/ok)))}))

(def archive-of-contacts-status
  (interceptor/interceptor
   {:name ::archive-of-contacts-status
    :enter (fn [context]
             (assoc context :archiver archiver/archiver))
    :leave (fn [context]
             (assoc context
                    :response
                    (-> (sht/contacts-archive @(:archiver context))
                        h2c/html
                        str
                        shr/ok)))}))

(comment
  (search-contacts "foo" @contacts-db)

  'a)
