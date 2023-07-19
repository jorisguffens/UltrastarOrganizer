package be.jorisg.ultrastarorganizer.utils;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import com.drew.lang.annotations.NotNull;
import picocli.CommandLine;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class Tasker {

    private final Iterator<Task> tasks;

    private final Map<Task, CompletableFuture<Void>> running = new ConcurrentHashMap<>();
    private final CompletableFuture<Void> thiz = new CompletableFuture<>();

    private final int maxThreads;

    public Tasker(Iterator<Task> tasks, int maxThreads) {
        this.tasks = tasks;
        this.maxThreads = maxThreads;
    }

    public void start() {
        for (int i = 0; i < maxThreads; i++) {
            next();
        }
    }

    public void join() {
        thiz.join();
    }

    //

    private void next() {
        if (!tasks.hasNext()) {
            if (running.isEmpty()) {
                thiz.complete(null);
            }
            return;
        }
        if (running.size() >= maxThreads) {
            return;
        }

        Task task = tasks.next();
        CompletableFuture<Void> future = CompletableFuture.runAsync(task.executor())
                .handle((v, e) -> {
                    if ( e != null )
                        e.printStackTrace(UltrastarOrganizer.out);
                    running.remove(task);
                    next();
                    return null;
                });
        running.put(task, future);
    }

    public record Task(@NotNull String name, @NotNull Runnable executor) {
    }

}
