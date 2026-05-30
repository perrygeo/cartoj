(ns cartoj.re-frame
  "Optional re-frame helpers for cartoj.

  This namespace is NOT required by any other cartoj namespace — it is
  an add-on for anyone using re-frame as their state manager.

  Provides:
    - ::set-view-state  event handler — store a view-state map in app-db
    - ::view-state      subscription  — read the current view-state map
    - on-move-handler   — returns an onMove callback that dispatches ::set-view-state

  Usage (controlled map with re-frame):

    ;; In your event/sub registration (call once at app startup — these are
    ;; registered automatically when this namespace is required):

    (require '[cartoj.re-frame :as cartoj-rf])
    (require '[cartoj.core     :as carto])

    ;; In your component:
    (let [vs @(rf/subscribe [::cartoj-rf/view-state])]
      [carto/map (merge vs
                        {:on-move  (cartoj-rf/on-move)
                         :map-style \"https://demotiles.maplibre.org/style.json\"})])"
  (:require [re-frame.core :as rf]
            [cartoj.core   :as core]))

(def ^:private db-key
  ::view-state)

(rf/reg-event-db ::set-view-state
                 (fn [db [_ view-state]]
                   (assoc db db-key view-state)))

(rf/reg-sub ::view-state
            (fn [db _]
              (get db db-key)))

(defn on-move
  "Returns an onMove callback suitable for passing as :on-move to cartoj.core/map.

  Options (optional map):
    :sync? – when true, uses rf/dispatch-sync (default: false, uses rf/dispatch)

  When called with a react-map-gl move event, it extracts the viewState
  JS object, converts it to a CLJS map via core/view-state->clj, and
  dispatches [::set-view-state view-state-map].

  Example:
    [core/map {:on-move (cartoj-rf/on-move) :map-style \"...\"}]"
  ([] (on-move nil))
  ([{:keys [sync?] :or {sync? false}}]
   (fn [^js evt]
     (let [vs     (core/view-state->clj (.-viewState evt))
           dispatch (if sync? rf/dispatch-sync rf/dispatch)]
       (dispatch [::set-view-state vs])))))
