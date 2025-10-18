(ns dev
  (:require
   [com.stuartsierra.component :as component]
   [com.stuartsierra.component.repl
    :refer [reset set-init start stop system]]
   [kaocha.repl :as k]
   [dm.main :as main]
   [clj-reload.core :as reload]))

(set-init main/new-system)

(reload/init
  {:dirs ["src" "dev" "test"]})

(defn refresh
  []
  (reload/reload))
