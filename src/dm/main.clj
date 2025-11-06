(ns dm.main
  (:gen-class)
  (:require
   [bling.core :as bling]
   [cli-matic.core :as cli]
   [com.stuartsierra.component :as component]
   [dm.components :as dm.components]
   [dm.log :as log]
   [dm.roll :as dm.roll]))

(defn new-system
  [_]
  (component/system-map
   :database
   (dm.components/map->Database {:db-path "dm.db"})

   :migrations
   (component/using
    (dm.components/map->Ragtime {:migration-dir "migrations"})
    {:database :database})

   :http-server
   (component/using
    (dm.components/map->HTTPKit {:port 7001 :timeout 100})
    {:database :database})))

(defn callout
  [messages]
  (bling/callout
   {:type :info
    :label ""
    :theme :gutter}
   (with-out-str

     (apply bling/print-bling messages))))

(def config-cli
  {:command "dm-assistant"
   :description ""
   :version "0.0.1"
   :subcommands [{:command "roll"
                  :description "Rolls a N dice"
                  :runs (fn [{[dn] :_arguments}]

                          (callout [[:bold "Rolling "]
                                    [:double-underline (str "d" dn)]])
                          (callout [[:bold "Result "]
                                    [:double-underline
                                     (dm.roll/roll (parse-long dn))]])

                          0)}]})

(comment
  (component/start (new-system nil)))

(defn -main [& args]
  ;; Initialize structured logging
  (log/init!)
  (log/info {:args (vec args)} "DM Assistant starting")
  (cli/run-cmd args config-cli))
