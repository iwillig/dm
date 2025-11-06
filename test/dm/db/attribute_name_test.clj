(ns dm.db.attribute-name-test
  (:require [clojure.test :as t]
            [dm.db.attribute-name :as attr-name]
            [next.jdbc :as jdbc]
            [matcher-combinators.test]))

(def test-db-spec "jdbc:sqlite:test-attribute-name.db")

(defn test-datasource []
  (jdbc/get-datasource test-db-spec))

(defn setup-test-db! [db]
  (try (jdbc/execute! db ["DROP TABLE IF EXISTS attribute_names"]) (catch Exception _))
  
  (jdbc/execute! db ["CREATE TABLE attribute_names (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    abbreviation TEXT,
    description TEXT,
    display_order INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  )"]))

(t/deftest test-attribute-name-get-all
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an empty attribute_names table"
      (t/testing "When: we get all attribute names"
        (let [result (attr-name/get-all db {})]
          (t/testing "Then: it returns an empty collection"
            (t/is (empty? result))))))
    
    (t/testing "Given: a table with multiple attributes"
      (attr-name/insert db {:code "strength" :name "Strength" :abbreviation "STR" :display_order 1})
      (attr-name/insert db {:code "dexterity" :name "Dexterity" :abbreviation "DEX" :display_order 2})
      
      (t/testing "When: we get all attributes"
        (let [result (attr-name/get-all db {})]
          (t/testing "Then: it returns all attributes ordered by display_order"
            (t/is (= 2 (count result)))
            (t/is (match? [{:name "Strength"} {:name "Dexterity"}] result))))))))

(t/deftest test-attribute-name-get-by-code
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an attribute exists in the database"
      (attr-name/insert db {:code "strength"
                            :name "Strength"
                            :abbreviation "STR"
                            :description "Physical power"
                            :display_order 1})
      
      (t/testing "When: we get the attribute by code"
        (let [result (attr-name/get-by-code db {:code "strength"})]
          (t/testing "Then: it returns the attribute"
            (t/is (match? {:code "strength"
                           :name "Strength"
                           :abbreviation "STR"}
                          result))))))
    
    (t/testing "Given: an attribute does not exist"
      (t/testing "When: we try to get it by code"
        (let [result (attr-name/get-by-code db {:code "nonexistent"})]
          (t/testing "Then: it returns nil"
            (t/is (nil? result))))))))

(t/deftest test-attribute-name-insert
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: valid attribute data"
      (t/testing "When: we insert an attribute"
        (attr-name/insert db {:code "wisdom"
                              :name "Wisdom"
                              :abbreviation "WIS"
                              :description "Perception and insight"
                              :display_order 5})
        
        (t/testing "Then: the attribute is persisted"
          (let [result (attr-name/get-by-code db {:code "wisdom"})]
            (t/is (match? {:code "wisdom"
                           :name "Wisdom"
                           :display_order 5}
                          result))))))))

(t/deftest test-attribute-name-update
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an existing attribute"
      (attr-name/insert db {:code "strength" :name "Strength" :abbreviation "STR" :display_order 1})
      
      (t/testing "When: we update the attribute"
        (attr-name/update-attribute-name db {:code "strength"
                                             :name "Mighty Strength"
                                             :description "Overwhelming physical power"})
        
        (t/testing "Then: the changes are persisted"
          (let [result (attr-name/get-by-code db {:code "strength"})]
            (t/is (match? {:code "strength"
                           :name "Mighty Strength"
                           :description "Overwhelming physical power"}
                          result))))))))

(t/deftest test-attribute-name-delete
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an existing attribute"
      (attr-name/insert db {:code "strength" :name "Strength" :abbreviation "STR" :display_order 1})
      
      (t/testing "When: we delete the attribute"
        (attr-name/delete db {:code "strength"})
        
        (t/testing "Then: the attribute no longer exists"
          (let [result (attr-name/get-by-code db {:code "strength"})]
            (t/is (nil? result))))))))

(t/deftest test-attribute-name-malli-validation
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: invalid data - missing required field 'name'"
      (t/testing "When: we try to insert"
        (t/testing "Then: it throws a validation error"
          (t/is (thrown? Exception
                         (attr-name/insert db {:code "strength"}))))))
    
    (t/testing "Given: invalid params - missing 'code' key"
      (t/testing "When: we try to get by code"
        (t/testing "Then: it throws a validation error"
          (t/is (thrown? Exception
                         (attr-name/get-by-code db {:wrong-key "strength"}))))))))
