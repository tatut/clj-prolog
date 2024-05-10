(ns clj-prolog.predicate
  "Extend Prolog predicates with Clojure."
  (:refer-clojure :exclude [sequence])
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

(defn arity ^long [f]
  (-> f class .getDeclaredMethods ^java.lang.reflect.Method first .getParameterTypes alength))

(defn add-predicate! [^Projog prolog predicate-name retryable? fn-arity get-predicate-fn]
  (assert (instance? Projog prolog) "Specify Prolog instance")
  (assert (keyword? predicate-name) "Predicate name must be a keyword")
  (assert (fn? get-predicate-fn))
  (-> prolog .getKnowledgeBase .getPredicates
      (.addPredicateFactory
       (PredicateKey. (name predicate-name) fn-arity)
       (reify PredicateFactory
         (^Predicate getPredicate [_ ^"[Lorg.projog.core.term.Term;" terms]
          (if-not (= fn-arity (count terms))
            (throw (IllegalArgumentException. (str "Expected " fn-arity " arguments.")))
            (apply get-predicate-fn terms)))

         (^boolean isRetryable [_] (boolean retryable?))))))

(defn lookup
  "Install the given ILookup (like a map) as an arity 2 predicate that looks
  up the first term as key (which must be known) and unifies the found
  value with the second term."
  ([predicate-name lookupable] (lookup *prolog* predicate-name lookupable))
  ([^Projog prolog predicate-name lookup]
   (assert (instance? clojure.lang.ILookup lookup))
   (add-predicate!
    prolog predicate-name false 2
    (fn [^Term in ^Term out]
      (success1
       (let [v (get lookup (to-clojure in) ::not-found)]
         (when (not= v ::not-found)
           (.unify ^Term out ^Term (to-prolog v)))))))))

(defn single
  "Install the given function that produces a single value as predicate."
  ([predicate-name single-value-fn] (single *prolog* predicate-name single-value-fn))
  ([^Projog prolog predicate-name single-value-fn]
   (add-predicate!
    prolog predicate-name false (inc (arity single-value-fn))
    (fn [& terms]
      (let [args (butlast terms)
            output (last terms)]
        (success1
         (.unify ^Term output
                 ^Term (to-prolog (apply single-value-fn (map to-clojure args))))))))))

(defn sequence
  "Install function of arity N as predicate of N+1 where the last
  term is each successive output of the function (that must return a seqable)."
  ([predicate-name seq-returning-fn] (sequence *prolog* predicate-name seq-returning-fn))
  ([prolog predicate-name seq-returning-fn]
   (add-predicate!
    prolog predicate-name true (inc (arity seq-returning-fn))
    (fn [& terms]
      (let [args (map to-clojure (butlast terms))
            vals (atom (apply seq-returning-fn args))
            output (last terms)]
        (reify Predicate
          (^boolean evaluate [_this]
           (boolean
            (let [vs @vals]
              (when (seq vs)
                (swap! vals rest)
                (.backtrack ^Term output)
                (.unify ^Term output (to-prolog (first vs)))))))
          (^boolean couldReevaluationSucceed [_this]
           (boolean (seq @vals)))))))))
