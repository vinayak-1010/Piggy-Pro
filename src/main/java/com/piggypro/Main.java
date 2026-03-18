package com.piggypro;

/**
 * Main.java
 * ─────────────────────────────────────────────────────
 * Plain Java entry point that launches the JavaFX App.
 *
 * Why a separate Main class?
 * When packaging as a JAR without a module-info.java,
 * the JVM sometimes fails to launch a class that directly
 * extends Application. This thin wrapper avoids that issue.
 *
 * Usage:  java -jar piggypro.jar
 *         (or just Run from IntelliJ)
 */
public class Main {
    public static void main(String[] args) {
        App.main(args);
    }
}
