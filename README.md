# TL;DR
> The Essence Comes First 

[![Clojars Project](https://img.shields.io/clojars/v/tldr.svg)](https://clojars.org/tldr) Beta
```clojure
(require '[tldr.core :refer :all])

(function -main [] 

          (println 'Hello something)
          
          where   
               something "world!") 
```

This Clojure library enables a coding style where the most important ideas are expressed first. 

Details come later, and can be read as needed.

This is important because code is *read* more often than it is *written*. 

## Reads Like a Press Release

Code can be read like a press release - only the first few lines matter; the rest is optional.

```clojure
(compute
    {:top-students 
          (take 3 (reverse students-by-grade))
     :avg-grade 
          (/ (sum student-grades)
             (count student-grades))}
 where
    sum (partial apply +)
    students-by-grade (sort-by :grade students)
    student-grades (map :grade students)
 
 where
    students '[{:name Peterson  :grade 8}
               {:name Zeus      :grade 100}
               {:name Zach      :grade 20}
               {:name Sleepy    :grade 0}
               {:name Droopy    :grade 7}]
)
```
Each added section provides you with more details if you care to read that far.


In terms of implementation `compute` is a macro, and `where` is just syntax.

### Example: The Quadratic Equation
```clojure
;; Find the solutions to the quadratic equation
;;     a*x^2 + b*x + c == 0
;; (Just like in school)

(compute
    (distinct solutions)
 where
    solutions (map 
                main-formula 
                (when D [D (- D)]))
 where
    main-formula #(/ (- % b) 2 a)
    D (try (Math/sqrt (- (* b b) (* 4 a c)))
           (catch Throwable _ nil))   
)
```

If you think it looks better, you can use two or more dashes instead of `where`.

The above example is equivlent to 
```clojure
(compute 
    (distinct solutions)
    ----------------------------------------
    solutions (map 
                main-formula 
                (when D [D (- D)]))
    ----------------------------------------
    main-formula #(/ (- % b) 2 a)
    D (try (Math/sqrt (- (* b b) (* 4 a c)))
           (catch Throwable _ nil)))
```
## Brain Protection
*The better your brain, the more you need it*

- Visual Guard Rails
  > The lines give you a visual indication of what code to waste your precious brain cycles on.
- Independent bindings
  
  All bindings within a `where` block are independent (sometimes called "parallel") and do not know of each other. 

  > You can be assured that *the order of bindings within a `where` block does not matter*.

  (Except if you bind the same name more than once in a block - in which case the last one wins. 
  Future releases are expected to contain some level of protection against this).

- Limited Visibility

  A name bound in a `where` block is *only* visible in the code block immediately preceeding it. This is a *good thing*, because

  > The less code you have to read in order to understand any given piece of a program, the better your chances are of succeeding. 

## But Wait ... There's More

### Nested functions 
***Question:*** *How do you know if an inner function closes over the arguments of an outer function?*

***Answer:*** *You don't* - unless you carefully read the source code of the inner function. You may also have to read other functions and expressions inside the outer function but outside the inner one to be sure. 

This is one of the reasons there is no bestseller named *"The Joy of Reading Long Clojure Functions Written by Other People"*.

It is customary for Clojure programs to contain a lot
of separate source files with very little code in each, and with a lot of functions that are used only 
locally and within the namespaces in which they are defined. And often only once! Just a few functions are used outside the namespace in which they are defined.

Clearly some kind of grouping other than the namespace could be useful.

Say hello to `function`
```clojure
(function print-labels [club-members]  
    (doseq [member club-members]
       (print-one (format-label member)))
    --------------------------------------------------
    print-one    (fn [label] .....)
    format-label (juxt :firstname :lastname :room))
```
The above code is a lot like 
```clojure
(defn print-labels [club-members]
   (let [print-one    (fn [label] .....)
         format-label (juxt :firstname :lastname :room)]
      (doseq [member club-members]
         (print-one (format-label member)))))
```
but there is one important difference: We know at a glance that the code after `-----------------`  defines two independent functions that do not close over each other or anything in the function body or the function arguments.

This also means that we can later refactor the code by moving for instance `print-one` function outside as `(defn print-one [label] ...)` without risk of breaking anything. 

### Of Course There's Some More Sugar

Instead of binding a name to a function in a `where` block, like for instance `f (fn [x y] ...)`
we can simply write `(function f [x y])`.
In the label example it would be

```clojure
(function print-labels [club-members]  
    (doseq [member club-members]
       (print-one (format-label member)))
    --------------------------------------------------
    (function print-one [label] 
        .....)
    format-label (juxt :firstname :lastname :room))
```
### Mutually Recursive Functions

Sometimes we absolutely need local functions to call each other in a criss cross pattern. Clojure has `letfn` for this, and we have some sugar for that:
```clojure
(compute
    (f "x")
  where-mutual
    (function f [x] (if (< 10 (count x))
                       x
                       (g (str "-" x))))
    (function g [s] (if (< 10 (count s))
                       s
                       (f (str "|" s)))))
```
You can even go a bit crazy and use anonymous functions in a `where-mutual` block
```clojure
(compute
    (f "x")
  where-mutual
    f #(if (< 10 (count %))
           %
           (g (str "-" %)))
    g (fn [s] (if (< 10 (count s))
                   s
                   (f (str "|" s)))))
```
You can also use higher order functions like `comp` or `juxt` to define
individual functions that can call each other mutually. 
```clojure
(compute
         (f {:name "Alfie" :age 21 :occupation "unknown" 
             :address {:street "Main Street" :number 1203}}) 
 where-mutual
         f (comp println (partial record ""))
         record #(str "RECORD\n"
                      (apply str (map field (repeat (str % "  ")) %2))
                      % "END;\n")
         (function field [indent [fieldname value]]
                   (str indent
                        (name fieldname)
                        " = "
                        (if (map? value)
                          (record (field-indent indent (name fieldname)) value)
                          (str (pr-str value) ";\n"))))
 where
         (function field-indent [indent fieldname]
                   (apply str indent
                          (repeat
                            (+ 3 (count (name fieldname)))
                            " "))))

;; The above code results in this being printed to stdout:

RECORD
  name = "Alfie";
  age = 21;
  occupation = "unknown";
  address = RECORD
              street = "Main Street";
              number = 1203;
            END;
END;
```

## Use in Clojure, ClojureScript, or both
To use from Clojure you need to require
```clojure
[tldr.core :refer :all]
```
To use from ClojureScript you need to require
```clojure
[tldr.core :refer-macros :all]
```
If you want your code to compile in both Clojure and ClojureScript you will have to require
```clojure
[tldr.core :refer-macros :all :refer :all]
```


## License

Copyright © 2019 Mads Olsen

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
