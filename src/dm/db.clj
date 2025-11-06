(ns dm.db)

(set! *warn-on-reflection* true)

(defprotocol IDB
  "Protocol for database access"
  (get-db [self] "Returns the datasource")
  (get-conn [self] "Returns the datasource (alias for get-db)"))

;; Reference data - these codes match the enum tables in SQL
(def species-codes
  #{"elf" "tiefling" "human" "orc" "halfling"
    "goliath" "gnome" "dwarf" "dragonborn" "aasimar"})

(def class-codes
  #{"druid" "barbarian" "bard" "cleric" "fighter"
    "monk" "paladin" "ranger" "rogue" "sorcerer" "warlock" "wizard"})

(def attribute-codes
  #{"strength" "dexterity" "constitution"
    "intelligence" "wisdom" "charisma"})

(def skill-codes
  #{"athletics" "acrobatics" "sleight-of-hand" "stealth"
    "arcana" "history" "investigation" "nature" "religion"
    "animal-handling" "insight" "medicine" "perception" "survival"
    "deception" "intimidation" "performance" "persuasion"})

;; Database configuration for SQLite
(def db-config
  {:dbtype "sqlite"
   :dbname "dm.db"})
