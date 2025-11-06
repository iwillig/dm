-- Items queries

-- :name get-all-items :? :*
-- :doc Get all items
SELECT * FROM items ORDER BY name;

-- :name get-item-by-id :? :1
-- :doc Get an item by ID
SELECT * FROM items WHERE id = :id;

-- :name get-items-by-type :? :*
-- :doc Get all items of a specific type
SELECT * FROM items WHERE item_type = :item_type ORDER BY name;

-- :name insert-item! :<! :1
-- :doc Insert a new item and return the generated ID
INSERT INTO items (name, description, item_type, quantity, weight, value_copper, properties)
VALUES (:name, :description, :item_type, :quantity, :weight, :value_copper, :properties)
RETURNING *;

-- :name update-item! :! :n
-- :doc Update an existing item
UPDATE items
SET name = :name,
    description = :description,
    item_type = :item_type,
    quantity = :quantity,
    weight = :weight,
    value_copper = :value_copper,
    properties = :properties
WHERE id = :id;

-- :name delete-item! :! :n
-- :doc Delete an item by ID
DELETE FROM items WHERE id = :id;
