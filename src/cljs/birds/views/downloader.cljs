(ns birds.views.downloader
  (:require [clojure.string :as str]))

(defn csv-row [headers item]
  (->> headers
       (map item)
       (map #(if-not (nil? %) (str %) ""))
       (str/join ",")))

(defn csv-raw-events [headers data]
  (->> data
       (map (partial csv-row headers))
       (concat [(->> headers (map name) (str/join ","))])
       (str/join "\r\n")
       vector))

(defn create-download-data [type headers data]
  (let [mime-type ({:csv "text/csv"
                    :json "application/json"} type)]
    (js/Blob. (condp = type
                :json (->> data clj->js (.stringify js/JSON) vector)
                :csv (csv-raw-events headers data))
              {:type mime-type})))

(defn download-data [file-name file-type headers data]
  (let [data-blob (create-download-data file-type headers data)
        link (.createElement js/document "a")]
    (set! (.-href link) (.createObjectURL js/URL data-blob))
    (.setAttribute link "download" (->> file-type name (str file-name ".")))
    (.appendChild (.-body js/document) link)
    (.click link)
    (.removeChild (.-body js/document) link)))
