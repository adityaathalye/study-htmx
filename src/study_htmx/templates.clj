(ns study-htmx.templates
  (:require [hiccup.page :as hp]
            [hiccup.form :as hf]))

(defn layout
  [body]
  (list
   [:head
    [:title "Contacts App"]
    (hp/include-css "/css/site.css")]
   [:body body]))

(defn contact-search-form
  [search-q]
  [:form {:action "/contacts" :method "get"}
   [:label {:for "search"} "Search Term: "]
   [:input {:id "search" :type "search" :name "q" :value search-q}]
   [:input {:type "submit" :value "Search"}]])

(defn contacts-list
  [contacts]
  (list
   [:table
    [:thead
     [:tr [:td "First Name"] [:td "Last Name"] [:td "Phone"] [:td "Email"] [:td "Action"]]]
    [:tbody
     (for [[id {:keys [fname lname phone email]}] contacts]
       [:tr
        [:td fname] [:td lname] [:td phone] [:td email]
        [:td
         [:a {:href (format "/contacts/edit/%s" id)} "Edit"] " / "
         [:a {:href (format "/contacts/view/%s" id)} "View"]]])]]
   [:p [:a {:href "/contacts/new"} [:strong "Add New Contact"]]]))

(defn contact-form
  [{:keys [fname lname phone email show-error-set]
    :or {show-error-set #{}}
    :as _contact}]
  [:fieldset
   [:legend "Contact Values"]
   [:p
    (hf/label "email" "Email: ")
    (hf/email-field "email" email)
    (when (show-error-set :email)
      [:span {:class "error"} "Bad email address."])]
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

(defn contacts-page-body
  [search-q contacts]
  (list (contact-search-form search-q)
        (contacts-list contacts)))

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
  (hf/form-to [:post (format "/contacts/edit/%s" id)]
              (contact-form contact)
              [:p
               [:a {:href "/contacts"} "Back"] " / "
               [:a {:href (format "/contacts/delete/%s" id)} "Delete"]]))

(comment
  (contact-view "2" study-htmx.web-one-app/contacts-db)

  #_())
