(ns cartoj.overlays-test
  "Tests for cartoj.overlays (Marker, Popup).

  Same approach as core_test: we test the *shape* of the hiccup vector
  returned by each component function, not full DOM rendering."
  (:require [cljs.test :refer [deftest is testing]]
            [cartoj.overlays :as overlays]))

;; ---------------------------------------------------------------------------
;; Marker

(deftest marker-returns-reagent-vector
  (testing "marker returns a [:r> ...] hiccup vector"
    (let [result (overlays/marker {:longitude -122.4 :latitude 37.8})]
      (is (vector? result))
      (is (= :r> (first result))))))

(deftest marker-converts-props
  (testing ":longitude and :latitude are passed as JS props"
    (let [[_ _comp ^js js-props] (overlays/marker {:longitude -122.4 :latitude 37.8})]
      (is (= -122.4 (.-longitude js-props)))
      (is (= 37.8   (.-latitude js-props))))))

(deftest marker-passes-callback
  (testing ":on-click callback passes through unchanged"
    (let [cb (fn [e] e)
          [_ _comp ^js js-props] (overlays/marker {:longitude 0 :latitude 0 :on-click cb})]
      (is (identical? cb (.-onClick js-props))))))

(deftest marker-accepts-children
  (testing "children are appended after the JS props"
    (let [child [:div "pin"]
          result (overlays/marker {:longitude 0 :latitude 0} child)]
      (is (= child (last result))))))

(deftest marker-accepts-no-props
  (testing "marker can be called with no args (no crash)"
    (is (vector? (overlays/marker)))))

;; ---------------------------------------------------------------------------
;; Popup

(deftest popup-returns-reagent-vector
  (testing "popup returns a [:r> ...] hiccup vector"
    (let [result (overlays/popup {:longitude -122.4 :latitude 37.8})]
      (is (vector? result))
      (is (= :r> (first result))))))

(deftest popup-converts-props
  (testing ":longitude, :latitude, :close-button converted to JS"
    (let [[_ _comp ^js js-props] (overlays/popup {:longitude -122.4
                                                   :latitude 37.8
                                                   :close-button false})]
      (is (= -122.4 (.-longitude js-props)))
      (is (= 37.8   (.-latitude js-props)))
      (is (= false  (.-closeButton js-props))))))

(deftest popup-passes-on-close
  (testing ":on-close callback passes through unchanged"
    (let [cb (fn [] nil)
          [_ _comp ^js js-props] (overlays/popup {:longitude 0 :latitude 0 :on-close cb})]
      (is (identical? cb (.-onClose js-props))))))

(deftest popup-accepts-children
  (testing "children are appended after the JS props"
    (let [child [:p "Hello"]
          result (overlays/popup {:longitude 0 :latitude 0} child)]
      (is (= child (last result))))))
