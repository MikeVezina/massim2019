//{ include("common.asl") }
//{ include("internal_actions.asl") }


/* Initial beliefs and rules */
direction(n).
direction(s).
direction(e).
direction(w).



isCurrentLocation(X, Y) :-
    (X == 0 & Y == 0).

isCurrentLocation(relative(X, Y)) :-
    isCurrentLocation(X, Y).

isCurrentLocation(absolute(X, Y)) :-
    percept::location(X, Y).

shouldNavigateAgain :-
    not(percept::lastActionResult(failed_path)).

hasNavigatedPath(PATH)
    :-  assertListEmpty(PATH).

isAgentBlocked(DIR)   :-
    eis.internal.is_agent_blocked(DIR).

canAgentMove(DIR)   :-
    eis.internal.can_agent_move(DIR).

areAttachmentsBlocked(DIR)   :-
    eis.internal.are_attachments_blocked(DIR).

getMovementDirections(DIRS)
    :-  eis.internal.get_movement_directions(DIRS).

getMovementDirection(DIR)
    :-  getMovementDirections([DIR|_]).


hasTeamAgent(AGENT) :-
    percept::teamAgent(_, _, AGENT).

+!navigateDestination(X, Y)
    :   navigationDirection([DIR | T], X, Y)
    <-  !performAction(move(DIR)).



+!navigation(absolute(X, Y))
    :   currentNavigationPath(X, Y, [DIR | T]) &
        assertListHasElements(T)
    <-  -currentNavigationPath(X, Y, _);
        +currentNavigationPath(X, Y, T);
        !performAction(move(DIR));
        !navigation(absolute(X, Y)).

+!navigation(absolute(X, Y))
    :   currentNavigationPath(X, Y, [DIR | T]) &
        assertListEmpty(T)
    <-  -currentNavigationPath(X, Y, _);
        !performAction(move(DIR));
        !navigation(absolute(X, Y)).

+!navigation(absolute(X, Y))
    :   not(currentNavigationPath(X, Y, _)) &
        navigationPath(X, Y, DIR_PATH)
    <- .print("Creating navigation path");
        +currentNavigationPath(X, Y, DIR_PATH);
        !navigation(absolute(X, Y)).

-!navigation(absolute(X, Y))
    :   navigationPath(X, Y, [TEST|PATH])
    <-  .print("Failed with Path: ", PATH);

        .print("Path: ", TEST).


// Plans for navigating to an absolute position
+!navigateToLocation(absolute(ABS_X, ABS_Y))
    :   ABSOLUTE = absolute(ABS_X, ABS_Y) &
        calculateRelativePosition(relative(X, Y), ABSOLUTE) &
        not(isCurrentLocation(X, Y))
    <-  !navigateDestination(X, Y);
        ?shouldNavigateAgain;
        !navigateToLocation(ABSOLUTE).

+!navigateToLocation(absolute(ABS_X, ABS_Y))
    :   ABSOLUTE = absolute(ABS_X, ABS_Y) &
        calculateRelativePosition(relative(X, Y), ABSOLUTE) &
        isCurrentLocation(X, Y)
    <-  .print("Absolute Location Reached: ", ABSOLUTE).


// Navigate to a relative position. Convert the location to an absolute position and
// call !navigateToLocation.
+!navigateToLocation(relative(X, Y))
    :   RELATIVE = relative(X, Y) &
        not(isCurrentLocation(X, Y)) &
        calculateAbsolutePosition(RELATIVE, ABSOLUTE)
    <-  !navigateToLocation(ABSOLUTE).

+!navigatePath(relative(X, Y))
    :   RELATIVE = relative(X, Y) &
        isCurrentLocation(X, Y) &
        calculateAbsolutePosition(RELATIVE, ABSOLUTE)
    <-  .print("Location Reached: ", ABSOLUTE).


+!navigatePath(absolute(X, Y))
    :   currentNavigationPath(X, Y, [DIR | T]) &
        assertListHasElements(T)
    <-  -currentNavigationPath(X, Y, _);
        +currentNavigationPath(X, Y, T);
        !performAction(move(DIR));
        !navigation(absolute(X, Y)).

+!performMove(DIR)
    :   not(areAttachmentsBlocked(DIR)) &
        not(isAgentBlocked(DIR))
    <-  !performAction(move(DIR)).

+!performMove(DIR)
    :   areAttachmentsBlocked(DIR) &
        not(isAgentBlocked(DIR)) &
        getRotation(ROT)
    <-  .print("Attachment Blocked Agent.", ROT);
        !performAction(rotate(ROT));
        !performMove(DIR).

//+!performMove(DIR)
//    :   not(areAttachmentsBlocked(DIR)) &
//        isAgentBlocked(DIR)
//    <-  .fail. // We need to regenerate our path if we are blocked.
//
//-!performMove(DIR)
//    <-  .fail.

+?hasNavigatedPath([DIR | REMAINING])
    :   assertListHasElements(REMAINING)
    <-  !performMove(DIR);
        ?hasNavigatedPath(REMAINING).

