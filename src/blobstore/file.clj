(ns blobstore.file
  (:require [blobstore.abstr]
            [chee.util :refer [->options]]
            [clojure.java.io :refer [copy reader output-stream file input-stream]]
            [hyperion.log :as log]))

(defn- read-index [root]
  (when (.exists (file (str root ".index")))
    (read (java.io.PushbackReader. (reader (str root ".index"))))))

(defn- write-index [root index]
  (spit (str root ".index") (pr-str index)))

(defn store-blob [root blob options]
  (let [index (read-index root)]
    (with-open [output (output-stream (str root (:key options)))]
      (copy blob output))
    (write-index root (assoc index (:key options) options))))

(defn get-blob [root key]
  (let [index (read-index root)]
    (when-let [meta (get index key)]
      (assoc meta :blob (input-stream (str root key))))))

(defn listing [root]
  (or (vals (read-index root)) []))

(defn delete-blob [root key]
  (let [file (file (str root key))]
    (when (.exists file)
      (.delete file)
      (write-index root (dissoc (read-index root) key)))))

(deftype FileBlobstore [root]
  blobstore.abstr.Blobstore
  (-store-blob [this blob options] (store-blob root blob options))
  (-get-blob [this key] (get-blob root key))
  (-delete-blob [this key] (delete-blob root key))
  (-list-blobs [this] (listing root)))

(defn new-file-blobstore [& args]
  (let [options (->options args)
        root (:root options)
        store (FileBlobstore. root)]
    (when-not (.exists (file root))
      (log/info "Root doesn't exist.  Creating: " root)
      (.mkdirs (file root)))
    store))

