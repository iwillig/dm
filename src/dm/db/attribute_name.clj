(ns dm.db.attribute-name
  "Attribute name enumeration table queries.
   
   Provides CRUD operations for the attribute_names table, which contains D&D 5e
   attributes (strength, dexterity, etc.) with their metadata."
  (:require
   [dm.db.query-helpers :as qh]
   [malli.core :as m]))

;; ============================================
;; Malli Schemas
;; ============================================

(def Code
  "String code identifier (primary key)."
  [:string {:min 1}])

(def AttributeNameEntity
  "Attribute name entity schema."
  [:map
   [:code Code]
   [:name :string]
   [:abbreviation {:optional true} [:maybe :string]]
   [:description {:optional true} [:maybe :string]]
   [:display_order {:optional true} [:maybe :int]]
   [:created_at {:optional true} [:maybe :string]]])

(def GetByCodeParams
  "Parameters for get-by-code function."
  [:map [:code Code]])

(def UpdateParams
  "Parameters for update function."
  [:map
   [:code Code]
   [:name {:optional true} :string]
   [:abbreviation {:optional true} [:maybe :string]]
   [:description {:optional true} [:maybe :string]]
   [:display_order {:optional true} [:maybe :int]]])

;; ============================================
;; CRUD Functions
;; ============================================

(defn get-all
  "Get all attribute names ordered by display_order.
   
   Returns a collection of all attribute names from the database.
   
   Args:
     db      - Database connection or datasource
     _params - Ignored (included for consistent handler signature)
   
   Returns:
     Vector of attribute name maps"
  [db _params]
  (qh/query db {:select [:*]
                :from [:attribute_names]
                :order-by [:display_order]}))

(defn get-by-code
  "Get an attribute name by its code.
   
   Retrieves a single attribute name by its primary key (code).
   
   Args:
     db     - Database connection or datasource
     params - Map with :code key (e.g., \"strength\", \"dexterity\")
   
   Returns:
     Attribute name map or nil if not found"
  [db params]
  (m/assert GetByCodeParams params)
  (let [{:keys [code]} params]
    (qh/query-one db {:select [:*]
                      :from [:attribute_names]
                      :where [:= :code code]})))

(defn insert
  "Insert a new attribute name.
   
   Adds a new attribute name to the database.
   
   Args:
     db             - Database connection or datasource
     attribute-name - Attribute name map with required keys: code, name
                      Optional keys: abbreviation, description, display_order
   
   Returns:
     Result map with :next.jdbc/update-count
   
   Example:
     (insert db {:code \"strength\" :name \"Strength\" :abbreviation \"STR\"})"
  [db attribute-name]
  (m/assert AttributeNameEntity attribute-name)
  (qh/execute! db {:insert-into :attribute_names
                   :values [attribute-name]}))

(defn update-attribute-name
  "Update an existing attribute name by code.
   
   Updates attribute name data while preserving code and created_at timestamp.
   
   Args:
     db             - Database connection or datasource
     attribute-name - Attribute name map with :code and fields to update
   
   Returns:
     Result map with :next.jdbc/update-count
   
   Example:
     (update-attribute-name db {:code \"strength\" :description \"Updated description\"})"
  [db attribute-name]
  (m/assert UpdateParams attribute-name)
  (let [{:keys [code]} attribute-name]
    (qh/execute! db {:update :attribute_names
                     :set (dissoc attribute-name :code :created_at)
                     :where [:= :code code]})))

(defn delete
  "Delete an attribute name by code.
   
   Removes an attribute name from the database. Note: This may affect skills
   and character_attributes records due to foreign key constraints.
   
   Args:
     db     - Database connection or datasource
     params - Map with :code key
   
   Returns:
     Result map with :next.jdbc/update-count
   
   Example:
     (delete db {:code \"strength\"})"
  [db params]
  (m/assert GetByCodeParams params)
  (let [{:keys [code]} params]
    (qh/execute! db {:delete-from :attribute_names
                     :where [:= :code code]})))
