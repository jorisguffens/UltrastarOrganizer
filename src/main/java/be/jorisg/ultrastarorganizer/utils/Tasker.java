package be.jorisg.ultrastarorganizer.utils;

import be.jorisg.ultrastarorganizer.UltrastarOrganizer;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;

public class Tasker {

    private final Iterator<Task> tasks;

    private final Set<CompletableFuture<Void>> running = new CopyOnWriteArraySet<>();
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
        CompletableFuture<Void> future = new CompletableFuture<>();
        running.add(future);

        future.completeAsync(() -> {
                    task.executor().run();
                    return null;
                })
                .handle((v, e) -> {
                    if (e != null)
                        e.printStackTrace(UltrastarOrganizer.out);
                    running.remove(future);
                    next();
                    return null;
                });
    }

    public record Task(@NotNull String name, @NotNull Runnable executor) {
    }

}
