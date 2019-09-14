(ns tldr.core)

(def ^:dynamic *local-function* nil)

(let [delimiters (->> (range 2 101)
                      (map #(symbol (apply str (repeat % \-))))
                      (apply conj #{'where 'where-mutual}))
      is-function? #(and (list? %) (= 'function (first %)))]
  
  (defmacro compute
    [& xs]
    (let [[body bindings] (split-with (complement delimiters) xs)
          throw-illegal-arg #(throw (IllegalArgumentException.
                                     (str (apply format
                                                 "%s/%s, line %s column %s: %s."
                                                 *ns*
                                                 (conj ((juxt first
                                                              (comp str :line meta)
                                                              (comp str :column meta))
                                                        &form)
                                                       %)))))
          function-name #(second (filter symbol? %))
          function-definition #(drop-while (complement (some-fn list? vector?)) %)
          bindings (partition-by (complement delimiters) bindings)
          expand-functions (partial
                            mapcat
                            #(if (is-function? %)
                               [(function-name %) (binding [*local-function* true] (macroexpand-1 %))]
                               [%]))
          bindings (mapcat (fn [xs prevs]
                             (cond (delimiters (first xs))
                                   nil

                                   (= (last prevs) 'where-mutual)
                                   (let [xs (expand-functions xs)]
                                     (mapv (fn [[nam fdef]]
                                             (when-not (and
                                                        (symbol? nam)
                                                        (sequential? fdef)
                                                        (#{'clojure.core/fn 'fn 'fn*} (first fdef)))
                                               (throw-illegal-arg "Each where-mutual binding must be of the form (function <name> ...) or <name> <anonymous-function>")))
                                           (partition-all 2 xs))
                                     [(cons true xs)])

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
                 (if (and mutual '(next pairs))
                   `(letfn [~@(map (fn [[k v]] (cons k (function-definition v))) pairs)] ~@body)
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

  (compute 23)