(ns study-htmx.pmjc)

(def initial-job-state
  (agent {:status (atom :waiting)
          :progress 0
          :job-file "resources/job-log.json"}
         :validator (fn [a]
                      (not (= @(:status a) :paused)))))

(defonce job-runner
  initial-job-state)

(defn do-batch! [job-runner batch]
  (spit (:job-file job-runner)
        (format "Completed batch %s\n" batch)
        :append true)
  (Thread/sleep 5000)
  job-runner)

(defn create-job!
  [job-runner batches batch-executor]
  (when (= @(:status @job-runner) :waiting)
    (swap! (:status @job-runner) (constantly :running))
    (spit (:job-file @job-runner) ""))

  (when (= @(:status @job-runner) :running)
    (doseq [batch batches]
      (send job-runner update :progress inc)
      (send-off job-runner batch-executor batch)))

  (send job-runner
        (fn [j] (swap! (:status j) (constantly :done)))))

(defn pause-job!
  [job-runner]
  (swap! (:status @job-runner) (constantly :paused)))

(defn resume-job!
  [job-runner]
  (swap! (:status @job-runner) :running)
  (restart-agent job-runner @job-runner))

(defn reset-job!
  [job-runner-var]
  (alter-var-root job-runner-var (constantly initial-job-state)))

(defn cancel-job!
  [job-runner job-runner-var]
  (pause-job! job-runner)
  (reset-job! job-runner-var))

(comment
  (create-job! job-runner
               ["ONE" "TWO" "THREE" "FOUR" "FIVE"]
               do-batch!)

  (pause-job! job-runner)
  (resume-job! job-runner)
  (reset-job! (var job-runner))
  (cancel-job! job-runner (var job-runner))

  (agent-error job-runner))
