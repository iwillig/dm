(ns dm.main
  (:gen-class)
  (:require
   [dm.components :as dm.components]
   [bling.core :as bling]
   [com.stuartsierra.component :as component]
   [cli-matic.core :as cli]
   [dm.roll :as dm.roll]))

(defn new-system
  [_]
  (component/system-map
   :database
   (dm.components/map->Database {:db-path "db"})
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
  {:command     "dm-assistant"
   :description ""
   :version     "0.0.1"
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
  (cli/run-cmd args config-cli))
