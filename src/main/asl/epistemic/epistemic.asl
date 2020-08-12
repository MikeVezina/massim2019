//kb::item(block_dispenser).
//kb::item(dispenser).
kb::item(block).
kb::item(none).

kb::position(0, 0).
kb::position(0, 1).
kb::position(0, 2).
kb::position(0, 3).
kb::position(0, 4).

kb::position(1, 0).
kb::position(1, 1).
kb::position(1, 2).
kb::position(1, 3).
kb::position(1, 4).

kb::position(2, 0).
kb::position(2, 1).
kb::position(2, 2).
kb::position(2, 3).
kb::position(2, 4).

kb::position(3, 0).
kb::position(3, 1).
kb::position(3, 2).
kb::position(3, 3).
kb::position(3, 4).

kb::position(4, 0).
kb::position(4, 1).
kb::position(4, 2).
kb::position(4, 3).
kb::position(4, 4).



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

// Unknown locations (Outside percepts)
kb::loc(X, Y)[prop] :- kb::position(X, Y).

//576
// Set valid locations for 0,0
//kb::~is_valid(location(-1, 0, LeftItem), location(1, 0, RightItem), location(0, -1, UpItem), location(0, 1, DownItem), loc(X, Y))
//    :- (X \== 0 | Y \== 0).

// Invalidate all worlds (general filter)
kb::~is_valid(0).

// Only validate the map info
// Commented out lines are block locations
kb::is_valid(loc(0, 0), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, none)).
kb::is_valid(loc(0, 1), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, none)).
kb::is_valid(loc(0, 2), location(-1, 0, none), location(1, 0, block), location(0, -1, none), location(0, 1, none)).
kb::is_valid(loc(0, 3), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, block)).
//kb::is_valid(loc(0, 4), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, none)).

kb::is_valid(loc(1, 0), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, none)).
kb::is_valid(loc(1, 1), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, block)).
//kb::is_valid(loc(1, 2), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, none)).
kb::is_valid(loc(1, 3), location(-1, 0, none), location(1, 0, none), location(0, -1, block), location(0, 1, none)).
kb::is_valid(loc(1, 4), location(-1, 0, block), location(1, 0, none), location(0, -1, none), location(0, 1, none)).

kb::is_valid(loc(2, 0), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, none)).
kb::is_valid(loc(2, 1), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, none)).
kb::is_valid(loc(2, 2), location(-1, 0, block), location(1, 0, none), location(0, -1, none), location(0, 1, none)).
kb::is_valid(loc(2, 3), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, none)).
kb::is_valid(loc(2, 4), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, none)).

kb::is_valid(loc(3, 0), location(-1, 0, none), location(1, 0, block), location(0, -1, none), location(0, 1, none)).
kb::is_valid(loc(3, 1), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, none)).
kb::is_valid(loc(3, 2), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, none)).
kb::is_valid(loc(3, 3), location(-1, 0, none), location(1, 0, block), location(0, -1, none), location(0, 1, none)).
kb::is_valid(loc(3, 4), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, none)).

//kb::is_valid(loc(4, 0), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, none)).
kb::is_valid(loc(4, 1), location(-1, 0, none), location(1, 0, none), location(0, -1, block), location(0, 1, none)).
kb::is_valid(loc(4, 2), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, block)).
//kb::is_valid(loc(4, 3), location(-1, 0, none), location(1, 0, none), location(0, -1, none), location(0, 1, none)).
kb::is_valid(loc(4, 4), location(-1, 0, none), location(1, 0, none), location(0, -1, block), location(0, 1, none)).

//+percept::task(Name,_,_,Reqs)
//    : percept::step(Step)
//    <-  .print(Step, ": ", Name, " - ", Reqs).

getPossibleLocations(Possibilities)
    :-  not .ground(Possibilities) &
        .findall(loc(X, Y), (possible(loc(X, Y)) & ~know(loc(X, Y))), Possibilities).


+percept::step(Step)
    : getPossibleLocations(Possibilities) &
      prevPoss(Step - 1, OldPoss) &
      eis.internal.filter_locations(OldPoss, OldTrans) &
      .intersection(OldTrans, Possibilities, Result)
    <-  .print(Step, ": ?It is possible that I am at ", Result);
        // Need to add non-possible locations to update reasoner
        +prevPoss(Step, Result).

+percept::step(Step)
    : getPossibleLocations(Possibilities) &
        not prevPoss(Step - 1, _)
    <-  .print(Step, ": It is possible that I am at ", Possibilities);
        +prevPoss(Step, Possibilities);
        !performAction(move(n)).

+know(loc(X, Y))
    <- .print("location is ", X, Y).

//+know(loc(X, Y)) <- .print("It is known (certain) that I am at (", X, ", ", Y, ")").
//+possible(loc(X, Y)) : ~know(loc(X, Y)) & percept::step(Step) <- .print(Step, ": It is possible that I am at (", X, ", ", Y, ")"); +hasMoved(Step).
//+possible(loc(X, Y)) : ~know(loc(X, Y)) <- .print("It is possible that I am at (", X, ", ", Y, ")"); !performAction(move(e));.
//+know(~loc(X, Y)) <- .print("I know that I am NOT at (", X, ", ", Y, ")").



//+know(location(1, 1, Item)) <- .print("It is known (certain) that there is a ", Item, " nearby").
//+possible(location(1, 1, Item)) <- .print("It is possibly known that a ", Item, " is nearby").
//+know(~location(1, 1, Item)) <- .print("I know there is NOT a ", Item, " nearby").