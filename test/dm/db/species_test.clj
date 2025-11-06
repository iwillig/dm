(ns dm.db.species-test
  (:require [clojure.test :as t]
            [dm.db.species :as species]
            [next.jdbc :as jdbc]
            [matcher-combinators.test]))

(def test-db-spec "jdbc:sqlite:test-species.db")

(defn test-datasource []
  (jdbc/get-datasource test-db-spec))

(defn setup-test-db! [db]
  (try (jdbc/execute! db ["DROP TABLE IF EXISTS species"]) (catch Exception _))
  
  (jdbc/execute! db ["CREATE TABLE species (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    size TEXT,
    speed INTEGER,
    special_traits TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  )"]))

(t/deftest test-species-get-all
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an empty species table"
      (t/testing "When: we get all species"
        (let [result (species/get-all db {})]
          (t/testing "Then: it returns an empty collection"
            (t/is (empty? result))))))
    
    (t/testing "Given: a table with multiple species"
      (species/insert db {:code "elf" :name "Elf" :speed 30})
      (species/insert db {:code "dwarf" :name "Dwarf" :speed 25})
      
      (t/testing "When: we get all species"
        (let [result (species/get-all db {})]
          (t/testing "Then: it returns all species ordered by name"
            (t/is (= 2 (count result)))
            (t/is (match? [{:name "Dwarf"} {:name "Elf"}] result))))))))

(t/deftest test-species-get-by-code
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: a species exists in the database"
      (species/insert db {:code "elf"
                          :name "Elf"
                          :description "Graceful and long-lived"
                          :size "Medium"
                          :speed 30
                          :special_traits "Darkvision, Fey Ancestry"})
      
      (t/testing "When: we get the species by code"
        (let [result (species/get-by-code db {:code "elf"})]
          (t/testing "Then: it returns the species"
            (t/is (match? {:code "elf"
                           :name "Elf"
                           :speed 30}
                          result))))))
    
    (t/testing "Given: a species does not exist"
      (t/testing "When: we try to get it by code"
        (let [result (species/get-by-code db {:code "nonexistent"})]
          (t/testing "Then: it returns nil"
            (t/is (nil? result))))))))

(t/deftest test-species-insert
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: valid species data"
      (t/testing "When: we insert a species"
        (species/insert db {:code "halfling"
                            :name "Halfling"
                            :description "Small and lucky"
                            :size "Small"
                            :speed 25
                            :special_traits "Lucky, Brave"})
        
        (t/testing "Then: the species is persisted"
          (let [result (species/get-by-code db {:code "halfling"})]
            (t/is (match? {:code "halfling"
                           :name "Halfling"
                           :speed 25}
                          result))))))))

(t/deftest test-species-update
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an existing species"
      (species/insert db {:code "elf" :name "Elf" :speed 30})
      
      (t/testing "When: we update the species"
        (species/update-species db {:code "elf"
                                    :name "High Elf"
                                    :description "Ancient and wise"
                                    :speed 35})
        
        (t/testing "Then: the changes are persisted"
          (let [result (species/get-by-code db {:code "elf"})]
            (t/is (match? {:code "elf"
                           :name "High Elf"
                           :speed 35}
                          result))))))))

(t/deftest test-species-delete
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an existing species"
      (species/insert db {:code "elf" :name "Elf" :speed 30})
      
      (t/testing "When: we delete the species"
        (species/delete db {:code "elf"})
        
        (t/testing "Then: the species no longer exists"
          (let [result (species/get-by-code db {:code "elf"})]
            (t/is (nil? result))))))))

(t/deftest test-species-malli-validation
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: invalid data - missing required field 'name'"
      (t/testing "When: we try to insert"
        (t/testing "Then: it throws a validation error"
          (t/is (thrown? Exception
                         (species/insert db {:code "elf"}))))))
    
    (t/testing "Given: invalid params - missing 'code' key"
      (t/testing "When: we try to get by code"
        (t/testing "Then: it throws a validation error"
          (t/is (thrown? Exception
                         (species/get-by-code db {:wrong-key "elf"}))))))))
