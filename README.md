# Cartoj

[![Tests](https://github.com/perrygeo/cartoj/actions/workflows/test.yml/badge.svg)](https://github.com/perrygeo/cartoj/actions/workflows/test.yml)
A ClojureScript / Reagent component for interactive map components.
Built on [react-map-gl](https://visgl.github.io/react-map-gl/) (MapLibre edition).

## Status

**Alpha** — API is not stable, not yet published to Clojars.


## Installation

> Not yet on Clojars. The deps coordinate below is the intended shape

Add to `deps.edn`:

```clojure
{:deps {io.github.perrygeo/cartoj {:mvn/version "0.1.0"}}}
```

Or in `shadow-cljs.edn`:

```clojure
:dependencies [[io.github.perrygeo/cartoj "0.1.0"]]
```

## Usage


```clojure
(defn on-click-example []
  (let [last-point    (r/atom nil)
        click-handler (fn [^js e]
                        (reset! last-point (coords-from-evt e)))]
    (fn []
      [:section
       [:h2 "On click event"]
       [cartoj/interactive-map {:initial-view-state {:latitude 16 :zoom 1}
                                :map-style default-stylesheet
                                :on-click click-handler}
        [ctrl/navigation-control {:position "top-right"}]]
       [:table {:style {:width "250px"}}
        [:tbody
         [:tr
          [:th "Longitude"]
          [:td (if-let [lng (:longitude @last-point)]
                 (.toFixed lng 4) "-")]]
         [:tr
          [:th "Latitude"]
          [:td (if-let [lat (:latitude @last-point)]
                 (.toFixed lat 4) "-")]]]]
       [:p "Get the coordinates of a map click."]])))
```

<img src="on-click-example.jpg">
