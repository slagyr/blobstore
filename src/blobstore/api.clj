(ns blobstore.api
  (:require [clojure.walk :refer [keywordize-keys]]
            [chee.util :refer [->options]]
            [hyperion.log :as log]
            [hyperion.key :refer [generate-id]]
            [blobstore.abstr :refer :all]))

(declare ^{:dynamic true
           :tag blobstore.abstr.Blobstore
           :doc "Stores the active datastore."} *blobstore*)

(defn set-blobstore!
  "Uses alter-var-root to set *blobstore*. A violent, but effective way to install a datastore."
  [^blobstore.abstr.Blobstore blobstore]
  (log/debug "focefully setting datastore:" blobstore)
  (alter-var-root (var *blobstore*) (fn [_] blobstore)))

(defn blobstore
  "Returns the currently bound datastore instance"
  []
  (if (and (bound? #'*blobstore*) *blobstore*)
    *blobstore*
    (throw (NullPointerException. "No Blobstore bound (blobstore/*blobstore*). Use clojure.core/binding to bind a value or blobstore.api/set-blobstore! to globally set it."))))

(defn new-blobstore [& args]
  (let [options (->options args)]
    (if-let [implementation (:implementation options)]
      (try
        (let [ns-sym (symbol (str "blobstore." (name implementation)))]
          (require ns-sym)
          (let [constructor-sym (symbol (format "new-%s-blobstore" (name implementation)))
                constructor (ns-resolve (the-ns ns-sym) constructor-sym)
                blobstore (constructor options)]
            (log/debug "new-blobstore.  config:" options "blobstore:" blobstore)
            blobstore))
        (catch java.io.FileNotFoundException e
          (throw (Exception. (str "Can't find blobstore implementation: " implementation) e))))
      (throw (Exception. "new-blobstore requires an :implementation entry (:memory, :file, :s3, ...)")))))

(defn store-blob [blob & args]
  (let [options (keywordize-keys (->options args))
        options (assoc options :key (or (:key options) (generate-id)))]
    (-store-blob (blobstore) blob options)
    options))

(defn get-blob [key] (-get-blob (blobstore) key))
(defn delete-blob [key] (-delete-blob (blobstore) key))
(defn list-blobs [] (-list-blobs (blobstore)))