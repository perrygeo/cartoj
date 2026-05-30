(ns cartoj.demo-test
  "Tests for the demo harness components.

  `code-block` is a Reagent class component that wraps a [:pre [:code]] pair
  and calls hljs.highlightElement on mount/update. We test its pure render
  function (`code-block-render`) without requiring a DOM."
  (:require [cljs.test :refer [deftest is testing]]
            [cartoj.demo :as demo]))

;; ---------------------------------------------------------------------------
;; code-block-render (pure render fn, testable without a DOM)

(deftest code-block-render-returns-pre-code
  (testing "returns a [:pre [:code {:class language-clojure} src]] vector"
    (let [src    "(defn hello [] :world)"
          result (demo/code-block src)]
      (is (vector? result))
      (is (= :pre (first result)))
      (let [[tag {:keys [class]} text] (second result)]
        (is (= :code tag))
        (is (= "language-clojure" class))
        (is (= src text))))))

(deftest code-block-render-preserves-source-text
  (testing "multi-line source text is kept verbatim"
    (let [src "(defn foo [x]\n  (* x x))"
          [[_ {:keys [class]} text]] (rest (demo/code-block src))]
      (is (= "language-clojure" class))
      (is (= src text)))))

;; ---------------------------------------------------------------------------
;; code-block (class component)

(deftest code-block-is-function
  (testing "code-block is a function (Reagent component factory)"
    (is (fn? demo/code-block))))
