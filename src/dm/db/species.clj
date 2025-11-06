(ns dm.db.species
  "Species enumeration table queries.
   
   Provides CRUD operations for the species table, which contains D&D 5e species
   (races) with their game mechanics and descriptions."
  (:require
   [dm.db.query-helpers :as qh]))

(defn get-all-species
  "Get all species ordered by name.
   
   Returns a collection of all species from the database.
   
   Args:
     db     - Database connection or datasource
     _params - Ignored (included for consistent handler signature)
   
   Returns:
     Vector of species maps with keys: code, name, description, size, speed, special_traits, created_at"
  [db _params]
  (qh/query db {:select [:*]
                :from [:species]
                :order-by [:name]}))

(defn get-species-by-code
  "Get a species by its code.
   
   Retrieves a single species by its primary key (code).
   
   Args:
     db    - Database connection or datasource
     params - Map with :code key (e.g., \"elf\", \"human\")
   
   Returns:
     Species map or nil if not found"
  [db {:keys [code]}]
  (qh/query-one db {:select [:*]
                    :from [:species]
                    :where [:= :code code]}))

(defn insert-species!
  "Insert a new species.
   
   Adds a new species to the database.
   
   Args:
     db      - Database connection or datasource
     species - Species map with required keys: code, name
              Optional keys: description, size, speed, special_traits
   
   Returns:
     Result map with :next.jdbc/update-count
   
   Example:
     (insert-species! db {:code \"elf\" :name \"Elf\" :description \"Graceful humanoids\"})"
  [db species]
  (qh/execute! db {:insert-into :species
                   :values [species]}))

(defn update-species!
  "Update an existing species by code.
   
   Updates species data while preserving code and created_at timestamp.
   
   Args:
     db      - Database connection or datasource
     species - Species map with :code and fields to update
   
   Returns:
     Result map with :next.jdbc/update-count
   
   Example:
     (update-species! db {:code \"elf\" :description \"Updated description\"})"
  [db {:keys [code] :as species}]
  (qh/execute! db {:update :species
                   :set (dissoc species :code :created_at)
                   :where [:= :code code]}))

(defn delete-species!
  "Delete a species by code.
   
   Removes a species from the database. Note: This will cascade delete any
   characters associated with this species due to foreign key constraints.
   
   Args:
     db    - Database connection or datasource
     params - Map with :code key
   
   Returns:
     Result map with :next.jdbc/update-count
   
   Example:
     (delete-species! db {:code \"elf\"})"
  [db {:keys [code]}]
  (qh/execute! db {:delete-from :species
                   :where [:= :code code]}))
