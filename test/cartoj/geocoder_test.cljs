(ns cartoj.geocoder-test
  "Tests for cartoj.geocoder.

  The geocoder-control component uses useControl and useState (React hooks)
  inside a :f> functional component, so we cannot test runtime behaviour here.
  Instead we verify the hiccup shape and prop handling."
  (:require [cartoj.geocoder :as geocoder]
            [cljs.test :refer [deftest is testing]]))

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
          result    (geocoder/geocoder-control {:position  "bottom-left"
                                                :on-result on-result})]
      (is (vector? result))
      (is (= :f> (first result)))
      (is (fn? (second result))))))

(deftest geocoder-control-with-marker-map
  (testing ":marker as a map of custom Marker props"
    (let [result (geocoder/geocoder-control {:position "top-right"
                                             :marker   {:color "red" :offset [10 0]}})]
      (is (vector? result))
      (is (= :f> (first result))))))

;; ---------------------------------------------------------------------------
;; Pure response-transformation helpers (private, tested via var quote)

(deftest nominatim-url-encodes-query
  (let [nominatim-url #'cartoj.geocoder/nominatim-url]
    (is (= "https://nominatim.openstreetmap.org/search?q=paris&format=geojson&polygon_geojson=1&addressdetails=1"
           (nominatim-url "paris")))
    (is (= "https://nominatim.openstreetmap.org/search?q=San%20Francisco&format=geojson&polygon_geojson=1&addressdetails=1"
           (nominatim-url "San Francisco")))))

(deftest bbox->center-computes-midpoint
  (let [bbox->center #'cartoj.geocoder/bbox->center]
    (is (= [0 0] (bbox->center [0 0 0 0])))
    (is (= [0.5 1.0] (bbox->center [0 0 1 2])))
    (is (= [-122.5 37.75] (bbox->center [-123 37.5 -122 38.0])))))

(deftest feature->point-builds-geocoder-result
  (let [feature->point #'cartoj.geocoder/feature->point
        feature        #js {:bbox       #js [0 0 2 4]
                            :properties #js {:display_name "Paris, France"}}
        ^js point      (feature->point feature)]
    (is (= "Feature" (.-type point)))
    (is (= "Point" (.-type (.-geometry point))))
    (is (= 1 (aget (.-coordinates (.-geometry point)) 0)))
    (is (= 2 (aget (.-coordinates (.-geometry point)) 1)))
    (is (= "Paris, France" (.-place_name point)))
    (is (= "Paris, France" (.-text point)))
    (is (identical? (.-properties feature) (.-properties point)))
    (is (array? (.-place_type point)))
    (is (= "place" (aget (.-place_type point) 0)))
    (is (= 1 (aget (.-center point) 0)))
    (is (= 2 (aget (.-center point) 1)))))
