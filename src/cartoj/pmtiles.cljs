(ns cartoj.pmtiles
  "Registers the pmtiles:// protocol with maplibre-gl so PMTiles archives
   can be used as map style URLs and tile sources."
  (:require ["maplibre-gl" :as ml]
            ["pmtiles" :refer [Protocol]]))

(defonce ^:private _registered
  (try
    (.addProtocol ml "pmtiles" (.-tile (Protocol.)))
    true
    (catch :default _e
      (js/console.warn "cartoj: pmtiles library not found, pmtiles:// protocol not registered")
      false)))
