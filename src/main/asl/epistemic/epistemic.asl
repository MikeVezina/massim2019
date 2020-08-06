kb::item(block_two).
kb::item(block_one).
kb::item(none).

kb::position(0, 0).
kb::position(0, 1).
kb::position(0, 2).
kb::position(0, 3).
//kb::position(0, 4).
//kb::position(0, 5).

kb::position(1, 0).
kb::position(1, 1).
kb::position(1, 2).
kb::position(1, 3).
//kb::position(1, 4).
//kb::position(1, 5).

kb::position(2, 0).
kb::position(2, 1).
kb::position(2, 2).
kb::position(2, 3).
//kb::position(2, 4).
//kb::position(2, 5).

kb::position(3, 0).
kb::position(3, 1).
kb::position(3, 2).
kb::position(3, 3).
//kb::position(3, 4).
//kb::position(3, 5).

//kb::position(4, 0).
//kb::position(4, 1).
//kb::position(4, 2).
//kb::position(4, 3).
//kb::position(4, 4).
//kb::position(4, 5).
//
//kb::position(5, 0).
//kb::position(5, 1).
//kb::position(5, 2).
//kb::position(5, 3).
//kb::position(5, 4).
//kb::position(5, 5).


// Based on the terrain that we are on, and the surrounding blocks,
// what other items are possible outside our current perception range?
// Second iteration: what items are possible outside our current perception range to the right/left/up/down/etc.

// Rules to specify mutually exclusive locations
// 1. Perception Locations
kb::location(0, 1, Item)[prop] :- kb::item(Item).
//kb::location(0, 2, Item)[prop] :- kb::item(Item).
kb::location(0, -1, Item)[prop] :- kb::item(Item).
//kb::location(0, -2, Item)[prop] :- kb::item(Item).

kb::location(1, 0, Item)[prop] :- kb::item(Item).
//kb::location(1, 1, Item)[prop] :- kb::item(Item).
//kb::location(1, -1, Item)[prop] :- kb::item(Item).

kb::location(-1, 0, Item)[prop] :- kb::item(Item).

//kb::location(-1, 1, Item)[prop] :- kb::item(Item).
//kb::location(-1, -1, Item)[prop] :- kb::item(Item).

//kb::location(2, 0, Item)[prop] :- kb::item(Item).
//kb::location(-2, 0, Item)[prop] :- kb::item(Item).

// kb::location(1, 0, Item)[prop] :- kb::item(Item).
// kb::location(-1, 0, Item)[prop] :- kb::item(Item).

// Unknown locations (Outside percepts)
//kb::location(-1, 1, Item)[prop] :- kb::item(Item).
//kb::location(-1, -1, Item)[prop] :- kb::item(Item).
//kb::location(1, -1, Item)[prop] :- kb::item(Item).
kb::loc(X, Y)[prop] :- kb::position(X, Y).

+know(loc(X, Y)) <- .print("It is known (certain) that I am at (", X, ", ", Y, ")").
+possible(loc(X, Y)) : ~know(location(X, Y)) <- .print("It is possible that I am at (", X, ", ", Y, ")").
+know(~loc(X, Y)) <- .print("I know that I am NOT at (", X, ", ", Y, ")").


//+know(location(1, 1, Item)) <- .print("It is known (certain) that there is a ", Item, " nearby").
//+possible(location(1, 1, Item)) <- .print("It is possibly known that a ", Item, " is nearby").
//+know(~location(1, 1, Item)) <- .print("I know there is NOT a ", Item, " nearby").