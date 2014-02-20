(ns blobstore.file
  (:require [blobstore.abstr :refer [->options]]
            [clojure.java.io :refer [copy reader output-stream file input-stream]]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

(defn- read-index [root]
  (when (.exists (file root ".index"))
    (read (java.io.PushbackReader. (reader (file root ".index"))))))

(defn- write-index [root index]
  (spit (file root ".index") (pr-str index)))

(defn store-blob [root blob options]
  (let [index (read-index root)]
    (with-open [output (output-stream (file root (:key options)))]
      (copy blob output))
    (write-index root (assoc index (:key options) options))))

(defn get-blob [root key]
  (let [index (read-index root)]
    (when-let [meta (get index key)]
      (assoc meta :blob (input-stream (file root key))))))

(defn listing [root]
  (or (vals (read-index root)) []))

(defn delete-blob [root key]
  (let [file (file root key)]
    (when (.exists file)
      (.delete file)
      (write-index root (dissoc (read-index root) key)))))

(defn url-for [config key options]
  (let [url-pattern (or (:url-pattern config) "/file-blob/%s")
        params (map (fn [[k v]] (str (java.net.URLEncoder/encode (name k) "UTF-8") "=" (java.net.URLEncoder/encode (str v) "UTF-8"))) options)
        query-string (str/join "&" params)]
    (str (format url-pattern key) (if (seq params) "?" "") query-string)))

(deftype FileBlobstore [root config]
  blobstore.abstr.Blobstore
  (-store-blob [this blob options] (store-blob root blob options))
  (-get-blob [this key] (get-blob root key))
  (-delete-blob [this key] (delete-blob root key))
  (-list-blobs [this] (listing root))
  (-blob-url [this key options] (url-for config key options)))

(defn new-file-blobstore [& args]
  (let [options (->options args)
        root (file (:root options))
        store (FileBlobstore. root options)]
    (when-not (.exists root)
      (log/info "Root doesn't exist.  Creating: " root)
      (.mkdirs root))
    store))

