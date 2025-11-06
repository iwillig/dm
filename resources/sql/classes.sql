-- Classes queries

-- :name get-all-classes :? :*
-- :doc Get all classes
SELECT * FROM classes ORDER BY name;

-- :name get-class-by-code :? :1
-- :doc Get a class by its code
SELECT * FROM classes WHERE code = :code;

-- :name insert-class! :! :n
-- :doc Insert a new class
INSERT INTO classes (code, name, description, hit_die, primary_ability,
                     saving_throw_proficiencies, armor_proficiencies, weapon_proficiencies)
VALUES (:code, :name, :description, :hit_die, :primary_ability,
        :saving_throw_proficiencies, :armor_proficiencies, :weapon_proficiencies);

-- :name update-class! :! :n
-- :doc Update an existing class
UPDATE classes
SET name = :name,
    description = :description,
    hit_die = :hit_die,
    primary_ability = :primary_ability,
    saving_throw_proficiencies = :saving_throw_proficiencies,
    armor_proficiencies = :armor_proficiencies,
    weapon_proficiencies = :weapon_proficiencies
WHERE code = :code;

-- :name delete-class! :! :n
-- :doc Delete a class by code
DELETE FROM classes WHERE code = :code;
