(ns clj-prolog.predicate-test
  (:require [clj-prolog.core :refer [with-prolog q]]
            [clj-prolog.predicate :refer [lookup]]
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