// This needs to be changed to check if we are at the destination
+?hasNavigatedPath([DIR | REMAINING])
    :   assertListEmpty(REMAINING)
    <-  !performMove(DIR);
        ?hasNavigatedPath(REMAINING).

//-?hasNavigatedPath(PATH)
//    <-  .print("Failed to navigate path: ", PATH, ". Recalculate path.");
//        .fail.


+!navigatePathList(PATH)
    : .list(PATH)
    <-  ?hasNavigatedPath(PATH).


+!navigatePathBetter(absolute(X, Y))
    :   navigationPath(X, Y, DIR_PATH) // Request navigation path
    <-  !navigatePathList(DIR_PATH).

-!navigatePathBetter(absolute(X, Y))
      <- //!explore;
        !navigatePathBetter(absolute(X, Y)).





+?hasTeamAgent(AGENT_NAME)
    <- !explore;
        .print("explored");
        ?hasTeamAgent(AGENT_NAME).

+!searchForAgent(AGENT_NAME)
    <-  ?hasTeamAgent(AGENT_NAME).


/** Beside Location **/

+!goBesideLocation(absolute(A_X, A_Y))
    :   calculateRelativePosition(relative(X, Y), absolute(A_X, A_Y)) &
        navigationDirection(DIR, X, Y)
    <-  .print("Loc: ", X, Y);
        !performAction(move(DIR));
        ?calculateRelativePosition(relative(NEW_X, NEW_Y), absolute(A_X, A_Y)); // Calculate new rel position after moving
        ?isBesideLocation(NEW_X, NEW_Y).


+?isBesideLocation(X, Y)
    :   isCurrentLocation(X, Y)
    <-  .print("Current location. Not Implemented. Choose a direction based off of obstacles.");
        .fail.

-?isBesideLocation(X, Y)
    :   isCurrentLocation(X, Y)
    <-  .print("WARN: -?isBesideLocation should be implemented with a non-blocked direction)");
        !performAction(move(w));
        ?isBesideLocation(X, Y).

+?isBesideLocation(X, Y)
    :   not(isCurrentLocation(X, Y))
    <-  .print("Is not beside location: ", X, ", ", Y);
        ?calculateAbsolutePosition(relative(X, Y), ABS);
        !goBesideLocation(ABS).







//
//
//+?isBlockAtLocation(BLOCK, relative(DEST_X, DEST_Y))
//    :   calculateAbsolutePosition(relative(DEST_X, DEST_Y), absolute(A_X, A_Y)) // Get the relative position of the block destination
//    <- ?isBlockAtLocation(BLOCK, absolute(A_X, A_Y)).
//
//+?canNavigateBlock(CUR_X, CUR_Y, DIR)
//    <-  !performAction(rotate(cw)).
//
//+?isBlockAtLocation(BLOCK, absolute(DEST_X, DEST_Y))
//    :   calculateRelativePosition(relative(R_X, R_Y), absolute(DEST_X, DEST_Y)) & // Get the relative position of the block destination
//        hasBlockAttached(CUR_X, CUR_Y, BLOCK) & // Get the location of the attached block
//        relative(BD_X, BD_Y) = relative(R_X - CUR_X, R_Y - CUR_Y) & // Relative block displacement
//        navigationDirection(DIR, BD_X, BD_Y)
//    <-  .print("Is block at ", DEST_X, ", ", DEST_Y);
//        ?canNavigateBlock(CUR_X, CUR_Y, DIR);
//        !performAction(move(DIR));
//        ?isBlockAtLocation(BLOCK, absolute(DEST_X, DEST_Y)).
//
//
//+!meetAgent(AGENT, req(R_X, R_Y, BLOCK), slave)
//    <-  ?getTeamAgentLocation(AGENT, relative(CUR_X, CUR_Y));
//        .print("Master At: ", CUR_X, ", ", CUR_Y);
//        ?isBlockAtLocation(BLOCK, relative(CUR_X + R_X, CUR_Y + R_Y)).
////        .send(SLAVE_AGENT, achieve, connectBlock(absolute(TARGET_X, TARGET_Y), BLOCK));
//
//+!meetAgent([SLAVE_AGENT, req(OTHER_X, OTHER_Y, BLOCK)], req(X, Y, B), master)
//    <-  ?getTeamAgentLocation(AGENT, relative(CUR_X, CUR_Y));
//        .print("Slave At: ", CUR_X, ", ", CUR_Y);
//        ?isAttachedToCorrectSide(R_X, R_Y, BLOCK);
//        !doNothing;
//        !meetAgent([SLAVE_AGENT, req(OTHER_X, OTHER_Y, BLOCK)], req(X, Y, B), master).
//
//
//+!connectBlock(req(X, Y, BLOCK))[source(MASTER_AGENT)]
//    :   hasBlockAttached(BLOCK)
//    <-  ?getTeamAgentLocation(MASTER_AGENT, relative(CUR_X, CUR_Y));
//        ?isBlockAtLocation(BLOCK, relative(CUR_X + X, CUR_Y + Y)).