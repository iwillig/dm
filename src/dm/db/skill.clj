(ns dm.db.skill
  "Skill enumeration table queries.
   
   Provides CRUD operations for the skills table, which contains D&D 5e skills
   (acrobatics, stealth, etc.) with their associated attributes."
  (:require
   [dm.db.query-helpers :as qh]
   [malli.core :as m]))

;; ============================================
;; Malli Schemas
;; ============================================

(def Code
  "String code identifier (primary key)."
  [:string {:min 1}])

(def SkillEntity
  "Skill entity schema."
  [:map
   [:code Code]
   [:name :string]
   [:attribute_code Code]
   [:description {:optional true} [:maybe :string]]
   [:created_at {:optional true} [:maybe :string]]])

(def GetByCodeParams
  "Parameters for get-by-code function."
  [:map [:code Code]])

(def GetByAttributeParams
  "Parameters for get-by-attribute function."
  [:map [:attribute_code Code]])

(def UpdateParams
  "Parameters for update function."
  [:map
   [:code Code]
   [:name {:optional true} :string]
   [:attribute_code {:optional true} Code]
   [:description {:optional true} [:maybe :string]]])

;; ============================================
;; CRUD Functions
;; ============================================

(defn get-all
  "Get all skills ordered by name.
   
   Returns a collection of all skills from the database.
   
   Args:
     db      - Database connection or datasource
     _params - Ignored (included for consistent handler signature)
   
   Returns:
     Vector of skill maps"
  [db _params]
  (qh/query db {:select [:*]
                :from [:skills]
                :order-by [:name]}))

(defn get-by-code
  "Get a skill by its code.
   
   Retrieves a single skill by its primary key (code).
   
   Args:
     db     - Database connection or datasource
     params - Map with :code key (e.g., \"acrobatics\", \"stealth\")
   
   Returns:
     Skill map or nil if not found"
  [db params]
  (m/assert GetByCodeParams params)
  (let [{:keys [code]} params]
    (qh/query-one db {:select [:*]
                      :from [:skills]
                      :where [:= :code code]})))

(defn get-by-attribute
  "Get all skills for a specific attribute.
   
   Retrieves all skills associated with a particular attribute code.
   
   Args:
     db     - Database connection or datasource
     params - Map with :attribute_code key (e.g., \"dexterity\", \"strength\")
   
   Returns:
     Vector of skill maps"
  [db params]
  (m/assert GetByAttributeParams params)
  (let [{:keys [attribute_code]} params]
    (qh/query db {:select [:*]
                  :from [:skills]
                  :where [:= :attribute_code attribute_code]
                  :order-by [:name]})))

(defn insert
  "Insert a new skill.
   
   Adds a new skill to the database.
   
   Args:
     db    - Database connection or datasource
     skill - Skill map with required keys: code, name, attribute_code
             Optional keys: description
   
   Returns:
     Result map with :next.jdbc/update-count
   
   Example:
     (insert db {:code \"acrobatics\" :name \"Acrobatics\" :attribute_code \"dexterity\"})"
  [db skill]
  (m/assert SkillEntity skill)
  (qh/execute! db {:insert-into :skills
                   :values [skill]}))

(defn update-skill
  "Update an existing skill by code.
   
   Updates skill data while preserving code and created_at timestamp.
   
   Args:
     db    - Database connection or datasource
     skill - Skill map with :code and fields to update
   
   Returns:
     Result map with :next.jdbc/update-count
   
   Example:
     (update-skill db {:code \"acrobatics\" :description \"Updated description\"})"
  [db skill]
  (m/assert UpdateParams skill)
  (let [{:keys [code]} skill]
    (qh/execute! db {:update :skills
                     :set (dissoc skill :code :created_at)
                     :where [:= :code code]})))

(defn delete
  "Delete a skill by code.
   
   Removes a skill from the database. Note: This may affect character_skills
   records due to foreign key constraints.
   
   Args:
     db     - Database connection or datasource
     params - Map with :code key
   
   Returns:
     Result map with :next.jdbc/update-count
   
   Example:
     (delete db {:code \"acrobatics\"})"
  [db params]
  (m/assert GetByCodeParams params)
  (let [{:keys [code]} params]
    (qh/execute! db {:delete-from :skills
                     :where [:= :code code]})))
