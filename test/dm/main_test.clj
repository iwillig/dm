(ns dm.main-test
  (:require [clojure.test :as t]
            [matcher-combinators.test]))

(t/deftest test-okay
  (t/testing "changes to the api"
    (t/is (= nil #{}))))
