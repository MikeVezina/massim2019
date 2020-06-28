//{ include("common.asl") }

hasCurrentDir(DIR)
    :- currentDir(DIR).

exploreDirection(DIR) :-
    eis.internal.explore_direction(DIR).

+?hasCurrentDir(DIR)
    :   randomDirection(D)
    <-  +currentDir(D);
        ?hasCurrentDir(DIR).

+?exploreDirection(DIR)
    <- .fail.


+!explore
    <-  ?exploreDirection(DIR);
        !move(DIR).

-!explore[moveError(failed_path)]
    <-  .print("Move failed with failed_path. Attempting to re-run explore.");
        !explore.

-!explore[rotationError(exhausted)]
    <-  .print("Rotations Exhausted. Attempting to re-run explore.");
        !explore.

-!explore[error(E)]
    <-  .print("Failed to explore. Trying again. ", E);
        .fail.

+!exploreForever
    <-  !explore;
        .print("Exploring Forever");
        !exploreForever.
