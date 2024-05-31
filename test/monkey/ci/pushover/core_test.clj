(ns monkey.ci.pushover.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [clojure.string :as cs]
            [monkey.ci.pushover.core :as sut]
            [monkey.jms :as jms]
            [monkey.pushover.core :as p]))

(defrecord TestBroker []
  java.lang.AutoCloseable
  (close [_]
    nil))

(defrecord TestConsumer [messages]
  clojure.lang.IFn
  (invoke [_]
    (when-let [v (first @messages)]
      (swap! messages rest)
      v))
  java.lang.AutoCloseable
  (close [_]
    nil))

(defn- test-consumer [messages]
  (->TestConsumer (atom messages)))

(deftest run
  (testing "connects to broker and polls for messages"
    (let [posted (atom [])]
      ;; Just fake external calls
      (with-redefs [jms/connect (constantly (->TestBroker))
                    jms/consume (constantly (test-consumer [(pr-str {:type :build/end
                                                                     :build
                                                                     {:build-id "test-build"}})]))
                    p/post-message (fn [_ msg]
                                     (swap! posted conj msg))]
        (is (nil? (sut/run {:events {:url "tcp://test"}})))
        (is (= 1 (count @posted)))
        (is (cs/includes? (-> @posted
                              first
                              :message)
                          "test-build"))))))
