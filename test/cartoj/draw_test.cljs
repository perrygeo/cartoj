(ns cartoj.draw-test
  "Tests for cartoj.draw — verifies hiccup shape and prop handling."
  (:require [cljs.test :refer [deftest is testing]]
            [cartoj.draw :as draw]))

(deftest default-styles-is-maplibre-compatible
  (testing "default-styles ships a corrected mapbox-gl-draw theme whose case
            branches wrap raw array literals in ['literal' ...], which MapLibre 5
            requires (otherwise gl-draw-lines is silently dropped on add)"
    (is (vector? draw/default-styles))
    (is (pos? (count draw/default-styles)))
    (let [lines-style (first (filter #(= "gl-draw-lines" (get % "id"))
                                     draw/default-styles))
          dash-expr   (get-in lines-style ["paint" "line-dasharray"])]
      (is (some? lines-style)
          "must include the gl-draw-lines layer")
      (is (= "case" (first dash-expr))
          "line-dasharray must still be a case expression for active vs inactive")
      (is (some #(and (vector? %) (= "literal" (first %))) dash-expr)
          "case branches that are arrays must be wrapped in ['literal' ...]"))))

(deftest compat-css-shim-covers-maplibre-aliases
  (testing "compat-css string aliases the legacy mapboxgl-* control classes
            that mapbox-gl-draw emits but MapLibre 3+ no longer styles"
    (is (string? draw/compat-css))
    (is (re-find #"\.mapboxgl-ctrl\b" draw/compat-css)
        "must target .mapboxgl-ctrl so pointer-events overrides the position slot")
    (is (re-find #"pointer-events\s*:\s*auto" draw/compat-css)
        "must restore pointer-events: auto so toolbar clicks land")
    (is (re-find #"\.mapboxgl-ctrl-group\b" draw/compat-css)
        "must target .mapboxgl-ctrl-group so the white pill background returns")))

(deftest draw-control-returns-f>-vector
  (testing "draw-control returns a [:f> fn] hiccup vector"
    (let [result (draw/draw-control {:position "top-left"})]
      (is (vector? result))
      (is (= :f> (first result)))
      (is (fn? (second result))))))

(deftest draw-control-accepts-no-props
  (testing "no-arg call does not crash"
    (is (vector? (draw/draw-control)))))

(deftest draw-control-with-callbacks
  (testing "event callback props yield valid :f> vector"
    (let [on-create (fn [^js _e] nil)
          on-update (fn [^js _e] nil)
          on-delete (fn [^js _e] nil)
          result    (draw/draw-control {:position "top-right"
                                        :on-create on-create
                                        :on-update on-update
                                        :on-delete on-delete})]
      (is (vector? result))
      (is (= :f> (first result)))
      (is (fn? (second result))))))

(deftest draw-control-recognizes-on-render
  (testing ":on-render is recognized as a callback prop — captured in the internal
            config (so draw-component can subscribe to draw.render) and NOT
            forwarded to MapboxDraw as a constructor option"
    (let [on-render (fn [^js _e ^js _d] nil)
          _         (draw/draw-control {:position "top-left"
                                        :on-render on-render})
          cfg       @cartoj.draw/draw-config]
      (is (identical? on-render (:on-render cfg))
          "on-render must be threaded into draw-config so the draw.render
           listener can invoke the current consumer callback")
      (is (undefined? (.-onRender ^js (:draw-props cfg)))
          "on-render must be dissoc'd from the props forwarded to MapboxDraw,
           otherwise mapbox-gl-draw will get an unknown option"))))

(deftest draw-control-passes-through-draw-options
  (testing "extra props like :display-controls-default pass through"
    (let [result (draw/draw-control {:position "top-left"
                                      :display-controls-default false
                                      :controls {:polygon true
                                                 :trash true}})]
      (is (vector? result))
      (is (= :f> (first result)))
      (is (fn? (second result))))))

(deftest draw-control-defaults-combine-disabled
  (testing "combine_features and uncombine_features default to false"
    (draw/draw-control {:position "top-left"})
    (let [cfg @draw/draw-config
          controls (aget (:draw-props cfg) "controls")]
      (is (some? controls) "controls should always be present")
      (is (false? (aget controls "combine_features"))
          "combine_features must default to false")
      (is (false? (aget controls "uncombine_features"))
          "uncombine_features must default to false"))))

(deftest draw-control-allows-overriding-combine
  (testing "user can explicitly enable combine_features"
    (draw/draw-control {:position "top-left"
                        :controls {:combine-features true}})
    (let [cfg @draw/draw-config
          controls (aget (:draw-props cfg) "controls")]
      (is (true? (aget controls "combine_features"))
          "explicit :combine-features true must override default false")
      (is (false? (aget controls "uncombine_features"))
          "uncombine_features must still default to false when not overridden"))))