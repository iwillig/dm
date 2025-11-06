-- Species queries

-- :name get-all-species :? :*
-- :doc Get all species
SELECT * FROM species ORDER BY name;

-- :name get-species-by-code :? :1
-- :doc Get a species by its code
SELECT * FROM species WHERE code = :code;

-- :name insert-species! :! :n
-- :doc Insert a new species
INSERT INTO species (code, name, description, size, speed, special_traits)
VALUES (:code, :name, :description, :size, :speed, :special_traits);

-- :name update-species! :! :n
-- :doc Update an existing species
UPDATE species
SET name = :name,
    description = :description,
    size = :size,
    speed = :speed,
    special_traits = :special_traits
WHERE code = :code;

-- :name delete-species! :! :n
-- :doc Delete a species by code
DELETE FROM species WHERE code = :code;
