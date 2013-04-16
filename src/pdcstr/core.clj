(ns pdcstr.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.edn :as edn]
            [clojure.data.zip.xml :as zxml]))

(defn to-byte-array [^java.io.InputStream x]
  (let [buffer (java.io.ByteArrayOutputStream.)]
    (io/copy x buffer)
    (.toByteArray buffer)))

(defn download-binary [^java.io.File to from]
  (with-open [out (java.io.FileOutputStream. to)]
    (.write out
            (to-byte-array
             (io/input-stream
              (io/as-url from))))))

(defn zip-feed [url]
  (zip/xml-zip (xml/parse url)))

(defn list-files [feed-url]
  (zxml/xml-> (zip-feed feed-url) :channel :item :enclosure (zxml/attr :url)))

(defn download-files [feed-url out-dir max-files]
  (doseq [f (take (or max-files Integer/MAX_VALUE) (list-files feed-url))]
    (let [file-name (.getName (io/as-file f))
          out-file (io/file out-dir file-name)
          state-token (io/file out-dir ".episodes.edn")
          old-episodes (try (edn/read-string (slurp state-token)) (catch Exception e #{}))]
      ;(println "  loaded old episodes" (str old-episodes))
      (when-not (or (some #{(.getName out-file)} old-episodes) (.exists out-file))
        (do
          (.mkdirs out-dir)
          (println "  downloading" file-name)
          (download-binary out-file f)
          (spit state-token (conj old-episodes (.getName out-file))))))))

(defn process-feed [feed-name feed-url out-dir max-files]
  (let [out-dir (io/file out-dir feed-name)]
    (println "processing" feed-name)
    (download-files feed-url out-dir max-files)))

(defn -main [& args]
  (let [[cfg dir _] args]
    ;(println "using subscription list" cfg)
    (doseq [feed (edn/read-string (slurp cfg))]
      (let [{feed-name :name feed-url :url max-files :max-files} feed]
        (process-feed feed-name feed-url dir max-files)))))
