(ns dm.db.character
  "Character and character-related table queries.

   Provides CRUD operations for characters, character_attributes, and
   character_skills tables in D&D 5e campaigns."
  (:require
   [dm.db.query-helpers :as qh]
   [malli.core :as m]))

;; ============================================
;; Malli Schemas - Characters
;; ============================================

(def CharacterEntity
  "Character entity schema."
  [:map
   [:id {:optional true} [:maybe :int]]
   [:name :string]
   [:species_code :string]
   [:class_code :string]
   [:armor_class {:optional true} [:maybe :int]]
   [:inspiration {:optional true} [:maybe :boolean]]
   [:level {:optional true} [:maybe :int]]
   [:hit_points_max {:optional true} [:maybe :int]]
   [:hit_points_current {:optional true} [:maybe :int]]
   [:created_at {:optional true} [:maybe :string]]
   [:updated_at {:optional true} [:maybe :string]]])

(def GetByIdParams
  "Parameters for get-by-id function."
  [:map [:id :int]])

(def UpdateCharacterParams
  "Parameters for update function."
  [:map
   [:id :int]
   [:name {:optional true} :string]
   [:species_code {:optional true} :string]
   [:class_code {:optional true} :string]
   [:armor_class {:optional true} [:maybe :int]]
   [:inspiration {:optional true} [:maybe :boolean]]
   [:level {:optional true} [:maybe :int]]
   [:hit_points_max {:optional true} [:maybe :int]]
   [:hit_points_current {:optional true} [:maybe :int]]])

;; ============================================
;; Malli Schemas - Character Attributes
;; ============================================

(def GetAttributesParams
  "Parameters for get-attributes function."
  [:map [:character_id :int]])

(def GetAttributeParams
  "Parameters for get-attribute function."
  [:map
   [:character_id :int]
   [:attribute_code :string]])

(def SetAttributeParams
  "Parameters for set-attribute function."
  [:map
   [:character_id :int]
   [:attribute_code :string]
   [:attribute_value :int]])

;; ============================================
;; Malli Schemas - Character Skills
;; ============================================

(def GetSkillsParams
  "Parameters for get-skills function."
  [:map [:character_id :int]])

(def GetSkillParams
  "Parameters for get-skill function."
  [:map
   [:character_id :int]
   [:skill_code :string]])

(def AddSkillParams
  "Parameters for add-skill function."
  [:map
   [:character_id :int]
   [:skill_code :string]
   [:proficiency_level :int]])

(def UpdateSkillProficiencyParams
  "Parameters for update-skill-proficiency function."
  [:map
   [:character_id :int]
   [:skill_code :string]
   [:proficiency_level :int]])

(def RemoveSkillParams
  "Parameters for remove-skill function."
  [:map
   [:character_id :int]
   [:skill_code :string]])

;; ============================================
;; Character CRUD Functions
;; ============================================

(defn get-all
  "Get all characters ordered by name.

   Returns a collection of all characters from the database.

   Args:
     db      - Database connection or datasource
     _params - Ignored (included for consistent handler signature)

   Returns:
     Vector of character maps"
  [db _params]
  (qh/query db {:select [:*]
                :from [:characters]
                :order-by [:name]}))

(defn get-by-id
  "Get a character by ID.

   Retrieves a single character by its primary key (id).

   Args:
     db     - Database connection or datasource
     params - Map with :id key

   Returns:
     Character map or nil if not found"
  [db params]
  (m/assert GetByIdParams params)
  (let [{:keys [id]} params]
    (qh/query-one db {:select [:*]
                      :from [:characters]
                      :where [:= :id id]})))

(defn get-with-details
  "Get a character with species and class details.

   Retrieves a character with joined species_name and class_name.

   Args:
     db     - Database connection or datasource
     params - Map with :id key

   Returns:
     Character map with :species_name and :class_name, or nil if not found"
  [db params]
  (m/assert GetByIdParams params)
  (let [{:keys [id]} params]
    (qh/query-one db {:select [:c.*
                               [:s.name :species_name]
                               [:cl.name :class_name]]
                      :from [[:characters :c]]
                      :left-join [[:species :s] [:= :c.species_code :s.code]
                                  [:classes :cl] [:= :c.class_code :cl.code]]
                      :where [:= :c.id id]})))

