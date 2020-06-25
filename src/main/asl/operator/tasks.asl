

+!refreshTaskAssignments
    <-  for(taskAssignment(AGENT, TASK, _))
        {
            .print("Test: ", TASK);
            if(not(eis.internal.is_task_valid(TASK)))
            {
                .print("Task ", TASK, " has expired for agent: ", AGENT);
                -taskAssignment(AGENT, TASK, _);
                .send(AGENT, tell, taskInvalid(TASK));
            }
        }.



+!informAgents
    :   .setof([O_AGENT, TASK], taskAssignment(O_AGENT, TASK, _), TASK_LIST) &
        not(.length(TASK_LIST, 0))
    <-  // Go through all agent assignments and inform them of all of their teammates
        for (.member([O_AGENT, TASK], TASK_LIST)
                & .setof(taskAssignment(AGENT, TASK, REQ), taskAssignment(AGENT, TASK, REQ), AGENTS))
        {
            .send(O_AGENT, tell, AGENTS);
            // For each task, we want to give each agent the list of all task assignments
            .print("Informing Agent: ", O_AGENT, ". Agent Assignments: ", AGENTS);
        }.

+!informAgents.

+!assignTasks
    :   getFreeAgents(FREE_AGENTS) &
        not(.length(FREE_AGENTS, 0)) &
        eis.internal.select_task(FREE_AGENTS, RESULTS) &
        not(.length(RESULTS, 0)) &
        step(STEP)
    <-  // I can't believe I'm using a for loop. This is disgusting but much better than the alternative. :).
        for (.member([AGENT, TASK, REQ], RESULTS) ) {
                +taskAssignment(AGENT, TASK, REQ);
                .print("Agent ", AGENT, " has been assigned ", REQ, " of task: ", TASK);
        };
        !informAgents;
        .print("Assigned tasks at step ", STEP).


+!assignTasks
    <-  .print("No Free Agents (Or Tasks) Available.").
