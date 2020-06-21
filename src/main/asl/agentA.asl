{ include("reset.asl") }
{ include("common.asl") }
{ include("internal_actions.asl") }
{ include("actions/actions.asl") }
{ include("auth/auth.asl") }
{ include("auth/team.asl") }

{ include("tasks/requirements.asl") }
{ include("tasks/tasks.asl") }
{ include("nav/navigation.asl") }





/* MVP:
 * Iteration 0: No obstacles, deadlines are irrelevant, only one agent on the map, all important locations are hard-coded (dispenser, goals)


 * NOTE: Navigation module can be stubbed out to hard-code positions for everything
 * NOTE: These steps require the agent to remember it's location. The simulator does not provide location perception.

 * Steps:

 * 1. If Task Exists -> Parse Task for requirements
 * 		        Else -> Navigation: Survey Surroundings -> Repeat Step 1.


 * 2. For Each Required Block:
 *      If we have belief of block dispenser location -> Navigation: GoTo Belief Location
 *                                              Else  -> Navigation: Search For Required Block Dispenser
 * 		(Request) Block
 * 		(Attach) Requirement to Agent

 * 3. If we have a belief of the goal location -> Navigation: GoTo Goal Location
 *                                        else -> Navigation: Search for Goal Location

 *
 * 
 */

// Operator Agent Belief
operator(operator).

hasBeenStuck(STEP)
    :-  stuck(STEP - 1) &
        stuck(STEP - 2) &
        stuck(STEP - 3) &
        stuck(STEP - 4).

+percept::score(SCORE)
    <-  .print("New Score: ", SCORE).

@contained_agent[atomic]
+percept::step(STEP)
    :   eis.internal.is_agent_contained(X, Y) &
        hasBeenStuck(STEP)
    <-  +stuck(STEP);
        .drop_all_intentions;
        !!breakOut(X, Y).

@contained_agent_increment[atomic]
+percept::step(STEP)
    :   eis.internal.is_agent_contained(X, Y) &
        not(hasBeenStuck(STEP))
    <-  +stuck(STEP).

+!breakOut(X, Y)
    <-  !clear(X, Y);
        !achieveTasks.


/***** Initial Goals ******/
// None right now. We wait for the simulation to start.

+!clear
    <-  !performAction(clear(0,-1));
        ?percept::thing(X,Y,marker,DET);
        .print("Marker Percept: ", X, ", ", Y, ", ", DET).

+!dropAnyAttach
    : percept::attached(X, Y) &
      xyToDirection(X, Y, DIR)
    <-  !detach(DIR);
        !dropAnyAttach.

-!dropAnyAttach
    <- .print("Drop Attachments failed. Trying again."); !dropAnyAttach.

+!dropAnyAttach
    <- .print("Attachments are cleared.").

+!startAgents
    <-  !dropAnyAttach;
        !achieveTasks.

+percept::simStart
    <-  .df_register("collector");
        !startAgents.

+!stayForever
    <-  !performAction(skip);
        !stayForever.




























//+percept::simStart
//    <-  !performAction(move(w)).


//+!navigateToRandomTest(X, Y)
//    <-  !navigateToDestination(X, Y);
//        .print(RESULT_VALUE).
//
//-!navigateToRandomTest(X, Y)
//    <-  .print("Could not find the destination. Let's explore!").



/***** Plan Definitions ******/
// TODO: Action failures, See: http://jason.sourceforge.net/faq/#_which_information_is_available_for_failure_handling_plans

