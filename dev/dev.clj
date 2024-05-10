(ns dev
  (:require [study-htmx.study-htmx :as study]))

(defn restart-server!
  []
  (study/stop-server! study/system)
  (study/start-server! study/system))

(comment
  (add-libs '{http-kit/http-kit {:mvn/version "2.5.1"}}))
