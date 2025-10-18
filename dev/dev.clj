(ns dev
  (:require
   [clj-reload.core :as reload]
   ;; [com.stuartsierra.component :as component]
   [com.stuartsierra.component.repl
    :refer [reset set-init start stop system]]
   [dm.main :as main]
   [kaocha.repl :as k]))

(set-init main/new-system)

(reload/init
 {:dirs ["src" "dev" "test"]})

(defn refresh
  []
  (reload/reload))


(comment
  (refresh)
  (k/run-all)
  (reset)
  (stop)
  (start)
  (system)

  )
