(ns blobstore.memory
  (:require [chee.util :refer [->options]]
            [clojure.java.io :refer [copy]]
            [blobstore.abstr]))

(defn- ->bytes [source]
  (let [buffer (java.io.ByteArrayOutputStream.)]
    (copy source buffer)
    (.toByteArray buffer)))

(defn- put-blob [store blob options]
  (let [key (:key options)]
    (-> store
      (assoc key (->bytes blob))
      (assoc-in [:meta key] (dissoc options :key)))))

(defn- find-blob [store key]
  (when-let [blob (get store key)]
    (assoc (get-in store [:meta key])
      :blob (java.io.ByteArrayInputStream. blob)
      :key key)))

(defn- delete-blob [store key]
  (let [meta (get-in @store [:meta key])]
    (swap! store (fn [s] (-> s
                           (dissoc key)
                           (update-in [:meta] dissoc key))))
    (assoc meta :key key)))

(defn- listing [store]
  (map
    (fn [[key meta]] (assoc meta :key key))
    (:meta store)))

(deftype MemoryBlobstore [store]
  blobstore.abstr.Blobstore
  (-store-blob [this blob options] (swap! store put-blob blob options))
  (-get-blob [this key] (find-blob @store key))
  (-delete-blob [this key] (delete-blob store key))
  (-list-blobs [this] (listing @store))
  )

(defn new-memory-blobstore [& args]
  (let [options (->options args)]
    (MemoryBlobstore. (atom {}))))
