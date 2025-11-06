-- Attribute names queries

-- :name get-all-attribute-names :? :*
-- :doc Get all attribute names
SELECT * FROM attribute_names ORDER BY display_order;

-- :name get-attribute-name-by-code :? :1
-- :doc Get an attribute name by its code
SELECT * FROM attribute_names WHERE code = :code;

-- :name insert-attribute-name! :! :n
-- :doc Insert a new attribute name
INSERT INTO attribute_names (code, name, abbreviation, description, display_order)
VALUES (:code, :name, :abbreviation, :description, :display_order);

-- :name update-attribute-name! :! :n
-- :doc Update an existing attribute name
UPDATE attribute_names
SET name = :name,
    abbreviation = :abbreviation,
    description = :description,
    display_order = :display_order
WHERE code = :code;

-- :name delete-attribute-name! :! :n
-- :doc Delete an attribute name by code
DELETE FROM attribute_names WHERE code = :code;
