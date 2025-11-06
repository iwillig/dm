(ns dm.components
  (:require
   [com.stuartsierra.component :as component]
   [dm.db :as dm.db]
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
      self
      (let [db-spec (assoc dm.db/db-config :dbname db-path)
            datasource (jdbc/get-datasource db-spec)]
        (println "Starting Database component with:" db-spec)
        (assoc self :datasource datasource))))
  (stop [self]
    (when-let [datasource (:datasource self)]
      (when (instance? java.io.Closeable datasource)
        (.close ^java.io.Closeable datasource)))
    (dissoc self :datasource)))

(defrecord Ragtime [migration-dir database]
  component/Lifecycle
  (start [self]
    (if (:migrated? self)
      self
      (let [datasource (dm.db/get-conn database)
            config {:datastore (ragtime-jdbc/sql-database datasource)
                    :migrations (ragtime-jdbc/load-resources
                                 (or migration-dir "migrations"))}]
        (println "Running migrations from:" (or migration-dir "migrations"))
        (ragtime-repl/migrate config)
        (assoc self :migrated? true :config config))))
  (stop [self]
    self))

(defrecord HTTPKit [port timeout database]
  component/Lifecycle
  (start [self]
    (if (:server self)
      self
      (let [server (http-kit/run-server
                    (dm.routes/handler database)
                    {:port port})]
        (println "Starting HTTP server on port:" port)
        (assoc self :server server))))
  (stop [self]
    (when-let [server (:server self)]
      (println "Stopping HTTP server")
      (server :timeout timeout))
    (dissoc self :server)))
