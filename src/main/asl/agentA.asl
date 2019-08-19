{ include("common.asl") }
{ include("internal_actions.asl") }

{ include("tasks/tasks.asl") }
{ include("tasks/requirements.asl") }

{ include("nav/navigation.asl", nav) }
{ include("internal_actions.asl") }
	
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


/***** Initial Goals ******/

// Do nothing, wait for request for help
+!coordinate : .my_name(agentA2).

+!test
    <-  .print("Selecting a Task: ", NAME);
        !selectTask(TASK);
        .print("Selected Task: ", TASK);
        ?selectTwoRequirements(REQ, REQQ);
        .print(REQ, ", ", REQQ);
        !achieveRequirement(REQ).

+!coordinate
    :   .my_name(agentA1)
    <-  .print("Selecting a Task: ", NAME);
        !selectTask(TASK);
        .print("Selected Task: ", TASK);
        .broadcast(tell, help(TASK)).

+accept(TASK) [source(SRC)]
    <-  .print("Task ", TASK, " accepted by Agent: ", SRC);
        ?selectTwoRequirements(R_1, R_2);
        .send(SRC, tell, assign(R_2));
        +assign(R_1).

+assign(REQ)
    <-  .print("Assigned: ", REQ);
        !achieveRequirement(REQ);
        !repeat.

+!repeat
    <-  !performAction(rotate(cw));
        !repeat.



+help(TASK)[source(SRC)]
    <-  .print("Help Requested for task: ", X, ", from Agent: ", SRC);
        .send(SRC, tell, accept(TASK)).

//!getPoints.
+percept::simStart
    <- !coordinate.

+percept::step(X)
    : percept::lastActionResult(RES) & percept::lastAction(ACT) & ACT \== no_action & percept::lastActionParams(PARAMS)
    <-  .print("Action: ", ACT, PARAMS, ". Result: ", RES).


/***** Plan Definitions ******/
// TODO: Action failures, See: http://jason.sourceforge.net/faq/#_which_information_is_available_for_failure_handling_plans


+!requestBlock(X, Y)
    :   hasDispenser(X, Y, _) &
        xyToDirection(DIR, X, Y)
    <-  !performAction(request(DIR));
        !performAction(attach(DIR)).


+!printReward
    :   percept::score(X)
    <-  .print("Current Score is: ", X).

/** Main Task Plan **/
+!getPoints
    <-  .print("Selecting a Task.");
        !selectTask(TASK);
        .print("Selected Task: ", TASK);
        !achieveTask.

+!achieveTask
    :   not(taskRequirementsMet)
    <-  !achieveNextRequirement.

+!achieveTask
    :   taskRequirementsMet
    <-  !nav::navigateToGoal;
        !submitTask;
        !printReward;
        .print("Finished");
        !getPoints.


+!achieveNextRequirement
    <-  !selectRequirements(REQ);
        .print("Selected Requirement: ", REQ);
        !achieveRequirement(REQ).

+!achieveRequirement(req(R_X, R_Y, BLOCK))
    <-  !nav::obtainBlock(BLOCK);
        !nav::navigateToGoal.
//        ?nav::isAttachedToCorrectSide(R_X, R_Y, BLOCK).



