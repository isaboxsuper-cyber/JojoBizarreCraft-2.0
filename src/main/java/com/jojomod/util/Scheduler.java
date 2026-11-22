package com.jojomod.util;

import java.util.ArrayList;
import java.util.List;

/** Agenda tarefas para daqui a N ticks do servidor (continua rodando mesmo com o tempo parado). */
public final class Scheduler {
    private static final class Task {
        int delay;
        final Runnable run;
        Task(int delay, Runnable run) { this.delay = delay; this.run = run; }
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static long ticks = 0;

    private Scheduler() {}

    public static long now() {
        return ticks;
    }

    public static void later(int t, Runnable r) {
        TASKS.add(new Task(Math.max(1, t), r));
    }

    public static void clear() {
        TASKS.clear();
    }

    public static void tick() {
        ticks++;
        if (TASKS.isEmpty()) return;
        List<Task> snapshot = new ArrayList<>(TASKS);
        TASKS.clear();
        for (Task t : snapshot) {
            if (--t.delay <= 0) {
                try {
                    t.run.run();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                TASKS.add(t);
            }
        }
    }
}
