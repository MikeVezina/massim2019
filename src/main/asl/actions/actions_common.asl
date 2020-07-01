getLastActionResult(RES) :-
    percept::lastActionResult(RES).

getLastAction(ACTION) :-
    percept::lastAction(ACTION).

getLastActionParams(PARAMS) :-
    percept::lastActionParams(PARAMS).


didActionSucceed :-
    getLastActionResult(success) & not(getLastAction(connect)) & not(getLastAction(submit)) & not(getLastAction(attach)) & not(getLastAction(detach)) & not(getLastAction(rotate)).

+!handleLastActionResult
    :   getLastActionResult(RESULT) &
        getLastAction(ACTION) &
        getLastActionParams(PARAMS)
    <-  !handleActionResult(ACTION, PARAMS, RESULT).

/* This is where we include action and plan failures */
@performAction[atomic]
+!performAction(ACTION)
    :   percept::step(STEP)
    <-  +lastAttemptedAction(ACTION); // Remember last action in case we need to re-attempt it
	    .print(STEP, ": Sending action: ", ACTION);
	    .print("Action Intention: ");
	    eis.internal.log_im_stack;
	    ACTION;
	    .wait("+percept::step(_)"); // Wait for the next simulation step
	    ?percept::step(STEP_AFTER);
	    .print("New Step: ", STEP_AFTER);
	    !handleLastActionResult;
	    -lastAttemptedAction(ACTION).



/**

Is this a Jason Bug???? Reported an issue.
* The following plans disallow backtracking for failed plans such as -!handleActionResult(_,_,_)


-!handleActionResult(ACTION, PARAMS, success)
    <-  .print("(Warning): No handler for successful Action: ", ACTION).

-!handleActionResult(_, _, failed_random)
    <-  .print("Failed randomly. Retrying.");
        !reattemptLastAction.

*/



@submit_handler[default]
+!handleActionResult(submit, [TASK], success)
    <-  .send(operator, tell, taskSubmitted(TASK));
        .abolish(slaveConnect(TASK,_)[source(_)]);
        .abolish(slaveDetached[source(_)]);
        taskSubmitted.

/** These are the default action handlers **/
@success_handler[default]
+!handleActionResult(ACTION, PARAMS, success)
    <-  .print("(Warning): No handler for successful Action: ", ACTION).

@failed_random_handler[default]
+!handleActionResult(_, _, failed_random)
    <-  .print("Failed randomly. Retrying.");
        !reattemptLastAction.

@failed_fallback[default]
+!handleActionResult(ACTION, PARAMS, RESULT)
    <-  .print("Action ", ACTION, "(", PARAMS, ") failure: ", RESULT, ". Not handled.");
        .fail.



+?didActionSucceed
    :   getLastAction(detach) &
        getLastActionResult(FAILURE) &
        (FAILURE == failed | FAILURE == failed_target)
    <-  .print("Failed to attempt detach. Failure: ", FAILURE).

+?didActionSucceed
    :   getLastAction(detach) &
        getLastActionResult(success) &
        getLastActionParams([DIR])
    <-  blockDetached(DIR).

+!reattemptLastAction
    :   lastAttemptedAction(ACTION)
    <-  .print("Re-attempting last action");
        !performAction(ACTION).

+!reattemptLastAction
    :   not(lastAttemptedAction(ACTION))
    <-  .print("Error: No Action Attempted.");
        .fail.

+?didActionSucceed
    :   getLastActionResult(failed_random)
    <-  .print("The action failed randomly.");
        !reattemptLastAction.

// This action failure occurs when an entity is blocking the dispenser.
// For now we just rotate and try again (assuming it is our own attached block that is blocking the dispenser)
+?didActionSucceed
    :   getLastAction(request) &
        getLastActionResult(failed_blocked)
    <-  .print("Blocked!");
        !performAction(rotate(cw));
        !reattemptLastAction.

+?didActionSucceed
    :   getLastAction(attach) &
        getLastActionResult(success) &
        getLastActionParams([DIR])
    <-  blockAttached(DIR).

+?didActionSucceed
    :   getLastAction(rotate) &
        getLastActionResult(success) &
        getLastActionParams([DIR])
    <-  .print("Rotate success").

+?didActionSucceed
    :   getLastAction(rotate) &
        getLastActionResult(failed) &
        getLastActionParams([DIR])
    <-  .print("Rotate Failed");
        .fail.


+?didActionSucceed
    :   getLastAction(connect) &
        getLastActionResult(failed_partner)
    <-  !reattemptLastAction.

+?didActionSucceed
    :   getLastAction(connect) &
        getLastActionResult(RESULT) &
        getLastActionParams(PARAMS)
    <-  .print("Connected Result: ", RESULT, ". Parameters: ", PARAMS).

+?didActionSucceed
    :   getLastAction(submit) &
        getLastActionResult(success) &
        getLastActionParams([TASK_NAME])
    <-  .print("Submit Success!");
        taskSubmitted;
        .send(operator, tell, taskSubmitted(TASK_NAME)).

+?didActionSucceed
    :   getLastAction(submit) &
        getLastActionResult(RESULT) &
        RESULT \== success
    <-  .print("Failed to submit. Reason: ", RESULT).

+?didActionSucceed
    :   getLastAction(attach) &
        getLastActionResult(failed_blocked)
    <-  .print("Blocked!");
        !performAction(rotate(cw));
        !reattemptLastAction.



+?didActionSucceed
    :   getLastAction(move) &
        getLastActionResult(failed_forbidden) &
        getLastActionParams([DIR])
    <-  addForbiddenDirection(DIR).

+?didActionSucceed
    :   getLastAction(move) &
        (getLastActionResult(failed_path))
    <-  .fail.

+?didActionSucceed
    :   getLastAction(move) &
        (getLastActionResult(failed_status))
    <-  !reattemptLastAction.

-?didActionSucceed
    :   percept::lastActionResult(FAILURE)
    <-  .print("Error: The action failed with: ", FAILURE, ". This should not have occurred.").