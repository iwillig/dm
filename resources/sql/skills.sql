-- Skills queries

-- :name get-all-skills :? :*
-- :doc Get all skills
SELECT * FROM skills ORDER BY name;

-- :name get-skill-by-code :? :1
-- :doc Get a skill by its code
SELECT * FROM skills WHERE code = :code;

-- :name get-skills-by-attribute :? :*
-- :doc Get all skills for a specific attribute
SELECT * FROM skills WHERE attribute_code = :attribute_code ORDER BY name;

-- :name insert-skill! :! :n
-- :doc Insert a new skill
INSERT INTO skills (code, name, attribute_code, description)
VALUES (:code, :name, :attribute_code, :description);

-- :name update-skill! :! :n
-- :doc Update an existing skill
UPDATE skills
SET name = :name,
    attribute_code = :attribute_code,
    description = :description
WHERE code = :code;

-- :name delete-skill! :! :n
-- :doc Delete a skill by code
DELETE FROM skills WHERE code = :code;
