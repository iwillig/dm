(ns dm.db)

(def schema
  {:attribute/name  {:db/valueType :db.type/keyword}
   :attribute/value {:db/valueType :db.type/long}

   :character/name        {:db/valueType :db.type/string}
   :character/armor-class {:db/valueType :db.type/long}
   :character/inspriation {:db/valueType :db.type/boolean}

   :character/skill       {:db/valueType   :db.type/ref
                           :db/cardinality :db.cardinality/many}

   :character/class {:db/valueType :db.type/ref}


   :character/race {:db/valueType :db.type/ref}

   :character/attributes {:db/valueType   :db.type/ref
                          :db/cardinality :db.cardinality/many}})


(comment

  {:character/name       "Varis"
   :character/race       :race/elf
   :character/class      :class/druid
   :character/armor-class 4
   :character/attributes #{{:attribute/name  :attribute.name/strength
                            :attribute/value 10}
                           {:attribute/name  :attribute.name/wisdom
                            :attribute/value 8}
                           {:attribute/name  :attribute.name/charisma
                            :attribute/value 7}
                           {:attribute/name  :attribute.name/intelligence
                            :attribute/value 9}
                           {:attribute/name  :attribute.name/constitution
                            :attribute/value 3}
                           {:attribute/name  :attribute.name/dexterity
                            :attribute/value 3}}}


  )
