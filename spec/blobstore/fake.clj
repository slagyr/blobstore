(ns blobstore.fake
  (:require [blobstore.abstr]))

(defn- stub-call [ds name & params]
  (swap! (.calls ds) conj [name params])
  (let [result (first @(.responses ds))]
    (swap! (.responses ds) rest)
    result))

(deftype FakeBlobstore [calls responses]
  blobstore.abstr.Blobstore
;  (ds-save [this records] (stub-call this "ds-save" records))
  )

(defn new-fake-blobstore []
  (FakeBlobstore. (atom []) (atom [])))