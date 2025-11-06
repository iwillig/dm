(ns dm.queries
  "Database queries using HoneySQL for composable, idiomatic Clojure query building."
  (:require
   [honey.sql :as sql]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

;; ============================================
;; Helper Functions
;; ============================================

(defn query
  "Execute a query returning multiple rows."
  [db sql-map]
  (jdbc/execute! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps}))

(defn query-one
  "Execute a query returning one row (or nil)."
  [db sql-map]
  (jdbc/execute-one! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps}))

(defn execute!
  "Execute a mutation (insert/update/delete) returning affected row count."
  [db sql-map]
  (jdbc/execute-one! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps}))

(defn insert-returning!
  "Execute an insert with RETURNING clause, returns inserted row with generated keys."
  [db sql-map]
  (jdbc/execute-one! db
                     (sql/format sql-map)
                     {:return-keys true
                      :builder-fn rs/as-unqualified-lower-maps}))

;; ============================================
;; Species Queries
;; ============================================

(defn get-all-species
  "Get all species ordered by name."
  [db _params]
  (query db {:select [:*]
             :from [:species]
             :order-by [:name]}))

(defn get-species-by-code
  "Get a species by its code."
  [db {:keys [code]}]
  (query-one db {:select [:*]
                 :from [:species]
                 :where [:= :code code]}))

(defn insert-species!
  "Insert a new species."
  [db species]
  (execute! db {:insert-into :species
                :values [species]}))

(defn update-species!
  "Update an existing species by code."
  [db {:keys [code] :as species}]
  (execute! db {:update :species
                :set (dissoc species :code :created_at)
                :where [:= :code code]}))

(defn delete-species!
  "Delete a species by code."
  [db {:keys [code]}]
  (execute! db {:delete-from :species
                :where [:= :code code]}))

;; ============================================
;; Classes Queries
;; ============================================

(defn get-all-classes
  "Get all classes ordered by name."
  [db _params]
  (query db {:select [:*]
             :from [:classes]
             :order-by [:name]}))

(defn get-class-by-code
  "Get a class by its code."
  [db {:keys [code]}]
  (query-one db {:select [:*]
                 :from [:classes]
                 :where [:= :code code]}))

(defn insert-class!
  "Insert a new class."
  [db class-data]
  (execute! db {:insert-into :classes
                :values [class-data]}))

(defn update-class!
  "Update an existing class by code."
  [db {:keys [code] :as class-data}]
  (execute! db {:update :classes
                :set (dissoc class-data :code :created_at)
                :where [:= :code code]}))

(defn delete-class!
  "Delete a class by code."
  [db {:keys [code]}]
  (execute! db {:delete-from :classes
                :where [:= :code code]}))

;; ============================================
;; Attribute Names Queries
;; ============================================

(defn get-all-attribute-names
  "Get all attribute names ordered by display_order."
  [db _params]
  (query db {:select [:*]
             :from [:attribute_names]
             :order-by [:display_order]}))

(defn get-attribute-name-by-code
  "Get an attribute name by its code."
  [db {:keys [code]}]
  (query-one db {:select [:*]
                 :from [:attribute_names]
                 :where [:= :code code]}))

(defn insert-attribute-name!
  "Insert a new attribute name."
  [db attribute-name]
  (execute! db {:insert-into :attribute_names
                :values [attribute-name]}))

(defn update-attribute-name!
  "Update an existing attribute name by code."
  [db {:keys [code] :as attribute-name}]
  (execute! db {:update :attribute_names
                :set (dissoc attribute-name :code :created_at)
                :where [:= :code code]}))

(defn delete-attribute-name!
  "Delete an attribute name by code."
  [db {:keys [code]}]
  (execute! db {:delete-from :attribute_names
                :where [:= :code code]}))

;; ============================================
;; Skills Queries
;; ============================================

(defn get-all-skills
  "Get all skills ordered by name."
  [db _params]
  (query db {:select [:*]
             :from [:skills]
             :order-by [:name]}))

(defn get-skill-by-code
  "Get a skill by its code."
  [db {:keys [code]}]
  (query-one db {:select [:*]
                 :from [:skills]
                 :where [:= :code code]}))

(defn get-skills-by-attribute
  "Get all skills for a specific attribute."
  [db {:keys [attribute_code]}]
  (query db {:select [:*]
             :from [:skills]
             :where [:= :attribute_code attribute_code]
             :order-by [:name]}))

(defn insert-skill!
  "Insert a new skill."
  [db skill]
  (execute! db {:insert-into :skills
                :values [skill]}))

(defn update-skill!
  "Update an existing skill by code."
  [db {:keys [code] :as skill}]
  (execute! db {:update :skills
                :set (dissoc skill :code :created_at)
                :where [:= :code code]}))

(defn delete-skill!
  "Delete a skill by code."
  [db {:keys [code]}]
  (execute! db {:delete-from :skills
                :where [:= :code code]}))

;; ============================================
;; Characters Queries
;; ============================================

(defn get-all-characters
  "Get all characters ordered by name."
  [db _params]
  (query db {:select [:*]
             :from [:characters]
             :order-by [:name]}))

(defn get-character-by-id
  "Get a character by ID."
  [db {:keys [id]}]
  (query-one db {:select [:*]
                 :from [:characters]
                 :where [:= :id id]}))

(defn get-character-with-details
  "Get a character with species and class details."
  [db {:keys [id]}]
  (query-one db {:select [:c.*
                          [:s.name :species_name]
                          [:cl.name :class_name]]
                 :from [[:characters :c]]
                 :left-join [[:species :s] [:= :c.species_code :s.code]
                             [:classes :cl] [:= :c.class_code :cl.code]]
                 :where [:= :c.id id]}))

