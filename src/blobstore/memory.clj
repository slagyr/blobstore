(ns blobstore.memory
  (:require [chee.util :refer [->options]]
            [clojure.java.io :refer [copy]]
            [clojure.string :as str]
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

(defn url-for [config key options]
  (let [url-pattern (or (:url-pattern config) "/memory-blob/%s")
        params (map (fn [[k v]] (str (java.net.URLEncoder/encode (name k) "UTF-8") "=" (java.net.URLEncoder/encode (str v) "UTF-8"))) options)
        query-string (str/join "&" params)]
    (str (format url-pattern key) (if (seq params) "?" "") query-string)))

(deftype MemoryBlobstore [store config]
  blobstore.abstr.Blobstore
  (-store-blob [this blob options] (swap! store put-blob blob options))
  (-get-blob [this key] (find-blob @store key))
  (-delete-blob [this key] (delete-blob store key))
  (-list-blobs [this] (listing @store))
  (-blob-url [this key options] (url-for config key options))
  )

(defn new-memory-blobstore [& args]
  (let [options (->options args)]
    (MemoryBlobstore. (atom {}) options)))
