(ns monkey.ci.pushover-notifier.build
  (:require [monkey.ci.build.core :as bc]
            [monkey.ci.plugin
             [clj :as clj]
             [kaniko :as kaniko]]))

(def test (clj/deps-test {}))

;; Build image if running from main branch, or from a tag
(def build-image? (some-fn bc/tag bc/main-branch?))

(def uberjar-artifact
  {:id "uberjar"
   :path "target/pushover-notifier.jar"})

(defn uberjar [ctx]
  (when (build-image? ctx)
    (-> (clj/clj-deps "uberjar" {} "-X:jar:uber")
        (assoc :save-artifacts [uberjar-artifact]
               :dependencies ["test"]))))

(defn image [ctx]
  (when (build-image? ctx)
    (let [version (or (bc/tag ctx) "latest")]
      (-> (kaniko/image {:target-img (str "fra.ocir.io/frjdhmocn5qi/pushover-notifier:" version)}
                        ctx)
          (assoc :restore-artifacts [uberjar-artifact]
                 :dependencies ["uberjar"])))))

[test
 uberjar
 image]
