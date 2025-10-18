(ns dm.main
  (:gen-class)
  (:require
   [com.stuartsierra.component :as component]
   [datalevin.core :as d]
   [cli-matic.core :as cli]
   [dm.db :as dm.db]
   [dm.roll :as dm.roll]
   [org.httpkit.server :as http-kit]))

(defn app
  [_req]
  {:body "hello"
   :status 200})

(defrecord Database [db-path]
  component/Lifecycle
  (start [self]
    (assoc self :conn (d/get-conn db-path dm.db/schema)))
  (stop  [self]
    (when-let [conn (:conn self)]
      (d/close conn))))


(defrecord HTTPKit [port timeout database]
  component/Lifecycle
  (start [self]
    (println database)
    (assoc self :server (http-kit/run-server app {:port port})))
  (stop  [self]
    (when-let [server (:server self)]
      (server :timeout timeout))))


(defn new-system
  [_]
  (component/system-map
   :database
   (map->Database {:db-path "db"})
   :http-server
   (component/using
    (map->HTTPKit {:port 7001 :timeout 100})
    {:database :database})))

(def config-cli
  {:command     "dm-assistant"
   :description ""
   :version     "0.0.1"
   :subcommands [{:command "roll"
                  :description "Rolls a N dice"
                  :runs (fn [& _args]
                          (println (dm.roll/roll 20)))}]})

(comment
  (component/start (new-system nil))
  )

(defn -main [& args]
  (cli/run-cmd args config-cli))
