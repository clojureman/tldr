(ns tldr.core-test
  (:require [clojure.test :refer :all]
            [tldr.core :refer :all]))

(deftest a-test
  (testing "Simple compute"
    (is (= [800 800]
           (compute
            (f x)
            where
            x 800
            (function f [x]
                      (vector x x)))))))

(deftest no-where-block
  (testing "No where block"
    (is (= 23 (compute 23)))))

(deftest top-students
  (testing "Top students example"
    (is (= 
         '{:top-students [{:name Zeus, :grade 100} 
                          {:name Zach, :grade 20}
                          {:name Peterson, :grade 8}],
           :avg-grade 27}
         
         (compute
              {:top-students (take 3 (reverse students-by-grade))
               :avg-grade    (/ (sum student-grades)
                                (count student-grades))}
          where
              sum                (partial apply +)
              students-by-grade  (sort-by :grade students)
              student-grades     (map :grade students)

          where
              students '[{:name Peterson  :grade 8}
                         {:name Zeus      :grade 100}
                         {:name Zach      :grade 20}
                         {:name SleepyJoe :grade 0}
                         {:name Droopy    :grade 7}])))))

(deftest mutual-recursion
  (testing "Mutuallly recursive functions"
    (is (= 1 1))))

