(ns study-htmx.templates)

(defn layout
  [body]
  (list
   [:head
    [:title "Contacts App"]]
   [:body body]))
