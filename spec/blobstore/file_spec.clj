(ns blobstore.file-spec
  (:require [blobstore.api :refer :all]
            [blobstore.file]
            [clojure.java.io :refer [file]]
            [speclj.core :refer :all]))

(def root (str (System/getProperty "java.io.tmpdir") "blobstore"))
;(println "root: " root)

(defn with-file-blobstore []
  (around [it]
    (binding [*blobstore* (new-blobstore {:implementation "file" :root root})]
      (it))))

(describe "File"

  (it "creation"
    (let [store (new-blobstore {:implementation "file" :root root})]
      (should= "blobstore.file.FileBlobstore" (.getName (class store)))
      (should= root (.getAbsolutePath (.root store)))))

  (context "with store"

    (with-file-blobstore)
    (after
      (doseq [f (seq (.listFiles (file root)))]
        (.delete f)))

    (it "stores a blob with a key"
      (let [result (store-blob "foobar" :key "abc123")]
        (should= "abc123" (:key result))))

    (it "returns nil for missing blob"
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
        (should= nil (get-blob "abc123"))
        (should= [] (list-blobs))))
    )
  )

(run-specs)