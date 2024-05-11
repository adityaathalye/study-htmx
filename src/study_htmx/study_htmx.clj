(ns study-htmx.study-htmx
  (:gen-class)
  (:require
   [study-htmx.response :as shr]
   [io.pedestal.http :as http]
   [io.pedestal.http.content-negotiation :as content-negotiation]
   [io.pedestal.http.route :as route]
   [io.pedestal.interceptor :as interceptor]
   [io.pedestal.http.body-params :as body-params]
   [io.pedestal.http.params :as params]
   [study-htmx.web-one-app :as web-one]
   [study-htmx.templates :as sht]))

(defn make-system
  [{:keys [host port object]
    :or {host "localhost" port 8081 object nil}
    :as server-config}
   {:keys [dbname dbtype writer-conn reader-conn]
    :or {dbname "data/example.sqlite3" dbtype "sqlite3" writer-conn nil reader-conn nil}
    :as db-config}]
  {::server (assoc server-config
                   :host host
                   :port port
                   :object object)
   ::db (assoc db-config
               :name dbname
               :type dbtype
               :writer-conn writer-conn
               :reader-conn reader-conn)})

(defonce system
  (atom (make-system {} {})))

(def supported-types
  ["application/json" "text/html"])

(def content-negotiation-interceptor
  (content-negotiation/negotiate-content supported-types))

(defn greet
  "Callable entry point to the application."
  [context]
  (->> [:h1 "Hello, World!"]
       sht/layout
       shr/ok
       (assoc context :response)))

(def greet-handler
  (interceptor/interceptor
   {:name ::greet
    :enter greet}))

(defn redirect-root-handler
  [path]
  {:name ::root
   :enter (fn [context]
            (assoc context :response
                   (shr/see-other path (:headers context))))})

(def routes
  (route/routes-from
   #{["/"
      :get (redirect-root-handler "/contacts")
      :route-name ::root]
     ["/greet"
      :get greet-handler
      :route-name ::greet]
     ["/contacts"
      :get web-one/search-contacts-handler
      :route-name ::web-one/search-contacts]
     ["/contacts/new"
      :get web-one/new-contact-page-handler
      :route-name ::web-one/new-contact-page]
     ["/contacts/new"
      :post [(body-params/body-params)
             params/keyword-params
             web-one/new-contact-add-handler]
      :route-name ::web-one/new-contact-add]
     ["/contacts/:id/view"
      :get [(body-params/body-params)
            web-one/view-contact-handler]
      :route-name ::web-one/view-contact-page]}))

(defn create-server
  [system-map]
  (let [service-map {::http/routes routes
                     ::http/type :jetty
                     ::http/join? false
                     ::http/port (-> system-map ::server :port)
                     ::http/resource-path "public"
                     ::http/secure-headers {:content-security-policy-settings
                                            {:object-src "none"}}}]
    (-> service-map
        http/default-interceptors
        (update ::http/interceptors concat
                [content-negotiation-interceptor])
        http/create-server)))

(defn start-server!
  [system-atom]
  (let [server-obj (-> @system-atom create-server http/start)]
    (swap! system-atom assoc-in [::server :object]
           server-obj)))

(defn stop-server!
  [system-atom]
  (when-let [server-obj (-> @system-atom ::server :object)]
    (http/stop server-obj))
  (swap! system-atom assoc-in [::server :object] nil))

(defn -main
  "I don't do a whole lot ... yet."
  []
  (let [live-system (-> system
                        start-server!)]))

(comment
  (start-server! system)

  (defn restart-server!
    []
    (stop-server! system)
    (start-server! system))

  (restart-server!)
  #_())
