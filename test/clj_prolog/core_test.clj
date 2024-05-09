(ns clj-prolog.core-test
  (:require [clj-prolog.core :refer [with-prolog consult consult-string q]]
            [clojure.test :refer [deftest testing is]]
            [clojure.java.io :as io])
  (:import (org.projog.api QueryResult)))


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


(deftest string-consultation
  (with-prolog
    (consult-string "mortal(X) :- human(X). human(socrates).")
    (is (= (list {:S :socrates})
           (q [:mortal :S])))))

(deftest members
  (with-prolog
    (is (= (for [i (range 1 11)]
             {:X i})
           (q [:between 1 10 :X])))))

(deftest filter-a-map
  (with-prolog
    (consult (io/resource "bigmap.pl"))
    (is (= (list {:M {:foo 420
                      :baz 666}})
           (q [:bigmap
               {:small 42
                :foo 420
                :hundred 100
                :baz 666
                :negative -55}
               :M])))))

(deftest extract-keys
  (testing "Extract map keys using maplist"
    (with-prolog
      (consult-string "
key(Key-_, Key).
mapkeys(map(Ks), Keys) :- maplist(key, Ks, Keys).")
      (is (= (list {:K (list :a :b :c)})
             (q [:mapkeys {:a 112323 :b 59849 :c 54754} :K])))))

  (testing "Backtrack over each key"
    (with-prolog
      (consult-string "mapkeys(map(Ks), Key) :- member(Key-_, Ks).")
      (is (= #{{:K :a} {:K :b} {:K :c}}
             (set (q [:mapkeys {:a 44 :b 99 :c 12321} :K])))))))

(deftest query-options
  (testing "Only return some mappings"
    (with-prolog
      (is (= (list {:X 42} {:X 43})
             (q "Lo = 42, Hi = 43, between(Lo,Hi,X)."
                {:only #{:X}})))))

  (testing "Raw results"
    (with-prolog
      (is (= (list "1" "2" "3")
             (q "between(1,3,X)."
                {:result-fn (fn [^QueryResult r]
                              (str (.getTerm r "X")))}))))))
