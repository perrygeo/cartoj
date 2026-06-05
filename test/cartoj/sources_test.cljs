(ns cartoj.sources-test
  "Tests for cartoj.sources (Source, Layer)."
  (:require [cljs.test :refer [deftest is testing]]
            [cartoj.sources :as sources]))

;; ---------------------------------------------------------------------------
;; Source

(deftest source-returns-reagent-vector
  (testing "source returns a [:r> ...] hiccup vector"
    (let [result (sources/source {:id "my-source" :type "geojson" :data "/data.geojson"})]
      (is (vector? result))
      (is (= :r> (first result))))))

(deftest source-converts-props
  (testing ":id and :type are passed as JS props"
    (let [[_ _comp ^js js-props] (sources/source {:id "pts" :type "geojson"})]
      (is (= "pts"     (.-id js-props)))
      (is (= "geojson" (.-type js-props))))))

(deftest source-accepts-children
  (testing "Layer children are appended after JS props"
    (let [child [:div "layer"]
          result (sources/source {:id "s" :type "geojson"} child)]
      (is (= child (last result))))))

(deftest source-accepts-no-props
  (testing "source can be called with no args"
    (is (vector? (sources/source)))))

;; ---------------------------------------------------------------------------
;; Layer

(deftest layer-returns-reagent-vector
  (testing "layer returns a [:r> ...] hiccup vector"
    (let [result (sources/layer {:id "my-layer" :type "circle" :source "my-source"})]
      (is (vector? result))
      (is (= :r> (first result))))))

(deftest layer-converts-props
  (testing ":id, :type, :source are passed as JS props"
    (let [[_ _comp ^js js-props] (sources/layer {:id "pts" :type "circle" :source "src"})]
      (is (= "pts"    (.-id js-props)))
      (is (= "circle" (.-type js-props)))
      (is (= "src"    (.-source js-props))))))

(deftest layer-converts-paint
  (testing ":paint nested CLJS map is converted to JS with kebab-case keys (MapLibre style spec)"
    (let [[_ _comp ^js js-props] (sources/layer {:id "l"
                                                 :type "circle"
                                                 :source "s"
                                                 :paint {:circle-radius 6
                                                         :circle-color "#f00"}})
          ^js paint (.-paint js-props)]
      (is (= 6     (aget paint "circle-radius")))
      (is (= "#f00" (aget paint "circle-color"))))))

(deftest layer-converts-layout
  (testing ":layout nested CLJS map is converted to JS with kebab-case keys (MapLibre style spec)"
    (let [[_ _comp ^js js-props] (sources/layer {:id "l"
                                                 :type "symbol"
                                                 :source "s"
                                                 :layout {:icon-image "marker"
                                                          :text-field "{name}"}})
          ^js layout (.-layout js-props)]

      (is (= "marker"  (aget layout "icon-image")))
      (is (= "{name}"  (aget layout "text-field"))))))

(deftest layer-preserves-kebab-and-camelcase-top-level-keys
  (testing ":source-layer, :min-zoom, :max-zoom kept as kebab-case; :beforeId kept as camelCase"
    (let [[_ _comp ^js js-props] (sources/layer {:id "l"
                                                 :type "line"
                                                 :source "s"
                                                 :source-layer "landcover"
                                                 :min-zoom 5
                                                 :max-zoom 18
                                                 :beforeId "waterway"})]
      (is (= "landcover"  (aget js-props "source-layer")))
      (is (= 5            (aget js-props "min-zoom")))
      (is (= 18           (aget js-props "max-zoom")))
      (is (= "waterway"   (aget js-props "beforeId"))))))

(deftest layer-accepts-no-props
  (testing "layer can be called with no args"
    (is (vector? (sources/layer)))))
