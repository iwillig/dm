(ns dm.db.item
  "Item table queries.

   Provides CRUD operations for the items table, which contains equipment,
   weapons, armor, and other items for D&D 5e campaigns."
  (:require
   [dm.db.query-helpers :as qh]
   [malli.core :as m]))

;; ============================================
;; Malli Schemas
;; ============================================

(def ItemEntity
  "Item entity schema."
  [:map
   [:id {:optional true} [:maybe :int]]
   [:name :string]
   [:description {:optional true} [:maybe :string]]
   [:item_type {:optional true} [:maybe :string]]
   [:quantity {:optional true} [:maybe :int]]
   [:weight {:optional true} [:maybe :double]]
   [:value_copper {:optional true} [:maybe :int]]
   [:properties {:optional true} [:maybe :string]]
   [:created_at {:optional true} [:maybe :string]]])

(def GetByIdParams
  "Parameters for get-by-id function."
  [:map [:id :int]])

(def GetByTypeParams
  "Parameters for get-by-type function."
  [:map [:item_type :string]])

(def UpdateParams
  "Parameters for update function."
  [:map
   [:id :int]
   [:name {:optional true} :string]
   [:description {:optional true} [:maybe :string]]
   [:item_type {:optional true} [:maybe :string]]
   [:quantity {:optional true} [:maybe :int]]
   [:weight {:optional true} [:maybe :double]]
   [:value_copper {:optional true} [:maybe :int]]
   [:properties {:optional true} [:maybe :string]]])

;; ============================================
;; CRUD Functions
;; ============================================

(defn get-all
  "Get all items ordered by name.

   Returns a collection of all items from the database.

   Args:
     db      - Database connection or datasource
     _params - Ignored (included for consistent handler signature)

   Returns:
     Vector of item maps"
  [db _params]
  (qh/query db {:select [:*]
                :from [:items]
                :order-by [:name]}))

(defn get-by-id
  "Get an item by its ID.

   Retrieves a single item by its primary key (id).

   Args:
     db     - Database connection or datasource
     params - Map with :id key

   Returns:
     Item map or nil if not found"
  [db params]
  (m/assert GetByIdParams params)
  (let [{:keys [id]} params]
    (qh/query-one db {:select [:*]
                      :from [:items]
                      :where [:= :id id]})))

(defn get-by-type
  "Get all items of a specific type.

   Retrieves all items matching the given item_type.

   Args:
     db     - Database connection or datasource
     params - Map with :item_type key (e.g., \"weapon\", \"armor\")

   Returns:
     Vector of item maps"
  [db params]
  (m/assert GetByTypeParams params)
  (let [{:keys [item_type]} params]
    (qh/query db {:select [:*]
                  :from [:items]
                  :where [:= :item_type item_type]
                  :order-by [:name]})))

(defn insert
  "Insert a new item and return it with generated ID.

   Adds a new item to the database.

   Args:
     db   - Database connection or datasource
     item - Item map with required keys: name
            Optional keys: description, item_type, quantity, weight,
            value_copper, properties

   Returns:
     Inserted item map with generated :id

   Example:
     (insert db {:name \"Longsword\" :item_type \"weapon\" :value_copper 1500})"
  [db item]
  (m/assert ItemEntity item)
  (qh/insert-returning! db {:insert-into :items
                            :values [item]
                            :returning [:*]}))

(defn update-item
  "Update an existing item by ID.

   Updates item data while preserving id and created_at timestamp.

   Args:
     db   - Database connection or datasource
     item - Item map with :id and fields to update

   Returns:
     Result map with :next.jdbc/update-count

   Example:
     (update-item db {:id 1 :name \"Magical Longsword\" :value_copper 5000})"
  [db item]
  (m/assert UpdateParams item)
  (let [{:keys [id]} item]
    (qh/execute! db {:update :items
                     :set (dissoc item :id :created_at)
                     :where [:= :id id]})))

(defn delete
  "Delete an item by ID.

   Removes an item from the database.

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
    (qh/execute! db {:delete-from :items
                     :where [:= :id id]})))
