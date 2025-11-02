(ns dev
  (:require
   [clj-reload.core :as reload]
   ;; [com.stuartsierra.component :as component]
   [com.stuartsierra.component.repl
    :refer [reset set-init start stop system]]
   [dm.main :as main]
   [clj-kondo.core :as clj-kondo]
   [kaocha.repl :as k]))

(set-init main/new-system)

(reload/init
 {:dirs ["src" "dev" "test"]})

(defn refresh
  []
  (reload/reload))

(defn all-tests
  []
  (k/run-all))

(defn lint
  []
  (-> (clj-kondo/run! {:lint ["src" "test" "dev"]})
      (clj-kondo/print!)))


(comment
  (refresh)
  (all-tests)
  (k/run-all)
  (reset)
  (stop)
  (start)
  (system)

  )
