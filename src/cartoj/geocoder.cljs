(ns cartoj.geocoder
  "Geocoder control component wrapping @maplibre/maplibre-gl-geocoder.

  Usage:
    [geocoder/geocoder-control {:position \"top-right\"}]

  The control uses Nominatim for forward geocoding and optionally displays
  a Marker at the selected result.

  Props:
    :position   — required, one of \"top-left\" \"top-right\" \"bottom-left\" \"bottom-right\"
    :marker     — boolean or map of Marker props (default true)
    :on-loading — callback (fn [^js evt])
    :on-results — callback (fn [^js evt])
    :on-result  — callback (fn [^js evt])
    :on-error   — callback (fn [^js evt])

  All other props are passed to the MaplibreGeocoder constructor
  (kebab-case keys converted to camelCase)."
  (:require ["react" :as react]
            ["@maplibre/maplibre-gl-geocoder/dist/maplibre-gl-geocoder.mjs" :default MaplibreGeocoder]
            [cartoj.interop :as interop]
            [cartoj.overlays :as overlays]
            [cartoj.props :as props]))

;; ---------------------------------------------------------------------------
;; Nominatim forward geocoding API

(def ^:private geocoder-api
  #js {:forwardGeocode
       (fn [^js config]
         (let [query (.-query config)
               url   (str "https://nominatim.openstreetmap.org/search?q="
                          (js/encodeURIComponent query)
                          "&format=geojson&polygon_geojson=1&addressdetails=1")]
           (-> (js/fetch url)
               (.then (fn [^js resp]
                        (.json resp)))
               (.then (fn [^js geojson]
                        (let [features (.-features geojson)
                              result   (array)]
                          (doseq [^js f features]
                            (let [^js bbox (.-bbox f)
                                  center  #js [(+ (aget bbox 0) (/ (- (aget bbox 2) (aget bbox 0)) 2))
                                               (+ (aget bbox 1) (/ (- (aget bbox 3) (aget bbox 1)) 2))]
                                  point   #js {:type       "Feature"
                                               :geometry   #js {:type        "Point"
                                                                :coordinates center}
                                               :place_name (.. f -properties -display_name)
                                               :properties (.-properties f)
                                               :text       (.. f -properties -display_name)
                                               :place_type #js ["place"]
                                               :center     center}]
                              (.push result point)))
                          #js {:features result})))
               (.catch (fn [^js e]
                         (js/console.error "Failed to forwardGeocode with error:" e)
                         #js {:features #js []})))))})

;; ---------------------------------------------------------------------------
;; Imperative setter helpers
;;
;; Maps kebab-case prop keys to [getter setter] method name pairs on the
;; MaplibreGeocoder instance.  Used to push prop updates into the control
;; after initial creation.

(def ^:private prop->methods
  {:proximity   ["getProximity"        "setProximity"]
   :render      ["getRenderFunction"   "setRenderFunction"]
   :language    ["getLanguage"         "setLanguage"]
   :zoom        ["getZoom"             "setZoom"]
   :fly-to      ["getFlyTo"            "setFlyTo"]
   :placeholder ["getPlaceholder"      "setPlaceholder"]
   :countries   ["getCountries"        "setCountries"]
   :types       ["getTypes"            "setTypes"]
   :min-length  ["getMinLength"        "setMinLength"]
   :limit       ["getLimit"            "setLimit"]
   :filter      ["getFilter"           "setFilter"]})

(defn- update-geocoder-props
  "For each key in prop->methods whose value differs from what the geocoder
  currently holds, call the corresponding setter."
  [^js geocoder prop-map]
  (when (.-_map geocoder)
    (doseq [[k [getter setter]] prop->methods]
      (when (contains? prop-map k)
        (let [v (get prop-map k)]
          (when (not= (.call (aget geocoder getter) geocoder) v)
            (.call (aget geocoder setter) geocoder v)))))))

;; ---------------------------------------------------------------------------
;; Public component

(defn geocoder-control
  "Reagent component wrapping @maplibre/maplibre-gl-geocoder via useControl.

  Required props:
    :position — control position string

  Optional props:
    :marker     — boolean or map of Marker props (default true)
    :on-loading — (fn [^js evt])
    :on-results — (fn [^js evt])
    :on-result  — (fn [^js evt])
    :on-error   — (fn [^js evt])

  Plus any MaplibreGeocoder options (kebab-case converted to camelCase)."
  [& args]
  (let [[prop-map _] (props/props-and-children args)
        prop-map     (or prop-map {})
        {:keys [marker position on-loading on-results on-result on-error]
         :or   {marker true}} prop-map
        js-props     (props/props->js (dissoc prop-map
                                              :marker :position
                                              :on-loading :on-results :on-result :on-error))]
    [:f>
     (fn []
       (let [[marker-pos set-marker!] (react/useState nil)
             geocoder (interop/use-control
                       (fn [^js ctx]
                         (let [^js map-lib (.-mapLib ctx)]
                           (aset js-props "marker" false)
                           (aset js-props "maplibregl" map-lib)
                           (let [^js ctrl (MaplibreGeocoder. geocoder-api js-props)]
                             (.on ctrl "loading" (or on-loading #()))
                             (.on ctrl "results" (or on-results #()))
                             (.on ctrl "result"
                                  (fn [^js evt]
                                    (when on-result (on-result evt))
                                    (let [^js result   (.-result evt)
                                          location    (when result
                                                        (or (.-center result)
                                                            (when (and (.. result -geometry -type)
                                                                       (= (.. result -geometry -type) "Point"))
                                                              (.. result -geometry -coordinates))))]
                                      (if (and location marker)
                                        (let [m-props (if (map? marker) marker {})]
                                          (set-marker! #js {:lx (aget location 0)
                                                            :ly (aget location 1)
                                                            :mp m-props}))
                                        (set-marker! nil)))))
                             (.on ctrl "error" (or on-error #()))
                             ctrl)))
                       #js {:position position})]
         (update-geocoder-props geocoder prop-map)
         (when ^js marker-pos
           [overlays/marker
            (merge (.-mp ^js marker-pos)
                   {:longitude (.-lx ^js marker-pos)
                    :latitude  (.-ly ^js marker-pos)})])))]))
