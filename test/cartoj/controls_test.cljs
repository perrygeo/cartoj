(ns cartoj.controls-test
  "Tests for cartoj.controls (NavigationControl, GeolocateControl,
  FullscreenControl, ScaleControl, AttributionControl)."
  (:require [cljs.test :refer [deftest is testing]]
            [cartoj.controls :as controls]))

;; Helper: all controls share the same structural contract.
(defn- assert-control [result]
  (is (vector? result))
  (is (= :r> (first result))))

;; ---------------------------------------------------------------------------
;; NavigationControl

(deftest navigation-control-returns-reagent-vector
  (testing "returns [:> ...]"
    (assert-control (controls/navigation-control {}))))

(deftest navigation-control-converts-position
  (testing ":position passes through as JS prop"
    (let [[_ _comp ^js js-props] (controls/navigation-control {:position "top-right"})]
      (is (= "top-right" (.-position js-props))))))

(deftest navigation-control-accepts-no-props
  (testing "no-arg call does not crash"
    (is (vector? (controls/navigation-control)))))

;; ---------------------------------------------------------------------------
;; GeolocateControl

(deftest geolocate-control-returns-reagent-vector
  (testing "returns [:> ...]"
    (assert-control (controls/geolocate-control {}))))

(deftest geolocate-control-converts-props
  (testing ":position and :track-user-location converted"
    (let [[_ _comp ^js js-props] (controls/geolocate-control {:position "bottom-right"
                                                               :track-user-location true})]
      (is (= "bottom-right" (.-position js-props)))
      (is (= true           (.-trackUserLocation js-props))))))

(deftest geolocate-control-accepts-no-props
  (testing "no-arg call does not crash"
    (is (vector? (controls/geolocate-control)))))

;; ---------------------------------------------------------------------------
;; FullscreenControl

(deftest fullscreen-control-returns-reagent-vector
  (testing "returns [:> ...]"
    (assert-control (controls/fullscreen-control {}))))

(deftest fullscreen-control-converts-position
  (testing ":position passes through as JS prop"
    (let [[_ _comp ^js js-props] (controls/fullscreen-control {:position "top-left"})]
      (is (= "top-left" (.-position js-props))))))

(deftest fullscreen-control-accepts-no-props
  (testing "no-arg call does not crash"
    (is (vector? (controls/fullscreen-control)))))

;; ---------------------------------------------------------------------------
;; ScaleControl

(deftest scale-control-returns-reagent-vector
  (testing "returns [:> ...]"
    (assert-control (controls/scale-control {}))))

(deftest scale-control-converts-props
  (testing ":max-width and :unit converted to JS"
    (let [[_ _comp ^js js-props] (controls/scale-control {:max-width 100 :unit "metric"})]
      (is (= 100      (.-maxWidth js-props)))
      (is (= "metric" (.-unit js-props))))))

(deftest scale-control-accepts-no-props
  (testing "no-arg call does not crash"
    (is (vector? (controls/scale-control)))))

;; ---------------------------------------------------------------------------
;; AttributionControl

(deftest attribution-control-returns-reagent-vector
  (testing "returns [:> ...]"
    (assert-control (controls/attribution-control {}))))

(deftest attribution-control-converts-props
  (testing ":compact and :custom-attribution converted to JS"
    (let [[_ _comp ^js js-props] (controls/attribution-control {:compact true
                                                                 :custom-attribution "© Me"})]
      (is (= true  (.-compact js-props)))
      (is (= "© Me" (.-customAttribution js-props))))))

(deftest attribution-control-accepts-no-props
  (testing "no-arg call does not crash"
    (is (vector? (controls/attribution-control)))))
