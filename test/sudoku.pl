% Solve sudoku with CLP

cubes([],[],[],[]).
cubes([R11,R12,R13|R1s], [R21,R22,R23|R2s], [R31,R32,R33|R3s],
      [[R11,R12,R13,R21,R22,R23,R31,R32,R33]|Cubes]) :-
    cubes(R1s,R2s,R3s, Cubes).

cubes([], []).
cubes([R1,R2,R3|Rows], CubesOut) :-
    cubes(R1,R2,R3, Cubes),
    cubes(Rows, Cubes1),
    append(Cubes, Cubes1, CubesOut).

% The 9x9 board is represented as list of rows where each row is a 9 element list.
% Rows is a board with some missing values to fill in (marked by a variable)
sudoku(Rows) :-
    append(Rows, Vs),
    Vs ins 1..9,
    transpose(Rows, Cols),
    maplist(all_different, Rows), % Each row has every number 1-9 once
    maplist(all_different, Cols), % As does each row
    cubes(Rows, Cubes),
    maplist(all_different, Cubes). % as does each 3x3 cube
