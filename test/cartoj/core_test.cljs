(ns cartoj.core-test
  "Tests for cartoj.core Map component.

  NOTE: Because react-map-gl renders into a DOM and requires WebGL, we test
  the *shape* of the hiccup produced by the component functions rather than
  full render correctness. This mirrors the approach used in reagent-material-ui
  and headlessui-reagent: verify the wrapper produces [:> ReactComp props ...]
  with correctly converted props, rather than trying to mount the full component."
  (:require [cljs.test :refer [deftest is testing]]
            [cartoj.core :as core]))

(deftest map-returns-reagent-vector
  (testing "map with no props returns [:r> Map ...] (pre-converted JS props)"
    (let [result (core/interactive-map {})]
      (is (vector? result))
      (is (= :r> (first result))))))

(deftest map-passes-map-style
  (testing ":map-style is converted to camelCase JS prop"
    (let [[_ _comp ^js js-props] (core/interactive-map {:map-style "https://example.com/style.json"})]
      (is (= "https://example.com/style.json" (.-mapStyle js-props))))))

(deftest map-passes-initial-view-state
  (testing ":initial-view-state nested map is converted to JS"
    (let [[_ _comp ^js js-props] (core/interactive-map {:initial-view-state {:longitude -122.4
                                                                             :latitude  37.8
                                                                             :zoom      14}})
          ^js ivs (.-initialViewState js-props)]
      (is (= -122.4 (.-longitude ivs)))
      (is (= 37.8   (.-latitude ivs)))
      (is (= 14     (.-zoom ivs))))))

(deftest map-passes-callbacks
  (testing ":on-click callback passes through unchanged"
    (let [cb (fn [e] e)
          [_ _comp ^js js-props] (core/interactive-map {:on-click cb})]
      (is (identical? cb (.-onClick js-props))))))

(deftest map-accepts-children
  (testing "children are appended after the JS props"
    (let [child [:div "hello"]
          result (core/interactive-map {} child)]
      (is (= child (last result))))))

(deftest map-accepts-no-props
  (testing "map can be called with no args (no crash)"
    (is (vector? (core/interactive-map)))))

(deftest map-class-name-wraps-in-div
  (testing ":class-name is applied to a wrapping div, not forwarded to Map"
    (let [result (core/interactive-map {:class-name "imap" :map-style "x"})]
      (is (= :div (first result)))
      (is (= "imap" (get-in result [1 :class])))
      (let [[_ _comp ^js js-props] (nth result 2)]
        (is (undefined? (.-className js-props)))
        (is (= "x" (.-mapStyle js-props)))))))

(deftest map-without-class-name-not-wrapped
  (testing "no :class-name means no wrapper div — returns [:r> Map ...] directly"
    (let [result (core/interactive-map {:map-style "x"})]
      (is (= :r> (first result))))))

(deftest map-class-name-wrapper-preserves-children
  (testing "children appear inside the inner Map element when wrapped"
    (let [child [:div "hello"]
          result (core/interactive-map {:class-name "imap"} child)
          inner (nth result 2)]
      (is (= child (last inner))))))

(deftest view-state-round-trip
  (testing "view-state->js converts a CLJS view-state map to JS"
    (let [vs {:longitude -122.4 :latitude 37.8 :zoom 14 :bearing 0 :pitch 0}
          js-vs (core/view-state->js vs)]
      (is (= -122.4 (.-longitude js-vs)))
      (is (= 37.8   (.-latitude js-vs)))
      (is (= 14     (.-zoom js-vs)))))

  (testing "view-state->clj converts a JS viewState event to CLJS"
    (let [js-vs (js-obj "longitude" -122.4 "latitude" 37.8 "zoom" 14)
          clj-vs (core/view-state->clj js-vs)]
      (is (= -122.4 (:longitude clj-vs)))
      (is (= 37.8   (:latitude clj-vs)))
      (is (= 14     (:zoom clj-vs))))))
