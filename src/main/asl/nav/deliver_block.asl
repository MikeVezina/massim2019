isMasterReq(req(X, Y, _))
    :-  X == 0 & Y == 1.

getAttachedAbsolute(BLOCK, A_X, A_Y)
    :-  hasBlockAttached(R_X, R_Y, BLOCK) &
        calculateAbsolutePosition(relative(R_X, R_Y), absolute(A_X, A_Y)).

isBesideAbsolute(X, Y)
    :-  .ground(X) &
        .ground(Y) &
        calculateRelativePosition(relative(R_X, R_Y), absolute(X, Y)) &
        xyToDirection(R_X, R_Y, _).

/** Aligns the block to the requirement spec **/
+!alignBlock(req(X, Y, BLOCK))
    :   hasBlockAttached(X_ACTUAL, Y_ACTUAL, BLOCK) &
        .ground(X_ACTUAL) & .ground(Y_ACTUAL) &
        (X_ACTUAL \== X | Y_ACTUAL \== Y)
    <-  .print("Aligning Block");
        !rotateToDirection(X_ACTUAL, Y_ACTUAL, X, Y);
        !alignBlock(req(X, Y, BLOCK)).

/** Aligns the block to the requirement spec **/
+!alignBlock(req(X, Y, BLOCK))
    :   hasBlockAttached(X_ACTUAL, Y_ACTUAL, BLOCK) &
        X_ACTUAL == X & Y_ACTUAL == Y
    <- .print("Has Good Alignment.").

+!alignBlock(_)
    <-  .print("Align Block Missing?").

+!informSlaves(TASK)
    :   eis.internal.get_next_req(TASK, _, done)
    <-  .print("All Requirements met.");
        !performAction(submit(TASK)).

+!informSlaves(TASK)
    :   eis.internal.get_next_req(TASK, PREV_REQ, REQ) &
        taskAssignment(SLAVE, TASK, REQ)
    <-  .print("Found Slave: ", SLAVE, ". Prev REQ: ", PREV_REQ, ". Next REQ = ", REQ);
        .send(SLAVE, tell, meetingPointSet(TASK));
        .abolish(slaveConnect(TASK,_)[source(_)]);
        !prepareForConnect(SLAVE); // Allow connect actions from the slave
        !waitForConnect(SLAVE, TASK, PREV_REQ, REQ);
        !informSlaves(TASK).

+!waitForConnect(SLAVE, TASK, PREV_REQ, REQ)
    :   not(slaveConnect(TASK, REQ)[source(SLAVE)]) &
        (req(CON_X, CON_Y, _) = PREV_REQ)
    <-  .print("Waiting for slave.");
        .wait(50);
        !skipUntilSlaveConnect(SLAVE);
        !waitForConnect(SLAVE, TASK, PREV_REQ, REQ).

+!skipUntilSlaveConnect(SLAVE)
    :   not(slaveConnect(_,_)[source(SLAVE)])
    <-  !performAction(skip).

+!skipUntilSlaveConnect(_)
    <-  .print("No Need to skip").

+!waitForConnect(SLAVE, TASK, PREV_REQ, REQ)
    :   slaveConnect(TASK, REQ)[source(SLAVE)] &
        (req(CON_X, CON_Y, _) = PREV_REQ)
    <-  .print("Attempting to connect.");
        !connect(SLAVE, CON_X, CON_Y);
        .print("Disconnecting Slave");
        !disconnect(SLAVE, REQ).

//+!waitForDetach(SLAVE, REQ)
//    :    not(slaveDetached[source(SLAVE)]) &
//            slaveDetachedNext[source(SLAVE)]
//    <-  .print("Slave detach on next step");
//         //!performAction(skip);
//         !disconnect(SLAVE, REQ).
////        !waitForDetach(SLAVE, REQ).

+!disconnect(Slave, req(ReqX, ReqY, Block))
    :   percept::teamAgent(SlaveX, SlaveY, Slave) &
        percept::attached(SlaveX, SlaveY) // Is the slave agent attached to us?
    <- !performAction(disconnect(ReqX, ReqY, SlaveX, SlaveY));
       ?percept::lastActionResult(success).
//       .send(Slave, tell, reqDisconnected(req(ReqX, ReqY, Block))).

+!disconnect(Slave, req(ReqX, ReqY, _))
    :   percept::teamAgent(SlaveX, SlaveY, Slave) &
        not(percept::attached(SlaveX, SlaveY)) // Is the slave agent attached to us?
    <- .print("Agent ", Slave, " already detached!").

@disconnect[breakpoint]
-!disconnect(_, _) <- .print("failed to disconnect!."); .fail.


// Disconnect handler
+!handleActionResult(disconnect, PARAMS, success)
    <-  .print("(Warning): No handler for successful Action: ", ACTION).

//+!waitForDetach(SLAVE, REQ)
//    :    not(slaveDetached[source(SLAVE)])
//    <-  .print("Waiting for slave detach");
//        .wait(50);
//        !waitForDetach(SLAVE, REQ).
//
//+!waitForDetach(SLAVE, REQ)
//    :   slaveDetached[source(SLAVE)] &
//        req(X, Y, BLOCK) = REQ
//    <-  .print("Slave ", SLAVE, " detached").


+!deliverBlock(TASK, REQ)
    :   isMasterReq(REQ) &
//        eis.internal.get_next_req(TASK)
        eis.internal.meeting_point(TASK, LOC) &
        location(X, Y) = LOC
    <-  .print("I Am Master! Meeting Point: ", LOC);
        !navigateToDestination(X, Y);
        .print(RES);
        !alignBlock(REQ);
        !informSlaves(TASK).

+!skipForever
    <-  !performAction(skip);
        !skipForever.




