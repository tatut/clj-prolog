(ns clj-prolog.convert
  "Convert between Clojure data and Projog Term classes."
  (:require [clojure.string :as str])
  (:import (org.projog.core.term Term TermType Atom Structure List ListFactory
                                 IntegerNumber DecimalFraction)
           (java.util Date)
           (java.time LocalDate LocalTime LocalDateTime)))

(set! *warn-on-reflection* true)

(defprotocol ToProlog
  (to-prolog [this] "Return this data as a Prolog term."))

(defprotocol ToClojure
  (to-clojure [this] "Return this term as Clojure data."))

(defn- prolog-list [l]
  (ListFactory/createList ^"[Lorg.projog.core.term.Term;"
                          (into-array Term (map to-prolog l))))

(defn- clojure-list [^Term l]
  (condp = (.getType l)
    TermType/EMPTY_LIST nil
    TermType/LIST (lazy-seq
                   (cons (to-clojure (.getArgument l 0))
                         (clojure-list (.getArgument l 1))))))

(defn- reduce-list [^Term l reduce-fn initial]
  (loop [val initial
         ^Term l l]
    (if (= TermType/EMPTY_LIST (.getType l))
      val
      (recur (reduce-fn val (.getArgument l 0))
             (.getArgument l 1)))))

(defn- compound
  "Create compound terms from functor name and arguments."
  [functor & args]
  (Structure/createStructure functor
                             (into-array Term (map to-prolog args))))


(defn- compound-clj
  "Return vector of functor and arguments from Prolog compound term."
  [^Structure s]
  (into [(.getName s)]
        (map to-clojure)
        (.getArgs s)))

(defn- compound-args [^Structure s]
  (map to-clojure (.getArgs s)))

(defn- compound-type [^Structure s]
  (str (.getName s) "/" (.getNumberOfArguments s)))

(extend-protocol ToProlog
  clojure.lang.PersistentVector
  (to-prolog [v]
    (if (keyword? (first v))
      (let [[functor & args] v]
        (Structure/createStructure
         (name functor)
         (into-array Term (map to-prolog args))))
      (prolog-list v)))

  clojure.lang.Keyword
  (to-prolog [kw] (Atom. (name kw)))

  clojure.lang.PersistentList
  (to-prolog [l] (prolog-list l))

  clojure.lang.LazySeq
  (to-prolog [l] (prolog-list l))

  java.lang.Long
  (to-prolog [n] (IntegerNumber. n))

  java.lang.Integer
  (to-prolog [n] (IntegerNumber. n))

  java.lang.Double
  (to-prolog [n] (DecimalFraction. n))

  String
  (to-prolog [s] (Structure/createStructure "string"
                                            (into-array Term [(Atom. s)])))

  Date
  (to-prolog [d] (compound "timestamp" (.getTime d)))

  LocalDate
  (to-prolog [d] (compound "date" (.getYear d) (.getMonthValue d) (.getDayOfMonth d)))

  LocalTime
  (to-prolog [t] (compound "time" (.getHour t) (.getMinute t) (.getSecond t) (.getNano t)))

  LocalDateTime
  (to-prolog [dt] (compound "datetime" (.toLocalDate dt) (.toLocalTime dt)))

  clojure.lang.APersistentMap
  (to-prolog [m]
    ;; Map is a prolog compound term map/1 with a list of Key-Val pairs
    (Structure/createStructure
     "map"
     (into-array Term [(ListFactory/createList
                        ^"[Lorg.projog.core.term.Term;"
                        (into-array
                         Term
                         (for [[k v] m]
                           (compound "-" k v))))]))))

(extend-protocol ToClojure
  Atom
  (to-clojure [n] (keyword (.getName n)))

  IntegerNumber
  (to-clojure [n] (.getLong n))

  DecimalFraction
  (to-clojure [n] (.getDouble n))

  Structure
  (to-clojure [s]
    (case (compound-type s)
      "timestamp/1" (let [[^long ms] (compound-args s)]
                      (Date. ms))
      "date/3" (let [[year month day] (compound-args s)]
                 (LocalDate/of ^int year ^int month ^int day))

      "time/4" (let [[hour minute second nano] (compound-args s)]
                 (LocalTime/of hour minute second nano))

      "datetime/2" (let [[date time] (compound-args s)]
                     (LocalDateTime/of date time))
      "string/1" (.getName (.getArgument s 0))

      "map/1" (reduce-list (.getArgument s 0)
                           (fn [m ^Term s]
                             (def *s s)
                             (assert (and (= TermType/STRUCTURE (.getType s))
                                          (= "-" (.getName s)))
                                     "Key-Val pair expected")
                             (assoc m
                                    (to-clojure (.getArgument s 0))
                                    (to-clojure (.getArgument s 1))))
                           {})

      ;; Defaults into vector with kw functor and args
      (into [(keyword (.getName s))]
            (compound-args s))))

  List
  (to-clojure [l]
    (clojure-list l)))

(defn prolog-query
  "Convert a query vector into a Prolog string, like regular data conversion
  but handles conjunction and disjunction (:and and :or)."
  [x]
  (if (vector? x)
    (condp = (first x)
      :and (str/join "," (map prolog-query (rest x)))
      :or (str "(" (str/join ";" (map prolog-query (rest x))) ")")
      (to-prolog x))
    (to-prolog x)))
