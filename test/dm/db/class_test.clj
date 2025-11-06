(ns dm.db.class-test
  (:require [clojure.test :as t]
            [dm.db.class :as class]
            [next.jdbc :as jdbc]
            [matcher-combinators.test]))

(def test-db-spec "jdbc:sqlite:test-class.db")

(defn test-datasource []
  (jdbc/get-datasource test-db-spec))

(defn setup-test-db! [db]
  (try (jdbc/execute! db ["DROP TABLE IF EXISTS classes"]) (catch Exception _))
  (jdbc/execute! db ["CREATE TABLE classes (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    hit_die INTEGER,
    primary_ability TEXT,
    saving_throw_proficiencies TEXT,
    armor_proficiencies TEXT,
    weapon_proficiencies TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  )"]))

(t/deftest test-class-get-all
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an empty classes table"
      (t/testing "When: we get all classes"
        (let [result (class/get-all db {})]
          (t/testing "Then: it returns an empty collection"
            (t/is (empty? result))))))
    
    (t/testing "Given: a table with multiple classes"
      (class/insert db {:code "wizard" :name "Wizard" :hit_die 6})
      (class/insert db {:code "fighter" :name "Fighter" :hit_die 10})
      
      (t/testing "When: we get all classes"
        (let [result (class/get-all db {})]
          (t/testing "Then: it returns all classes ordered by name"
            (t/is (= 2 (count result)))
            (t/is (match? [{:name "Fighter"} {:name "Wizard"}] result))))))))

(t/deftest test-class-get-by-code
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: a class exists in the database"
      (class/insert db {:code "wizard"
                        :name "Wizard"
                        :description "Master of arcane magic"
                        :hit_die 6
                        :primary_ability "intelligence"})
      
      (t/testing "When: we get the class by code"
        (let [result (class/get-by-code db {:code "wizard"})]
          (t/testing "Then: it returns the class"
            (t/is (match? {:code "wizard"
                           :name "Wizard"
                           :hit_die 6
                           :primary_ability "intelligence"}
                          result))))))
    
    (t/testing "Given: a class does not exist"
      (t/testing "When: we try to get it by code"
        (let [result (class/get-by-code db {:code "nonexistent"})]
          (t/testing "Then: it returns nil"
            (t/is (nil? result))))))))

(t/deftest test-class-insert
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: valid class data"
      (t/testing "When: we insert a class"
        (class/insert db {:code "rogue"
                          :name "Rogue"
                          :description "Sneaky and skilled"
                          :hit_die 8
                          :primary_ability "dexterity"
                          :saving_throw_proficiencies "dexterity,intelligence"
                          :armor_proficiencies "light"
                          :weapon_proficiencies "simple"})
        
        (t/testing "Then: the class is persisted"
          (let [result (class/get-by-code db {:code "rogue"})]
            (t/is (match? {:code "rogue"
                           :name "Rogue"
                           :hit_die 8}
                          result))))))))

(t/deftest test-class-update
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an existing class"
      (class/insert db {:code "wizard" :name "Wizard" :hit_die 6})
      
      (t/testing "When: we update the class"
        (class/update-class db {:code "wizard"
                                :name "Grand Wizard"
                                :hit_die 8
                                :description "Supreme master of magic"})
        
        (t/testing "Then: the changes are persisted"
          (let [result (class/get-by-code db {:code "wizard"})]
            (t/is (match? {:code "wizard"
                           :name "Grand Wizard"
                           :hit_die 8
                           :description "Supreme master of magic"}
                          result))))))))

(t/deftest test-class-delete
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an existing class"
      (class/insert db {:code "wizard" :name "Wizard" :hit_die 6})
      
      (t/testing "When: we delete the class"
        (class/delete db {:code "wizard"})
        
        (t/testing "Then: the class no longer exists"
          (let [result (class/get-by-code db {:code "wizard"})]
            (t/is (nil? result))))))))

(t/deftest test-class-malli-validation
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: invalid data - missing required field 'name'"
      (t/testing "When: we try to insert"
        (t/testing "Then: it throws a validation error"
          (t/is (thrown? Exception
                         (class/insert db {:code "wizard"}))))))
    
    (t/testing "Given: invalid params - missing 'code' key"
      (t/testing "When: we try to get by code"
        (t/testing "Then: it throws a validation error"
          (t/is (thrown? Exception
                         (class/get-by-code db {:wrong-key "wizard"}))))))))
