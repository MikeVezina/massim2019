/**
 * Attach Component (attach.asl)
 * This component is responsible for handling the attach command.
 * This also includes action failures.
 */

// Checks if a given block is attached to anything
isBlockAttached(X, Y) :- percept::attached(X, Y).

hasDoubleAttachment(NextAction) :- eis.internal.fix_shared_attachment(NextAction) & .ground(NextAction).

// Returns a block that is currently free (not attached to any entity)
hasFreeBlock(X, Y, BLOCK_TYPE) :- hasBlockPerception(X, Y, BLOCK_TYPE)
                                    & not(isBlockAttached(X, Y)). // Ensure block is not attached to anything.

hasFreeBlockBeside(BLOCK_TYPE, DIR) :-
    hasFreeBlock(X, Y, BLOCK_TYPE) &
    xyToDirection(X, Y, DIR). // Determine if the X, Y coordinates correspond to a direction

// Attaches a block of type BLOCK_TYPE
+!attachBlock(BLOCK_TYPE)
    :   hasFreeBlockBeside(BLOCK_TYPE, DIR) // Get a block beside us that isn't attached
    <-  .print("Attaching a free (block): ",BLOCK_TYPE , " in direction: ", DIR);
        !attach(DIR).

// Attaches a block in the given direction
+!attachBlockDirection(Dir)
    :   hasFreeBlockBeside(BLOCK_TYPE, Dir) // Check to see if we have a free block beside us.
    <-  .print("Attaching a free block: ", BLOCK_TYPE, " in (direction): ", Dir);
        !attach(Dir).
/**
    Reasons for this failure to trigger:
    -   No block of type BLOCK_TYPE is beside us
    -   BLOCK_TYPE is beside us, but already attached to our self or a nearby agent
        (check attached percept)
**/
+!attachBlock(BLOCK_TYPE)
    :   not(hasFreeBlockBeside(BLOCK_TYPE, _))
    <-  .print("There was an issue attaching block type ", BLOCK_TYPE, ".");
        .fail(attachError(no_free_block)).

+!attachBlockDirection(Dir)
    :   not(hasFreeBlockBeside(_, Dir))
    <-  .print("There was an issue attaching block in direction ", Dir, ".");
        .fail(attachError(no_free_block)).


// This checks to see if we have attached to a block that another teammate has attached to (in the same step)
// If there is a double attachment, we need to determine if we should drop it. Otherwise, movement becomes unpredictable.
+!checkRemoveDoubleAttach(dropAttach(X, Y))
    :   xyToDirection(X, Y, Dir)
    <- .print("We should drop attachment at: (", X, ", ", Y, "): ", Dir);
       !detach(Dir);
       !checkRemoveDoubleAttach. // Remove additional double attachments (if any)

// We get to keep the attachment. Wait until the other agent drops it.
+!checkRemoveDoubleAttach(waitDetach)
    <- .print("We should wait for an agent to drop a shared attachment");
       !performAction(skip)
       !checkRemoveDoubleAttach. // Check if we need to wait another step

// Should not occur...
-!checkRemoveDoubleAttach(NextAction)
        <-  .print("Failed to check double action with: ", NextAction);
            !checkRemoveDoubleAttach. // Remove additional double attachments (if any)

// Check if two (or more) agents have attached the same block
+!checkRemoveDoubleAttach
    :   hasDoubleAttachment(NextAction)
    <-  !checkRemoveDoubleAttach(NextAction).

+!checkRemoveDoubleAttach : not hasDoubleAttachment(_) <- .print("No shared attachments").

/** Handle Attach Actions **/
+!attach(X, Y) :  xyToDirection(X, Y, DIR) <- !attach(DIR).
+!attach(DIR)  <- !performAction(attach(DIR)).

+!detach(X, Y) :  xyToDirection(X, Y, DIR)  <- !detach(DIR).
+!detach(DIR)                               <- !performAction(detach(DIR)).

// On successful attachment, we want to determine if there was more than one attachment
// that was attached. We also want to run the attach action, so that we can update our internal model
+!handleActionResult(attach, [DIR], success)
    <-  //blockAttached(DIR);
        !checkRemoveDoubleAttach;
        !afterAttachSuccess. // Print the blocks after any processing


+!afterAttachSuccess
    : .findall([X, Y], attached(X, Y), ATTACHED_BLOCKS)
    <- .print("Successfully Attached. Attachment Percepts: ", ATTACHED_BLOCKS).


// Attach Action failures
+!handleActionResult(attach, [DIR], FAILURE)
    : FAILURE \== success
    <-  .print("This is where the attach failure is handled: ", FAILURE); .fail.
