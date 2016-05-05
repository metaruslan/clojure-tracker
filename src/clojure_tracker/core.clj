(ns clojure-tracker.core
  (:require [clj-http.client :as client]))

(defn request-url [url param1 param2]
  (let [response-with-request (client/get url {:save-request? true ;for debug
                                               :query-params {"param1" param1 "param2" param2}
                                               :retry-handler (fn [ex try-count http-context]
                                                                (println "Got:" ex)
                                                                (if (> try-count 4) false true))})]
    ;for debug
    (println "Request: " (->
                           response-with-request
                           :request
                           :http-url)
             " Response status: " (:status response-with-request))))

(defn do-and-increment [counter f & params]
  "A helper function to invoke anything inside an agent's action queue and increment the agent's counter."
  (apply f params)
  (inc counter))

;creating a global agent to execute the async calls
;the agent state is just a number of trackings performed, starting with 0
(def tracker-agent (agent 0))

(defn track [param1 param2]
  (send-off tracker-agent #(do-and-increment % request-url "http://example.com" param1 param2)))

;for debug
(defn main []
  ;normal flow would be like this
  (println "doing something 1...")
  (track "foo1" "bar1")
  (println "doing something 2...")
  (println "doing something 3...")
  (track "foo2" "bar2")

  ;;;;;;;;;;;;;;;;;;;;;shutting down;;;;;;;;;;;;;;;;;;;;;;

  (await tracker-agent)
  ;shutting down the agents system to allow the JVM to shutdown
  (shutdown-agents)
  ;looking at the state of the agent after all the actions have completed
  (println "number of tracker-agent actions performed: " @tracker-agent)
  (println "finishing the main thread")
  )

(main)

;Sample program invocation

;doing something 1...
;doing something 2...
;doing something 3...
;Request:  http://example.com?param1=foo1&param2=bar1  Response status:  200
;Request:  http://example.com?param1=foo2&param2=bar2  Response status:  200
;number of tracker-agent actions performed:  2
;finishing the main thread
;
;Process finished with exit code 0
