"""Run actual compiled standby classes with minimal host Android stubs.
Not a Thor/Android runtime test: verifies parking, retention and executor lifetime.
"""
import argparse
import os
from pathlib import Path
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("--android-jar", required=True)
parser.add_argument("--java-home", default=os.environ.get("JAVA_HOME"))
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
work = root / "build" / "standby-host-tests"
classes = root / "assistant/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes"
sources = {
    "android/os/Binder.java": """package android.os;
public class Binder {
    public void attachInterface(IInterface owner, String descriptor) {}
    protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
            throws RemoteException { return false; }
}""",
    "android/os/Process.java": """package android.os;
public final class Process {
    public static long getElapsedCpuTime() { return 0L; }
}""",
    "StandbyHostTest.java": """import java.lang.reflect.*;
import java.util.concurrent.ScheduledExecutorService;
public final class StandbyHostTest {
    static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
    static Field field(Class<?> cls, String name) throws Exception {
        Field field = cls.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }
    static Method method(Class<?> cls, String name, Class<?>... types) throws Exception {
        Method method = cls.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method;
    }
    static void awaitParked(Thread thread) throws Exception {
        long deadline = System.nanoTime() + 1000000000L;
        while (thread.getState() != Thread.State.WAITING && System.nanoTime() < deadline)
            Thread.sleep(5);
        check(thread.getState() == Thread.State.WAITING,
                "monitor must block without periodic idle polling");
    }
    public static void main(String[] args) throws Exception {
        String pkg = "com.mastercook777.heimdall.";
        Class<?> service = Class.forName(pkg + "ShizukuGameContextUserService");
        Object instance = service.getConstructor().newInstance();
        Object lock = field(service, "lock").get(instance);
        Thread monitor = (Thread) field(service, "monitorThread").get(instance);
        Method pause = method(service, "setMonitorPaused", boolean.class);
        try {
            awaitParked(monitor);
            pause.invoke(instance, true);
            synchronized (lock) {
                field(service, "requestedPackage").set(instance, "org.ppsspp.ppsspp");
                field(service, "monitoredPackage").set(instance, "org.ppsspp.ppsspp");
                field(service, "monitoredPid").set(instance, 123);
                field(service, "activeValue").set(instance, "retained-identity");
                field(service, "observedAt").set(instance, 1234L);
                lock.notifyAll();
            }
            awaitParked(monitor);
            Thread.sleep(60);
            check(monitor.getState() == Thread.State.WAITING,
                    "suspended monitor must remain parked even with a target");
            check(!(boolean) method(service, "resetForTarget", String.class, int.class)
                    .invoke(instance, "org.ppsspp.ppsspp", -1), "paused race must reject reset");
            check("retained-identity".equals(field(service, "activeValue").get(instance)),
                    "pause/reset race lost ROM identity");
            check((long) field(service, "observedAt").get(instance) == 1234L,
                    "pause changed identity timestamp");
            long replayFrom = (long) field(service, "resumeReplayFrom").get(instance);
            pause.invoke(instance, true);
            check((long) field(service, "resumeReplayFrom").get(instance) == replayFrom,
                    "duplicate sleep discarded the replay watermark");
            synchronized (lock) {
                field(service, "requestedPackage").set(instance, "");
                field(service, "cachedActivityCheckedAt").set(instance, 999L);
            }
            pause.invoke(instance, false);
            awaitParked(monitor);
            check((long) field(service, "cachedActivityCheckedAt").get(instance) == 0L,
                    "wake must invalidate cached Activity state");
            check((long) field(service, "resumeReplayFrom").get(instance) == replayFrom,
                    "wake lost replay watermark before attaching a stream");
            check("retained-identity".equals(field(service, "activeValue").get(instance)),
                    "wake lost same-process identity");
        } finally {
            synchronized (lock) {
                field(service, "destroyed").set(instance, true);
                lock.notifyAll();
            }
            monitor.join(1000);
            check(!monitor.isAlive(), "monitor failed to retire");
        }
        Class<?> diagnostics = Class.forName(pkg + "DebugPerformanceDiagnostics");
        field(diagnostics, "debugEnabled").set(null, true);
        Method resume = method(diagnostics, "resumeReporting");
        Method stop = method(diagnostics, "pauseReporting");
        try {
            resume.invoke(null);
            ScheduledExecutorService first = (ScheduledExecutorService)
                    field(diagnostics, "reporter").get(null);
            resume.invoke(null);
            check(first == field(diagnostics, "reporter").get(null),
                    "duplicate wake created another reporter");
            stop.invoke(null);
            check(first.isShutdown() && field(diagnostics, "reporter").get(null) == null,
                    "reporter kept scheduling during standby");
            stop.invoke(null);
            resume.invoke(null);
            check(first != field(diagnostics, "reporter").get(null),
                    "reporter did not restart after wake");
        } finally { stop.invoke(null); }
        System.out.println("PASS: monitor parking, sleep/reset race, identity/replay retention, "
                + "wake cache invalidation, retirement, reporter stop/restart/idempotence");
    }
}""",
}
paths = []
for name, source in sources.items():
    path = work / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(source)
    paths.append(str(path))
bin_dir = Path(args.java_home) / "bin" if args.java_home else Path("")
subprocess.run([str(bin_dir / "javac"), "-cp", args.android_jar,
                "-d", str(work), *paths], check=True)
classpath = os.pathsep.join([str(work), str(classes), args.android_jar])
subprocess.run([str(bin_dir / "java"), "-cp", classpath, "StandbyHostTest"], check=True)
