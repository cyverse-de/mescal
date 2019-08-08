(ns mescal.util-test
  (:require [clojure.test :refer :all]
            [mescal.util :refer [encode-path]]))

(deftest encode-test
  (testing "No encoding necessary"
    (is (= "foo/bar/baz" (encode-path "/iplant/home" "/iplant/home/foo/bar/baz"))))
  (testing "Encoding spaces"
    (is (= "foo/bar%20baz" (encode-path "/iplant/home" "/iplant/home/foo/bar baz")))
    (is (= "foo%20quux/bar%20baz" (encode-path "/iplant/home" "/iplant/home/foo quux/bar baz"))))
  (testing "Encoding other characters"
    (is (= "foo/bar%3Abaz" (encode-path "/iplant/home" "/iplant/home/foo/bar:baz")))))
