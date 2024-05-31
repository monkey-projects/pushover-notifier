(ns monkey.ci.pushover.core
  "Core entrypoint namespace for the MonkeyCI pushover notifier"
  (:gen-class)
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [monkey.jms :as jms]
            [monkey.pushover.core :as p]
            [taoensso.telemere :as t])
  (:import [java.io PushbackReader StringReader]))

(defn- parse-edn [in]
  (with-open [r (PushbackReader. in)]
    (edn/read r)))

(defn- parse-edn-str [s]
  (parse-edn (StringReader. s)))

(defn load-config
  "Loads configuration from given edn file"
  [f]
  (parse-edn (io/reader f)))

(defn connect-broker [conf]
  (t/log! {:level :info :data (select-keys conf [:url :username])} "Connecting to broker")
  (jms/connect conf))

(def poll-timeout 1000) ; msecs

(defn- dispatch-evt? [evt]
  ;; Only dispatch build end events
  (= :build/end (:type evt)))

(defn evt->message [evt]
  (format "Build completed: %s" (get-in evt [:build :build-id])))

(defn make-poster
  "Creates a poster fn for pushover"
  [conf]
  (let [client (p/make-client conf)]
    (fn [msg]
      (p/post-message client (-> (select-keys conf [:token :user])
                                 (assoc :message msg))))))

(defn read-bytes [^jakarta.jms.BytesMessage msg]
  (let [buf (byte-array (.getBodyLength msg))]
    (.readBytes msg buf)
    (String. buf)))

(defn run
  "Runs the application loop with given configuration"
  [conf]
  (let [poster (make-poster (:pushover conf))]
    (with-open [broker (connect-broker (:events conf))
                consumer (jms/consume broker (get-in conf [:events :topic])
                                      {:deserializer read-bytes})]
      ;; Start consuming events
      (loop [msg (consumer)]
        (when-let [evt (some-> msg (parse-edn-str))]
          (t/log! {:level :debug :data evt} "Received next message")
          ;; Post a message to pushover whenever a build completes
          (when (dispatch-evt? evt)
            (poster (evt->message evt)))
          (recur (consumer)))))))

(defn -main [& args]
  (let [config-file (first args)
        conf (load-config config-file)]
    (t/log! {:level :debug :data conf} "Config loaded")
    (run conf)))
