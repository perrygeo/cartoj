# Terminology

Using `Cartoj` requires several concepts that require more explanation if you're new to geospatial data. There's nothing new here, just an overview of the vocabulary and some links to their origin.

## Map

Cartographers almost universally refer to visualizations of spatial data as a _map_.

Clojure users already use _map_ for two other concepts. First, `map` as function application for functional programming. Second `map` as the associative data structure denoted with curly braces `{}`.

Cartoj tries to use the term "interactive map" to disambiguate. Yes `carto/interactive-map` is more verbose, but less confusing than the alternative. We don't need a third `map`.

## Features

In geospatial data terms, a *Feature* is a "spatially bounded thing", an entity with a spatial representation.

The term was originally used in cartography (i.e. "Features of the map"). It has been formalized in both the [OGC Simple Features](https://en.wikipedia.org/wiki/Simple_Features) and the [GeoJSON](https://datatracker.ietf.org/doc/html/rfc7946#section-3.2) standard. The concepts in Maplibre are based on the GeoJSON flavor.

A Feature consists of two parts:

* *properties*: a dictionary of the feature's attributes. Typically flat, aka columns.
* *geometry*: the feature's spatial representation in 2D space. Can be a point, line, polygon, or their multipart variations.

Another way of thinking about a Feature: It's like a *row* in a relational database, with a special column and data type for representing the geometry.


## Sources

*Sources* are the concrete implementation format, the data delivery mechanism. [Documentation](https://maplibre.org/maplibre-style-spec/sources/)
There are four primary source types:

* *vector*: Tiled vector data, in the [Mapbox Vector Tile](https://docs.mapbox.com/data/tilesets/guides/vector-tiles-standards/) format
* *raster*: Tiled, in standard image formats (PNG, JPG, etc.). These are either inherently raster (imagery) or are vector sources rendered to images server-side.
* *raster-dem*: Like raster but with a special encoding to define terrain for 3D maps, [Mapbox Terrain RGB](https://blog.mapbox.com/global-elevation-data-6689f1d0ba65)
* *geojson*: Not tiled, typically a complete dataset in [GeoJSON](https://geojson.org/) format


## Styles

> A MapLibre style is a document that defines the visual appearance of a map: what data to draw, the order to draw it in, and how to style the data when drawing it. 

From the [Mapblibre Style Spec](https://maplibre.org/maplibre-style-spec/)

## Layers

*Layers* are the main abstraction for building interactive maps. They combine a data source with a style.

One source can be associated with many styles thus many layers. The source layer and the visual layer are decoupled. It's common to have one source with two layers; one for the geometry itself, another for its label.
