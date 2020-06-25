
// Drops any attachments that aren't the passed-in block.
// Also drops if we have a chain of blocks (even if one of them is the required block)
+!dropOtherAttachments(Needed)
    :  eis.internal.get_attached_blocks(Blocks) &
       .length(Blocks, Sz) & Sz > 1 & // If we have more than one block attached
       .member(attached(X, Y, block, Block), Blocks) & // Find one that can be detached
        xyToDirection(X, Y, Dir)
    <-  .print("Dropping multiple blocks: ",  X, Y, Block);
        !moveOffGoal(Block);
        !performAction(detach(Dir));
        !dropOtherAttachments(Needed).

// Check for duplicate blocks of the correct type (And drop one)
+!dropOtherAttachments(BLOCK)
    :   eis.internal.get_attached_blocks(BLOCKS) &
        .member(attached(X, Y, block, BLOCK), BLOCKS) &
        .member(attached(X_O, Y_O, block, BLOCK), BLOCKS) & // find duplicate block
        ((X \== X_O) | (Y \== Y_O)) &
        xyToDirection(X, Y, DIR) &
        xyToDirection(X_O, Y_O, DIR)
    <-  .print("Dropping Duplicate: ",  X, Y, BLOCK);
        !moveOffGoal(BLOCK);
        !performAction(detach(DIR));
        !dropOtherAttachments(BLOCK).

// Maybe make sure we aren't dropping any blocks on dispensers?
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