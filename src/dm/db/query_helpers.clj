(ns dm.db.query-helpers
  "Shared query helper functions used by all table-specific query namespaces.
   
   This module provides the core functions for executing SQL via next.jdbc,
   abstracting away the details of HoneySQL formatting and result mapping."
  (:require
   [honey.sql :as sql]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

(defn query
  "Execute a SELECT query returning multiple rows.
   
   Args:
     db       - Database connection or datasource
     sql-map  - HoneySQL map representing the query
   
   Returns:
     Vector of maps (one per row), with snake_case keys converted to lowercase.
     Returns empty vector if no results.
   
   Example:
     (query db {:select [:*] :from [:species] :order-by [:name]})"
  [db sql-map]
  (jdbc/execute! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps}))

(defn query-one
  "Execute a SELECT query returning at most one row.
   
   Args:
     db       - Database connection or datasource
     sql-map  - HoneySQL map representing the query
   
   Returns:
     Single row as a map with lowercase keys, or nil if no result found.
   
   Example:
     (query-one db {:select [:*] :from [:species] :where [:= :code \"elf\"]})"
  [db sql-map]
  (jdbc/execute-one! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps}))

(defn execute!
  "Execute a mutation (INSERT/UPDATE/DELETE) returning affected row count.
   
   Args:
     db       - Database connection or datasource
     sql-map  - HoneySQL map representing the mutation
   
   Returns:
     Result map containing :next.jdbc/update-count key with number of rows affected.
   
   Example:
     (execute! db {:delete-from :species :where [:= :code \"elf\"]})"
  [db sql-map]
  (jdbc/execute-one! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps}))

(defn insert-returning!
  "Execute an INSERT returning the inserted row(s) with generated keys.
   
   Args:
     db       - Database connection or datasource
     sql-map  - HoneySQL map with RETURNING clause
   
   Returns:
     Inserted row as a map with lowercase keys, including auto-generated ID.
   
   Example:
     (insert-returning! db {:insert-into :items
                            :values [{:name \"Sword\"}]
                            :returning [:*]})"
  [db sql-map]
  (jdbc/execute-one! db
                     (sql/format sql-map)
                     {:return-keys true
                      :builder-fn rs/as-unqualified-lower-maps}))
