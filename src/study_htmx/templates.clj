(ns study-htmx.templates
  (:require [hiccup.page :as hp]))

(defn layout
  [body]
  (list
   [:head
    (hp/include-css "css/site.css")
    [:title "Contacts App"]]
   [:body body]))

(defn contact-form
  [search-q]
  [:form {:action "/contacts" :method "get"}
   [:label {:for "search"} "Search Term: "]
   [:input {:id "search" :type "search" :name "q" :value search-q}]
   [:input {:type "submit" :value "Search"}]])

(defn contacts-list
  [contacts]
  [:table
   [:thead
    [:tr [:td "First Name"] [:td "Last Name"] [:td "Phone"] [:td "Email"] [:td "Action"]]]
   [:tbody
    (for [[id {:keys [fname lname phone email]}] contacts]
      [:tr
       [:td fname] [:td lname] [:td phone] [:td email]
       [:td
        [:a {:href (format "/contacts/%s/edit" id)} "Edit"] " / "
        [:a {:href (format "/contacts/%s/view" id)} "View"]]])]])

(defn contacts-page-body
  [search-q contacts]
  (list (contact-form search-q)
        (contacts-list contacts)))
