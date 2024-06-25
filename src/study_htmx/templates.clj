(ns study-htmx.templates
  (:require [hiccup.page :as hp]
            [hiccup.form :as hf]))

(defn layout
  [body]
  (hp/html5
   [:head
    [:title "Contacts App"]
    (hp/include-css "/css/site.css")
    [:script {:type "text/javascript"
              :src "https://unpkg.com/htmx.org@1.9.12"
              :integrity "sha384-ujb1lZYygJmzgSwoxRggbCHcjc0rB2XoQrxeTUQyRjrOnlCoYta87iKBWq3EsdM2"
              :crossorigin "anonymous"}]]
   [:body {:hx-boost "true"} body]))

(defn contact-search-form
  [search-q]
  [:form {:action "/contacts" :method "get"}
   [:label {:for "search"} "Search Term: "]
   [:input {:id "search" :type "search" :name "q" :value search-q
            :hx-get "/contacts"
            :hx-trigger "search, keyup delay:200ms changed"
            :hx-target "tbody"
            :hx-swap "innerHTML"
            :hx-push-url "true"
            :hx-indicator "#spinner"}]
   [:input {:type "submit" :value "Search"}]
   [:img {:id "spinner" :class "htmx-indicator"
          :style "width: 1em; vertical-align: middle"
          :src "/img/spinning-circles.svg"
          :alt "Searching contacts..."}]])

(defn contact-rows
  [contacts]
  (doall (for [[id {:keys [fname lname phone email]}] contacts]
           [:tr
            [:td [:input {:type "checkbox" :name "selected_contact_ids" :value id :autocomplete "off"}]]
            [:td fname] [:td lname] [:td phone] [:td email]
            [:td
             [:a {:href (format "/contacts/edit/%s" id)} "Edit"] " / "
             [:a {:href (format "/contacts/view/%s" id)} "View"] " / "
             [:a {:class "delete-contact"
                  :href "#"
                  :hx-delete (format "/contacts/delete/%s" id)
                  :hx-confirm "Delete for sure, forever?!"
                  :hx-swap "outerHTML swap:0.5s"
                  :hx-target "closest tr"}
              "Delete"]]])))

(defn contacts-list
  [contacts total-pages current-page]
  [:form
   [:table
    [:thead
     [:tr [:td "Bulk Select"] [:td "First Name"] [:td "Last Name"] [:td "Phone"] [:td "Email"] [:td "Action"]]]
    [:tbody
     (contact-rows contacts)]
    [:tfoot {:id "load-more"
             :hx-swap-oob "true"}
     (when (< current-page total-pages)
       [:tr
        [:td {:colspan 6 :style "text-align: center"}
         [:button {:hx-target "previous tbody > tr"
                   :hx-swap "afterend"
                   :hx-select "tbody > tr"
                   :hx-get (format "/contacts?page=%s" (max (inc current-page) 1))}
          "Load More"]]])]]
   [:p [:a {:href "/contacts/new"} [:strong "Add New Contact"]]
    [:span {:hx-get "/contacts/count" :hx-trigger "load, click from:(tbody tr a.delete-contact)"}
     [:img {:id "spinner" :class "htmx-indicator"
            :style "width: 1em; vertical-align: middle"
            :src "/img/spinning-circles.svg"}]]]
   [:p [:button {:hx-delete "/contacts"
                 :hx-confirm "Are you sure you want to delete these contacts?"
                 :hx-target "body"}
        "Delete Contacts"]]])

(defn contact-form
  [{:keys [id fname lname phone email show-error-set]
    :or {show-error-set #{}}
    :as _contact}]
  [:fieldset
   [:legend "Contact Values"]
   [:p
    (hf/label "email" "Email: ")
    (hf/email-field {:hx-get (format "/contacts/validate-email/%s" (or id "new"))
                     :hx-target "next .error"
                     :hx-trigger "change, keyup delay:500ms changed"
                     :placeholder "Email"}
                    "email" email)
    [:span {:class "error"}
     (when (show-error-set :email) "Bad email address. ")
     (when (show-error-set :email-duplicate) "Sorry. Email already exists. ")]]
   [:p
    (hf/label "fname" "First Name: ")
    (hf/text-field "fname" fname)
    (when (show-error-set :fname)
      [:span {:class "error"} "Bad first name."])]
   [:p
    (hf/label "lname" "Last Name: ")
    (hf/text-field "lname" lname)
    (when (show-error-set :lname)
      [:span {:class "error"} "Bad last name."])]
   [:p
    (hf/label "phone" "Phone Number: ")
    (hf/text-field "phone" phone)
    (when (show-error-set :phone)
      [:span {:class "error"} "Bad phone number."])]
   (hf/submit-button "Submit")])

(defn contact-new
  [contact]
  (hf/form-to [:post "/contacts/new"]
              (contact-form contact)
              [:a {:href "/contacts"} "Back"]))

(defn contacts-archive
  [archiver]
  [:div {:id "archive-ui" :hx-target "this" :hx-swap "outerHTML" :style "margin-bottom: 0.5em"}
   (if (= (get archiver :status :waiting) :running)
     "Running..."
     [:button {:hx-post "/contacts/archive"}
      "Download Contacts Archive"])])

(defn contacts-page-body
  ([search-q contacts total-pages current-page]
   (contacts-page-body search-q contacts total-pages current-page nil))
  ([search-q contacts total-pages current-page archiver]
   (list (contacts-archive archiver)
         (contact-search-form search-q)
         (contacts-list contacts total-pages current-page))))

(defn contact-view
  [id {:keys [fname lname email phone] :as _contact-details}]
  (list
   [:h1 fname " " lname]
   [:p "Phone: " phone]
   [:p "Email: " email]
   [:p
    [:a {:href (format "/contacts/edit/%s" id)} "Edit"]
    " / "
    [:a {:href "/contacts"} "back"]]))

(defn contact-edit
  [id contact]
  (list
   (hf/form-to [:post (format "/contacts/edit/%s" id)]
               (contact-form contact)
               [:p [:a {:href "/contacts"} "Back"]])
   [:button {:id "delete-btn"
             :hx-delete (format "/contacts/delete/%s" id)
             :hx-target "body"
             :hx-push-url "true"
             :hx-confirm "Delete for sure?!"} "Delete Contact"]))

(comment
  (contact-view "2" study-htmx.web-one-app/contacts-db)

  #_())
