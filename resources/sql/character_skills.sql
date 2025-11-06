-- Character skills queries

-- :name get-character-skills :? :*
-- :doc Get all skills for a character
SELECT cs.*, s.name as skill_name, s.attribute_code
FROM character_skills cs
JOIN skills s ON cs.skill_code = s.code
WHERE cs.character_id = :character_id
ORDER BY s.name;

-- :name get-character-skill :? :1
-- :doc Get a specific skill for a character
SELECT cs.*, s.name as skill_name, s.attribute_code
FROM character_skills cs
JOIN skills s ON cs.skill_code = s.code
WHERE cs.character_id = :character_id AND cs.skill_code = :skill_code;

-- :name add-character-skill! :! :n
-- :doc Add a skill proficiency to a character
INSERT INTO character_skills (character_id, skill_code, proficiency_level)
VALUES (:character_id, :skill_code, :proficiency_level);

-- :name update-skill-proficiency! :! :n
-- :doc Update a character's skill proficiency level
UPDATE character_skills
SET proficiency_level = :proficiency_level
WHERE character_id = :character_id AND skill_code = :skill_code;

-- :name remove-character-skill! :! :n
-- :doc Remove a skill proficiency from a character
DELETE FROM character_skills
WHERE character_id = :character_id AND skill_code = :skill_code;

-- :name remove-all-character-skills! :! :n
-- :doc Remove all skill proficiencies for a character
DELETE FROM character_skills WHERE character_id = :character_id;
