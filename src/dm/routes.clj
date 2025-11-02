(ns dm.routes
  (:require
   [dm.html :as dm.html]
   [reitit.core :as r]
   [reitit.ring :as reitit.ring]
   [liberator.core :as liberator]))

(defn index
  [db]
  (liberator/resource
   :available-media-types ["text/html"]
   :handle-ok (partial dm.html/index-page db)))

(defn router
  [db]
  (reitit.ring/router
   [["/" {:name ::index
          :handler (index db)}]]))

(comment
  (r/match-by-name (router nil) ::index)
  (r/match-by-path (router nil) "/"))



(defn handler
  [db]
  (reitit.ring/ring-handler
   (router db)))
