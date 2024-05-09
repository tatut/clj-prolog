(ns clj-prolog.predicate
  "Extend Prolog predicates with Clojure."
  (:require [clj-prolog.core :refer [*prolog*]]
            [clj-prolog.convert :refer [to-clojure to-prolog]])
  (:import (org.projog.api Projog)
           (org.projog.core.term Term)
           (org.projog.core.predicate Predicate PredicateKey PredicateFactory
                                      SucceedsOncePredicate SucceedsNeverPredicate)))

(set! *warn-on-reflection* true)

(defn- success1 [suc]
  (if suc
    SucceedsOncePredicate/SINGLETON
    SucceedsNeverPredicate/SINGLETON))

(defn lookup
  "Install the given ILookup (like a map) as an arity 2 predicate that looks
  up the first term as key (which must be known) and unifies the found
  value with the second term."
  ([predicate-name lookupable] (lookup *prolog* predicate-name lookupable))
  ([^Projog prolog predicate-name lookup]
   (assert (keyword? predicate-name) "Predicate name must be a keyword.")
   (assert (instance? clojure.lang.ILookup lookup))
   (-> prolog .getKnowledgeBase .getPredicates
       (.addPredicateFactory
        (PredicateKey. (name predicate-name) 2)
        (reify PredicateFactory
          (^Predicate getPredicate [_ ^"[Lorg.projog.core.term.Term;" terms]
            (if-not (= 2 (count terms))
              (throw (IllegalArgumentException. "Lookup needs exactly 2 arguments."))
              (success1
               (let [[in out] terms
                     v (get lookup (to-clojure in) ::not-found)]
                 (when (not= v ::not-found)
                   (.unify ^Term out ^Term (to-prolog v)))))))

          (^boolean isRetryable [_] false))))))
