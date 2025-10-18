(ns dm.db)

(def schema
  {:character/name {:db/valueType   :db.type/string}
   :character/skill {:db/valueType   :db.type/ref
                     :db/cardinality :db.cardinality/many}
   :character/class {:db/valueType   :db.type/ref}
   :character/race {:db/valueType :db.type/ref}})
