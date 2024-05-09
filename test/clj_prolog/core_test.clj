(ns clj-prolog.core-test
  (:require [clj-prolog.core :refer [with-prolog consult q]]
            [clojure.test :refer [deftest testing is]]
            [clojure.java.io :as io]))


(deftest ancestry
  (with-prolog
    (consult (io/resource "ancestry.pl"))

    (testing "alice is parent of bob"
      (is (= (list {})
             (q [:ancestor :alice :bob]))))

    (testing "no results for unknown people"
      (is (empty?
           (q [:ancestor :unknown :incognito]))))

    (testing "All descendants of alice"
      (is (= #{{:D :bob}
               {:D :charlie}
               {:D :dylan}
               {:D :frank}
               {:D :gabriella}}
             (set (q [:ancestor :alice :D])))))

    (testing "All ancestors of dylan"
      (is (= #{{:A :alice} {:A :bob} {:A :charlie}}
             (set (q [:ancestor :A :dylan])))))

    (testing "Prolog query as string"
      (is (= (list {:A :alice :B :bob})
             (q "parent(A,B), A = alice."))))))