(defn insert
  "Insert a new character and return it with generated ID.

   Adds a new character to the database.

   Args:
     db        - Database connection or datasource
     character - Character map with required keys: name, species_code, class_code
                 Optional keys: armor_class, inspiration, level, hit_points_max,
                 hit_points_current

   Returns:
     Inserted character map with generated :id

   Example:
     (insert db {:name \"Conan\" :species_code \"human\" :class_code \"fighter\"})"
  [db character]
  (m/assert CharacterEntity character)
  (qh/insert-returning! db {:insert-into :characters
                            :values [character]
                            :returning [:*]}))

(defn update-character
  "Update an existing character by ID.

   Updates character data while preserving id and created_at timestamp.
   Updated_at is automatically set to current timestamp.

   Args:
     db        - Database connection or datasource
     character - Character map with :id and fields to update

   Returns:
     Result map with :next.jdbc/update-count

   Example:
     (update-character db {:id 1 :name \"Conan the Barbarian\" :level 6})"
  [db character]
  (m/assert UpdateCharacterParams character)
  (let [{:keys [id]} character]
    (qh/execute! db {:update :characters
                     :set (-> character
                              (dissoc :id :created_at)
                              (assoc :updated_at [:raw "CURRENT_TIMESTAMP"]))
                     :where [:= :id id]})))

(defn delete
  "Delete a character by ID.

   Removes a character from the database. This will cascade delete all
   character_attributes and character_skills due to foreign key constraints.

   Args:
     db     - Database connection or datasource
     params - Map with :id key

   Returns:
     Result map with :next.jdbc/update-count

   Example:
     (delete db {:id 1})"
  [db params]
  (m/assert GetByIdParams params)
  (let [{:keys [id]} params]
    (qh/execute! db {:delete-from :characters
                     :where [:= :id id]})))

;; ============================================
;; Character Attributes Functions
;; ============================================

(defn get-attributes
  "Get all attributes for a character with attribute details.

   Returns character attributes joined with attribute_names table.

   Args:
     db     - Database connection or datasource
     params - Map with :character_id key

   Returns:
     Vector of attribute maps with :attribute_name and :abbreviation"
  [db params]
  (m/assert GetAttributesParams params)
  (let [{:keys [character_id]} params]
    (qh/query db {:select [:ca.*
                           [:an.name :attribute_name]
                           :an.abbreviation]
                  :from [[:character_attributes :ca]]
                  :join [[:attribute_names :an] [:= :ca.attribute_code :an.code]]
                  :where [:= :ca.character_id character_id]
                  :order-by [:an.display_order]})))

(defn get-attribute
  "Get a specific attribute for a character.

   Returns a single character attribute with details.

   Args:
     db     - Database connection or datasource
     params - Map with :character_id and :attribute_code keys

   Returns:
     Attribute map with :attribute_name and :abbreviation, or nil if not found"
  [db params]
  (m/assert GetAttributeParams params)
  (let [{:keys [character_id attribute_code]} params]
    (qh/query-one db {:select [:ca.*
                               [:an.name :attribute_name]
                               :an.abbreviation]
                      :from [[:character_attributes :ca]]
                      :join [[:attribute_names :an] [:= :ca.attribute_code :an.code]]
                      :where [:and
                              [:= :ca.character_id character_id]
                              [:= :ca.attribute_code attribute_code]]})))

(defn set-attribute
  "Set or update a character attribute value (upsert).

   Creates or updates a character attribute. Uses ON CONFLICT to handle upserts.

   Args:
     db     - Database connection or datasource
     params - Map with :character_id, :attribute_code, and :attribute_value keys

   Returns:
     Result map with :next.jdbc/update-count

   Example:
     (set-attribute db {:character_id 1 :attribute_code \"strength\" :attribute_value 16})"
  [db params]
  (m/assert SetAttributeParams params)
  (let [{:keys [character_id attribute_code attribute_value]} params]
    (qh/execute! db {:insert-into :character_attributes
                     :values [{:character_id character_id
                               :attribute_code attribute_code
                               :attribute_value attribute_value}]
                     :on-conflict [:character_id :attribute_code]
                     :do-update-set [:attribute_value]})))

(defn delete-attribute
  "Delete a specific character attribute.

   Removes a single attribute from a character.

   Args:
     db     - Database connection or datasource
     params - Map with :character_id and :attribute_code keys

   Returns:
     Result map with :next.jdbc/update-count

   Example:
     (delete-attribute db {:character_id 1 :attribute_code \"strength\"})"
  [db params]
  (m/assert GetAttributeParams params)
  (let [{:keys [character_id attribute_code]} params]
    (qh/execute! db {:delete-from :character_attributes
                     :where [:and
                             [:= :character_id character_id]
                             [:= :attribute_code attribute_code]]})))

(defn delete-all-attributes
  "Delete all attributes for a character.

   Removes all character_attributes records for a given character.

   Args:
     db     - Database connection or datasource
     params - Map with :character_id key

   Returns:
     Result map with :next.jdbc/update-count

   Example:
     (delete-all-attributes db {:character_id 1})"
  [db params]
  (m/assert GetAttributesParams params)
  (let [{:keys [character_id]} params]
    (qh/execute! db {:delete-from :character_attributes
                     :where [:= :character_id character_id]})))

;; ============================================
;; Character Skills Functions
;; ============================================

(defn get-skills
  "Get all skills for a character with skill details.

   Returns character skills joined with skills table.

   Args:
     db     - Database connection or datasource
     params - Map with :character_id key

   Returns:
     Vector of skill maps with :skill_name and :attribute_code"
  [db params]
  (m/assert GetSkillsParams params)
  (let [{:keys [character_id]} params]
    (qh/query db {:select [:cs.*
                           [:s.name :skill_name]
                           :s.attribute_code]
                  :from [[:character_skills :cs]]
                  :join [[:skills :s] [:= :cs.skill_code :s.code]]
                  :where [:= :cs.character_id character_id]
                  :order-by [:s.name]})))

(defn get-skill
  "Get a specific skill for a character.

   Returns a single character skill with details.

   Args:
     db     - Database connection or datasource
     params - Map with :character_id and :skill_code keys

   Returns:
     Skill map with :skill_name and :attribute_code, or nil if not found"
  [db params]
  (m/assert GetSkillParams params)
  (let [{:keys [character_id skill_code]} params]
    (qh/query-one db {:select [:cs.*
                               [:s.name :skill_name]
                               :s.attribute_code]
                      :from [[:character_skills :cs]]
                      :join [[:skills :s] [:= :cs.skill_code :s.code]]
                      :where [:and
                              [:= :cs.character_id character_id]
                              [:= :cs.skill_code skill_code]]})))

(defn add-skill
  "Add a skill proficiency to a character.

   Creates a new character_skills record.

   Args:
     db     - Database connection or datasource
     params - Map with :character_id, :skill_code, and :proficiency_level keys

   Returns:
     Result map with :next.jdbc/update-count

   Example:
     (add-skill db {:character_id 1 :skill_code \"stealth\" :proficiency_level 1})"
  [db params]
  (m/assert AddSkillParams params)
  (let [{:keys [character_id skill_code proficiency_level]} params]
    (qh/execute! db {:insert-into :character_skills
                     :values [{:character_id character_id
                               :skill_code skill_code
                               :proficiency_level proficiency_level}]})))

(defn update-skill-proficiency
  "Update a character's skill proficiency level.

   Updates the proficiency_level for an existing character skill.

   Args:
     db     - Database connection or datasource
     params - Map with :character_id, :skill_code, and :proficiency_level keys

   Returns:
     Result map with :next.jdbc/update-count

   Example:
     (update-skill-proficiency db {:character_id 1 :skill_code \"stealth\" :proficiency_level 2})"
  [db params]
  (m/assert UpdateSkillProficiencyParams params)
  (let [{:keys [character_id skill_code proficiency_level]} params]
    (qh/execute! db {:update :character_skills
                     :set {:proficiency_level proficiency_level}
                     :where [:and
                             [:= :character_id character_id]
                             [:= :skill_code skill_code]]})))

(defn remove-skill
  "Remove a skill from a character.

   Deletes a character_skills record.

   Args:
     db     - Database connection or datasource
     params - Map with :character_id and :skill_code keys

   Returns:
     Result map with :next.jdbc/update-count

   Example:
     (remove-skill db {:character_id 1 :skill_code \"stealth\"})"
  [db params]
  (m/assert RemoveSkillParams params)
  (let [{:keys [character_id skill_code]} params]
    (qh/execute! db {:delete-from :character_skills
                     :where [:and
                             [:= :character_id character_id]
                             [:= :skill_code skill_code]]})))

(defn remove-all-skills
  "Remove all skills from a character.

   Deletes all character_skills records for a given character.

   Args:
     db     - Database connection or datasource
     params - Map with :character_id key

   Returns:
     Result map with :next.jdbc/update-count

   Example:
     (remove-all-skills db {:character_id 1})"
  [db params]
  (m/assert GetSkillsParams params)
  (let [{:keys [character_id]} params]
    (qh/execute! db {:delete-from :character_skills
                     :where [:= :character_id character_id]})))
