(ns clj-prolog.predicate-test
  (:require [clj-prolog.core :refer [with-prolog q]]
            [clj-prolog.predicate :refer [lookup] :as p]
            [clojure.test :refer [deftest testing is]]))


(deftest lookup-map
  (with-prolog
    (lookup :paradigm {:clojure :functional
                       :prolog :logic
                       :java :imperative})

    (testing "Simple lookup as predicate works"
      (is (= (list {:P :functional})
             (q [:paradigm :clojure :P]))))

    (testing "Key and value succeeds"
      (is (= (list {})
             (q [:paradigm :prolog :logic]))))

    (testing "Value not found does not succeed"
      (is (empty?
           (q [:paradigm :cobol :Business]))))))


(deftest sequence-fn
  (testing "Simple seq producing fn call"
    (with-prolog
      (p/sequence :myrange range)
      (is (= (list {:N 42} {:N 43})
             (q [:myrange 42 44 :N])))))

  (testing "Parameters from backtrack between"
    (with-prolog
      (p/sequence :myrange range)
      (is (=
           (set (for [lo (range 1 4)
                      n (range lo (* 3 lo))]
                  {:Lo lo :Hi (* 3 lo) :N n}))
           (set (q "between(1,3,Lo), Hi is 3 * Lo, myrange(Lo,Hi,N).")))))))
