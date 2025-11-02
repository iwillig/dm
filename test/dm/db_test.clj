(ns dm.db-test
  (:require [clojure.test :as t]
            [matcher-combinators.test]))

(t/deftest test-db
  (t/testing ""
    (t/is (match? nil {}))))
