(ns dm.db)

(defprotocol IDB
  (get-db [self])
  (get-conn [self]))


(def species
  #{:species/elf
    :species/tiefling
    :species/human
    :species/orc
    :species/halfing
    :species/goliath
    :species/gnome
    :species/dwarf
    :species/dragonborn
    :species/aasimar})

(def classes
  #{:class/druid
    :class/barbarian
    :class/bard
    :class/cleric
    :class/fighter
    :class/monk
    :class/paladin
    :class/ranger
    :class/rouge
    :class/sorcerer
    :class/warlock
    :class/wizard})

(def schema
  {:attribute/name  {:db/valueType :db.type/keyword}
   :attribute/value {:db/valueType :db.type/long}

   :item/name   {:db/valueType :db.type/keyword}
   :item/number {:db/valueType :db.type/long}

   :character/name        {:db/valueType :db.type/string}
   :character/armor-class {:db/valueType :db.type/long}
   :character/inspriation {:db/valueType :db.type/boolean}

   :character/skill {:db/valueType   :db.type/ref
                     :db/cardinality :db.cardinality/many}

   :character/class   {:db/valueType :db.type/ref}
   :character/species {:db/valueType :db.type/ref}

   :character/attributes {:db/valueType   :db.type/ref
                          :db/cardinality :db.cardinality/many}})

(comment

  {:character/name       "Varis"
   :character/species     :species/elf
   :character/class       :class/druid
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
                            :attribute/value 3}}})
