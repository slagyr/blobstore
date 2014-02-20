(defproject com.slagyr/blobstore "1.0.0"
  :description "API for storing blobs of data"
  :url ""
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[clj-aws-s3 "0.3.7"]
                 [com.taoensso/timbre "3.0.1"]
                 [org.clojure/clojure "1.5.0"]]
  :profiles {:dev {:dependencies [[speclj "2.7.4"]]}}
  :plugins [[speclj "2.7.4"]]
  :test-paths ["spec"])
