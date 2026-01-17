package com.declutter.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class ConsoleUtil {
  private static final String[] FRAMES = new String[] {"|", "/", "-", "\\"};

  private ConsoleUtil() {}

  public static void ok(String message) {
    System.out.println("[ok] " + message);
  }

  public static void warn(String message) {
    System.out.println("[warn] " + message);
  }

  public static <T> T runLoading(Supplier<T> task, String message) {
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    final int[] index = {0};
    scheduler.scheduleAtFixedRate(() -> {
      System.out.print("\r" + FRAMES[index[0] % FRAMES.length] + " " + message);
      index[0]++;
    }, 0, 75, TimeUnit.MILLISECONDS);

    try {
      return task.get();
    } finally {
      scheduler.shutdownNow();
      System.out.print("\r");
    }
  }
}
