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
