(ns dm.db.class
  "Class enumeration table queries.
   
   Provides CRUD operations for the classes table, which contains D&D 5e classes
   (fighter, wizard, etc.) with their game mechanics and descriptions."
  (:require
   [dm.db.query-helpers :as qh]
   [malli.core :as m]))

;; ============================================
;; Malli Schemas
;; ============================================

(def Code
  "String code identifier (primary key)."
  [:string {:min 1}])

(def ClassEntity
  "Class entity schema."
  [:map
   [:code Code]
   [:name :string]
   [:description {:optional true} [:maybe :string]]
   [:hit_die {:optional true} [:maybe :int]]
   [:primary_ability {:optional true} [:maybe :string]]
   [:saving_throw_proficiencies {:optional true} [:maybe :string]]
   [:armor_proficiencies {:optional true} [:maybe :string]]
   [:weapon_proficiencies {:optional true} [:maybe :string]]
   [:created_at {:optional true} [:maybe :string]]])

(def GetByCodeParams
  "Parameters for get-by-code function."
  [:map [:code Code]])

(def UpdateParams
  "Parameters for update function."
  [:map
   [:code Code]
   [:name {:optional true} :string]
   [:description {:optional true} [:maybe :string]]
   [:hit_die {:optional true} [:maybe :int]]
   [:primary_ability {:optional true} [:maybe :string]]
   [:saving_throw_proficiencies {:optional true} [:maybe :string]]
   [:armor_proficiencies {:optional true} [:maybe :string]]
   [:weapon_proficiencies {:optional true} [:maybe :string]]])

;; ============================================
;; CRUD Functions
;; ============================================

(defn get-all
  "Get all classes ordered by name.
   
   Returns a collection of all classes from the database.
   
   Args:
     db      - Database connection or datasource
     _params - Ignored (included for consistent handler signature)
   
   Returns:
     Vector of class maps"
  [db _params]
  (qh/query db {:select [:*]
                :from [:classes]
                :order-by [:name]}))

(defn get-by-code
  "Get a class by its code.
   
   Retrieves a single class by its primary key (code).
   
   Args:
     db     - Database connection or datasource
     params - Map with :code key (e.g., \"wizard\", \"fighter\")
   
   Returns:
     Class map or nil if not found"
  [db params]
  (m/assert GetByCodeParams params)
  (let [{:keys [code]} params]
    (qh/query-one db {:select [:*]
                      :from [:classes]
                      :where [:= :code code]})))

(defn insert
  "Insert a new class.
   
   Adds a new class to the database.
   
   Args:
     db         - Database connection or datasource
     class-data - Class map with required keys: code, name
                  Optional keys: description, hit_die, primary_ability,
                  saving_throw_proficiencies, armor_proficiencies,
                  weapon_proficiencies
   
   Returns:
     Result map with :next.jdbc/update-count
   
   Example:
     (insert db {:code \"wizard\" :name \"Wizard\" :hit_die 6})"
  [db class-data]
  (m/assert ClassEntity class-data)
  (qh/execute! db {:insert-into :classes
                   :values [class-data]}))

(defn update-class
  "Update an existing class by code.
   
   Updates class data while preserving code and created_at timestamp.
   
   Args:
     db         - Database connection or datasource
     class-data - Class map with :code and fields to update
   
   Returns:
     Result map with :next.jdbc/update-count
   
   Example:
     (update-class db {:code \"wizard\" :description \"Updated description\"})"
  [db class-data]
  (m/assert UpdateParams class-data)
  (let [{:keys [code]} class-data]
    (qh/execute! db {:update :classes
                     :set (dissoc class-data :code :created_at)
                     :where [:= :code code]})))

(defn delete
  "Delete a class by code.
   
   Removes a class from the database. Note: This will cascade delete any
   characters associated with this class due to foreign key constraints.
   
   Args:
     db     - Database connection or datasource
     params - Map with :code key
   
   Returns:
     Result map with :next.jdbc/update-count
   
   Example:
     (delete db {:code \"wizard\"})"
  [db params]
  (m/assert GetByCodeParams params)
  (let [{:keys [code]} params]
    (qh/execute! db {:delete-from :classes
                     :where [:= :code code]})))
