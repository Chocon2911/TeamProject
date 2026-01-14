package com.ossimulator.core;

import com.ossimulator.kernel.Kernel;

/**
 * Standard simulation scenarios for testing and demonstration.
 * Loads a uniform set of processes into the kernel.
 */
public class ScenarioLoader {

    public static void loadStandardScenario(Kernel kernel) {
        kernel.createProcess("Chrome", 10, 5);

        System.out.println("--- Medium Priority (4) [Round Robin Group] ---");
        kernel.createProcess("Terminal", 4, 4);
        kernel.createProcess("Spotify", 6, 2);
        kernel.createProcess("Discord", 5, 3);

        System.out.println("=== Loading Standard Scenario (9 Processes) ===");
        System.out.println("--- High Priority (7) [Round Robin Group] ---");
        kernel.createProcess("VSCode", 8, 7);

        System.out.println("--- Min Priority (1) ---");
        kernel.createProcess("Calculator", 2, 1);

        System.out.println("--- Low Priority (2) [Round Robin Group] ---");
        kernel.createProcess("Docker", 7, 2);
        System.out.println("===============================================");
    }
}
