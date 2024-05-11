(ns study-htmx.response
  (:require [hiccup.page :as hp]))

(defn response
  ([_status])
  ([status body & {:as headers}]
   (merge
    {:status status
     :headers (assoc headers "Content-Type" "text/html")}
    (when body {:body (hp/html5 body)}))))

(defn ok
  [body & {:as headers}]
  (response 200 body headers))

(defn not-found
  [body & {:as headers}]
  (response 404 body headers))

(defn see-other
  [url & {:as headers}]
  {:status 303
   :headers (assoc headers "Location" url)
   :body ""})
