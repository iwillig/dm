-- Characters queries

-- :name get-all-characters :? :*
-- :doc Get all characters
SELECT * FROM characters ORDER BY name;

-- :name get-character-by-id :? :1
-- :doc Get a character by ID
SELECT * FROM characters WHERE id = :id;

-- :name insert-character! :<! :1
-- :doc Insert a new character and return the generated ID
INSERT INTO characters (name, species_code, class_code, armor_class, inspiration,
                        level, hit_points_max, hit_points_current)
VALUES (:name, :species_code, :class_code, :armor_class, :inspiration,
        :level, :hit_points_max, :hit_points_current)
RETURNING *;

-- :name update-character! :! :n
-- :doc Update an existing character
UPDATE characters
SET name = :name,
    species_code = :species_code,
    class_code = :class_code,
    armor_class = :armor_class,
    inspiration = :inspiration,
    level = :level,
    hit_points_max = :hit_points_max,
    hit_points_current = :hit_points_current,
    updated_at = CURRENT_TIMESTAMP
WHERE id = :id;

-- :name delete-character! :! :n
-- :doc Delete a character by ID
DELETE FROM characters WHERE id = :id;

-- :name get-character-with-details :? :1
-- :doc Get a character with species and class details
SELECT c.*, s.name as species_name, cl.name as class_name
FROM characters c
LEFT JOIN species s ON c.species_code = s.code
LEFT JOIN classes cl ON c.class_code = cl.code
WHERE c.id = :id;
