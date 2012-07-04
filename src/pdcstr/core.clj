(ns pdcstr.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clj-http.client :as client])
  (:use [clojure.data.zip.xml])
  (:import [java.io BufferedWriter ByteArrayOutputStream File FileOutputStream FileReader FileWriter InputStream PushbackReader])
  (:gen-class))

(defmulti to-byte-array type)

(defmethod to-byte-array InputStream [#^InputStream x]
  (let [buffer (ByteArrayOutputStream.)]
    (io/copy x buffer)
    (.toByteArray buffer)))

(defn serialize
  [data-structure ^String filename]
  (with-open [out-stream (BufferedWriter. (FileWriter. filename))]
    (binding [*out* out-stream
              *print-dup* true]
      (prn data-structure))))

(defn deserialize [^String filename]
  (with-open [r (PushbackReader. (FileReader. filename))]
    (read r)))

(defn download-binary [to from]
  (with-open [out (FileOutputStream. to)]
    (.write out
            (to-byte-array
             (io/input-stream
              (io/as-url from))))))

(defn zip-url [url]
  (zip/xml-zip (xml/parse url)))

(defn get-title [pcast]
  (str/replace (first (:content (zip/node (xml1-> pcast :channel :title)))) #"[^A-Za-z0-9]" ""))

(defn get-enclosures [pcast]
  (xml-> pcast :channel :item :enclosure (attr :url)))

(defn download-enclosures [pcast out-dir]
  (doseq [enc (get-enclosures pcast)]
    (let [file-name (.getName (io/as-file enc))
          title (get-title pcast)
          out-file (File. (str/join "/" [out-dir title file-name]))
          state-token (str/join "/" [out-dir title ".old-episodes.txt"])
          old-episodes (try (deserialize state-token) (catch Exception e #{}))]
      (if (or (some #{(.getName out-file)} old-episodes) (.exists out-file))
        () ;(println "  skipping" file-name)
        (do
          (println "  downloading" file-name)
          (try
            (download-binary out-file enc)
            (serialize (conj old-episodes (.getName out-file)) state-token)
            (catch Exception e (println (.getMessage e)))))))))

(defn -main [& args]
  (let [[dir subs _] args]
    (with-open [rdr (io/reader subs)]
      (doseq [rss-url (remove #(str/blank? %) (line-seq rdr))]
        (let [pcast (zip-url rss-url)
              out-dir (File. dir)
              title (get-title pcast)]
          (.mkdir (File. dir title))
          (println "processing" title)
          (download-enclosures pcast out-dir))))))
