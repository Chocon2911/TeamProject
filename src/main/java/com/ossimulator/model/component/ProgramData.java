package com.ossimulator.model.component;

import java.util.Map;
import java.util.Stack;

public class ProgramData {
    //==========================================Variable==========================================
    private final Map<String, Integer> variables;
    private final Stack<String> callStack;

    //========================================Constructor=========================================
    public ProgramData() {
        this.variables = new java.util.HashMap<>();
        this.callStack = new Stack<>();
    }

    public ProgramData(Map<String, Integer> variables) {
        this.variables = variables;
        this.callStack = new Stack<>();
    }

    //==========================================Get Set===========================================
    public Map<String, Integer> getVariables() { return variables; }
    public Stack<String> getCallStack() { return callStack; }

    public void pushToCallStack(String functionName) { callStack.push(functionName); }
    public String popFromCallStack() { return callStack.pop(); }
}