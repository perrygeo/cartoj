(ns cartoj.re-frame-test
  "Tests for cartoj.re-frame — optional re-frame helpers.

  These test the event handler and subscription logic in isolation
  (no DOM/React involved). We use re-frame's dispatch-sync for synchronous
  testing without needing the re-frame-test library."
  (:require [cljs.test :refer [deftest is testing use-fixtures]]
            [re-frame.core :as rf]
            [cartoj.re-frame :as cartoj-rf]))

;; ---------------------------------------------------------------------------
;; Fixtures — clear subscription cache and reset db key between tests

(use-fixtures :each
  {:before (fn []
             (rf/clear-subscription-cache!)
             (rf/dispatch-sync [::cartoj-rf/set-view-state nil]))})

;; ---------------------------------------------------------------------------
;; ::set-view-state event

(deftest set-view-state-stores-value
  (testing "dispatching ::set-view-state stores the value in app-db"
    (rf/dispatch-sync [::cartoj-rf/set-view-state {:longitude -122.4 :latitude 37.8 :zoom 14}])
    (let [vs @(rf/subscribe [::cartoj-rf/view-state])]
      (is (= -122.4 (:longitude vs)))
      (is (= 37.8   (:latitude vs)))
      (is (= 14     (:zoom vs))))))

(deftest set-view-state-replaces-previous
  (testing "a second dispatch replaces the previous view-state"
    (rf/dispatch-sync [::cartoj-rf/set-view-state {:longitude 0 :latitude 0 :zoom 1}])
    (rf/dispatch-sync [::cartoj-rf/set-view-state {:longitude 10 :latitude 20 :zoom 5}])
    (let [vs @(rf/subscribe [::cartoj-rf/view-state])]
      (is (= 10 (:longitude vs)))
      (is (= 20 (:latitude vs)))
      (is (= 5  (:zoom vs))))))

;; ---------------------------------------------------------------------------
;; ::view-state subscription

(deftest view-state-subscription-returns-nil-when-not-set
  (testing "subscription returns nil when view-state has been reset to nil"
    (is (nil? @(rf/subscribe [::cartoj-rf/view-state])))))

;; ---------------------------------------------------------------------------
;; on-move helper

(deftest on-move-returns-a-function
  (testing "on-move returns a callable function"
    (is (fn? (cartoj-rf/on-move)))))

(deftest on-move-handler-dispatches-view-state
  (testing "calling the on-move handler with a fake JS event updates the view-state"
    (let [handler (cartoj-rf/on-move {:sync? true})
          fake-event (js-obj "viewState"
                             (js-obj "longitude" -73.9
                                     "latitude"  40.7
                                     "zoom"      12))]
      (handler fake-event)
      (let [vs @(rf/subscribe [::cartoj-rf/view-state])]
        (is (= -73.9 (:longitude vs)))
        (is (= 40.7  (:latitude vs)))
        (is (= 12    (:zoom vs)))))))
