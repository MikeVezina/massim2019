package eis.percepts.requirements;

import map.Direction;
import map.Position;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * This is essentially a linked list node but specific to block requirements.
 *
 */
public class AttachableRequirement extends Requirement {

    private AttachableRequirement previousRequirement;
    private AttachableRequirement nextRequirement;

    private Direction nextRequirementDirection = Direction.NONE;
    private Direction previousRequirementDirection = Direction.NONE;


    public AttachableRequirement(Position position, String blockType) {
        super(position, blockType);
    }

    public AttachableRequirement(int x, int y, String blockType) {
        super(x, y, blockType);
    }

    public AttachableRequirement(Requirement requirement) {
        super(requirement.getPosition(), requirement.getBlockType());
    }

    public boolean attachRequirement(Requirement requirement) {
        return attachRequirement(new AttachableRequirement(requirement));
    }

    public Direction getAttachableDirection(Requirement requirement)
    {
        if (requirement == null)
            return Direction.NONE;

        // if the requirement is already attached to the block on the other side, return no direction.
        if(previousRequirement != null && previousRequirement.getPosition().equals(requirement.getPosition()))
            return Direction.NONE;

        Direction transition = getTransition(requirement);

        // North is not an attachable direction.
        if (transition.equals(Direction.NORTH))
            return Direction.NONE;

        // Disallow attaching requirements if the next requirement is already in the EAST / WEST direction.
        // If the next requirement is currently south, overwrite it with an EAST / WEST requirement.
        // This is an edge case that occurs when an requirement is both beside and below the current requirement.
        if (nextRequirement != null) {
            if (nextRequirementDirection.equals(Direction.EAST) || nextRequirementDirection.equals(Direction.WEST)) {
                return Direction.NONE;
            }
        }

        // Return the transition direction (could be None if transition is not valid)
        return transition;
    }

    public boolean attachRequirement(AttachableRequirement requirement) {

        Direction transition = getAttachableDirection(requirement);

        if(transition.equals(Direction.NONE))
            return false;

        // If the requirement is attachable and we already have a requirement, then it should be overwritten.
        // If the nextRequirement can not be overwritten, isAttachable would have returned false.
        if (nextRequirement != null)
            // Disconnect the next block, since it will be overwritten. (Do not force this == false)
            disconnectNext(false);

        // If there is no valid transition or if the next requirement is already connected.
        if (!requirement.setPreviousRequirement(this))
            return false;

        forceAttachRequirement(requirement, transition);
        return true;
    }

    public void forceAttachRequirement(AttachableRequirement requirement, Direction direction)
    {
        this.nextRequirement = requirement;
        this.nextRequirementDirection = direction;
    }

    public boolean isHeadRequirement() {
        return previousRequirement == null;
    }

    public int size() {
        if (isTailRequirement())
            return 1;

        return 1 + nextRequirement.size();
    }

    public boolean isTailRequirement() {
        return nextRequirement == null;
    }

    private boolean canSetPreviousRequirement(AttachableRequirement requirement)
    {
        if (requirement == null)
            return false;


        if (previousRequirement != null) {
            System.out.println("A Previous Requirement has already been attached.");
            return false;
        }
        return true;
    }

    private boolean setPreviousRequirement(AttachableRequirement requirement) {
        if(canSetPreviousRequirement(requirement)) {
            this.previousRequirement = requirement;
            this.previousRequirementDirection = getTransition(requirement);
            return true;
        }

        return false;
    }

    public Direction getTransition(Requirement requirement) {
        Position transition = requirement.getPosition().subtract(this.getPosition());
        return Direction.GetDirection(transition);
    }

    private void disconnectPrevious(boolean force) {
        if (!force && nextRequirement != null) {
            previousRequirement.disconnectNext(true);
        }

        previousRequirement = null;
        previousRequirementDirection = Direction.NONE;
    }

    private void disconnectNext(boolean force) {
        if (!force && nextRequirement != null) {
            nextRequirement.disconnectPrevious(true);
        }

        nextRequirement = null;
        nextRequirementDirection = Direction.NONE;
    }

    public AttachableRequirement getNextRequirement() {
        return nextRequirement;
    }

    public AttachableRequirement getPreviousRequirement() {
        return previousRequirement;
    }

    public Direction getNextRequirementDirection() {
        return nextRequirementDirection;
    }

    public Direction getPreviousRequirementDirection() {
        return previousRequirementDirection;
    }

    public List<AttachableRequirement> toList()
    {
        AttachableRequirement curReq = this;
        List<AttachableRequirement> requirementList = new ArrayList<>();

        while(curReq != null) {
            requirementList.add(curReq);
            curReq = curReq.getNextRequirement();
        }

        return requirementList;
    }

    public Deque<Requirement> toRequirementList()
    {
        AttachableRequirement curReq = this;
        Deque<Requirement> requirementList = new LinkedList<>();

        while(curReq != null) {
            requirementList.add(curReq);
            curReq = curReq.getNextRequirement();
        }

        return requirementList;
    }

    public static void main(String[] args) {

        AttachableRequirement headRequirement = new AttachableRequirement(0, 1, "b0");
        headRequirement.attachRequirement(new AttachableRequirement(0, 2, "b1"));

        headRequirement.getNextRequirement().attachRequirement(new AttachableRequirement(0, 3, "b1"));
        headRequirement.getNextRequirement().attachRequirement(new AttachableRequirement(1, 2, "b1"));
        printAttachedChain(headRequirement);
    }

    private static void printAttachedChain(AttachableRequirement head) {
        AttachableRequirement cur = head;
        System.out.println(head.size());
        while (cur != null) {
            System.out.println(cur);
            System.out.println(cur.getNextRequirementDirection());
            cur = cur.getNextRequirement();
        }
    }

    public void setPrevious(AttachableRequirement previous) {
        this.previousRequirement = previous;
    }
}
