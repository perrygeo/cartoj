(ns codox-build
  "Codox entry point that reads the library version from VERSION so the
  generated docs never drift from build.clj / package.json."
  (:require [clojure.string :as str]
            [codox.main :as codox]))

(defn generate-docs [opts]
  (codox/generate-docs
    (assoc opts :version (str/trim (slurp "VERSION")))))
