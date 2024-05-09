(ns clj-prolog.core
  (:require [clojure.java.io :as io]
            [clj-prolog.convert :refer [to-prolog to-clojure prolog-query]]
            [clojure.spec.alpha :as s])
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
   (with-open [in (io/reader input)]
     (.consultReader prolog in))))

(defn consult-string
  "Consult a Prolog source string."
  ([input] (consult-string *prolog* input))
  ([prolog input]
   (assert (string? input) "Input must be a string.")
   (.consultReader prolog (java.io.StringReader. input))))

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
    [(str (prolog-query query) ".")
     (into #{}
           (filter #(and (keyword? %)
                         (Character/isUpperCase ^Character (first (name %)))))
           (flatten query))]))

(defn map-result [mappings]
  (fn [^QueryResult query-result]
    (into {}
          (map (fn [kw]
                 [(keyword kw) (to-clojure (.getTerm query-result (name kw)))]))
          (or mappings (.getVariableIds query-result)))))

(defn lazy-response-seq [process-result-fn ^QueryResult query-result]
  (if-not (.next query-result)
    nil
    (lazy-seq
     (cons (process-result-fn query-result)
           (lazy-response-seq process-result-fn query-result)))))


(s/def ::query-def (s/or :prolog string?
                         :vector vector?))

(s/def ::q (s/or
            :query (s/cat :q ::query-def)
            :engine-query (s/cat :prolog #(instance? Projog %) :q ::query-def)
            :query-opts (s/cat :q ::query-def :opts map?)
            :engine-query-opts (s/cat :prolog #(instance? Projog %) :q ::query-def :opts map?)))

(defn q-args [args]
  (let [{:keys [prolog q opts]} (second (s/conform ::q args))]
    {:prolog (or prolog *prolog*)
     :query (second q)
     :opts (or opts {})}))

(defn q
  "Run the given query, returns lazy sequence of matching mappings.
  Arguments can contains:
  - optional Prolog engine (by default dynamically bound *prolog* is used)
  - the query (Prolog source string or Clojure vector representation)
  - options map

  Options map may contain the following keys:
  - :only       set of keywords to return mappings for (instead of all variables)
  - :result-fn  instead of returning maps, use given fn to turn raw query result
                into result data
"
  [& args]
  (let [{:keys [prolog query opts]} (q-args args)
        [query mappings] (->query query)]
    (lazy-response-seq (or (:result-fn opts)
                           (map-result (or (:only opts) mappings)))
                       (.executeQuery prolog query))))
