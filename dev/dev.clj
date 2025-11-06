(ns dev
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-reload.core :as reload]
   ;; [com.stuartsierra.component :as component]
   [com.stuartsierra.component.repl
    :refer [reset set-init start stop system]]
   ;; Used in REPL and comment block
   [dm.log :as log]
   [dm.main :as main]
   [kaocha.repl :as k]))

(set-init main/new-system)

;; Initialize logging on REPL startup
(log/init!)

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
  (lint)
  (refresh)
  (all-tests)
  (k/run-all)
  (reset)
  (stop)
  (start)
  (system))
