# Changelog

## 0.4.0-alpha1
* `pmtiles` is now *actually* optional; a basic map needs only `react`, `react-dom`, `react-map-gl`, and `maplibre-gl`. You must explicitly require `cartoj.pmtiles` to register the `pmtiles://` protocol.
## 0.3.0

* `sources` and `layers` are now in the `cartoj.core` namespace
* The view helpers were moved out of core into `cartoj.interop`
## 0.2.0

- Make the `pmtiles` npm library optional. #1

## 0.1.0

* Initial Release
