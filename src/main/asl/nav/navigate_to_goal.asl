//{ include("nav/nav_common.asl") } // Also includes common.asl via transitivity

closestGoalPerception(goal(X, Y)) :-
    percept::goal(X, Y) &
    not(percept::goal(X_2, Y_2) &
    calculateDistance(DIST, X, Y) &
    calculateDistance(DIST_2, X_2, Y_2) &
    DIST > DIST_2).


isAtGoal :-
    hasGoalPerception(X, Y) &
    isCurrentLocation(X, Y).

+?isAtGoal
    <-  ?hasGoalPerception(_, _); // Checks to see if we can find a goal perception
        ?closestGoalPerception(goal(X, Y)); // Finds the closest goal perception
        !navigateToLocation(relative(X, Y)); // Navigates to the goal location
        ?isAtGoal. // Re-test goal

// If this test goal addition occurs, that means we do not see any goal locations
// and we need to explore for a goal location
// TODO: Test this.
+?hasGoalPerception(X, Y)
    <-  !explore;
        ?hasGoalPerception(X, Y).


+!navigateToGoal
    <-  ?isAtGoal.