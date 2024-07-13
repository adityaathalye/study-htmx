(ns study-htmx.pmjc
  "Poor Man's Job Control")

(defn make-initial-job-state
  []
  (agent {:status (atom :waiting)
          :total-batches 0
          :progress 0
          :job-file "resources/job-log.json"}
         :validator (fn [job-runner]
                      (not= :paused @(:status job-runner)))))

(defonce job-runner
  (make-initial-job-state))

(defn create-job!
  "Queue all the batches for the given job and
  keep the job progress current."
  [job-runner batches batch-executor]
  ;; Start the job when it is parked in the initial
  ;; :waiting state. Also rotate the job file.
  (when (= @(:status @job-runner) :waiting)
    (swap! (:status @job-runner)
           (constantly :running))
    (send job-runner assoc
          :total-batches (count batches))
    (spit (:job-file @job-runner)
          ""))

  ;; As soon as a job is set to run, queue all batches
  ;; and progress updates
  (when (= @(:status @job-runner) :running)
    (doseq [batch batches]
      (send job-runner update :progress inc)
      (send-off job-runner batch-executor batch)))

  ;; Queue a final action to mark the job as :done
  (send-off job-runner
            (fn [runner]
              (swap! (:status runner) (constantly :done))
              runner)))

(defn pause-job!
  "Out-of-band job control by reaching into the :status atom."
  [job-runner]
  (swap! (:status @job-runner)
         (constantly :paused))
  job-runner)

(defn resume-job!
  "Out-of-band job control by reaching into the :status atom."
  [job-runner]
  (when (= :paused @(:status @job-runner))
    (swap! (:status @job-runner) (constantly :running))
    (restart-agent job-runner @job-runner))
  job-runner)

(defn reset-job!
  "Cheaping out by resetting the var because we mean to be
  destructive and consign the agent to garbage collector.
  Wrapping the agent in an atom would be better."
  [job-runner-var]
  (alter-var-root job-runner-var
                  (constantly (make-initial-job-state))))

(defn cancel-job!
  [job-runner job-runner-var]
  (pause-job! job-runner)
  (reset-job! job-runner-var))

(defn do-batch!
  "Presumably a long-running batch. We must always accept
  and return the job runner as this is an action sent off
  to the job runner agent."
  [job-runner batch]
  (Thread/sleep 5000) ; the batch is running
  (spit (:job-file job-runner)
        (format "Completed batch %s\n" batch)
        :append true)
  job-runner)

(comment
  (create-job! job-runner
               ["ONE" "TWO" "THREE" "FOUR" "FIVE"]
               do-batch!)

  (pause-job! job-runner)
  (resume-job! job-runner)
  (reset-job! (var job-runner))
  (cancel-job! job-runner (var job-runner))

  (agent-error job-runner))
