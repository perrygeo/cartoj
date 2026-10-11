# Changelog

## 0.4.0

* breaking: remove `cartoj.re-frame` namespace and the `re-frame` dependency
* feature: add `globe-control` and `logo-control` components to `cartoj.controls`
* feature: add `coords->center` and `coords->lnglat` converters to `cartoj.interop`
* deps: `pmtiles` is now truly optional — require `cartoj.pmtiles` to register the `pmtiles://` protocol
* docs: overhaul README, getting-started guide, and starter template
* test: add Playwright end-to-end test suite
* test: confirm the full set of props for react-map-gl 8
## 0.3.0

* `sources` and `layers` are now in the `cartoj.core` namespace
* The view helpers were moved out of core into `cartoj.interop`
## 0.2.0

- Make the `pmtiles` npm library optional. #1

## 0.1.0

* Initial Release
