(ns cartoj.demo-macros
  "Compile-time macro for emitting ClojureScript function source as HTML.

  Defines `code-for`, a macro that resolves the source text of a named defn
  at compile time and emits a [:pre [:code ...]] hiccup vector.

  Handles both unqualified symbols (resolved against the calling namespace)
  and fully-qualified symbols (e.g. cartoj.core/map).

  The source file is located by converting the namespace to a classpath-
  relative path (e.g. cartoj.demo -> cartoj/demo.cljs) and reading it via
  clojure.java.io/resource.  This works for any namespace whose source file
  is on the compile-time classpath (i.e. in :source-paths in shadow-cljs.edn)."
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

;; ---------------------------------------------------------------------------
;; Helpers (public so they can be unit-tested from demo_macros_test.clj)

(defn ns->resource-path
  "Converts a namespace symbol to a classpath-relative .cljs file path.
  e.g. cartoj.re-frame => \"cartoj/re_frame.cljs\""
  [ns-sym]
  (-> (str ns-sym)
      (str/replace "." "/")
      (str/replace "-" "_")
      (str ".cljs")))

(defn defn-source-text
  "Returns the source text of the first top-level `defn` form whose name
  matches `sym-name` in the file at `file-resource` (a java.net.URL).
  Returns nil if no matching form is found.

  Preserves original whitespace and indentation. Finds the start line via a
  regex, then walks the file character-by-character counting bracket depth to
  locate the end of the form without invoking the Clojure reader — so it
  handles ClojureScript-specific syntax (e.g. ::alias/kw, #js, ^:export)
  that the JVM reader would reject."
  [file-resource sym-name]
  (let [lines   (vec (line-seq (io/reader file-resource)))
        pattern (re-pattern (str "^\\(defn\\s+"
                                 (java.util.regex.Pattern/quote sym-name)
                                 "(\\s|$)"))
        start   (some (fn [[i line]] (when (re-find pattern line) i))
                      (map-indexed vector lines))]
    (when start
      (let [content (str/join "\n" (subvec lines start))]
        ;; Walk the content char-by-char tracking bracket depth, string
        ;; literals, and line comments — without invoking the Clojure reader.
        (loop [idx     0
               depth   0
               in-str? false
               escape? false
               end     nil]
          (cond
            ;; A complete form was found — slice lines from start to here.
            (some? end)
            (let [prefix  (subs content 0 end)
                  n-lines (count (str/split-lines prefix))]
              (str/join "\n" (subvec lines start (+ start n-lines))))

            ;; Ran off the end without closing — return everything from start.
            (>= idx (count content))
            (str/join "\n" (subvec lines start))

            :else
            (let [c (nth content idx)]
              (cond
                ;; Inside a string: track escape sequences and closing quote.
                escape?
                (recur (inc idx) depth in-str? false nil)

                in-str?
                (condp = c
                  \\ (recur (inc idx) depth true  true  nil)
                  \" (recur (inc idx) depth false false nil)
                  (recur (inc idx) depth true  false nil))

                ;; Line comment: skip ahead to the next newline.
                (= c \;)
                (let [nl (.indexOf content "\n" (int idx))]
                  (recur (if (neg? nl) (count content) (inc nl))
                         depth false false nil))

                ;; Start of a string literal.
                (= c \")
                (recur (inc idx) depth true false nil)

                ;; Opening bracket — increase depth.
                (#{(char 40) (char 91) (char 123)} c)
                (recur (inc idx) (inc depth) false false nil)

                ;; Closing bracket — decrease depth; depth 0 means form ends.
                (#{(char 41) (char 93) (char 125)} c)
                (let [d' (dec depth)]
                  (if (zero? d')
                    (recur (inc idx) d' false false (inc idx))
                    (recur (inc idx) d' false false nil)))

                :else
                (recur (inc idx) depth false false nil)))))))))

(defmacro code-string
  "Like `code-for`, but emits only the source text string (no hiccup wrapper).
  Useful when the caller wants to control the wrapping.

  Usage:
    (code-string uncontrolled-section)
    (code-string cartoj.core/map)

  Returns a plain string (wrapped in the <code> element) at compile time."
  [sym]
  (let [ns-part   (namespace sym)
        name-part (name sym)
        ns-sym    (if ns-part
                    (symbol ns-part)
                    (ns-name *ns*))
        resource  (io/resource (ns->resource-path ns-sym))
        src       (when resource (defn-source-text resource name-part))]
    (if src
      `~src
      (throw (ex-info (str "code-string: could not find defn `" sym
                           "` in namespace `" ns-sym "`")
                      {:sym sym :ns ns-sym})))))
