(ns blobstore.s3
  (:require [aws.sdk.s3 :as s3]
            [blobstore.abstr]
            [chee.util :refer [->options]]
            [clojure.java.io :refer [copy]]
            [hyperion.log :as log]))

(defn store-blob [creds bucket blob options]
  (s3/put-object creds bucket (:key options) blob (dissoc options :key)))

(defn listing [creds bucket]
  (let [result (s3/list-objects creds bucket)]
    (map
      (fn [b]
        (assoc (:metadata b) :key (:key b) :bucket (:bucket b)))
      (:objects result))))

(defn get-blob [creds bucket key]
  (try
    (when-let [blob (s3/get-object creds bucket key)]
      (merge-with #(or %1 %2)
        (:user (:metadata blob))
        (:metadata blob)
        {:key key :bucket bucket :blob (:content blob)}))
    (catch com.amazonaws.services.s3.model.AmazonS3Exception e
      (when-not (= "NoSuchKey" (.getErrorCode e))
        (throw (ex-info "AWS Problem" {:key key :bucket bucket} e))))))

(defn url-for [creds bucket key options]
  (format "https://s3.amazonaws.com/%s/%s" bucket key))

(deftype S3Blobstore [creds bucket]
  blobstore.abstr.Blobstore
  (-store-blob [this blob options] (store-blob creds bucket blob options))
  (-get-blob [this key] (get-blob creds bucket key))
  (-delete-blob [this key] (s3/delete-object creds bucket key))
  (-list-blobs [this] (listing creds bucket))
  (-blob-url [this key options] (url-for creds bucket key options)))

(defn new-s3-blobstore [& args]
  (let [options (->options args)
        creds {:secret-key (:secret-key options) :access-key (:access-key options)}
        bucket (:bucket options)
        store (S3Blobstore. creds bucket)]
    (when (and (not (:skip-bucket-check options)) (not (s3/bucket-exists? creds bucket)))
      (do
        (log/info (format "Bucket '%s' missing.  Creating it." bucket))
        (s3/create-bucket creds bucket)))
    store))

