(ns dm.db.skill-test
  (:require [clojure.test :as t]
            [dm.db.skill :as skill]
            [next.jdbc :as jdbc]
            [matcher-combinators.test]))

(def test-db-spec "jdbc:sqlite:test-skill.db")

(defn test-datasource []
  (jdbc/get-datasource test-db-spec))

(defn setup-test-db! [db]
  (try (jdbc/execute! db ["DROP TABLE IF EXISTS skills"]) (catch Exception _))
  (try (jdbc/execute! db ["DROP TABLE IF EXISTS attribute_names"]) (catch Exception _))
  
  (jdbc/execute! db ["CREATE TABLE attribute_names (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    abbreviation TEXT,
    description TEXT,
    display_order INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  )"])
  
  (jdbc/execute! db ["CREATE TABLE skills (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    attribute_code TEXT NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (attribute_code) REFERENCES attribute_names(code)
  )"])
  
  ;; Insert prerequisite attribute
  (jdbc/execute! db ["INSERT INTO attribute_names (code, name, abbreviation, display_order)
                      VALUES ('dexterity', 'Dexterity', 'DEX', 2)"]))

(t/deftest test-skill-get-all
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an empty skills table"
      (t/testing "When: we get all skills"
        (let [result (skill/get-all db {})]
          (t/testing "Then: it returns an empty collection"
            (t/is (empty? result))))))
    
    (t/testing "Given: a table with multiple skills"
      (skill/insert db {:code "acrobatics" :name "Acrobatics" :attribute_code "dexterity"})
      (skill/insert db {:code "stealth" :name "Stealth" :attribute_code "dexterity"})
      
      (t/testing "When: we get all skills"
        (let [result (skill/get-all db {})]
          (t/testing "Then: it returns all skills ordered by name"
            (t/is (= 2 (count result)))
            (t/is (match? [{:name "Acrobatics"} {:name "Stealth"}] result))))))))

(t/deftest test-skill-get-by-code
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: a skill exists in the database"
      (skill/insert db {:code "acrobatics"
                        :name "Acrobatics"
                        :attribute_code "dexterity"
                        :description "Balance and tumbling"})
      
      (t/testing "When: we get the skill by code"
        (let [result (skill/get-by-code db {:code "acrobatics"})]
          (t/testing "Then: it returns the skill"
            (t/is (match? {:code "acrobatics"
                           :name "Acrobatics"
                           :attribute_code "dexterity"}
                          result))))))
    
    (t/testing "Given: a skill does not exist"
      (t/testing "When: we try to get it by code"
        (let [result (skill/get-by-code db {:code "nonexistent"})]
          (t/testing "Then: it returns nil"
            (t/is (nil? result))))))))

(t/deftest test-skill-get-by-attribute
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: skills for a specific attribute"
      (skill/insert db {:code "acrobatics" :name "Acrobatics" :attribute_code "dexterity"})
      (skill/insert db {:code "stealth" :name "Stealth" :attribute_code "dexterity"})
      
      (t/testing "When: we get skills by attribute"
        (let [result (skill/get-by-attribute db {:attribute_code "dexterity"})]
          (t/testing "Then: it returns only skills for that attribute"
            (t/is (= 2 (count result)))
            (t/is (every? #(= "dexterity" (:attribute_code %)) result))))))))

(t/deftest test-skill-insert
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: valid skill data"
      (t/testing "When: we insert a skill"
        (skill/insert db {:code "athletics"
                          :name "Athletics"
                          :attribute_code "dexterity"
                          :description "Physical power and coordination"})
        
        (t/testing "Then: the skill is persisted"
          (let [result (skill/get-by-code db {:code "athletics"})]
            (t/is (match? {:code "athletics"
                           :name "Athletics"
                           :attribute_code "dexterity"}
                          result))))))))

(t/deftest test-skill-update
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an existing skill"
      (skill/insert db {:code "acrobatics" :name "Acrobatics" :attribute_code "dexterity"})
      
      (t/testing "When: we update the skill"
        (skill/update-skill db {:code "acrobatics"
                                :name "Advanced Acrobatics"
                                :description "Expert tumbling and balance"})
        
        (t/testing "Then: the changes are persisted"
          (let [result (skill/get-by-code db {:code "acrobatics"})]
            (t/is (match? {:code "acrobatics"
                           :name "Advanced Acrobatics"
                           :description "Expert tumbling and balance"}
                          result))))))))

(t/deftest test-skill-delete
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an existing skill"
      (skill/insert db {:code "acrobatics" :name "Acrobatics" :attribute_code "dexterity"})
      
      (t/testing "When: we delete the skill"
        (skill/delete db {:code "acrobatics"})
        
        (t/testing "Then: the skill no longer exists"
          (let [result (skill/get-by-code db {:code "acrobatics"})]
            (t/is (nil? result))))))))

(t/deftest test-skill-malli-validation
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: invalid data - missing required field 'name'"
      (t/testing "When: we try to insert"
        (t/testing "Then: it throws a validation error"
          (t/is (thrown? Exception
                         (skill/insert db {:code "acrobatics" :attribute_code "dexterity"}))))))
    
    (t/testing "Given: invalid params - missing 'code' key"
      (t/testing "When: we try to get by code"
        (t/testing "Then: it throws a validation error"
          (t/is (thrown? Exception
                         (skill/get-by-code db {:wrong-key "acrobatics"}))))))))
