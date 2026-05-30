(ns user
  (:require
   [shadow.cljs.devtools.api]))

;; To connect to the CLJS repl from the CLJ repl
;; alternative to running separate process e.g.
;;   npx shadow-cljs cljs-repl dev

;; !!! must run `make dev` and open browser first!

(shadow.cljs.devtools.api/nrepl-select :dev)

(comment ;; namespaces work slightly differenctly in cljs
  *ns* ; nil
  (symbol (namespace ::x)) ; user

  ;; fails
  ;; ns-publics needs a symbol like user. Since ns-publics is a 
  ;; macro that runs at compile-time, you can't use runtime values. Use a 
  ;; literal quoted symbol or a compile-time macro.
  {:namespace (symbol (namespace ::x))
   :publics (keys (ns-publics (symbol (namespace ::x))))}
  ;; wip
  )
