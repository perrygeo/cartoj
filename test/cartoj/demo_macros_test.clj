(ns cartoj.demo-macros-test
  "Clojure (JVM) tests for the compile-time helpers in cartoj.demo-macros.
   Run with: clj -M -e \"(require 'cartoj.demo-macros-test)\""
  (:require
   [cartoj.demo-macros :as dm]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.test :refer [deftest is run-tests testing]]))

;; ---------------------------------------------------------------------------
;; ns->resource-path

(deftest ns->resource-path-test
  (testing "converts namespace symbol to classpath-relative .cljs path"
    (is (= "cartoj/demo.cljs"     (dm/ns->resource-path 'cartoj.demo)))
    (is (= "cartoj/core.cljs"     (dm/ns->resource-path 'cartoj.core)))
    (is (= "cartoj/re_frame.cljs" (dm/ns->resource-path 'cartoj.re-frame))))

  (testing "the path is resolvable on the classpath"
    (is (some? (io/resource (dm/ns->resource-path 'cartoj.demo))))
    (is (some? (io/resource (dm/ns->resource-path 'cartoj.core))))))

;; ---------------------------------------------------------------------------
;; defn-source-text

(deftest defn-source-text-test
  (let [demo-res (io/resource (dm/ns->resource-path 'cartoj.demo))
        core-res (io/resource (dm/ns->resource-path 'cartoj.core))]

    (testing "returns nil for a symbol that does not exist in the file"
      (is (nil? (dm/defn-source-text demo-res "nonexistent-fn-xyz"))))

    (testing "returns source text starting with (defn <name>"
      (let [src (dm/defn-source-text demo-res "uncontrolled-section")]
        (is (string? src))
        (is (str/starts-with? src "(defn uncontrolled-section"))))

    (testing "returned text is a complete top-level form"
      (let [src (dm/defn-source-text demo-res "sources-section")]
        (is (string? src))
        (is (str/starts-with? src "(defn sources-section"))
        ;; bracket-counter must have found the closing paren
        (is (str/ends-with? (str/trim src) "}}]]]])"))))

    (testing "preserves original whitespace and indentation"
      (let [src (dm/defn-source-text demo-res "uncontrolled-section")]
        ;; multi-line: must contain a newline
        (is (str/includes? src "\n"))
        ;; body lines must have leading spaces
        (is (re-find #"(?m)^  \[" src))))

    (testing "works for ClojureScript that contains ::alias/kw (controlled-section)"
      (let [src (dm/defn-source-text demo-res "controlled-section")]
        (is (string? src))
        (is (str/starts-with? src "(defn controlled-section"))
        ;; must contain the ns-aliased keyword unchanged
        (is (str/includes? src "::cartoj-rf/view-state"))))

    (testing "works for vars in a different namespace (cartoj.core)"
      (let [src (dm/defn-source-text core-res "map")]
        (is (string? src))
        (is (str/starts-with? src "(defn map"))))))

(run-tests 'cartoj.demo-macros-test)
