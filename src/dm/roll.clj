(ns dm.roll)

(defn roll
  [dx]
  (inc (rand-int dx)))


(comment

  (roll 20)

  )
