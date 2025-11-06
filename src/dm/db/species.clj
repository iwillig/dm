(ns dm.db.species
  "Species enumeration table queries.
   
   Provides CRUD operations for the species table, which contains D&D 5e species
   (races) with their game mechanics and descriptions."
  (:require
   [dm.db.query-helpers :as qh]
   [malli.core :as m]))

;; ============================================
;; Malli Schemas
;; ============================================

(def Code
  "String code identifier (primary key)."
  [:string {:min 1}])

(def SpeciesEntity
  "Species entity schema."
  [:map
   [:code Code]
   [:name :string]
   [:description {:optional true} [:maybe :string]]
   [:size {:optional true} [:maybe :string]]
   [:speed {:optional true} [:maybe :int]]
   [:special_traits {:optional true} [:maybe :string]]
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
   [:size {:optional true} [:maybe :string]]
   [:speed {:optional true} [:maybe :int]]
   [:special_traits {:optional true} [:maybe :string]]])

;; ============================================
;; CRUD Functions
;; ============================================

(defn get-all
  "Get all species ordered by name.
   
   Returns a collection of all species from the database.
   
   Args:
     db      - Database connection or datasource
     _params - Ignored (included for consistent handler signature)
   
   Returns:
     Vector of species maps"
  [db _params]
  (qh/query db {:select [:*]
                :from [:species]
                :order-by [:name]}))

(defn get-by-code
  "Get a species by its code.
   
   Retrieves a single species by its primary key (code).
   
   Args:
     db     - Database connection or datasource
     params - Map with :code key (e.g., \"elf\", \"human\")
   
   Returns:
     Species map or nil if not found"
  [db params]
  (m/assert GetByCodeParams params)
  (let [{:keys [code]} params]
    (qh/query-one db {:select [:*]
                      :from [:species]
                      :where [:= :code code]})))

(defn insert
  "Insert a new species.
   
   Adds a new species to the database.
   
   Args:
     db      - Database connection or datasource
     species - Species map with required keys: code, name
               Optional keys: description, size, speed, special_traits
   
   Returns:
     Result map with :next.jdbc/update-count
   
   Example:
     (insert db {:code \"elf\" :name \"Elf\" :description \"Graceful humanoids\"})"
  [db species]
  (m/assert SpeciesEntity species)
  (qh/execute! db {:insert-into :species
                   :values [species]}))

(defn update-species
  "Update an existing species by code.
   
   Updates species data while preserving code and created_at timestamp.
   
   Args:
     db      - Database connection or datasource
     species - Species map with :code and fields to update
   
   Returns:
     Result map with :next.jdbc/update-count
   
   Example:
     (update-species db {:code \"elf\" :description \"Updated description\"})"
  [db species]
  (m/assert UpdateParams species)
  (let [{:keys [code]} species]
    (qh/execute! db {:update :species
                     :set (dissoc species :code :created_at)
                     :where [:= :code code]})))

(defn delete
  "Delete a species by code.
   
   Removes a species from the database. Note: This will cascade delete any
   characters associated with this species due to foreign key constraints.
   
   Args:
     db     - Database connection or datasource
     params - Map with :code key
   
   Returns:
     Result map with :next.jdbc/update-count
   
   Example:
     (delete db {:code \"elf\"})"
  [db params]
  (m/assert GetByCodeParams params)
  (let [{:keys [code]} params]
    (qh/execute! db {:delete-from :species
                     :where [:= :code code]})))

;; ============================================
;; Legacy Function Names (for backward compatibility)
;; ============================================

(def get-all-species get-all)
(def get-species-by-code get-by-code)
(def insert-species! insert)
(def update-species! update-species)
(def delete-species! delete)
