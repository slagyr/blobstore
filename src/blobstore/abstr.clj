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

(defn ->options
  "Takes keyword argument and converts them to a map.  If the args are prefixed with a map, the rest of the
  args are merged in."
  [options]
  (if (map? (first options))
    (merge (first options) (apply hash-map (rest options)))
    (apply hash-map options)))

