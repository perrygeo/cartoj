(ns cartoj.bench
  "Throughput benchmark for cartoj.props/props->js.

  This module was vibe-coded by Claude. I can't fully vouch for it.
  Proved that memoization of kebab->camel is a win, ~2x improvement.
  Figured I'd keep it around.

  Goal: measure the cost of CLJS→JS prop conversion under realistic
  workloads to decide whether additional cachine is worth the complexity.

  Run with:

      npm run bench

  Each scenario generates N distinct prop maps (so the conversion is
  doing real work, not hitting any incidental cache) and times how long
  it takes to convert them all. Each measurement is repeated S times and
  we report min / median across samples.

  Output is ops/sec and µs/op. To gut-check whether caching matters at
  all, compare to a 60fps React frame budget (16.7 ms) — if your typical
  app does, say, 50 props->js calls per frame, anything above ~3 µs/op
  is a non-issue in absolute terms."
  (:require [cartoj.props :as props]))

(defn- now [] (js/performance.now))

(def ^:private sink
  "Anti-DCE sink. We push results in so the JIT can't prove they're
  unused and eliminate the work."
  #js [])

(defn- reset-sink! []
  (set! (.-length sink) 0))

(defn- run-once [maps]
  (doseq [m maps]
    (.push sink (props/props->js m)))
  (.-length sink))

;; ---------------------------------------------------------------------------
;; Scenario generators — produce N *distinct* prop maps so neither prop->js
;; nor any internal memoization can hit a hot path on repeated identity.

(defn- gen-simple [n]
  (mapv (fn [i]
          {:width 800
           :height 600
           :longitude (* 0.01 i)
           :latitude (* 0.005 i)
           :zoom (mod i 20)
           :on-click identity
           :on-move identity})
        (range n)))

(defn- gen-with-style [n]
  (mapv (fn [i]
          {:longitude (* 0.01 i)
           :latitude (* 0.005 i)
           :initial-view-state {:longitude (* 0.01 i)
                                :latitude (* 0.005 i)
                                :zoom (mod i 20)
                                :pitch 45
                                :bearing 0}
           :style {:position "absolute"
                   :width "100%"
                   :height "100%"}
           :map-style "https://demotiles.maplibre.org/style.json"})
        (range n)))

(defn- gen-style-spec [n]
  (mapv (fn [i]
          {:id (str "layer-" i)
           :type "circle"
           :source "points"
           :paint {:circle-radius (mod i 20)
                   :circle-color "red"
                   :circle-opacity 0.7
                   :circle-stroke-width 2
                   :circle-stroke-color "white"
                   :circle-blur 0.1}
           :layout {:visibility "visible"}
           :filter ["==" "kind" (str "k-" (mod i 10))]})
        (range n)))

(defn- gen-kitchen-sink [n]
  (mapv (fn [i]
          {:initial-view-state {:longitude (* 0.01 i)
                                :latitude 0
                                :zoom 10
                                :pitch 45
                                :bearing 0}
           :style {:width "100%" :height "100%" :position "absolute"}
           :map-style "https://demotiles.maplibre.org/style.json"
           :interactive-layer-ids ["points" "lines" "polygons"]
           :reuse-maps true
           :on-click identity
           :on-move identity
           :on-load identity
           :on-resize identity})
        (range n)))

;; ---------------------------------------------------------------------------
;; Timing harness

(defn- bench
  "Time `n-ops` calls to props->js over `maps`, `n-samples` times.
  Returns {:min :median :max} in ms for the full batch."
  [maps n-ops n-samples]
  ;; warmup — let V8's optimizer kick in
  (dotimes [_ 3]
    (reset-sink!)
    (run-once maps))
  (let [samples (vec (for [_ (range n-samples)]
                       (do (reset-sink!)
                           (let [t0 (now)]
                             (run-once maps)
                             (- (now) t0)))))
        sorted (vec (sort samples))]
    {:min    (first sorted)
     :median (nth sorted (quot n-samples 2))
     :max    (last sorted)
     :n-ops  n-ops}))

(defn- fmt-ms [x] (.toFixed x 2))
(defn- fmt-us [x] (.toFixed x 2))
(defn- fmt-int [x] (.toFixed x 0))

(defn- report [label {:keys [min median max n-ops]}]
  (let [best-us-per-op (* 1000 (/ min n-ops))
        med-us-per-op  (* 1000 (/ median n-ops))
        best-ops-per-s (/ n-ops (/ min 1000))]
    (println (str "  " label))
    (println (str "    min " (fmt-ms min) " ms"
                  "  median " (fmt-ms median) " ms"
                  "  max " (fmt-ms max) " ms   (batch of " n-ops ")"))
    (println (str "    → " (fmt-us best-us-per-op) " µs/op best, "
                  (fmt-us med-us-per-op) " µs/op median, "
                  (fmt-int best-ops-per-s) " ops/sec best"))
    (println)))

;; ---------------------------------------------------------------------------
;; Micro-benchmark: just kebab->camel on a realistic key set.
;; This is the one piece that was memoized in the original code, so it's
;; worth seeing what it costs uncached.

(defn- bench-kebab []
  (let [keys [:initial-view-state :on-click :map-style :interactive-layer-ids
              :reuse-maps :on-move :on-load :on-resize :on-data
              :circle-radius :circle-color :icon-image :longitude :latitude
              :zoom :pitch :bearing :max-bounds :fit-bounds-options]
        n-ops 100000
        n-samples 7
        ;; warmup
        _ (dotimes [_ 3] (doseq [k keys] (props/kebab->camel k)))
        samples (vec (for [_ (range n-samples)]
                       (let [t0 (now)]
                         (dotimes [_ (quot n-ops (count keys))]
                           (doseq [k keys] (props/kebab->camel k)))
                         (- (now) t0))))
        sorted (vec (sort samples))]
    (report "kebab->camel (19 distinct keywords, repeated)"
            {:min (first sorted)
             :median (nth sorted (quot n-samples 2))
             :max (last sorted)
             :n-ops (* (quot n-ops (count keys)) (count keys))})))

;; ---------------------------------------------------------------------------
;; Entry point

(defn main [& _args]
  (println "cartoj.props benchmark")
  (println "======================")
  (println (str "Node " js/process.version
                ", V8 " (.-v8 js/process.versions)))
  (println)
  (let [n-ops 10000
        n-samples 7]
    (println (str "Each scenario: " n-ops " distinct prop maps converted, "
                  n-samples " samples"))
    (println)
    (println "props->js scenarios:")
    (println)
    (report "Simple — top-level primitives + callbacks"
            (bench (gen-simple n-ops) n-ops n-samples))
    (report "With :style + :initial-view-state"
            (bench (gen-with-style n-ops) n-ops n-samples))
    (report "Style spec — :paint + :layout + :filter"
            (bench (gen-style-spec n-ops) n-ops n-samples))
    (report "Kitchen sink — all of the above"
            (bench (gen-kitchen-sink n-ops) n-ops n-samples)))
  (println "Micro-benchmark:")
  (println)
  (bench-kebab))
