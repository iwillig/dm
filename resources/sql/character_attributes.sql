-- Character attributes queries

-- :name get-character-attributes :? :*
-- :doc Get all attributes for a character
SELECT ca.*, an.name as attribute_name, an.abbreviation
FROM character_attributes ca
JOIN attribute_names an ON ca.attribute_code = an.code
WHERE ca.character_id = :character_id
ORDER BY an.display_order;

-- :name get-character-attribute :? :1
-- :doc Get a specific attribute for a character
SELECT ca.*, an.name as attribute_name, an.abbreviation
FROM character_attributes ca
JOIN attribute_names an ON ca.attribute_code = an.code
WHERE ca.character_id = :character_id AND ca.attribute_code = :attribute_code;

-- :name set-character-attribute! :! :n
-- :doc Set or update a character attribute value
INSERT INTO character_attributes (character_id, attribute_code, attribute_value)
VALUES (:character_id, :attribute_code, :attribute_value)
ON CONFLICT(character_id, attribute_code)
DO UPDATE SET attribute_value = :attribute_value;

-- :name delete-character-attribute! :! :n
-- :doc Delete a character attribute
DELETE FROM character_attributes
WHERE character_id = :character_id AND attribute_code = :attribute_code;

-- :name delete-all-character-attributes! :! :n
-- :doc Delete all attributes for a character
DELETE FROM character_attributes WHERE character_id = :character_id;
