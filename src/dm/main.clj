(ns dm.main
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :as http-kit]))

(defn app
  [_req]
  {:body "hello"
   :status 200})

(defrecord HTTPKit [port timeout]
  component/Lifecycle
  (start [self]
    (assoc self :server (http-kit/run-server app {:port port})))
  (stop  [self]
    (when-let [server (:server self)]
      (server :timeout timeout))))

(defn new-system
  [_]
  (component/system-map
   :http-server (map->HTTPKit {:port 7001 :timeout 100})))

(defn -main [& args]
  (println args))
