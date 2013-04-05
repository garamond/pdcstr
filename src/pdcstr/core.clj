(ns pdcstr.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clj-http.client :as client]
            [clojure.edn :as edn]
            [clojure.data.zip.xml :as zxml])
  (:import [java.io BufferedWriter ByteArrayOutputStream File FileOutputStream FileReader FileWriter InputStream PushbackReader]))

(defn to-byte-array [^InputStream x]
  (let [buffer (ByteArrayOutputStream.)]
    (io/copy x buffer)
    (.toByteArray buffer)))

(defn download-binary [^File to from]
  (with-open [out (FileOutputStream. to)]
    (.write out
            (to-byte-array
             (io/input-stream
              (io/as-url from))))))

(defn zip-feed [url]
  (zip/xml-zip (xml/parse url)))

(defn list-files [feed-url]
  (zxml/xml-> (zip-feed feed-url) :channel :item :enclosure (zxml/attr :url)))

(defn download-files [feed-url out-dir latest-only?]
  (let [files (list-files feed-url)]
    (doseq [f (if latest-only? (vector (first files)) files)]
      (let [file-name (.getName (io/as-file f))
            out-file (File. out-dir file-name)
            state-token (str/join "/" [out-dir ".episodes.edn"])
            old-episodes (try (edn/read-string (slurp state-token)) (catch Exception e #{}))]
        ;(println "  loaded old episodes" (str old-episodes))
        (if (or (some #{(.getName out-file)} old-episodes) (.exists out-file))
          ();(println "  skipped" file-name)
          (do
            (try
              (.mkdirs out-dir)
              (println "  downloading" file-name)
              (download-binary out-file f)
              (spit state-token (conj old-episodes (.getName out-file))))))))))

(defn process-feed [feed-url out-dir latest-only?]
  (let [out-dir (io/file out-dir)]
    (println "processing" feed-url)
    (download-files feed-url out-dir latest-only?)))

(defn -main [& args]
  (let [[cfg dir _] args]
    ;(println "using subscription list" cfg)
    (let [config (edn/read-string (slurp cfg))]
      (doseq [feed (:feeds config)]
        (let [{feed-name :name feed-url :url latest-only? :latest-only?} (merge (:defaults config) feed)]
          (process-feed feed-url (str/join "/" [dir feed-name]) latest-only?))))))
