// Maybe make sure we aren't dropping any blocks on dispensers?
@drop[breakpoint]
+!dropOtherAttachments(BLOCK)
    :   eis.internal.get_attached_blocks(BLOCKS) &
        .member(attached(X, Y, block, O_BLOCK), BLOCKS) &
        O_BLOCK \== BLOCK &
        xyToDirection(X, Y, DIR)
    <-  .print("Dropping: ",  X, Y, O_BLOCK);
        !moveOffGoal(BLOCK);
        !performAction(detach(DIR));
        !dropOtherAttachments(BLOCK).

+!moveOffGoal(BLOCK)
    :   eis.internal.get_attached_blocks(BLOCKS) &
        .member(attached(X, Y, block, O_BLOCK), BLOCKS) &
        O_BLOCK \== BLOCK &
        xyToDirection(X, Y, DIR) &
        percept::goal(X, Y)
    <-  .print("Block on goal. Moving");
        !explore;
        !moveOffGoal(BLOCK).


+!moveOffGoal(GOAL)
    <-  .print("Moved off goal.").

+!dropOtherAttachments(BLOCK) <- .print("No Other Blocks to Detach! Requirement Block: ", BLOCK);.

+!obtainRequirement(REQ)
    :   .ground(REQ) &
        (req(X, Y, BLOCK) = REQ)
    <-  .print("Obtaining Requirement: ", REQ);
        !dropOtherAttachments(BLOCK);
        !obtainBlock(BLOCK).