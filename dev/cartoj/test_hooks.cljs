(ns cartoj.test-hooks
  "Dev/test-only instrumentation that exposes MapLibre readiness to an
   out-of-process test runner (Playwright) via a `window.__cartoj` object.

   This namespace is NOT part of the published library. It is only required
   by the demo harness so that end-to-end tests can deterministically wait
   for maps to finish rendering instead of sleeping.

   Mechanism: it wraps `cartoj.core/interactive-map` in place (via `set!`)
   so every map created by any demo section gets an `:on-load` hook injected,
   without touching the source of the individual sections (keeping the
   on-screen code samples accurate). On load it records the map and, once the
   map emits `idle` (tiles loaded, animations settled), bumps an idle counter.

   Test contract (all under `window.__cartoj`):
     maps        – array of live MapLibre map instances for the current tab
     loadCount   – number of maps that have fired `load`
     idleCount   – number of maps that have reached `idle`
     errorCount  – number of maps that have fired `error`
     reset()     – clear all counters/maps (called automatically on tab change)"
  (:require
   [cartoj.core :as core]))

(defn reset-state!
  "Clear counters and the list of tracked maps. Called on each tab change so
   that readiness waits only observe maps belonging to the current tab."
  []
  (let [s (.-__cartoj js/window)]
    (when s
      (set! (.-maps s) #js [])
      (set! (.-loadCount s) 0)
      (set! (.-idleCount s) 0)
      (set! (.-errorCount s) 0))))

(defn- state
  "Return the (lazily created) window.__cartoj state object."
  []
  (or (.-__cartoj js/window)
      (let [s #js {:maps       #js []
                   :loadCount  0
                   :idleCount  0
                   :errorCount 0}]
        (set! (.-reset s) (fn [] (reset-state!)))
        (set! (.-__cartoj js/window) s)
        s)))

(defn- register!
  "Record a freshly loaded map and arm an idle/error listener on it."
  [^js e]
  (let [s (state)
        m (.-target e)]
    (.push (.-maps s) m)
    (set! (.-loadCount s) (inc (.-loadCount s)))
    (.once m "idle" (fn [] (set! (.-idleCount s) (inc (.-idleCount s)))))
    (.on m "error" (fn [] (set! (.-errorCount s) (inc (.-errorCount s)))))))

(defonce ^:private original (atom nil))

(defn- wrapped-interactive-map
  "Drop-in replacement for cartoj.core/interactive-map that injects an
   :on-load hook (chained after any user-supplied :on-load) and then
   delegates to the original implementation."
  [& args]
  (let [has-props? (map? (first args))
        props      (if has-props? (first args) {})
        rest-args  (if has-props? (rest args) args)
        user-cb    (:on-load props)
        props'     (assoc props :on-load
                          (fn [e]
                            (register! e)
                            (when user-cb (user-cb e))))]
    (apply @original props' rest-args)))

(defn install!
  "Install the instrumentation. Idempotent. Pass the demo's `selected-tab`
   reagent atom so counters reset automatically whenever the tab changes."
  [selected-tab]
  (when (nil? @original)
    (reset! original core/interactive-map)
    (set! core/interactive-map wrapped-interactive-map)
    (add-watch selected-tab ::reset (fn [_ _ _ _] (reset-state!))))
  (state)
  (reset-state!)
  nil)
