(ns cartoj.interop-test
  "Tests for cartoj.interop.

  useMap and useControl are React hooks — they can only be called inside a
  running React functional component, so we cannot unit-test their runtime
  behaviour here.  We instead verify:
    - with-map returns a [:f> fn] vector (functional-component shim)
    - map-provider returns a [:> MapProvider ...] vector
    - use-map and use-control are exported functions (callable references)"
  (:require [cartoj.interop :as interop]
            [cljs.test :refer [deftest is testing]]))

;; ---------------------------------------------------------------------------
;; with-map

(deftest with-map-returns-f>-vector
  (testing "with-map returns a [:f> fn] hiccup vector"
    (let [result (interop/with-map (fn [_map-ref] [:div "content"]))]
      (is (vector? result))
      (is (= :f> (first result)))
      (is (fn? (second result))))))

;; ---------------------------------------------------------------------------
;; map-provider

(deftest map-provider-returns-reagent-vector
  (testing "map-provider returns a [:r> MapProvider ...] vector"
    (let [result (interop/map-provider {})]
      (is (vector? result))
      (is (= :r> (first result))))))

(deftest map-provider-accepts-children
  (testing "children are appended after the JS props"
    (let [child  [:div "maps here"]
          result (interop/map-provider {} child)]
      (is (= child (last result))))))

(deftest map-provider-accepts-no-props
  (testing "map-provider can be called with no args"
    (is (vector? (interop/map-provider)))))

;; ---------------------------------------------------------------------------
;; use-map / use-control — exported as callable values

(deftest use-map-is-a-function
  (testing "use-map is exported as a function (hooks are passed through)"
    (is (fn? interop/use-map))))

(deftest use-control-is-a-function
  (testing "use-control is exported as a function"
    (is (fn? interop/use-control))))

(deftest view-state-round-trip
  (testing "view-state->js converts a CLJS view-state map to JS"
    (let [vs    {:longitude -122.4 :latitude 37.8 :zoom 14 :bearing 0 :pitch 0}
          js-vs (interop/view-state->js vs)]
      (is (= -122.4 (.-longitude js-vs)))
      (is (= 37.8   (.-latitude js-vs)))
      (is (= 14     (.-zoom js-vs)))))

  (testing "view-state->clj converts a JS viewState event to CLJS"
    (let [js-vs  (js-obj "longitude" -122.4 "latitude" 37.8 "zoom" 14)
          clj-vs (interop/view-state->clj js-vs)]
      (is (= -122.4 (:longitude clj-vs)))
      (is (= 37.8   (:latitude clj-vs)))
      (is (= 14     (:zoom clj-vs))))))
