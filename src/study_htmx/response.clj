(ns study-htmx.response
  (:require [hiccup2.core :as h]))

(defn response
  ([_status])
  ([status body & {:as headers}]
   (merge
    {:status status
     :headers (assoc headers "Content-Type" "text/html")}
    (when body {:body (-> body h/html str)}))))

(defn ok
  [body & {:as headers}]
  (response 200 body headers))

(defn not-found
  [body & {:as headers}]
  (response 404 body headers))

(defn redirect
  [url & {:as headers}]
  {:status 302
   :headers (assoc headers "Location" url)
   :body ""})
