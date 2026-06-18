(ns build
  "Build the cartoj JAR and publish it to Clojars.

  Tasks:
    clj -T:build clean
    clj -T:build jar
    clj -T:build install   ; install to local ~/.m2 for testing
    clj -T:build deploy    ; push to Clojars (needs CLOJARS_USERNAME / CLOJARS_PASSWORD)

  Bump `version` below for each release. Tag the commit `v<version>` to keep
  the pom.xml :tag in sync."
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib       'io.github.perrygeo/cartoj)
(def version   "0.2.0-SNAPSHOT")
(def class-dir "target/classes")
(def jar-file  (format "target/%s-%s.jar" (name lib) version))
(def basis     (delay (b/create-basis {:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn jar [_]
  (clean nil)
  (b/write-pom {:class-dir class-dir
                :lib       lib
                :version   version
                :basis     @basis
                :src-dirs  ["src"]
                :scm       {:url                 "https://github.com/perrygeo/cartoj"
                            :connection          "scm:git:git://github.com/perrygeo/cartoj.git"
                            :developerConnection "scm:git:ssh://git@github.com/perrygeo/cartoj.git"
                            :tag                 (str "v" version)}
                :pom-data  [[:description "ClojureScript / Reagent wrapper around react-map-gl (MapLibre)."]
                            [:url "https://github.com/perrygeo/cartoj"]
                            [:licenses
                             [:license
                              [:name "MIT"]
                              [:url "https://opensource.org/license/mit"]]]]})
  (b/copy-dir {:src-dirs   ["src"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file  jar-file})
  (println "Built" jar-file))

(defn install [_]
  (jar nil)
  (b/install {:basis     @basis
              :lib       lib
              :version   version
              :jar-file  jar-file
              :class-dir class-dir})
  (println "Installed" lib version "to ~/.m2"))

(defn deploy [_]
  (jar nil)
  (dd/deploy {:installer :remote
              :artifact  jar-file
              :pom-file  (b/pom-path {:lib lib :class-dir class-dir})}))
