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

/***** Initial Goals ******/
// None right now. We wait for the simulation to start.

@contained_agentOff[atomic]
+percept::step(_)
    :   eis.internal.is_agent_contained(X, Y)
    <-  .drop_all_intentions;
        !!breakOut(X, Y).

+!breakOut(X, Y)
    <-  !clear(X, Y);
        !achieveTasks.

-!breakOut(X, Y)
    <-  !!breakOut(X, Y).

+percept::simStart
    <-  !searchForEnemy.

+percept::energy(NEW_ENERGY)
    <-  .print("Updated Energy: ", NEW_ENERGY).

hasBlockAttachedToEntity(ENT_X, ENT_Y)
    :-   percept::thing(B_X, B_Y, block, _) &
        (D_X = B_X - ENT_X) &
        (D_Y = B_Y - ENT_Y) &
        xyToDirection(D_X, D_Y, DIR) &
        percept::attached(B_X, B_Y).

+!searchForEnemy
    :   percept::thing(X, Y, entity, OTHER_TEAM) &
        not(percept::team(OTHER_TEAM)) &
        percept::goal(X, Y) &
        hasBlockAttachedToEntity(X, Y)
    <-  .print("Found at ", X, ", ", Y, OTHER_TEAM);
        !clear(X, Y);
        !searchForEnemy.


+!searchOrExplore
    :   eis.internal.navigation_goal(location(X, Y))
    <-  .print("Going to goal: ", loc(X, Y));
        !navigateToDestination(X, Y).

+!searchOrExplore
    :   not(eis.internal.navigation_goal(location(X, Y)))
    <-  !explore.

+!searchForEnemy
    <-  .print("Searching");
        !searchOrExplore;
        !searchForEnemy.

-!searchForEnemy
    <-  .print("Failed. Trying again.");
        .wait(100);
        !searchForEnemy.