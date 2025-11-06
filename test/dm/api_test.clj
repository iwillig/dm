(ns dm.api-test
  (:require
   [clojure.test :as t]
   [matcher-combinators.test]))

(t/deftest test-okay-api
  (t/testing ""
    (t/is (match? true true))))
