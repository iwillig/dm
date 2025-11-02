(ns dm.components
  (:require
   [org.httpkit.server :as http-kit]
   [com.stuartsierra.component :as component]
   [dm.db :as dm.db]
   [dm.routes :as dm.routes]
   [datalevin.core :as d]))

(defrecord Database [db-path]
  dm.db/IDB
  (get-db [self]
    (when-let [conn (dm.db/get-conn self)]
      (d/db conn)))
  (get-conn [self]
    (:conn self))
  component/Lifecycle
  (start [self]
    (assoc self :conn (d/get-conn db-path dm.db/schema)))
  (stop  [self]
    (when-let [conn (:conn self)]
      (d/close conn))))

(defrecord HTTPKit [port timeout database]
  component/Lifecycle
  (start [self]
    (assoc self :server (http-kit/run-server
                         (dm.routes/handler database)
                         {:port port})))
  (stop  [self]
    (when-let [server (:server self)]
      (server :timeout timeout))))
