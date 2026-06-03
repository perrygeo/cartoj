(ns cartoj.props-test
  (:require [cljs.test :refer [deftest is testing]]
            [cartoj.props :as props]))

;; ---------------------------------------------------------------------------
;; kebab->camel

(deftest kebab->camel-simple
  (testing "single word unchanged"
    (is (= "foo" (props/kebab->camel :foo))))
  (testing "two words"
    (is (= "fooBar" (props/kebab->camel :foo-bar))))
  (testing "three words"
    (is (= "initialViewState" (props/kebab->camel :initial-view-state))))
  (testing "already camelCase string passthrough"
    (is (= "onMove" (props/kebab->camel "onMove")))))

;; ---------------------------------------------------------------------------
;; props->js — shallow conversion of a CLJS map to a JS object

(deftest props->js-empty
  (testing "empty map yields empty JS object"
    (let [result (props/props->js {})]
      (is (object? result))
      (is (= 0 (count (js/Object.keys result)))))))

(deftest props->js-converts-keys
  (testing "kebab keys become camelCase"
    (let [result (props/props->js {:initial-view-state {:longitude 0 :latitude 0 :zoom 5}
                                   :map-style "https://example.com/style.json"
                                   :on-click identity})]
      (is (= "https://example.com/style.json" (.-mapStyle result)))
      (is (fn? (.-onClick result))))))

(deftest props->js-passes-callbacks-through
  (testing "callback functions are passed through without wrapping"
    (let [cb (fn [e] e)
          result (props/props->js {:on-move cb})]
      (is (identical? cb (.-onMove result))))))

(deftest props->js-converts-nested-data-maps
  (testing ":initial-view-state nested CLJS map is converted to JS"
    (let [result (props/props->js {:initial-view-state {:longitude -122.4
                                                        :latitude 37.8
                                                        :zoom 14}})
          ivs (.-initialViewState result)]

      (is (= -122.4 (.-longitude ivs)))
      (is (= 37.8   (.-latitude ivs)))
      (is (= 14     (.-zoom ivs)))))

  (testing ":style nested CLJS map is converted to JS"
    (let [result (props/props->js {:style {:width "100%" :height "400px"}})
          s      (.-style result)]
      (is (= "100%"  (.-width s)))
      (is (= "400px" (.-height s)))))

  (testing ":max-bounds vector-of-vectors is converted to JS array"
    (let [result (props/props->js {:max-bounds [[-122.75 37.0] [-121.5 38.5]]})
          mb (.-maxBounds result)]
      (is (array? mb))
      (is (= 2 (.-length mb)))
      (is (array? (aget mb 0)))
      (is (= -122.75 (aget (aget mb 0) 0)))
      (is (= 38.5    (aget (aget mb 1) 1))))))

(deftest props->js-string-key-passthrough
  (testing "string keys are used as-is (no camel conversion)"
    (let [result (props/props->js {"mapStyle" "https://example.com/style.json"})]
      (is (= "https://example.com/style.json" (.-mapStyle result))))))

(deftest props->js-converts-interactive-layer-ids
  (testing ":interactive-layer-ids vector is converted to JS array"
    (let [result (props/props->js {:interactive-layer-ids ["clusters" "cluster-count"]})
          ids (.-interactiveLayerIds result)]
      (is (array? ids))
      (is (= 2 (.-length ids)))
      (is (= "clusters" (aget ids 0)))
      (is (= "cluster-count" (aget ids 1))))))

(deftest props->js-converts-paint-expressions
  (testing ":paint vector expressions (heatmap-color) become JS arrays"
    (let [result (props/props->js {:paint {:heatmap-color ["interpolate" ["linear"] ["heatmap-density"]
                                                           0 "rgba(33,102,172,0)"
                                                           0.2 "rgb(103,169,207)"
                                                           1 "rgb(178,24,43)"]}})
          paint (.-paint result)]
      (is (object? paint))
      (let [hc (aget paint "heatmap-color")]
        (is (array? hc))
        (is (= 9 (.-length hc)) "hc length is 9")
        (is (= "interpolate" (aget hc 0)))
        (let [inner (aget hc 1)]
          (is (array? inner))
          (is (= 1 (.-length inner)))
          (is (= "linear" (aget inner 0))))))))

(deftest props->js-converts-terrain-sky-light-maps
  (testing ":terrain nested CLJS map converted with kebab keys"
    (let [^js result (props/props->js {:terrain {:source "my-dem" :exaggeration 1.5}})
          ter (.-terrain result)]
      (is (= "my-dem" (.-source ter)))
      (is (= 1.5 (.-exaggeration ter)))))
  (testing ":sky nested CLJS map converted with kebab keys"
    (let [^js result (props/props->js {:sky {:sky-color "#80ccff"
                                              :sky-horizon-blend 0.5}})
          sky (.-sky result)]
      (is (= "#80ccff" (aget sky "sky-color")))
      (is (= 0.5 (aget sky "sky-horizon-blend")))))
  (testing ":light nested CLJS map converted with kebab keys"
    (let [^js result (props/props->js {:light {:anchor "map"
                                                :color "#ffffff"}})
          light (.-light result)]
      (is (= "map" (.-anchor light)))
      (is (= "#ffffff" (.-color light)))))
  (testing ":map-style with nested sources gets camelCase, sky gets kebab-case"
    (let [^js result (props/props->js {:map-style {:version 8
                                                    :sources {:dem {:type "raster-dem"
                                                                    :tile-size 256}}
                                                    :sky {:sky-color "#80ccff"}
                                                    :layers [{:type "hillshade"
                                                              :paint {:hillshade-exaggeration 0.5}}]}})
          ^js ms (.-mapStyle result)]
      (is (object? ms))
      (let [^js sources (.-sources ms)
            ^js dem (.-dem sources)]
        (is (object? dem))
        (is (= "raster-dem" (.-type dem)))
        (is (= 256 (.-tileSize dem))))
      (let [sky (.-sky ms)]
        (is (= "#80ccff" (aget sky "sky-color"))))
      (let [^js layers (.-layers ms)
            ^js l0 (aget layers 0)
            paint (.-paint l0)]
        (is (= 0.5 (aget paint "hillshade-exaggeration")))))))
