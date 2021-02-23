(ns mescal.util-test
  (:require [clojure.test :refer :all]
            [mescal.util :refer [encode-path to-millis to-utc]]))

(deftest encode-test
  (testing "No encoding necessary"
    (is (= "foo/bar/baz" (encode-path "/iplant/home" "/iplant/home/foo/bar/baz"))))
  (testing "Encoding spaces"
    (is (= "foo/bar%20baz" (encode-path "/iplant/home" "/iplant/home/foo/bar baz")))
    (is (= "foo%20quux/bar%20baz" (encode-path "/iplant/home" "/iplant/home/foo quux/bar baz"))))
  (testing "Encoding other characters"
    (is (= "foo/bar%3Abaz" (encode-path "/iplant/home" "/iplant/home/foo/bar:baz")))))

(deftest timestamp-test
  (testing "to-utc for a timestamp without milliseconds"
    (is (= "2021-02-23T21:50:33+00:00" (to-utc "2021-02-23T21:50:33Z")))
    (is (= "2021-02-23T22:50:33+00:00" (to-utc "2021-02-23T21:50:33-01:00"))))
  (testing "to-utic for a timestamp with milliseconds"
    (is (= "2021-02-23T21:50:33+00:00" (to-utc "2021-02-23T21:50:33.123Z")))
    (is (= "2021-02-23T22:50:33+00:00" (to-utc "2021-02-23T21:50:33.123-01:00"))))
  (testing "to-millis for a timestamp without milliseconds"
    (is (= 1614117033000 (to-millis "2021-02-23T21:50:33Z")))
    (is (= 1614120633000 (to-millis "2021-02-23T21:50:33-01:00"))))
  (testing "to-millis for a timestamp with milliseconds"
    (is (= 1614117033123 (to-millis "2021-02-23T21:50:33.123Z")))
    (is (= 1614120633123 (to-millis "2021-02-23T21:50:33.123-01:00")))))
