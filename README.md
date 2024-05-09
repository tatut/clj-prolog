# clj-prolog: Clojure interface to Prolog

![test workflow](https://github.com/tatut/clj-prolog/actions/workflows/test.yml/badge.svg)

Use Prolog from Clojure via [Projog](http://www.projog.org/).

This library provides a convenient Clojure wrapper to JVM Prolog implementation.
Clojure data and select Java classes are roundtripped to Prolog terms
and back.

## Usage

Query can be passed in as a Clojure vector or a Prolog string.

See `clj-prolog.core` namespace and [tests](https://github.com/tatut/clj-prolog/blob/main/test/clj_prolog/core_test.clj) for more examples..

```clojure
(with-prolog
  (consult-string "mortal(X) :- human(X). human(socrates).")
  (q [:mortal :Name])) ;; => ({:Name :socrates})
```

```clojure
(with-prolog
  (q "between(1,7,X), 0 is X mod 2.")) ;; => ({:X 2} {:X 4} {:X 6})
```



## Type mapping

Type mapping from Clojure data to Prolog terms is made to be convenient to use from Clojure.

Keywords are mapped to Prolog atoms and vectors to compound terms. Some compound terms are reserved
for mapping different Clojure or Java types.

Note if the first element of a vector is a keyword, it is turned into a
compound term, otherwise a list.

| Clojure/Java type | Prolog term type | Example |
| ----------------- | ---------------- | ------- |
| keyword | atom | `:foo` => `foo` |
| long | long | `42` => `42` |
| double | double | `6.66` => `6.66` |
| vector | compound term | `[:foo 123 :bar]` => `foo(123,bar)` |
| list | list | `(list 1 2 3)` => `[1,2,3]` |
| string | compound term | `"Hello"` => `string(Hello)` |
| map | compound term | `{:foo 42 :bar "something"}` => `map([foo-42,bar-string(something)])` |
| Date | compound term |  `#inst "2024-05-09T05:08:58.458-00:00"` => `timestamp(1715231338458)` |
| LocalDate | compound term | `(LocalDate/of 2024 5 9)` => `date(2024, 5, 9)` |
| LocalTime | compound term | `(LocalTime/parse "10:45")` => `time(10,45,0,0)` |
| LocalDateTime | compound term | `(LocalDateTime/of (LocalDate/of 2024 5 9) (LocalTime/of 10 45))` =>  `datetime(date(2024,5,9),time(10,45,0,0))` |



## Licence

MIT for this wrapper code. Projog implementation is Apache-2.0.

## Changes

### 2024-05-09
- Initial implementation work, added type mappings
- Basic consultation of files and strings and queries work
