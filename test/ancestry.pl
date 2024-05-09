parent(alice, bob).
parent(bob, charlie).
parent(charlie, dylan).
parent(dylan, frank).
parent(frank, gabriella).

ancestor(Anc, Dec) :- parent(Anc, Dec).
ancestor(Anc, Dec) :-
    parent(Anc, Intermediate),
    ancestor(Intermediate, Dec).
