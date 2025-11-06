(ns dm.components
  (:require
   [com.stuartsierra.component :as component]
   [dm.db :as dm.db]
   [dm.log :as log]
   [dm.routes :as dm.routes]
   [next.jdbc :as jdbc]
   [org.httpkit.server :as http-kit]
   [ragtime.next-jdbc :as ragtime-jdbc]
   [ragtime.repl :as ragtime-repl]))

(defrecord Database [db-path]
  dm.db/IDB
  (get-db [self]
    (:datasource self))
  (get-conn [self]
    (:datasource self))
  component/Lifecycle
  (start [self]
    (if (:datasource self)
      (do
        (log/debug {:component "Database"} "Already started, skipping")
        self)
      (let [db-spec (assoc dm.db/db-config :dbname db-path)
            datasource (jdbc/get-datasource db-spec)]
        (log/component-started "Database" {:db-path db-path
                                           :db-spec (dissoc db-spec :password)})
        (assoc self :datasource datasource))))
  (stop [self]
    (when-let [datasource (:datasource self)]
      (log/component-stop "Database")
      (when (instance? java.io.Closeable datasource)
        (.close ^java.io.Closeable datasource))
      (log/component-stopped "Database"))
    (dissoc self :datasource)))

(defrecord Ragtime [migration-dir database]
  component/Lifecycle
  (start [self]
    (if (:migrated? self)
      (do
        (log/debug {:component "Ragtime"} "Already migrated, skipping")
        self)
      (let [datasource (dm.db/get-conn database)
            dir (or migration-dir "migrations")
            config {:datastore (ragtime-jdbc/sql-database datasource)
                    :migrations (ragtime-jdbc/load-resources dir)}]
        (log/migration-start dir)
        (ragtime-repl/migrate config)
        (log/migration-complete dir (count (:migrations config)))
        (assoc self :migrated? true :config config))))
  (stop [self]
    self))

(defrecord HTTPKit [port timeout database]
  component/Lifecycle
  (start [self]
    (if (:server self)
      (do
        (log/debug {:component "HTTPKit"} "Already started, skipping")
        self)
      (let [server (http-kit/run-server
                    (dm.routes/handler database)
                    {:port port})]
        (log/component-started "HTTPKit" {:port port :timeout timeout})
        (assoc self :server server))))
  (stop [self]
    (when-let [server (:server self)]
      (log/component-stop "HTTPKit")
      (server :timeout timeout)
      (log/component-stopped "HTTPKit"))
    (dissoc self :server)))
