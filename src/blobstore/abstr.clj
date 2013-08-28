(ns blobstore.abstr
;  (:require [])
  )

(defprotocol Blobstore
  (-store-blob [this blob options])
  (-get-blob [this key])
  (-delete-blob [this key])
  (-list-blobs [this])
  (-blob-url [this key options])
  )

