(ns cartoj.core-test
  "Tests for cartoj.core Map component.

  NOTE: Because react-map-gl renders into a DOM and requires WebGL, we test
  the *shape* of the hiccup produced by the component functions rather than
  full render correctness. This mirrors the approach used in reagent-material-ui
  and headlessui-reagent: verify the wrapper produces [:> ReactComp props ...]
  with correctly converted props, rather than trying to mount the full component."
  (:require [cartoj.core :as core]
            [cljs.test :refer [deftest is testing]]))

(deftest map-returns-reagent-vector
  (testing "map with no props returns [:div ... :r> Map ...] (always wrapped)"
    (let [result (core/interactive-map {})]
      (is (vector? result))
      (is (= :div (first result)))
      (is (= "cartoj-interactive-map" (get-in result [1 :class]))))))

(deftest map-passes-map-style
  (testing ":map-style is converted to camelCase JS prop"
    (let [result                 (core/interactive-map {:map-style "https://example.com/style.json"})
          [_ _comp ^js js-props] (nth result 2)]
      (is (= "https://example.com/style.json" (.-mapStyle js-props))))))

(deftest map-passes-initial-view-state
  (testing ":initial-view-state nested map is converted to JS"
    (let [result                 (core/interactive-map {:initial-view-state {:longitude -122.4
                                                                             :latitude  37.8
                                                                             :zoom      14}})
          [_ _comp ^js js-props] (nth result 2)
          ^js ivs                (.-initialViewState js-props)]
      (is (= -122.4 (.-longitude ivs)))
      (is (= 37.8   (.-latitude ivs)))
      (is (= 14     (.-zoom ivs))))))

(deftest map-passes-callbacks
  (testing ":on-click callback passes through unchanged"
    (let [cb                     (fn [e] e)
          result                 (core/interactive-map {:on-click cb})
          [_ _comp ^js js-props] (nth result 2)]
      (is (identical? cb (.-onClick js-props))))))

(deftest map-accepts-children
  (testing "children are appended after the JS props, inside the wrapped :r>"
    (let [child  [:div "hello"]
          result (core/interactive-map {} child)
          inner  (nth result 2)]
      (is (= child (last inner))))))

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

(deftest map-default-class-name
  (testing "default class-name is cartoj-interactive-map when not provided"
    (let [result (core/interactive-map {:map-style "x"})]
      (is (= :div (first result)))
      (is (= "cartoj-interactive-map" (get-in result [1 :class]))))))

(deftest map-class-name-wrapper-preserves-children
  (testing "children appear inside the inner Map element when wrapped"
    (let [child  [:div "hello"]
          result (core/interactive-map {:class-name "imap"} child)
          inner  (nth result 2)]
      (is (= child (last inner))))))

;; ---------------------------------------------------------------------------
;; Source

(deftest source-returns-reagent-vector
  (testing "source returns a [:r> ...] hiccup vector"
    (let [result (core/source {:id "my-source" :type "geojson" :data "/data.geojson"})]
      (is (vector? result))
      (is (= :r> (first result))))))

(deftest source-converts-props
  (testing ":id and :type are passed as JS props"
    (let [[_ _comp ^js js-props] (core/source {:id "pts" :type "geojson"})]
      (is (= "pts"     (.-id js-props)))
      (is (= "geojson" (.-type js-props))))))

(deftest source-accepts-children
  (testing "Layer children are appended after JS props"
    (let [child  [:div "layer"]
          result (core/source {:id "s" :type "geojson"} child)]
      (is (= child (last result))))))

(deftest source-accepts-no-props
  (testing "source can be called with no args"
    (is (vector? (core/source)))))

;; ---------------------------------------------------------------------------
;; Layer

(deftest layer-returns-reagent-vector
  (testing "layer returns a [:r> ...] hiccup vector"
    (let [result (core/layer {:id "my-layer" :type "circle" :source "my-source"})]
      (is (vector? result))
      (is (= :r> (first result))))))

(deftest layer-converts-props
  (testing ":id, :type, :source are passed as JS props"
    (let [[_ _comp ^js js-props] (core/layer {:id "pts" :type "circle" :source "src"})]
      (is (= "pts"    (.-id js-props)))
      (is (= "circle" (.-type js-props)))
      (is (= "src"    (.-source js-props))))))

(deftest layer-converts-paint
  (testing ":paint nested CLJS map is converted to JS with kebab-case keys (MapLibre style spec)"
    (let [[_ _comp ^js js-props] (core/layer {:id     "l"
                                              :type   "circle"
                                              :source "s"
                                              :paint  {:circle-radius 6
                                                       :circle-color  "#f00"}})
          ^js paint              (.-paint js-props)]
      (is (= 6     (aget paint "circle-radius")))
      (is (= "#f00" (aget paint "circle-color"))))))

(deftest layer-converts-layout
  (testing ":layout nested CLJS map is converted to JS with kebab-case keys (MapLibre style spec)"
    (let [[_ _comp ^js js-props] (core/layer {:id     "l"
                                              :type   "symbol"
                                              :source "s"
                                              :layout {:icon-image "marker"
                                                       :text-field "{name}"}})
          ^js layout             (.-layout js-props)]

      (is (= "marker"  (aget layout "icon-image")))
      (is (= "{name}"  (aget layout "text-field"))))))

(deftest layer-preserves-kebab-and-camelcase-top-level-keys
  (testing ":source-layer, :min-zoom, :max-zoom kept as kebab-case; :beforeId kept as camelCase"
    (let [[_ _comp ^js js-props] (core/layer {:id           "l"
                                              :type         "line"
                                              :source       "s"
                                              :source-layer "landcover"
                                              :min-zoom     5
                                              :max-zoom     18
                                              :beforeId     "waterway"})]
      (is (= "landcover"  (aget js-props "source-layer")))
      (is (= 5            (aget js-props "min-zoom")))
      (is (= 18           (aget js-props "max-zoom")))
      (is (= "waterway"   (aget js-props "beforeId"))))))

(deftest layer-accepts-no-props
  (testing "layer can be called with no args"
    (is (vector? (core/layer)))))
