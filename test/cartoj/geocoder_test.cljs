(ns cartoj.geocoder-test
  "Tests for cartoj.geocoder.

  The geocoder-control component uses useControl and useState (React hooks)
  inside a :f> functional component, so we cannot test runtime behaviour here.
  Instead we verify the hiccup shape and prop handling."
  (:require [cljs.test :refer [deftest is testing]]
            [cartoj.geocoder :as geocoder]))

(deftest geocoder-control-returns-f>-vector
  (testing "geocoder-control returns a [:f> fn] hiccup vector"
    (let [result (geocoder/geocoder-control {:position "top-right"})]
      (is (vector? result))
      (is (= :f> (first result)))
      (is (fn? (second result))))))

(deftest geocoder-control-accepts-no-props
  (testing "no-arg call does not crash (defaults: marker true, no position)"
    (is (vector? (geocoder/geocoder-control)))))

(deftest geocoder-control-with-marker-false
  (testing ":marker false yields valid :f> vector"
    (let [result (geocoder/geocoder-control {:position "top-left" :marker false})]
      (is (vector? result))
      (is (= :f> (first result)))
      (is (fn? (second result))))))

(deftest geocoder-control-with-callbacks
  (testing "event callback props yield valid :f> vector"
    (let [on-result (fn [^js _e] nil)
          result    (geocoder/geocoder-control {:position "bottom-left"
                                                :on-result on-result})]
      (is (vector? result))
      (is (= :f> (first result)))
      (is (fn? (second result))))))

(deftest geocoder-control-with-marker-map
  (testing ":marker as a map of custom Marker props"
    (let [result (geocoder/geocoder-control {:position "top-right"
                                             :marker {:color "red" :offset [10 0]}})]
      (is (vector? result))
      (is (= :f> (first result))))))
