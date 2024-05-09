(ns clj-prolog.convert-test
  (:require [clojure.test :refer [deftest testing is]]
            [clj-prolog.convert :refer [to-prolog to-clojure]])
  (:import (java.util Date)
           (java.time LocalDate LocalTime LocalDateTime)
           (org.projog.core.term Structure Term IntegerNumber ListFactory)))

(defn rt [clj-in]
  (let [prolog (to-prolog clj-in)
        clj-out (to-clojure prolog)]
    (is (= clj-in clj-out) "Roundtripped value equals input")))


(deftest roundtrip
  (testing "Conversion roundtrips values with fidelity"
    (rt 1)
    (rt 4.20)
    (rt [:something "completely" "different"])
    (rt [42 123])
    (rt (list :my :list :of :atoms))
    (rt {})
    (rt {:foo 42
         :bar 666})
    (rt [:compound_with {:map "argument"} (list 1 4 8)])
    (rt (Date.))
    (rt (LocalDate/now))
    (rt (LocalTime/now))
    (rt (LocalDateTime/now))))

(deftest vector-conversion
  (testing "Vector with keyword as first item is converted to compound term"
    (is (= (to-prolog [:foo 42 123])
           (Structure/createStructure
            "foo"
            (into-array Term [(IntegerNumber. 42)
                              (IntegerNumber. 123)])))))

  (testing "Vector with something else as first item is converted into a list"
    (is (= (to-prolog [1 2 3])
           (ListFactory/createList ^"[Lorg.projog.core.term.Term;"
                                   (into-array Term
                                               [(IntegerNumber. 1)
                                                (IntegerNumber. 2)
                                                (IntegerNumber. 3)]))))))
