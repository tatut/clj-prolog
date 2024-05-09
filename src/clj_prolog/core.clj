(ns clj-prolog.core
  (:require [clojure.java.io :as io]
            [clj-prolog.convert :refer [to-prolog to-clojure]])
  (:import (org.projog.api Projog  QueryResult)
           (org.projog.core.event ProjogListener)))

(def ^:dynamic *prolog* "The currently bound Prolog engine." nil)

(defn prolog
  "Construct a new empty Prolog engine."
  []
  (Projog. (into-array ProjogListener [])))

(defn consult
  "Consult the given input. Input can be anything
  that can be read with [[clojure.java.io/reader]]"
  ([input] (consult *prolog* input))
  ([prolog input]
   (.consultReader prolog (io/reader input))))

(defmacro with-prolog
  "Run body with [[*prolog*]] bound to a fresh Prolog instance."
  [& body]
  `(binding [*prolog* (prolog)]
     ~@body))

(defn ->query [query]
  ;; PENDING: would be better to execute the already parsed, instead of stringifying
  (if (string? query)
    ;; A Prolog source query, use that as is
    [query nil]
    (let [prolog-query (str (to-prolog query) ".")]
      [prolog-query
       (into #{}
             (filter #(and (keyword? %)
                           (Character/isUpperCase ^Character (first (name %)))))
             (flatten query))])))

(defn lazy-response-seq [mappings ^QueryResult query-result]
  (if-not (.next query-result)
    nil
    (lazy-seq
     (cons (into {}
                 (map (fn [kw]
                        [(keyword kw) (to-clojure (.getTerm query-result (name kw)))]))
                 (or mappings (.getVariableIds query-result)))
           (lazy-response-seq mappings query-result)))))

(defmacro q
  "Run the given query, returns lazy sequence of matching mappings."
  [& prolog-and-query]
  (let [argc (count prolog-and-query)
        [prolog query] (case argc
                         1 ['clj-prolog.core/*prolog* (first prolog-and-query)]
                         2 prolog-and-query)]
    `(let [[query# mappings#] (->query ~query)]
       (lazy-response-seq mappings# (.executeQuery ~prolog query#)))))
