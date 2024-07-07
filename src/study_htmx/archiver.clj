(ns study-htmx.archiver
  (:require
   [clojure.data.json :as json]))

(defn make-archiver
  []
  (agent {:status (atom :waiting)
          :total-contacts 0
          :progress 0
          :archive-file nil}
         :validator (fn [a]
                      (not (#{:cancelled :paused} @(:status a))))))

(defonce archiver
  (make-archiver))

(defn do-archive!
  [contacts total-contacts archiver]
  (when (= @(:status @archiver) :waiting)
    (let [file-path "resources/archive.json" #_(format "resources/%s.json" (random-uuid))]
      (swap! (:status @archiver) (constantly :running))
      (spit file-path "[")
      (send archiver assoc
            :total-contacts total-contacts
            :archive-file file-path)))

  (when (= @(:status @archiver) :running)
    (doseq [contact contacts]
      (send archiver update :progress inc)
      (send-off archiver
                (fn write-archive! [arch]
                  (spit (:archive-file arch)
                        (str (json/write-str contact)
                             (if (= (:progress arch) total-contacts) "]" ","))
                        :append true)
                  (Thread/sleep 5000)
                  arch))))
  (send-off archiver (fn [a]
                       (swap! (:status a) (constantly :done))
                       a))
  archiver)

(defn pause-archive!
  [archiver]
  (swap! (:status @archiver) (constantly :paused))
  archiver)

(defn resume-archive!
  [archiver]
  (swap! (:status @archiver) (constantly :running))
  (restart-agent archiver
                 @archiver)
  archiver)

(defn reset-archive!
  [archiver]
  (alter-var-root #'archiver (constantly (make-archiver))))

(defn cancel-archive!
  [archiver]
  (pause-archive! archiver)
  (reset-archive! archiver))

(comment
  (require 'study-htmx.web-one-app)
  (do-archive! (into [] @study-htmx.web-one-app/contacts-db)
               (count @study-htmx.web-one-app/contacts-db)
               archiver)

  (pause-archive! archiver)
  (resume-archive! archiver)
  (reset-archive! archiver)
  (cancel-archive! archiver)

  (agent-error archiver))
