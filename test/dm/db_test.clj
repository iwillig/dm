(ns dm.db-test
  (:require [clojure.test :as t]
            [dm.db :as dm.db]
            [dm.queries :as q]
            [next.jdbc :as jdbc]
            [matcher-combinators.test]))

(def test-db-spec
  "jdbc:sqlite:test.db")

(defn test-datasource []
  (jdbc/get-datasource test-db-spec))

(defn cleanup-test-db! []
  (try
    (java.io.File. "test.db")
    (.delete (java.io.File. "test.db"))
    (catch Exception _)))

(defn setup-test-db! [db]
  ;; Drop tables if they exist
  (try (jdbc/execute! db ["DROP TABLE IF EXISTS character_skills"]) (catch Exception _))
  (try (jdbc/execute! db ["DROP TABLE IF EXISTS character_attributes"]) (catch Exception _))
  (try (jdbc/execute! db ["DROP TABLE IF EXISTS characters"]) (catch Exception _))
  (try (jdbc/execute! db ["DROP TABLE IF EXISTS skills"]) (catch Exception _))
  (try (jdbc/execute! db ["DROP TABLE IF EXISTS items"]) (catch Exception _))
  (try (jdbc/execute! db ["DROP TABLE IF EXISTS attribute_names"]) (catch Exception _))
  (try (jdbc/execute! db ["DROP TABLE IF EXISTS classes"]) (catch Exception _))
  (try (jdbc/execute! db ["DROP TABLE IF EXISTS species"]) (catch Exception _))

  ;; Run migrations manually for test database
  (jdbc/execute! db ["CREATE TABLE species (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    size TEXT,
    speed INTEGER,
    special_traits TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  )"])

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
  )"])

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

  (jdbc/execute! db ["CREATE TABLE characters (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    species_code TEXT NOT NULL,
    class_code TEXT NOT NULL,
    armor_class INTEGER,
    inspiration BOOLEAN DEFAULT FALSE,
    level INTEGER DEFAULT 1,
    hit_points_max INTEGER,
    hit_points_current INTEGER,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (species_code) REFERENCES species(code),
    FOREIGN KEY (class_code) REFERENCES classes(code)
  )"])

  (jdbc/execute! db ["CREATE TABLE character_attributes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    character_id INTEGER NOT NULL,
    attribute_code TEXT NOT NULL,
    attribute_value INTEGER NOT NULL CHECK(attribute_value >= 0 AND attribute_value <= 30),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE,
    FOREIGN KEY (attribute_code) REFERENCES attribute_names(code),
    UNIQUE(character_id, attribute_code)
  )"])

  (jdbc/execute! db ["CREATE TABLE character_skills (
    character_id INTEGER NOT NULL,
    skill_code TEXT NOT NULL,
    proficiency_level INTEGER DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (character_id, skill_code),
    FOREIGN KEY (character_id) REFERENCES characters(id) ON DELETE CASCADE,
    FOREIGN KEY (skill_code) REFERENCES skills(code)
  )"])

  (jdbc/execute! db ["CREATE TABLE items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    item_type TEXT,
    quantity INTEGER DEFAULT 1,
    weight REAL,
    value_copper INTEGER,
    properties TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  )"]))

(t/deftest test-db
  (t/testing ""
    (t/is (match? true true))))

(t/deftest test-temp-id
  (t/testing "Given: no args"
    (t/testing "When: We attempt to get the next temp-id"
      (t/is (match? neg-int? (dm.db/next-temp-id))
            "Then: The fn should return a neg-int"))))

;; Species tests
(t/deftest test-species-crud
  (let [db (test-datasource)]
    (setup-test-db! db)

    (t/testing "Given: an empty species table"
      (t/testing "When: we insert a species"
        (q/insert-species! db {:code "elf"
                               :name "Elf"
                               :description "Graceful and long-lived"
                               :size "Medium"
                               :speed 30
                               :special_traits "Darkvision, Fey Ancestry"})
        (t/testing "Then: we can retrieve it by code"
          (let [result (q/get-species-by-code db {:code "elf"})]
            (t/is (match? {:code "elf"
                           :name "Elf"
                           :description "Graceful and long-lived"}
                          result))))

        (t/testing "Then: it appears in get-all-species"
          (let [results (q/get-all-species db {})]
            (t/is (match? [{:code "elf"}]
                          results))))))

    (t/testing "Given: an existing species"
      (t/testing "When: we update it"
        (q/update-species! db {:code "elf"
                               :name "High Elf"
                               :description "Ancient and wise"
                               :size "Medium"
                               :speed 35
                               :special_traits "Darkvision, Enhanced Fey Ancestry"})
        (t/testing "Then: the changes are persisted"
          (let [result (q/get-species-by-code db {:code "elf"})]
            (t/is (match? {:name "High Elf"
                           :speed 35}
                          result))))))

    (t/testing "Given: an existing species"
      (t/testing "When: we delete it"
        (q/delete-species! db {:code "elf"})
        (t/testing "Then: it no longer exists"
          (let [result (q/get-species-by-code db {:code "elf"})]
            (t/is (nil? result))))))))

;; Classes tests
(t/deftest test-classes-crud
  (let [db (test-datasource)]
    (setup-test-db! db)

    (t/testing "Given: an empty classes table"
      (t/testing "When: we insert a class"
        (q/insert-class! db {:code "wizard"
                             :name "Wizard"
                             :description "Master of arcane magic"
                             :hit_die 6
                             :primary_ability "intelligence"
                             :saving_throw_proficiencies "intelligence,wisdom"
                             :armor_proficiencies "none"
                             :weapon_proficiencies "simple"})
        (t/testing "Then: we can retrieve it by code"
          (let [result (q/get-class-by-code db {:code "wizard"})]
            (t/is (match? {:code "wizard"
                           :name "Wizard"
                           :hit_die 6}
                          result))))

        (t/testing "Then: it appears in get-all-classes"
          (let [results (q/get-all-classes db {})]
            (t/is (match? [{:code "wizard"}]
                          results))))))

    (t/testing "Given: an existing class"
      (t/testing "When: we update it"
        (q/update-class! db {:code "wizard"
                             :name "Grand Wizard"
                             :description "Supreme master of arcane magic"
                             :hit_die 8
                             :primary_ability "intelligence"
                             :saving_throw_proficiencies "intelligence,wisdom,charisma"
                             :armor_proficiencies "light"
                             :weapon_proficiencies "simple,staff"})
        (t/testing "Then: the changes are persisted"
          (let [result (q/get-class-by-code db {:code "wizard"})]
            (t/is (match? {:name "Grand Wizard"
                           :hit_die 8}
                          result))))))

    (t/testing "Given: an existing class"
      (t/testing "When: we delete it"
        (q/delete-class! db {:code "wizard"})
        (t/testing "Then: it no longer exists"
          (let [result (q/get-class-by-code db {:code "wizard"})]
            (t/is (nil? result))))))))

;; Attribute names tests
(t/deftest test-attribute-names-crud
  (let [db (test-datasource)]
    (setup-test-db! db)

    (t/testing "Given: an empty attribute_names table"
      (t/testing "When: we insert an attribute name"
        (q/insert-attribute-name! db {:code "strength"
                                      :name "Strength"
                                      :abbreviation "STR"
                                      :description "Physical power"
                                      :display_order 1})
        (t/testing "Then: we can retrieve it by code"
          (let [result (q/get-attribute-name-by-code db {:code "strength"})]
            (t/is (match? {:code "strength"
                           :name "Strength"
                           :abbreviation "STR"}
                          result))))

        (t/testing "Then: it appears in get-all-attribute-names"
          (let [results (q/get-all-attribute-names db {})]
            (t/is (match? [{:code "strength"}]
                          results))))))

    (t/testing "Given: an existing attribute name"
      (t/testing "When: we update it"
        (q/update-attribute-name! db {:code "strength"
                                      :name "Mighty Strength"
                                      :abbreviation "MSTR"
                                      :description "Overwhelming physical power"
                                      :display_order 1})
        (t/testing "Then: the changes are persisted"
          (let [result (q/get-attribute-name-by-code db {:code "strength"})]
            (t/is (match? {:name "Mighty Strength"
                           :abbreviation "MSTR"}
                          result))))))

    (t/testing "Given: an existing attribute name"
      (t/testing "When: we delete it"
        (q/delete-attribute-name! db {:code "strength"})
        (t/testing "Then: it no longer exists"
          (let [result (q/get-attribute-name-by-code db {:code "strength"})]
            (t/is (nil? result))))))))

;; Skills tests
(t/deftest test-skills-crud
  (let [db (test-datasource)]
    (setup-test-db! db)

    ;; Setup prerequisite data
    (q/insert-attribute-name! db {:code "dexterity"
                                  :name "Dexterity"
                                  :abbreviation "DEX"
                                  :description "Agility"
                                  :display_order 2})

    (t/testing "Given: an empty skills table"
      (t/testing "When: we insert a skill"
        (q/insert-skill! db {:code "acrobatics"
                             :name "Acrobatics"
                             :attribute_code "dexterity"
                             :description "Balance and tumbling"})
        (t/testing "Then: we can retrieve it by code"
          (let [result (q/get-skill-by-code db {:code "acrobatics"})]
            (t/is (match? {:code "acrobatics"
                           :name "Acrobatics"
                           :attribute_code "dexterity"}
                          result))))

        (t/testing "Then: it appears in get-all-skills"
          (let [results (q/get-all-skills db {})]
            (t/is (match? [{:code "acrobatics"}]
                          results))))

        (t/testing "Then: it appears in get-skills-by-attribute"
          (let [results (q/get-skills-by-attribute db {:attribute_code "dexterity"})]
            (t/is (match? [{:code "acrobatics"}]
                          results))))))

    (t/testing "Given: an existing skill"
      (t/testing "When: we update it"
        (q/update-skill! db {:code "acrobatics"
                             :name "Advanced Acrobatics"
                             :attribute_code "dexterity"
                             :description "Advanced balance and tumbling"})
        (t/testing "Then: the changes are persisted"
          (let [result (q/get-skill-by-code db {:code "acrobatics"})]
            (t/is (match? {:name "Advanced Acrobatics"}
                          result))))))

    (t/testing "Given: an existing skill"
      (t/testing "When: we delete it"
        (q/delete-skill! db {:code "acrobatics"})
        (t/testing "Then: it no longer exists"
          (let [result (q/get-skill-by-code db {:code "acrobatics"})]
            (t/is (nil? result))))))))

;; Characters tests
(t/deftest test-characters-crud
  (let [db (test-datasource)]
    (setup-test-db! db)

    ;; Setup prerequisite data
    (q/insert-species! db {:code "human"
                           :name "Human"
                           :description "Versatile"
                           :size "Medium"
                           :speed 30
                           :special_traits "Adaptable"})
    (q/insert-class! db {:code "fighter"
                         :name "Fighter"
                         :description "Warrior"
                         :hit_die 10
                         :primary_ability "strength"
                         :saving_throw_proficiencies "strength,constitution"
                         :armor_proficiencies "all"
                         :weapon_proficiencies "all"})

    (t/testing "Given: an empty characters table"
      (t/testing "When: we insert a character"
        (let [result (q/insert-character! db {:name "Conan"
                                              :species_code "human"
                                              :class_code "fighter"
                                              :armor_class 16
                                              :inspiration false
                                              :level 5
                                              :hit_points_max 50
                                              :hit_points_current 50})]
          (t/testing "Then: we get back the generated ID"
            (t/is (match? pos-int? (:id result))))

          (t/testing "Then: we can retrieve it by ID"
            (let [character (q/get-character-by-id db {:id (:id result)})]
              (t/is (match? {:name "Conan"
                             :species_code "human"
                             :class_code "fighter"
                             :armor_class 16
                             :level 5}
                            character))))

          (t/testing "Then: we can retrieve it with details"
            (let [character (q/get-character-with-details db {:id (:id result)})]
              (t/is (match? {:name "Conan"
                             :species_name "Human"
                             :class_name "Fighter"}
                            character))))

          (t/testing "Then: it appears in get-all-characters"
            (let [results (q/get-all-characters db {})]
              (t/is (match? [{:name "Conan"}]
                            results))))

          (t/testing "Given: an existing character"
            (t/testing "When: we update it"
              (q/update-character! db {:id (:id result)
                                       :name "Conan the Barbarian"
                                       :species_code "human"
                                       :class_code "fighter"
                                       :armor_class 18
                                       :inspiration true
                                       :level 6
                                       :hit_points_max 60
                                       :hit_points_current 45})
              (t/testing "Then: the changes are persisted"
                (let [character (q/get-character-by-id db {:id (:id result)})]
                  ;; Note: SQLite stores booleans as integers (1 for true, 0 for false)
                  (t/is (match? {:name "Conan the Barbarian"
                                 :armor_class 18
                                 :level 6
                                 :inspiration 1}
                                character))))))

          (t/testing "Given: an existing character"
            (t/testing "When: we delete it"
              (q/delete-character! db {:id (:id result)})
              (t/testing "Then: it no longer exists"
                (let [character (q/get-character-by-id db {:id (:id result)})]
                  (t/is (nil? character)))))))))))

;; Character attributes tests
(t/deftest test-character-attributes-crud
  (let [db (test-datasource)]
    (setup-test-db! db)

    ;; Setup prerequisite data
    (q/insert-species! db {:code "elf"
                           :name "Elf"
                           :description "Graceful"
                           :size "Medium"
                           :speed 30
                           :special_traits "Darkvision"})
    (q/insert-class! db {:code "ranger"
                         :name "Ranger"
                         :description "Hunter"
                         :hit_die 10
                         :primary_ability "dexterity"
                         :saving_throw_proficiencies "strength,dexterity"
                         :armor_proficiencies "light,medium"
                         :weapon_proficiencies "simple,martial"})
    (q/insert-attribute-name! db {:code "strength"
                                  :name "Strength"
                                  :abbreviation "STR"
                                  :description "Physical power"
                                  :display_order 1})
    (q/insert-attribute-name! db {:code "wisdom"
                                  :name "Wisdom"
                                  :abbreviation "WIS"
                                  :description "Perception"
                                  :display_order 5})

    (let [char-result (q/insert-character! db {:name "Legolas"
                                               :species_code "elf"
                                               :class_code "ranger"
                                               :armor_class 15
                                               :inspiration false
                                               :level 3
                                               :hit_points_max 30
                                               :hit_points_current 30})
          char-id (:id char-result)]

      (t/testing "Given: a character with no attributes"
        (t/testing "When: we set an attribute"
          (q/set-character-attribute! db {:character_id char-id
                                          :attribute_code "strength"
                                          :attribute_value 14})
          (t/testing "Then: we can retrieve it"
            (let [result (q/get-character-attribute db {:character_id char-id
                                                        :attribute_code "strength"})]
              (t/is (match? {:attribute_value 14
                             :attribute_name "Strength"
                             :abbreviation "STR"}
                            result))))

          (t/testing "Then: it appears in get-character-attributes"
            (let [results (q/get-character-attributes db {:character_id char-id})]
              (t/is (match? [{:attribute_code "strength"
                              :attribute_value 14}]
                            results))))))

      (t/testing "Given: a character with an existing attribute"
        (t/testing "When: we upsert the same attribute with a new value"
          (q/set-character-attribute! db {:character_id char-id
                                          :attribute_code "strength"
                                          :attribute_value 16})
          (t/testing "Then: the value is updated"
            (let [result (q/get-character-attribute db {:character_id char-id
                                                        :attribute_code "strength"})]
              (t/is (match? {:attribute_value 16}
                            result))))))

      (t/testing "Given: a character with multiple attributes"
        (q/set-character-attribute! db {:character_id char-id
                                        :attribute_code "wisdom"
                                        :attribute_value 18})
        (t/testing "When: we get all character attributes"
          (let [results (q/get-character-attributes db {:character_id char-id})]
            (t/is (= 2 (count results)))
            (t/is (match? [{:attribute_code "strength"}
                           {:attribute_code "wisdom"}]
                          results)))))

      (t/testing "Given: a character with an attribute"
        (t/testing "When: we delete a specific attribute"
          (q/delete-character-attribute! db {:character_id char-id
                                             :attribute_code "strength"})
          (t/testing "Then: only that attribute is removed"
            (let [results (q/get-character-attributes db {:character_id char-id})]
              (t/is (= 1 (count results)))
              (t/is (match? [{:attribute_code "wisdom"}]
                            results))))))

      (t/testing "Given: a character with attributes"
        (t/testing "When: we delete all attributes"
          (q/delete-all-character-attributes! db {:character_id char-id})
          (t/testing "Then: no attributes remain"
            (let [results (q/get-character-attributes db {:character_id char-id})]
              (t/is (empty? results)))))))))

;; Character skills tests
(t/deftest test-character-skills-crud
  (let [db (test-datasource)]
    (setup-test-db! db)

    ;; Setup prerequisite data
    (q/insert-species! db {:code "halfling"
                           :name "Halfling"
                           :description "Small and quick"
                           :size "Small"
                           :speed 25
                           :special_traits "Lucky"})
    (q/insert-class! db {:code "rogue"
                         :name "Rogue"
                         :description "Sneaky"
                         :hit_die 8
                         :primary_ability "dexterity"
                         :saving_throw_proficiencies "dexterity,intelligence"
                         :armor_proficiencies "light"
                         :weapon_proficiencies "simple"})
    (q/insert-attribute-name! db {:code "dexterity"
                                  :name "Dexterity"
                                  :abbreviation "DEX"
                                  :description "Agility"
                                  :display_order 2})
    (q/insert-skill! db {:code "stealth"
                         :name "Stealth"
                         :attribute_code "dexterity"
                         :description "Hide and move silently"})
    (q/insert-skill! db {:code "sleight-of-hand"
                         :name "Sleight of Hand"
                         :attribute_code "dexterity"
                         :description "Pick pockets"})

    (let [char-result (q/insert-character! db {:name "Bilbo"
                                               :species_code "halfling"
                                               :class_code "rogue"
                                               :armor_class 14
                                               :inspiration false
                                               :level 4
                                               :hit_points_max 32
                                               :hit_points_current 32})
          char-id (:id char-result)]

      (t/testing "Given: a character with no skills"
        (t/testing "When: we add a skill proficiency"
          (q/add-character-skill! db {:character_id char-id
                                      :skill_code "stealth"
                                      :proficiency_level 1})
          (t/testing "Then: we can retrieve it"
            (let [result (q/get-character-skill db {:character_id char-id
                                                    :skill_code "stealth"})]
              (t/is (match? {:skill_code "stealth"
                             :skill_name "Stealth"
                             :proficiency_level 1}
                            result))))

          (t/testing "Then: it appears in get-character-skills"
            (let [results (q/get-character-skills db {:character_id char-id})]
              (t/is (match? [{:skill_code "stealth"}]
                            results))))))

      (t/testing "Given: a character with a skill proficiency"
        (t/testing "When: we update the proficiency level"
          (q/update-skill-proficiency! db {:character_id char-id
                                           :skill_code "stealth"
                                           :proficiency_level 2})
          (t/testing "Then: the level is updated"
            (let [result (q/get-character-skill db {:character_id char-id
                                                    :skill_code "stealth"})]
              (t/is (match? {:proficiency_level 2}
                            result))))))

      (t/testing "Given: a character with multiple skills"
        (q/add-character-skill! db {:character_id char-id
                                    :skill_code "sleight-of-hand"
                                    :proficiency_level 1})
        (t/testing "When: we get all character skills"
          (let [results (q/get-character-skills db {:character_id char-id})]
            (t/is (= 2 (count results)))
            (t/is (match? [{:skill_code "sleight-of-hand"}
                           {:skill_code "stealth"}]
                          results)))))

      (t/testing "Given: a character with a skill"
        (t/testing "When: we remove a specific skill"
          (q/remove-character-skill! db {:character_id char-id
                                         :skill_code "stealth"})
          (t/testing "Then: only that skill is removed"
            (let [results (q/get-character-skills db {:character_id char-id})]
              (t/is (= 1 (count results)))
              (t/is (match? [{:skill_code "sleight-of-hand"}]
                            results))))))

      (t/testing "Given: a character with skills"
        (t/testing "When: we remove all skills"
          (q/remove-all-character-skills! db {:character_id char-id})
          (t/testing "Then: no skills remain"
            (let [results (q/get-character-skills db {:character_id char-id})]
              (t/is (empty? results)))))))))

;; Items tests
(t/deftest test-items-crud
  (let [db (test-datasource)]
    (setup-test-db! db)

    (t/testing "Given: an empty items table"
      (t/testing "When: we insert an item"
        (let [result (q/insert-item! db {:name "Longsword"
                                         :description "A versatile blade"
                                         :item_type "weapon"
                                         :quantity 1
                                         :weight 3.0
                                         :value_copper 1500
                                         :properties "{\"damage\":\"1d8\"}"})]
          (t/testing "Then: we get back the generated ID"
            (t/is (match? pos-int? (:id result))))

          (t/testing "Then: we can retrieve it by ID"
            (let [item (q/get-item-by-id db {:id (:id result)})]
              (t/is (match? {:name "Longsword"
                             :item_type "weapon"
                             :quantity 1
                             :weight 3.0}
                            item))))

          (t/testing "Then: it appears in get-all-items"
            (let [results (q/get-all-items db {})]
              (t/is (match? [{:name "Longsword"}]
                            results))))

          (t/testing "Then: it appears in get-items-by-type"
            (let [results (q/get-items-by-type db {:item_type "weapon"})]
              (t/is (match? [{:name "Longsword"}]
                            results))))

          (t/testing "Given: an existing item"
            (t/testing "When: we update it"
              (q/update-item! db {:id (:id result)
                                  :name "Magical Longsword"
                                  :description "A blade imbued with magic"
                                  :item_type "magic-weapon"
                                  :quantity 1
                                  :weight 3.0
                                  :value_copper 5000
                                  :properties "{\"damage\":\"1d8+1\"}"})
              (t/testing "Then: the changes are persisted"
                (let [item (q/get-item-by-id db {:id (:id result)})]
                  (t/is (match? {:name "Magical Longsword"
                                 :item_type "magic-weapon"
                                 :value_copper 5000}
                                item))))))

          (t/testing "Given: an existing item"
            (t/testing "When: we delete it"
              (q/delete-item! db {:id (:id result)})
              (t/testing "Then: it no longer exists"
                (let [item (q/get-item-by-id db {:id (:id result)})]
                  (t/is (nil? item)))))))))))
