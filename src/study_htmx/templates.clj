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

(defn contact-form
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
         [:a {:href (format "/contacts/%s/edit" id)} "Edit"] " / "
         [:a {:href (format "/contacts/%s/view" id)} "View"]]])]]
   [:p [:a {:href "/contacts/new"} [:strong "Add New Contact"]]]))

(defn contact-new
  [{:keys [fname lname phone email show-error-set]
    :or {show-error-set #{}}
    :as _contact}]
  (hf/form-to [:post "/contacts/new"]
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
               (hf/submit-button "Submit")]
              [:a {:href "/contacts"} "Back"]))

(defn contacts-page-body
  [search-q contacts]
  (list (contact-form search-q)
        (contacts-list contacts)))

(defn contact-view
  [id {:keys [fname lname email phone] :as _contact-details}]
  (list
   [:h1 fname " " lname]
   [:p "Phone: " phone]
   [:p "Email: " email]
   [:p
    [:a {:href (format "/contacts/%s/edit" id)} "Edit"]
    " / "
    [:a {:href "/contacts"} "back"]]))

(comment
  (contact-view "2" study-htmx.web-one-app/contacts-db))
