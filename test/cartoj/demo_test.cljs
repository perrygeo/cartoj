(ns cartoj.demo-test
  "Tests for the demo harness components.

  `code-block` is a Reagent Form-3 component (r/create-class) that wires
  hljs.highlightElement into :component-did-mount and :component-did-update.
  We test `code-block-render` (the pure render function) and the Form-3
  shape without requiring a DOM."
  (:require [cljs.test :refer [deftest is testing]]
            [cartoj.demo :as demo]))

;; ---------------------------------------------------------------------------
;; code-block-render (pure render fn, testable without a DOM)

(deftest code-block-render-returns-pre-code
  (testing "returns a [:pre [:code {:class language-clojure} src]] vector"
    (let [src    "(defn hello [] :world)"
          result (demo/code-block-render src)]
      (is (vector? result))
      (is (= :pre (first result)))
      (let [[tag {:keys [class]} text] (second result)]
        (is (= :code tag))
        (is (= "language-clojure" class))
        (is (= src text))))))

(deftest code-block-render-preserves-source-text
  (testing "multi-line source text is kept verbatim"
    (let [src "(defn foo [x]\n  (* x x))"
          [[_ {:keys [class]} text]] (rest (demo/code-block-render src))]
      (is (= "language-clojure" class))
      (is (= src text)))))

;; ---------------------------------------------------------------------------
;; code-block (Form-3 component with create-class lifecycle hooks)

(deftest code-block-is-function
  (testing "code-block is a function (Reagent component constructor)"
    (is (fn? demo/code-block))))

(deftest code-block-returns-react-class
  (testing "code-block returns a React class component with lifecycle hooks"
    (let [src    "[:div \"hello\"]"
          result (demo/code-block src)]
      ;; r/create-class returns a JS class (a function in JS).
      (is (some? result))
      (is (fn? result))
      ;; Verify the prototype carries the lifecycle methods we wired up.
      (let [proto (.-prototype result)]
        (is (fn? (.-componentDidMount proto)))
        (is (fn? (.-componentDidUpdate proto)))))))
