(ns blobstore.spec-helper
  (:require [speclj.core :refer :all]
            [blobstore.api :refer [*blobstore* new-blobstore]]))

(defn with-memory-blobstore []
  (around [it]
    (binding [*blobstore* (new-blobstore {:implementaiton :memory})]
      (it))))