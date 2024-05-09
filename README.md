# clj-prolog: Clojure interface to Prolog

Use Prolog from Clojure via [Projog](http://www.projog.org/).

This library provides a convenient Clojure wrapper to JVM Prolog implementation.
Clojure data and select Java classes are roundtripped to Prolog terms
and back.

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
| LocalDate | compound term | `(LocalDate/of 2024 5 9)` => `date(2024, 5, )` |
| LocalTime | compound term | `(LocalTime/parse "10:45")` => `time(10,45,0,0)` |
| LocalDateTime | compound term | `(LocalDateTime/of (LocalDate/of 2024 5 9) (LocalTime/of 10 45))` =>  `datetime(date(2024,5,9),time(10,45,0,0))` |



## Licence

MIT for this wrapper code. Projog implementation is Apache-2.0.

## Changes

### 2024-05-09
- Initial implementation work, added type mappings
