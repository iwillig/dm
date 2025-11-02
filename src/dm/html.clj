(ns dm.html
  (:require
   [hiccup2.core :as h]))

(defn base-page
  [content]
  [:html {:lang "en"}
   [:head
    [:title "dm"]
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1"}]
    [:meta {:name "color-scheme" :content "light dark"}]
    [:link {:type "text/css"
            :href "https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css"
            :rel "stylesheet"}]]
   [:body
    [:main content]]])

(defn index-page
  [db ctx]
  (str
   (h/html
       (base-page
        [:div [:h1 "dm"]
         [:ul
          [:li [:p "system"]]
          [:li [:p "db"]
           [:code (pr-str db)]]
          [:li [:p "ctx"]
           [:code (pr-str ctx)]]]]))))
