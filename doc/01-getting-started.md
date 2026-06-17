# Getting Started

## Installation

In a `deps.edn` file install cartoj and the shadow-cljs tooling

```clojure
{:paths   ["src"]
 :deps    {io.github.perrygeo/cartoj {:mvn/version "0.1.0"}}
 :aliases {:shadow-cljs {:extra-deps {thheller/shadow-cljs {:mvn/version "3.4.11"}}}}}
```

## Configuration


We have to configure both the shadow-cljs app and the npm dependencies.
### `shadow-cljs.edn`


```clojure
{:deps     {:aliases [:shadow-cljs]}
 :dev-http {8080 "public"}
 :builds   {:app {:target     :browser
                  :output-dir "public/js"
                  :asset-path "/js"
                  :modules    {:main {:init-fn example.core/init}}}}}
```

### `package.json`

Defines our required NPM dependencies

```json
{
  "name": "cartoj-example",
  "version": "0.1.0",
  "private": true,
  "dependencies": {
    "maplibre-gl": "^5.0.0",
    "pmtiles": "^4.4.1",
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "react-map-gl": "^8.0.0"
  },
  "devDependencies": {
    "shadow-cljs": "^2.28.0"
  }
}
```


## HTML

A basic web page containing some essential CSS styling. As a single-page application,
our app will fetch the javascript payload and take over the `"app"` div.

```html
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <title>Cartoj example</title>
    <link
      rel="stylesheet"
      href="https://unpkg.com/maplibre-gl/dist/maplibre-gl.css"
    />
    <style>
      body {
        margin: 1.5rem;
      }
      .cartoj-interactive-map {
        margin: 0;
        height: 480px;
        width: 100%;
        max-width: 960px;
      }
    </style>
  </head>
  <body>
    <div id="app"></div>
    <script src="/js/main.js"></script>
  </body>
</html>
```

## Application

Finally, the application itself

```clojure
(ns example.core
  (:require [reagent.core :as r]
            [reagent.dom.client :as rdom]
            [cartoj.core :as cartoj]))

(defn app []
  [:div
   [:h2 "Cartoj Hello World"]
   [cartoj/interactive-map
    {:initial-view-state {:longitude 0
                          :latitude 16
                          :zoom 1}
     :map-style "https://tiles.openfreemap.org/styles/bright"}]])

(defonce root (atom nil))

(defn ^:dev/after-load re-render []
  (.render ^js @root (r/as-element [app])))

(defn init []
  (reset! root (rdom/create-root (js/document.getElementById "app")))
  (.render ^js @root (r/as-element [app])))
```

## Development

```bash
npm install
npx shadow-cljs watch app
```

Open <http://localhost:8080>. You should see the MapLibre demo basemap
covering the viewport.

## Compile for production

To create a minified, optimized version of the app

```bash
npx shadow-cljs compile app
```
