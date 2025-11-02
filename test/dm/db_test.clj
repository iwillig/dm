(ns dm.db-test
  (:require [clojure.test :as t]
            [dm.db :as dm.db]
            [matcher-combinators.test]))

(t/deftest test-db
  (t/testing ""
    (t/is (match? nil {}))))

(t/deftest test-temp-id
  (t/testing "Given: no args"
    (t/testing "When: We attempt to get the next temp-id"
      (t/is (match? neg-int? (dm.db/next-temp-id))
            "Then: The fn should return a neg-int"))))
