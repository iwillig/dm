(ns dm.log
  "Structured logging utilities for the DM application.
  
  This namespace wraps Cambium for consistent structured logging across the application.
  All log functions accept either a message string or a map of data plus a message.
  
  Examples:
    (log/info \"Server started\")
    (log/info {:port 7001 :timeout 100} \"Server started\")
    (log/error {:error (.getMessage e)} \"Database connection failed\")
  "
  (:require
   [cambium.codec :as codec]
   [cambium.core :as cambium]
   [cambium.logback.json.flat-layout :as flat]))

;; Initialize Cambium with JSON codec
(defn init!
  "Initialize the logging system. Call this once at application startup."
  []
  (flat/set-decoder! codec/destringify-val))

;; Log level functions
(defn trace
  "Log at TRACE level. Accepts either message or [data message]."
  ([msg]
   (cambium/trace msg))
  ([data msg]
   (cambium/trace data msg)))

(defn debug
  "Log at DEBUG level. Accepts either message or [data message]."
  ([msg]
   (cambium/debug msg))
  ([data msg]
   (cambium/debug data msg)))

(defn info
  "Log at INFO level. Accepts either message or [data message]."
  ([msg]
   (cambium/info msg))
  ([data msg]
   (cambium/info data msg)))

(defn warn
  "Log at WARN level. Accepts either message or [data message]."
  ([msg]
   (cambium/warn msg))
  ([data msg]
   (cambium/warn data msg)))

(defn error
  "Log at ERROR level. Accepts either message or [data message].
   For exceptions, include the exception in the data map."
  ([msg]
   (cambium/error msg))
  ([data msg]
   (cambium/error data msg)))

;; Component-specific logging helpers

(defn component-start
  "Log component start with structured data."
  [component-name data]
  (info (merge {:component component-name
                :lifecycle :start}
               data)
        (str component-name " component starting")))

(defn component-started
  "Log successful component start."
  [component-name data]
  (info (merge {:component component-name
                :lifecycle :started}
               data)
        (str component-name " component started")))

(defn component-stop
  "Log component stop."
  [component-name]
  (info {:component component-name
         :lifecycle :stop}
        (str component-name " component stopping")))

(defn component-stopped
  "Log successful component stop."
  [component-name]
  (info {:component component-name
         :lifecycle :stopped}
        (str component-name " component stopped")))

;; HTTP request logging
(defn http-request
  "Log HTTP request with structured data."
  [request]
  (debug {:request-method (:request-method request)
          :uri (:uri request)
          :remote-addr (:remote-addr request)}
         "HTTP request"))

(defn http-response
  "Log HTTP response with structured data."
  [request response duration-ms]
  (info {:request-method (:request-method request)
         :uri (:uri request)
         :status (:status response)
         :duration-ms duration-ms}
        "HTTP response"))

;; Database logging
(defn db-query
  "Log database query execution."
  [query-name params duration-ms]
  (debug {:query query-name
          :params params
          :duration-ms duration-ms}
         "Database query executed"))

(defn db-error
  "Log database error."
  [query-name error]
  (error {:query query-name
          :error-type (type error)
          :error-message (.getMessage ^Exception error)}
         "Database error"))

;; Migration logging
(defn migration-start
  "Log migration start."
  [migration-dir]
  (info {:migration-dir migration-dir}
        "Running database migrations"))

(defn migration-complete
  "Log migration completion."
  [migration-dir count]
  (info {:migration-dir migration-dir
         :migrations-applied count}
        "Database migrations complete"))
