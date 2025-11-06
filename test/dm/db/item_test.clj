(ns dm.db.item-test
  (:require [clojure.test :as t]
            [dm.db.item :as item]
            [next.jdbc :as jdbc]
            [matcher-combinators.test]))

(def test-db-spec "jdbc:sqlite:test-item.db")

(defn test-datasource []
  (jdbc/get-datasource test-db-spec))

(defn setup-test-db! [db]
  (try (jdbc/execute! db ["DROP TABLE IF EXISTS items"]) (catch Exception _))
  
  (jdbc/execute! db ["CREATE TABLE items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    description TEXT,
    item_type TEXT,
    quantity INTEGER DEFAULT 1,
    weight REAL,
    value_copper INTEGER,
    properties TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
  )"]))

(t/deftest test-item-get-all
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an empty items table"
      (t/testing "When: we get all items"
        (let [result (item/get-all db {})]
          (t/testing "Then: it returns an empty collection"
            (t/is (empty? result))))))
    
    (t/testing "Given: a table with multiple items"
      (item/insert db {:name "Longsword" :item_type "weapon"})
      (item/insert db {:name "Shield" :item_type "armor"})
      
      (t/testing "When: we get all items"
        (let [result (item/get-all db {})]
          (t/testing "Then: it returns all items ordered by name"
            (t/is (= 2 (count result)))
            (t/is (match? [{:name "Longsword"} {:name "Shield"}] result))))))))

(t/deftest test-item-get-by-id
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an item exists in the database"
      (let [inserted (item/insert db {:name "Longsword"
                                      :description "A versatile blade"
                                      :item_type "weapon"
                                      :value_copper 1500
                                      :weight 3.0})]
        
        (t/testing "When: we get the item by ID"
          (let [result (item/get-by-id db {:id (:id inserted)})]
            (t/testing "Then: it returns the item"
              (t/is (match? {:id (:id inserted)
                             :name "Longsword"
                             :item_type "weapon"
                             :value_copper 1500}
                            result)))))))
    
    (t/testing "Given: an item does not exist"
      (t/testing "When: we try to get it by ID"
        (let [result (item/get-by-id db {:id 999})]
          (t/testing "Then: it returns nil"
            (t/is (nil? result))))))))

(t/deftest test-item-get-by-type
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: items of different types"
      (item/insert db {:name "Longsword" :item_type "weapon"})
      (item/insert db {:name "Dagger" :item_type "weapon"})
      (item/insert db {:name "Shield" :item_type "armor"})
      
      (t/testing "When: we get items by type"
        (let [result (item/get-by-type db {:item_type "weapon"})]
          (t/testing "Then: it returns only items of that type"
            (t/is (= 2 (count result)))
            (t/is (every? #(= "weapon" (:item_type %)) result))))))))

(t/deftest test-item-insert
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: valid item data"
      (t/testing "When: we insert an item"
        (let [result (item/insert db {:name "Healing Potion"
                                      :description "Restores health"
                                      :item_type "consumable"
                                      :quantity 3
                                      :value_copper 500})]
          
          (t/testing "Then: it returns the item with generated ID"
            (t/is (match? {:id pos-int?
                           :name "Healing Potion"
                           :item_type "consumable"
                           :quantity 3}
                          result))))))))

(t/deftest test-item-update
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an existing item"
      (let [inserted (item/insert db {:name "Longsword" :value_copper 1500})]
        
        (t/testing "When: we update the item"
          (item/update-item db {:id (:id inserted)
                                :name "Magical Longsword"
                                :value_copper 5000
                                :description "Imbued with magic"})
          
          (t/testing "Then: the changes are persisted"
            (let [result (item/get-by-id db {:id (:id inserted)})]
              (t/is (match? {:id (:id inserted)
                             :name "Magical Longsword"
                             :value_copper 5000
                             :description "Imbued with magic"}
                            result)))))))))

(t/deftest test-item-delete
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: an existing item"
      (let [inserted (item/insert db {:name "Longsword"})]
        
        (t/testing "When: we delete the item"
          (item/delete db {:id (:id inserted)})
          
          (t/testing "Then: the item no longer exists"
            (let [result (item/get-by-id db {:id (:id inserted)})]
              (t/is (nil? result)))))))))

(t/deftest test-item-malli-validation
  (let [db (test-datasource)]
    (setup-test-db! db)
    
    (t/testing "Given: invalid data - missing required field 'name'"
      (t/testing "When: we try to insert"
        (t/testing "Then: it throws a validation error"
          (t/is (thrown? Exception
                         (item/insert db {:item_type "weapon"}))))))
    
    (t/testing "Given: invalid params - missing 'id' key"
      (t/testing "When: we try to get by ID"
        (t/testing "Then: it throws a validation error"
          (t/is (thrown? Exception
                         (item/get-by-id db {:wrong-key 1}))))))))