(defn insert-character!
  "Insert a new character and return it with generated ID."
  [db character]
  (insert-returning! db {:insert-into :characters
                         :values [character]
                         :returning [:*]}))

(defn update-character!
  "Update an existing character by ID."
  [db {:keys [id] :as character}]
  (execute! db {:update :characters
                :set (-> character
                         (dissoc :id :created_at)
                         (assoc :updated_at [:raw "CURRENT_TIMESTAMP"]))
                :where [:= :id id]}))

(defn delete-character!
  "Delete a character by ID."
  [db {:keys [id]}]
  (execute! db {:delete-from :characters
                :where [:= :id id]}))

;; ============================================
;; Character Attributes Queries
;; ============================================

(defn get-character-attributes
  "Get all attributes for a character with attribute details."
  [db {:keys [character_id]}]
  (query db {:select [:ca.*
                      [:an.name :attribute_name]
                      :an.abbreviation]
             :from [[:character_attributes :ca]]
             :join [[:attribute_names :an] [:= :ca.attribute_code :an.code]]
             :where [:= :ca.character_id character_id]
             :order-by [:an.display_order]}))

(defn get-character-attribute
  "Get a specific attribute for a character."
  [db {:keys [character_id attribute_code]}]
  (query-one db {:select [:ca.*
                          [:an.name :attribute_name]
                          :an.abbreviation]
                 :from [[:character_attributes :ca]]
                 :join [[:attribute_names :an] [:= :ca.attribute_code :an.code]]
                 :where [:and
                         [:= :ca.character_id character_id]
                         [:= :ca.attribute_code attribute_code]]}))

(defn set-character-attribute!
  "Set or update a character attribute value (upsert)."
  [db {:keys [character_id attribute_code attribute_value]}]
  (execute! db {:insert-into :character_attributes
                :values [{:character_id character_id
                          :attribute_code attribute_code
                          :attribute_value attribute_value}]
                :on-conflict [:character_id :attribute_code]
                :do-update-set [:attribute_value]}))

(defn delete-character-attribute!
  "Delete a specific character attribute."
  [db {:keys [character_id attribute_code]}]
  (execute! db {:delete-from :character_attributes
                :where [:and
                        [:= :character_id character_id]
                        [:= :attribute_code attribute_code]]}))

(defn delete-all-character-attributes!
  "Delete all attributes for a character."
  [db {:keys [character_id]}]
  (execute! db {:delete-from :character_attributes
                :where [:= :character_id character_id]}))

;; ============================================
;; Character Skills Queries
;; ============================================

(defn get-character-skills
  "Get all skills for a character with skill details."
  [db {:keys [character_id]}]
  (query db {:select [:cs.*
                      [:s.name :skill_name]
                      :s.attribute_code]
             :from [[:character_skills :cs]]
             :join [[:skills :s] [:= :cs.skill_code :s.code]]
             :where [:= :cs.character_id character_id]
             :order-by [:s.name]}))

(defn get-character-skill
  "Get a specific skill for a character."
  [db {:keys [character_id skill_code]}]
  (query-one db {:select [:cs.*
                          [:s.name :skill_name]
                          :s.attribute_code]
                 :from [[:character_skills :cs]]
                 :join [[:skills :s] [:= :cs.skill_code :s.code]]
                 :where [:and
                         [:= :cs.character_id character_id]
                         [:= :cs.skill_code skill_code]]}))

(defn add-character-skill!
  "Add a skill proficiency to a character."
  [db {:keys [character_id skill_code proficiency_level]}]
  (execute! db {:insert-into :character_skills
                :values [{:character_id character_id
                          :skill_code skill_code
                          :proficiency_level proficiency_level}]}))

(defn update-skill-proficiency!
  "Update a character's skill proficiency level."
  [db {:keys [character_id skill_code proficiency_level]}]
  (execute! db {:update :character_skills
                :set {:proficiency_level proficiency_level}
                :where [:and
                        [:= :character_id character_id]
                        [:= :skill_code skill_code]]}))

(defn remove-character-skill!
  "Remove a skill from a character."
  [db {:keys [character_id skill_code]}]
  (execute! db {:delete-from :character_skills
                :where [:and
                        [:= :character_id character_id]
                        [:= :skill_code skill_code]]}))

(defn remove-all-character-skills!
  "Remove all skills from a character."
  [db {:keys [character_id]}]
  (execute! db {:delete-from :character_skills
                :where [:= :character_id character_id]}))

;; ============================================
;; Items Queries
;; ============================================

(defn get-all-items
  "Get all items ordered by name."
  [db _params]
  (query db {:select [:*]
             :from [:items]
             :order-by [:name]}))

(defn get-item-by-id
  "Get an item by ID."
  [db {:keys [id]}]
  (query-one db {:select [:*]
                 :from [:items]
                 :where [:= :id id]}))

(defn get-items-by-type
  "Get all items of a specific type."
  [db {:keys [item_type]}]
  (query db {:select [:*]
             :from [:items]
             :where [:= :item_type item_type]
             :order-by [:name]}))

(defn insert-item!
  "Insert a new item and return it with generated ID."
  [db item]
  (insert-returning! db {:insert-into :items
                         :values [item]
                         :returning [:*]}))

(defn update-item!
  "Update an existing item by ID."
  [db {:keys [id] :as item}]
  (execute! db {:update :items
                :set (dissoc item :id :created_at)
                :where [:= :id id]}))

(defn delete-item!
  "Delete an item by ID."
  [db {:keys [id]}]
  (execute! db {:delete-from :items
                :where [:= :id id]}))
