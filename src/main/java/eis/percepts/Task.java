package eis.percepts;

import eis.EISAdapter;
import eis.iilang.*;
import eis.percepts.requirements.Requirement;
import jason.asSyntax.Literal;
import massim.protocol.data.TaskInfo;
import massim.protocol.data.Thing;
import utils.RequirementPlanner;

import java.util.*;

public class Task extends ParsedPercept {
    public static final String PERCEPT_NAME = "task";
    private String name;
    private int deadline;
    private int reward;
    private boolean isExpired;
    private boolean isCompleted;

    private Literal taskLiteral;
    private final Deque<Requirement> requirementList;

    private Task(String name, int deadline, int reward, List<Requirement> requirements) {
        this.name = name;
        this.deadline = deadline;
        this.reward = reward;
        this.requirementList = RequirementPlanner.SortRequirements(requirements);
        this.isExpired = false;
        this.isCompleted = false;
    }

    public String getName() {
        return name;
    }

    public int getDeadline() {
        return deadline;
    }

    public int getReward() {
        return reward;
    }

    public List<Requirement> getRequirementList() {
        return new ArrayList<>(requirementList);
    }

    public Deque<Requirement> getPlannedRequirements()
    {
        return requirementList;
    }

    public boolean isExpired() {
        return isExpired;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public static Task parseTask(Percept l) {
        String name = ((Identifier) l.getParameters().get(0)).getValue();
        int deadline = ((Numeral) l.getParameters().get(1)).getValue().intValue();
        int reward = ((Numeral) l.getParameters().get(2)).getValue().intValue();
        ParameterList requirementParamList = ((ParameterList) l.getParameters().get(3));

        List<Requirement> requirementList = parseRequirementList(requirementParamList);
        Task newTask = new Task(name, deadline, reward, requirementList);
        newTask.setTaskLiteral(EISAdapter.perceptToLiteral(l));

        return newTask;
    }

    private static List<Requirement> parseRequirementList(ParameterList reqParamList) {
        ArrayList<Requirement> reqs = new ArrayList<>();
        reqParamList.forEach(req -> reqs.add(Requirement.parseRequirementParam((Function) req)));
        return reqs;
    }

    public void setExpired(boolean expired) {
        isExpired = expired;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Task)) return false;
        Task task = (Task) o;
        return name.equals(task.name);
    }

    public Literal getTaskLiteral() {
        return taskLiteral;
    }

    public void setTaskLiteral(Literal taskLiteral) {
        this.taskLiteral = taskLiteral;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public static boolean canParse(Percept l) {
        return l != null && l.getName().equalsIgnoreCase(PERCEPT_NAME);
    }
}
