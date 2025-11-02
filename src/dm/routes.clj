(ns dm.routes
  (:require
   [dm.html :as dm.html]
   [reitit.core :as r]
   [ataraxy.core :as ataraxy]
   [liberator.core :as liberator]))

(def routes
  '{"/" [:index]})

(defn index
  [db]
  (liberator/resource
   :available-media-types ["text/html"]
   :handle-ok (partial dm.html/index-page db)))

(defn handler
  [db]
  (ataraxy/handler
   {:routes   routes
    :handlers {:index (index db)}}))


(comment

  (ataraxy/matches routes {:uri "/"})

  )
