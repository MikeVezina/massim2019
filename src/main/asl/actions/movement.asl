/* Movement Component
 *
 * movement.asl should define all plans and test goals for achieving one unit of movement.
 * This file should handle obstacle avoidance, as well as action failures, so that the agent reliably gets
 * to it's destination.
 *
 * This component makes the assumption that the agent has either 0 or 1 attached blocks for the sake of
 * rotational and movement simplicity. This component also makes the assumption that path finding has
 * already generated a suitable path for the agent. This means that we can guarantee that the agent will not be blocked.
 * Any attachments however may be blocked in their current orientation. If the agent itself is blocked, we will trigger a
 * plan failure, otherwise if the attachment is blocked, we will try to find a suitable rotation so that
 * we may successfully navigate in the specified direction.
 */

direction(n).
direction(s).
direction(e).
direction(w).


// Check if the attached block is unblocked in the given direction
attachmentsUnblocked(DIR) :-
    not(eis.internal.are_attachments_blocked(DIR)).

// Checks if the agent is unblocked in the given direction
agentUnblocked(DIR) :-
    not(eis.internal.is_agent_blocked(DIR)).

// Checks to see if the agent can move in the given direction.
// This should also check to see if any attachments are blocked.
canMove(DIR)
    :-  agentUnblocked(DIR) &
        not(hasAgentAttached) &
        attachmentsUnblocked(DIR).

hasAgentAttached :- hasAttached(X, Y, entity, _) & .print("Has agent attached: (", X, Y, ").").


/** Attachments Unblocked Test Goal Events (An attachment is blocked) **/
// This test goal event occurs when the attachments are blocked.
// Checks if there is an available rotation
+?attachmentsUnblocked(DIR)
    <-  .print("Exhausting Rotation to unblock attachments.");
        !exhaustedRotation;
        ?attachmentsUnblocked(DIR).

/** Can Move Test Goal Events (The agent or attachment is blocked) **/
+?canMove(DIR)
    <-  ?agentUnblocked(DIR); // Test if the agent is unblocked
        !resetExhaustedRotations;
        ?attachmentsUnblocked(DIR); // Test if the attachments are unblocked.
        ?canMove(DIR). // Re-test goal


/** Same as below, but does not attempt to rotate the agent to free up the attachments. **/
+!move(DIR)[no_rotation]
    :   .ground(DIR) & direction(DIR) & canMove(DIR)
    <-  .print("Moving ", DIR, " without rotation.");
        !performAction(move(DIR)).

+!move(DIR)[no_rotation]
    :   .ground(DIR) & direction(DIR) & not(canMove(DIR))
    <-  .print("Could not move in ", DIR, ".");
        .fail(moveError(blocked)).


/**
    Checks if the agent can move in the given direction, and attempts basic rotations if our attachments
    are blocking our movement. This plan will fail if we are blocked and no rotations are available,
    or if the agent is blocked (this should not occur because path finding
     should give us a path that doesn't block the agent).
**/
+!move(DIR) : .ground(DIR) & direction(DIR)  <- .print("Moving in Direction: ", DIR); ?canMove(DIR);  !performAction(move(DIR)).
+!move(DIR)                                  <- .print("Failed to provide a valid direction: ", DIR); .fail.


+!handleActionResult(move, [DIR], failed_forbidden)
    <-  .print("Forbidden location encountered.");
        addForbiddenDirection(DIR).

/** failed_path can also occur if two agents attempt to move to the same cell on the same step **/
+!handleActionResult(move, [DIR], failed_path)
    :   canMove(DIR)
    <-  .print("Forbidden Path: ", DIR).

/** failed_path can also occur if two agents attempt to move to the same cell on the same step **/
+!handleActionResult(move, [DIR], failed_path)
    <-  .print("Possible path finding error OR forbidden location (if there are attachments in direction: ", DIR, ").");
        eis.internal.debug;
        .fail(moveError(failed_path)).