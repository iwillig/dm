(ns dm.queries
  (:require
   [hugsql.adapter.next-jdbc :as next-adapter]
   [hugsql.core :as hugsql]))

;; Set the adapter to use next.jdbc
(hugsql/set-adapter! (next-adapter/hugsql-adapter-next-jdbc))

;; Load all HugSQL query definitions
(hugsql/def-db-fns "sql/species.sql")
(hugsql/def-db-fns "sql/classes.sql")
(hugsql/def-db-fns "sql/attribute_names.sql")
(hugsql/def-db-fns "sql/skills.sql")
(hugsql/def-db-fns "sql/characters.sql")
(hugsql/def-db-fns "sql/character_attributes.sql")
(hugsql/def-db-fns "sql/character_skills.sql")
(hugsql/def-db-fns "sql/items.sql")
