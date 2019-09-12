(ns tldr.core)

(def ^:dynamic *local-function* nil)

(let [delimiters (->> (range 2 101)
                      (map #(symbol (apply str (repeat % \-))))
                      (apply conj #{'where}))
      is-function? #(and (list? %) (= 'function (first %)))]

  (defmacro compute
    [& xs]
    (let [[body bindings] (split-with (complement delimiters) xs)
          throw-imbalanced-binding #(throw (IllegalArgumentException.
                                            (str (apply format
                                                        "%s/%s, line %s column %s: Each binding needs a value."
                                                        *ns*
                                                        ((juxt first
                                                               (comp str :line meta)
                                                               (comp str :column meta))
                                                         &form)))))
          bindings (->> bindings
                        (partition-by (complement delimiters))
                        (remove (comp delimiters first))
                        (map #(if (even? (count (remove is-function? %)))
                                %
                                (throw-imbalanced-binding))))
          function-name (constantly 'f)]
      (if (seq bindings)
        ((fn f [[x & xs] body]
           (let [x (mapcat #(if (is-function? %)
                              [(function-name %) (binding [*local-function* true] (macroexpand-1 %))]
                              [%])
                           x)
                 pairs (partition-all 2 x)]
             (let [lft (map first pairs)
                   rgt (mapv second pairs)
                   multiple (< 1 (count pairs))
                   wrap (if multiple vec first)]
               (if (seq pairs)
                 `(let [~(wrap lft) ~(if (seq xs)
                                       (f xs [(wrap rgt)])
                                       (wrap rgt))]
                    ~@body)
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
