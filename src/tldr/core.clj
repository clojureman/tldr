(ns tldr.core)
  
(def ^:dynamic *local-function* nil)
  
(let [delimiters (->> (range 2 101)
                      (map #(symbol (apply str (repeat % \-))))
                      (apply conj #{'where 'where-mutual}))
      is-function? #(and (list? %) (= 'function (first %)))
      function-name #(second (filter symbol? %))
      function-definition #(drop-while (complement (some-fn list? vector?)) %)
      expand-functions (partial
                        mapcat
                        #(if (is-function? %)
                           [(function-name %) (binding [*local-function* true] (macroexpand-1 %))]
                           [%]))]

  (defmacro compute
    [& xs]
    (let [throw-illegal-arg #(throw (IllegalArgumentException.
                                     (str (apply format
                                                 "%s/%s, line %s column %s: %s."
                                                 *ns*
                                                 (conj ((juxt first
                                                              (comp str :line meta)
                                                              (comp str :column meta))
                                                        &form)
                                                       %)))))
          [body bindings] (split-with (complement delimiters) xs)
          bindings (partition-by (complement delimiters) bindings)
          bindings (mapcat (fn [xs prevs]
                             (cond (delimiters (first xs))
                                   nil

                                   (= (last prevs) 'where-mutual)
                                   (->> (expand-functions xs)
                                        (partition-all 2)
                                        (mapcat (fn [[nam fdef]]
                                                  (when-not (symbol? nam)
                                                    (throw-illegal-arg "Each where-mutual binding must be of the form (function <name> ...) or <name> <value-that-behaves-as-a-function>"))
                                                  [nam
                                                   (if (and (sequential? fdef)
                                                            (#{'clojure.core/fn 'fn 'fn*} (first fdef)))
                                                     fdef
                                                     `#(apply ~fdef %&))]))
                                        (cons true)
                                        vector)
                                   :else
                                   (let [xs (expand-functions xs)]
                                     (when (odd? (count xs))
                                       (throw-illegal-arg "Each where-binding must be of the form (function <name> ...) or <name-or-destructuring-expression> <value>"))
                                     [(cons false xs)])))
                           bindings
                           (cons nil bindings))]
      (if (seq bindings)
        ((fn f [[[mutual & x] & xs] body]
           (let [pairs (partition-all 2 x)]
             (let [lft (map first pairs)
                   rgt (mapv second pairs)
                   multiple (< 1 (count pairs))
                   wrap (if multiple vec first)]
               (if (seq pairs)
                 (if (and mutual (next pairs))
                   (let [lf `(letfn [~@(map (fn [[k v]] (cons k (function-definition v))) pairs)] ~@body)]
                     (if (seq xs)
                       (f xs [lf])
                       lf))
                   `(let [~(wrap lft) ~(if (seq xs)
                                         (f xs [(wrap rgt)])
                                         (wrap rgt))]
                      ~@body))
                 (if (seq xs)
                   (f xs body)
                   body)))))
         bindings
         body)
        `(do ~@body))))

  (defmacro function [& xs]
    (let [[hd tl] (split-with (complement sequential?) xs)
          [args & body] tl
          [body-hd body-tl] (split-with (complement delimiters) body)
          fun (if *local-function* `fn `defn)]
      (if (and (< (count hd) 5) (seq body-tl))
        `(compute (~fun ~@hd ~args ~@body-hd) ~@body-tl)
        `(~fun ~@xs)))))
