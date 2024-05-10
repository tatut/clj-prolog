(ns clj-prolog.sudoku-test
  (:require [clj-prolog.core :refer [with-prolog q consult]]
            [clj-prolog.predicate :as p]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]])
  (:import (org.projog.core.math Numeric)))

(defn transpose [m]
  (apply map list m))

(use-fixtures
  :once (fn [t]
          (with-prolog
            (consult (io/resource "sudoku.pl"))
            (p/single :transpose transpose)
            (t))))

(defn- vars [rows]
  ;; Turn 0 into variable
  (for [r rows]
    (for [c r]
      (if (= 0 c)
        ;; Make variable
        (keyword (gensym "G"))
        c))))

(defn- result [{rows :Sudoku}]
  (vec
   (for [r rows]
     (vec (for [c r]
            (if (instance? Numeric c)
              (.getLong c)
              c))))))

(defn sudoku [rows]
  (map result
       (q [:and
           [:= :Sudoku (vars rows)]
           ;; Apply constraints
           [:sudoku :Sudoku]
           ;; Label to get all possible results
           [:maplist :label :Sudoku]])))

(deftest easy
  (is (=
       (list [[4 3 5 2 6 9 7 8 1]
              [6 8 2 5 7 1 4 9 3]
              [1 9 7 8 3 4 5 6 2]
              [8 2 6 1 9 5 3 4 7]
              [3 7 4 6 8 2 9 1 5]
              [9 5 1 7 4 3 6 2 8]
              [5 1 9 3 2 6 8 7 4]
              [2 4 8 9 5 7 1 3 6]
              [7 6 3 4 1 8 2 5 9]])
       (sudoku
        [[0 0 0 2 6 0 7 0 1]
         [6 8 0 0 7 0 0 9 0]
         [1 9 0 0 0 4 5 0 0]
         [8 2 0 1 0 0 0 4 0]
         [0 0 4 6 0 2 9 0 0]
         [0 5 0 0 0 3 0 2 8]
         [0 0 9 3 0 0 0 7 4]
         [0 4 0 0 5 0 0 3 6]
         [7 0 3 0 1 8 0 0 0]]))))

(deftest difficult
  (is (= (list
          [[5 8 1 6 7 2 4 3 9]
           [7 9 2 8 4 3 6 5 1]
           [3 6 4 5 9 1 7 8 2]
           [4 3 8 9 5 7 2 1 6]
           [2 5 6 1 8 4 9 7 3]
           [1 7 9 3 2 6 8 4 5]
           [8 4 5 2 1 9 3 6 7]
           [9 1 3 7 6 8 5 2 4]
           [6 2 7 4 3 5 1 9 8]])
         (sudoku
          [[0 0 0 6 0 0 4 0 0]
           [7 0 0 0 0 3 6 0 0]
           [0 0 0 0 9 1 0 8 0]
           [0 0 0 0 0 0 0 0 0]
           [0 5 0 1 8 0 0 0 3]
           [0 0 0 3 0 6 0 4 5]
           [0 4 0 2 0 0 0 6 0]
           [9 0 3 0 0 0 0 0 0]
           [0 2 0 0 0 0 1 0 0]]))))

(deftest check
  (testing "We can use it to check if something follows the sudoku rules"
    (let [s [[5 8 1 6 7 2 4 3 9]
             [7 9 2 8 4 3 6 5 1]
             [3 6 4 5 9 1 7 8 2]
             [4 3 8 9 5 7 2 1 6]
             [2 5 6 1 8 4 9 7 3]
             [1 7 9 3 2 6 8 4 5]
             [8 4 5 2 1 9 3 6 7]
             [9 1 3 7 6 8 5 2 4]
             [6 2 7 4 3 5 1 9 8]]]
      (is (= (list s)
             (sudoku s))))))


(deftest incomplete
  (testing "Incomplete sudoku that has multiple solutions generates all"
    (is (= #{[[2 9 5 7 4 3 8 6 1]
              [4 3 1 8 6 5 9 7 2]
              [8 7 6 1 9 2 5 4 3]
              [3 8 7 4 5 9 2 1 6]
              [6 1 2 3 8 7 4 9 5]
              [5 4 9 2 1 6 7 3 8]
              [7 6 3 5 2 4 1 8 9]
              [9 2 8 6 7 1 3 5 4]
              [1 5 4 9 3 8 6 2 7]]
             [[2 9 5 7 4 3 8 6 1]
              [4 3 1 8 6 5 9 2 7]
              [8 7 6 1 9 2 5 4 3]
              [3 8 7 4 5 9 2 1 6]
              [6 1 2 3 8 7 4 9 5]
              [5 4 9 2 1 6 7 3 8]
              [7 6 3 5 2 4 1 8 9]
              [9 2 8 6 7 1 3 5 4]
              [1 5 4 9 3 8 6 7 2]]}
           (set (sudoku
                 [[2 9 5 7 4 3 8 6 1]
                  [4 3 1 8 6 5 9 0 0]
                  [8 7 6 1 9 2 5 4 3]
                  [3 8 7 4 5 9 2 1 6]
                  [6 1 2 3 8 7 4 9 5]
                  [5 4 9 2 1 6 7 3 8]
                  [7 6 3 5 2 4 1 8 9]
                  [9 2 8 6 7 1 3 5 4]
                  [1 5 4 9 3 8 6 0 0]]))))))