+!moveAndRotateBlock(BLOCK, DEST_X, DEST_Y) // DEST_X and DEST_Y are absolute coordinates for where the block needs to go
    :   hasBlockAttached(R_X, R_Y, BLOCK) & // Get the relative position of the block
        calculateRelativePosition(relative(D_RX, D_RY), absolute(DEST_X, DEST_Y)) & // Calculate the relative position of the destination
        (D_RX == 0 & D_RY == 0) // If the agent is currently on the destination
    <-  !moveOrExplore(BLOCK, DEST_X, DEST_Y); // Move one space away from the cell
        !moveAndRotateBlock(BLOCK, DEST_X, DEST_Y).

+!moveAndRotateBlock(BLOCK, DEST_X, DEST_Y) // DEST_X and DEST_Y are absolute coordinates for where the block needs to go
    :   hasBlockAttached(R_X, R_Y, BLOCK) & // Get the relative position of the block
        calculateRelativePosition(relative(D_RX, D_RY), absolute(DEST_X, DEST_Y)) & // Calculate the relative position of the destination
        (D_RX \== R_X | D_RY \== R_Y) // If the block is currently NOT on the destination
    <-  !rotateToDirection(R_X, R_Y, D_RX, D_RY);
        !moveAndRotateBlock(BLOCK, DEST_X, DEST_Y). // Move one space away from the cell

+!moveAndRotateBlock(BLOCK, DEST_X, DEST_Y)
    :   hasBlockAttached(R_X, R_Y, BLOCK) & // Get the relative position of the block
            calculateRelativePosition(relative(D_RX, D_RY), absolute(DEST_X, DEST_Y)) & // Calculate the relative position of the destination
            (D_RX == R_X & D_RY == R_Y) // If the block is currently on the destination
    <-  .print("No need to move/rotate block: ", BLOCK, ", ", DEST_X, ", ", DEST_Y).

+!moveAndRotateBlock(BLOCK, DEST_X, DEST_Y)
    <-  .print("No Applicable plan for ", BLOCK, ", ", dest(DEST_X, DEST_Y), ". May be due to a blocked destination.").


+!moveOrExplore(BLOCK, DEST_X, DEST_Y)
    :   hasBlockAttached(R_X, R_Y, BLOCK) &
        xyToDirection(-R_X, -R_Y, DIR) &
        eis.internal.can_agent_move(DIR)
     <- .print("Moving... ", DIR);
        !move(DIR).

+!moveOrExplore(BLOCK, DEST_X, DEST_Y)
    :   hasBlockAttached(R_X, R_Y, BLOCK)
     <- .print("Exploring... ", DIR);
        !explore.

//+!navigateBlock(req(_, _, BLOCK), X, Y)
//    :   getAttachedAbsolute(BLOCK, A_X, A_Y) &
//        not(isAtLocation(X, Y)) &
//        isBesideAbsolute(A_X, A_Y)
//    <-


+!navigateBlock(req(_, _, BLOCK), X, Y)
    :   getAttachedAbsolute(BLOCK, A_X, A_Y) &
        A_X == X & A_Y == Y
    <-  .print("Block Delivered!").


+!navigateBlock(req(_, _, BLOCK), X, Y) // Deliver BLOCK to X, Y absolute
    :   getAttachedAbsolute(BLOCK, A_X, A_Y) & // Get Current Absolute of attached BLOCK
        (A_X \== X | A_Y \== Y) & // Compare that the block coordinates do not match the destination
        isAtLocation(X, Y) &
        D_X = X - A_X &
        D_Y = Y - A_Y &
        xyToDirection(D_X, D_Y, DIR)
    <-  .print("Delivering Block. I'm standing next to the delivery destination!");
        !moveAndRotateBlock(BLOCK, X, Y);
        !navigateBlock(req(_, _, BLOCK), X, Y).

+!navigateBlock(req(_, _, BLOCK), X, Y)
    :   getAttachedAbsolute(BLOCK, A_X, A_Y) &
        (A_X \== X | A_Y \== Y) &
        not(isCurrentLocation(X, Y))
    <-  .print("Delivering Block!");
        !stepToDestination(X, Y);
        !navigateBlock(req(_, _, BLOCK), X, Y).

+!slaveConnect(MASTER, TASK, REQ)
    :   req(_, _, BLOCK) = REQ &
        hasBlockAttached(X, Y, BLOCK)
    <-  .send(MASTER, tell, slaveConnect(TASK, REQ));
        !prepareForConnect(MASTER);
        !connect(MASTER, X, Y);
        !skipUntilDisconnect(REQ, MASTER);
        !exploreForever.

+!skipUntilDisconnect(req(R_X, R_Y, Block), Master)
    :   hasBlockAttached(X, Y, Block)
    <-  .print("Skipping until disconnect");
        !performAction(skip);
        !skipUntilDisconnect(req(R_X, R_Y, Block), Master).

+!skipUntilDisconnect(req(R_X, R_Y, Block), Master)
    :   not(hasBlockAttached(X, Y, Block))
    <- .print("Block disconnected?").

-!skipUntilDisconnect(Req, Master)
    <- .print("Failed to skip until disconnect?");
        .fail.

+!deliverBlock(TASK, REQ)
    :   not(isMasterReq(REQ)) &
        meetingPointSet(TASK)[source(MASTER)] &
        eis.internal.get_slave_meeting_point(MASTER, REQ, location(X, Y))
    <-  .print("I Am Slave: ", REQ, " with meeting point.", X, " ", Y);
        !navigateBlock(REQ, X, Y);
        !slaveConnect(MASTER, TASK, REQ).

+!deliverBlock(TASK, REQ)
    <-  .print("I am not master, and do not have a meeting point.");
        !explore;
        !deliverBlock(TASK, REQ).