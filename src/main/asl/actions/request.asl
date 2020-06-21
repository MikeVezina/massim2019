/**
 * Attach Component (attach.asl)
 * This component is responsible for handling the attach command.
 * This also includes action failures.
 */

+!request(DIR)  :   .ground(DIR)    <-  !performAction(request(DIR)).

// On successful attachment, we want to determine if there was more than one attachment
// that was attached. We also want to run the attach action, so that we can update our internal model
+!handleActionResult(request, [DIR], success)
    <-  .print("Request Success. Attaching Self to block.");
        !attachBlockDirection(DIR).
