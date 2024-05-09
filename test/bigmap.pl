bignumbers([], []).
bignumbers([_-Val|Bigs1], Bigs) :-
  Val =< 100, bignumbers(Bigs1, Bigs).
bignumbers([Key-Val|Bigs1], [Key-Val|Bigs]) :-
  Val > 100, bignumbers(Bigs1, Bigs).
bigmap(map(ValsIn), map(ValsOut)) :- bignumbers(ValsIn,ValsOut).
