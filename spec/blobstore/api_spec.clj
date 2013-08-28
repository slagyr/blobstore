(ns blobstore.api-spec
  (:require [speclj.core :refer :all]
            [blobstore.abstr :as abstr]
            [blobstore.api :refer :all]
            [blobstore.fake :refer [new-fake-blobstore]]
            [blobstore.memory :refer [new-memory-blobstore]]))


(defn with-memory-blobstore []
  (around [it]
    (binding [*blobstore* (new-memory-blobstore)]
      (it))))

(describe "blobstore api"

  (context "creation"

    (it "memory"
      (let [store (new-blobstore {:implementation "memory"})]
        (should= "blobstore.memory.MemoryBlobstore" (.getName (class store)))))
    )

  (it "has no blobstore by default"
    (set-blobstore! nil)
    (should-throw Exception "No Blobstore bound (blobstore/*blobstore*). Use clojure.core/binding to bind a value or blobstore.api/set-blobstore! to globally set it."
      (blobstore)))

  (it "the blobstore can be bound"
    (let [fake-blobstore (new-fake-blobstore)]
      (binding [*blobstore* fake-blobstore]
        (should= fake-blobstore (blobstore)))))

  (it "the blobstore can be installed"
    (let [fake-blobstore (new-fake-blobstore)]
      (try
        (set-blobstore! fake-blobstore)
        (should= fake-blobstore (blobstore))
        (finally (set-blobstore! nil)))))

  (context "with store"

    (with-memory-blobstore)

    (it "stores a blob with a key"
      (let [result (store-blob "foobar" :key "abc123")]
        (should= "abc123" (:key result))))

    (it "generate key for blob missing key"
      (let [result (store-blob "foobar")]
        (should-not= nil (:key result))))

    (it "returns nill for missing blob"
      (should= nil (get-blob "missing")))

    (it "gets a stored blob"
      (store-blob "foobar" :key "abc123")
      (let [result (get-blob "abc123")]
        (should= "abc123" (:key result))
        (should= "foobar" (slurp (:blob result)))))

    (it "gets metadata stored with the blob"
      (let [saved (store-blob "foobar" "foo" "bar" "fizz" "bang")
            loaded (get-blob (:key saved))]
        (should= "bar" (get saved :foo))
        (should= "bar" (get loaded :foo))
        (should= "bang" (get saved :fizz))
        (should= "bang" (get loaded :fizz))))

    (it "lists blobs"
      (store-blob "foobar" :key "abc123")
      (store-blob "fizzbang" :key "xyz789")
      (let [result (sort-by :key (list-blobs))]
        (should= "abc123" (:key (first result)))
        (should= "xyz789" (:key (second result)))))

    (it "deleteing a blob"
      (store-blob "foobar" :key "abc123")
      (let [result (delete-blob "abc123")]
        (should= "abc123" (:key result))
        (should= nil (get-blob "abc123"))
        (should= [] (list-blobs))))

    (it "generates a url"
      (should= (abstr/-blob-url *blobstore* "abc123" {}) (blob-url "abc123")))

    )
  )
