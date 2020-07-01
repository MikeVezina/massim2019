{ include("epistemic/epistemic.asl") }
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
    <-  .df_register("collector").
//        !startAgents.

+!stayForever
    <-  !performAction(skip);
        !stayForever.