//
//+!requestBlock(X, Y)
//    :   hasDispenser(X, Y, _) &
//        xyToDirection(X, Y, DIR)
//    <-  !performAction(request(DIR));
//        !performAction(attach(DIR)).
//
//
//+!navigationTest(X, Y)
//    <-  !navigatePathBetter(absolute(X, Y)).
//
//+!printReward
//    :   percept::score(X)
//    <-  .print("Current Score is: ", X).
//
///** Main Task Plan **/
//+!getPoints
//    <-  .print("Selecting a Task.");
//        !selectTask(TASK);
//        .print("Selected Task: ", TASK);
//        !achieveTask.
//
//+!achieveTask
//    :   not(taskRequirementsMet)
//    <-  !achieveNextRequirement.
//
//+!achieveTask
//    :   taskRequirementsMet
//    <-  !navigateToGoal;
//        !submitTask;
//        !printReward;
//        .print("Finished");
//        !getPoints.
//
//+!achieveNextRequirement
//    <-  !selectRequirements(REQ);
//        .print("Selected Requirement: ", REQ);
//        !achieveRequirement(REQ).
//
//+!clearExistingAttachments
//    :   hasAttached(X, Y) &
//        xyToDirection(X, Y, DIR)
//    <-  !performAction(detach(DIR));
//        !clearExistingAttachments.
//
//+!clearExistingAttachments
//    :   hasAttached(X, Y) &
//        not(xyToDirection(X, Y, _))
//    <- .print("not attached", X, Y);
//        !clearExistingAttachments.
//
//+!clearExistingAttachments
//    :   not(hasAttached(_,_,_))
//    <- .print("clearExistingAttachments: No Attachments exist!").
//
////+dropOffLocation(TASK, BLOCK)
////    :
////    <-
//
//// Place block assumes we are on the destination for the block
//+!placeBlock(BLOCK)
//    :   hasBlockAttached(A_X, A_Y, BLOCK) &
//        xyToDirection(-A_X, -A_Y, MOVE_DIR) &
//        isAgentBlocked(MOVE_DIR) &
//        getRotation(ROT)
//    <-  !performAction(rotate(ROT)); // Rotate the block to an available position
//        !placeBlock(BLOCK).
//
//+!placeBlock(BLOCK)
//    :   hasBlockAttached(A_X, A_Y, BLOCK) &
//        xyToDirection(A_X, A_Y, BLOCK_DIR) &
//        xyToDirection(-A_X, -A_Y, MOVE_DIR) &
//        not(isAgentBlocked(MOVE_DIR))
//    <-  .print("Agent Not Blocked."); !performMove(MOVE_DIR); !placeBlock(BLOCK). // Rotate the block to an available position
//       // !performAction(detach(BLOCK_DIR)).
//
//
//+!alignBlock(X, Y, BLOCK)
//    :   calculateRelativePosition(relative(R_X, R_Y), absolute(X, Y)) &
//        hasBlockAttached(R_X, R_Y, BLOCK) &
//        xyToDirection(R_X, R_Y, BLOCK_DIR) // Block is already on the destination
//    <-  .print("Success!").
////        !performAction(detach(BLOCK_DIR)).
//
//+!alignBlock(X, Y, BLOCK)
//    :   calculateRelativePosition(relative(R_X, R_Y), absolute(X, Y)) &
//        hasBlockAttached(B_X, B_Y, BLOCK) &
//        eis.internal.calculate_rotation(B_X, B_Y, R_X, R_Y, ROT) & // Checks if destination can be achieved through rotation
//        canRotate(ROT) // Get an unblocked rotation
//    <-  .print("Rotation: ", ROT);
//        !performAction(rotate(ROT));
//        !alignBlock(X, Y, BLOCK).
//
//+!alignBlock(X, Y, BLOCK)
//    :   calculateRelativePosition(relative(R_X, R_Y), absolute(X, Y)) &
//        hasBlockAttached(B_X, B_Y, BLOCK) &
//        eis.internal.calculate_rotation(B_X, B_Y, R_X, R_Y, ROT) & // Checks if destination can be achieved through rotation
//        not(canRotate(ROT)) & // Get an unblocked rotation
//        canRotate(ROT_OTHER) & ROT \== ROT_OTHER
//    <-  .print("Can't Rotate: ", ROT, ROT_OTHER);
//        !performAction(rotate(ROT_OTHER));
//        !alignBlock(X, Y, BLOCK).
//
//+!moveOnce
//    :   getMovementDirection(MOVE_DIR)
//    <-  !performAction(move(MOVE_DIR)).
//
//+!switchBlock(X, Y, BLOCK)
//    :   hasBlockAttached(A_X, A_Y, BLOCK) &
//        xyToDirection(A_X, A_Y, BLOCK_DIR)
//    <-  !moveOnce;
//        !alignBlock(X, Y, BLOCK).
//
//
//+!alignBlock(X, Y, BLOCK)
//    :   not(calculateRelativePosition(relative(R_X, R_Y), absolute(X, Y)) &
//        hasBlockAttached(R_X, R_Y, BLOCK))
//    <-  !navigatePathBetter(absolute(X, Y));
//        .print("Arrived at block destination. Time to place block.");
//        !switchBlock(X, Y, BLOCK).
//       // !placeBlock(BLOCK).
//
//+!dropOffBlock(TASK, REQ, master, SLAVE)
//    :   task(T_NAME, _,_,_) = TASK &
//        req(R_X, R_Y, BLOCK) = REQ &
//        .my_name(NAME) & eis.internal.meeting_point(T_NAME, NAME, location(D_X, D_Y))
//    <-  .print("Has Drop off: ", T_NAME, D_X, D_Y);
//        !navigatePathBetter(absolute(D_X, D_Y));
//        !alignBlock(D_X + R_X, D_Y + R_Y, BLOCK);
//        .print("Told ", SLAVE);
//        .send(SLAVE, tell, startSlave).
//
//
//
//+!dropOffBlock(TASK, REQ, slave, MASTER)
//    :   task(T_NAME, _,_,_) = TASK &
//        req(R_X, R_Y, BLOCK) = REQ &
//        percept::name(SIM_NAME) &
//        .my_name(NAME) & eis.internal.meeting_point(T_NAME, NAME, location(D_X, D_Y))
//    <-  .print("Has Drop off: ", T_NAME, D_X, D_Y);
//        !navigatePathBetter(absolute(D_X, D_Y));
//        !alignBlock(D_X + R_X, D_Y + R_Y, BLOCK);
//        .send(MASTER, tell, slaveFinished(NAME, SIM_NAME)).
//
//-!dropOffBlock(TASK, REQ, ROLE, OTHER_AGENT)[error(ERR)]
//    <-  .print("Drop off Failure. Trying again. ", ERR);
//        !dropOffBlock(TASK, REQ, ROLE, OTHER_AGENT).
//
////
////+!dropOffBlock(task(T_NAME, _,_,_), req(R_X, R_Y, BLOCK))
////    : .my_name(NAME) & eis.internal.meeting_point(T_NAME, NAME, location(D_X, D_Y)) & NAME == agentA2
////    <-  .print("A2 Has Drop off: ", TASK, D_X, D_Y);
////        !exploreForever.
//
//+slaveFinished(SLAVE, SLAVE_NAME)
//    :   percept::name(NAME)
//    <-  .send(SLAVE, achieve, connectBlock(NAME));
//        !connectBlock(SLAVE_NAME);
//        .abolish(slaveFinished(SLAVE, SLAVE_NAME)).
//
//+!submitCurrentTask
//    :   currentTask(task(TASK_NAME,DEADLINE,_,_))
//    <-  .print("Submitting task: ", TASK_NAME, ". Deadline: ", DEADLINE);
//        !performAction(submit(TASK_NAME)).
//
//+!connectBlock(OTHER_AGENT)[source(SRC)]
//    :   hasBlockAttached(X, Y, _) &
//        xyToDirection(X, Y, _) &
//        SRC == self
//    <-  .print("Connecting to ", OTHER_AGENT, ". ", X,  Y);
//        !performAction(connect(OTHER_AGENT, X, Y));
//        .print("Connected successfully. Waiting for detach.");
//        !skipUntilTrigger(detached);
//        !submitCurrentTask.
//
//wasConnectSuccess(_) :- getLastAction(connect) & getLastActionResult(success).
//
//+?wasConnectSuccess(CONNECT_ACT)
//    :   getLastAction(ACT) & getLastActionResult(RES)
//    <-  .print("Connect not success. Trying again. ", ACT, RES);
//        !performAction(CONNECT_ACT);
//        ?wasConnectSuccess(CONNECT_ACT).
//
//+!connectBlock(OTHER_AGENT)[source(SRC)]
//    :   hasBlockAttached(X, Y, _) &
//        xyToDirection(X, Y, DIR) &
//        SRC \== self
//    <-  .print("Slave connecting to ", OTHER_AGENT, ". ", X,  Y);
//        !performAction(connect(OTHER_AGENT, X, Y));
//        ?wasConnectSuccess(connect(OTHER_AGENT, X, Y));
//        .print("Connected block to master.");
//        !performAction(detach(DIR));
//        .send(SRC, tell, detached).
//
//
//+!dropOffBlock(TASK, req(R_X, R_Y, BLOCK), ROLE, OTHER_AGENT)
//    : .my_name(NAME) & not(eis.internal.meeting_point(T_NAME, NAME, LOC))
//    <- .print("Failed to find a Drop off location: ", TASK);
//        !explore;
//        !dropOffBlock(TASK, req(R_X, R_Y, BLOCK), ROLE, OTHER_AGENT).
//
//+!prepareForRequirement(OTHER_AGENT)
//    :   .my_name(AGENT)
//    <-  !clearExistingAttachments;
//        !searchForAgent(OTHER_AGENT);
//        .send(operator, askOne, taskAssignment(TASK, AGENT, REQ, OTHER_AGENT, ROLE)).
//
//+taskAssignment(TASK, AGENT, req(R_X, R_Y, BLOCK), OTHER_AGENT, master)
//    <-  .abolish(currentTask(_));
//        +currentTask(TASK);
//        !obtainBlock(BLOCK);
//        !dropOffBlock(TASK, req(R_X, R_Y, BLOCK), master, OTHER_AGENT).
//
//+taskAssignment(TASK, AGENT, req(R_X, R_Y, BLOCK), OTHER_AGENT, slave)
//    <-  .abolish(currentTask(_));
//        +currentTask(TASK);
//        !obtainBlock(BLOCK);
//        !exploreUntilTrigger(startSlave);
//        !dropOffBlock(TASK, req(R_X, R_Y, BLOCK), slave, OTHER_AGENT).
//
//+!skipUntilTrigger(TRIGGER)
//    :   not(TRIGGER)
//    <-  !performAction(skip);
//        !skipUntilTrigger(TRIGGER).
//
//+!skipUntilTrigger(TRIGGER)
//    :   TRIGGER
//    <-  .abolish(TRIGGER).
//
//
//
//+!exploreUntilTrigger(TRIGGER)
//    :   not(TRIGGER)
//    <-  !explore;
//        !exploreUntilTrigger(TRIGGER).
//
//+!exploreUntilTrigger(TRIGGER)
//    :   TRIGGER
//    <-  .abolish(TRIGGER). // Remove trigger so it doesnt get picked up twice
//
//+!achieveRequirement(TASK, req(R_X, R_Y, BLOCK), OTHER_AGENT, ROLE)[source(SRC)]
//    <-  !obtainBlock(BLOCK);
//        !dropOffBlock(TASK, req(R_X, R_Y, BLOCK), ROLE, OTHER_AGENT).


//-!achieveRequirement(TASK, REQ, OTHER_AGENT)[source(SRC)]
//    <-  .print("Plan Failure.");
//        !achieveRequirement(TASK, REQ, OTHER_AGENT)[source(SRC)].
//        ?isAttachedToCorrectSide(R_X, R_Y, BLOCK).

