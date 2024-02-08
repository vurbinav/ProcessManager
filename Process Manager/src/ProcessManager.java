import com.sun.management.OperatingSystemMXBean;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.Optional;

public class ProcessManager {
    public ProcessManager() {
    }

    public static void main(String[] args) {
        OperatingSystemMXBean osBean = (OperatingSystemMXBean)ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        ProcessHandle.allProcesses().filter(ProcessHandle::isAlive).forEach((process) -> {
            System.out.println("Process ID: " + process.pid());
            ProcessHandle.Info info = process.info();
            PrintStream var10000 = System.out;
            Optional var10001 = info.command();
            var10000.println("Path: " + (String)var10001.orElse("Unknown"));

            System.out.println("CPU Duration: " + String.valueOf(info.totalCpuDuration().orElseThrow()));

            double value = osBean.getProcessCpuLoad();
            if (value == -1.0)
                value = Double.NaN;
            value = ((int) (value * 1000) / 10.0);
            System.out.println("CPU Usage: " + value + "%");

            var10000 = System.out;
            var10001 = info.user();
            var10000.println("User: " + (String)var10001.orElse("Unknown"));

            try {
                long memoryUsage = getMemoryUsage(process.pid());
                System.out.println("Memory Usage: " + memoryUsage/1000000 + " Megabytes");
                long ioOperations = getTotalIoOperations(process.pid());
                System.out.println("I/O Utilization: " + ioOperations + " Operation Count");
                String classification = classifyProcess(process.info(), ioOperations);
                System.out.println("Process Classification: " + classification);
            } catch (InterruptedException | IOException var7) {
                var7.printStackTrace();
            }

        });
    }

    private static long getMemoryUsage(long pid) throws IOException, InterruptedException {
        String command = "tasklist /FI \"PID eq " + pid + "\" /FO CSV /NH";
        Process process = Runtime.getRuntime().exec(command);

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            label90: {
                long var10;
                try {
                    String line = reader.readLine();
                    if (line == null) {
                        break label90;
                    }

                    String[] parts = line.split("\",\"");
                    if (parts.length <= 4) {
                        break label90;
                    }

                    String memoryStr = parts[4].replace("\"", "").trim();
                    memoryStr = memoryStr.replaceAll("\\,", "").replace("KB", "").replace("K", "").trim();
                    long memoryKb = Long.parseLong(memoryStr);
                    var10 = memoryKb * 1024L;
                } catch (Throwable var17) {
                    try {
                        reader.close();
                    } catch (Throwable var16) {
                        var17.addSuppressed(var16);
                    }

                    throw var17;
                }

                reader.close();
                return var10;
            }

            reader.close();
        } finally {
            process.waitFor();
        }

        return 0L;
    }

    private static long getTotalIoOperations(long pid) throws IOException, InterruptedException {
        String command = "wmic process where ProcessId=" + pid + " get ReadOperationCount,WriteOperationCount";
        Process process = Runtime.getRuntime().exec(command);
        long readCount = 0L;
        long writeCount = 0L;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            try {
                boolean headerParsed = false;
                String line;
                while((line = reader.readLine()) != null) {
                    if (!headerParsed) {
                        headerParsed = true;
                    } else if (!line.trim().isEmpty()) {
                        String[] values = line.trim().split("\\s+");
                        if (values.length == 2) {
                            readCount = Long.parseLong(values[0]);
                            writeCount = Long.parseLong(values[1]);
                        }
                    }
                }
            } catch (Throwable var17) {
                try {
                    reader.close();
                } catch (Throwable var16) {
                    var17.addSuppressed(var16);
                }

                throw var17;
            }

            reader.close();
        } finally {
            process.waitFor();
        }

        return readCount + writeCount;
    }

    private static String classifyProcess(ProcessHandle.Info info, long totalIoOperations) {
        Optional<Duration> cpuDurationOpt = info.totalCpuDuration();
        if (cpuDurationOpt.isPresent()) {
            Duration cpuDuration = (Duration)cpuDurationOpt.get();
            long cpuDurationMillis = cpuDuration.toMillis();

            if (cpuDurationMillis == 0 && totalIoOperations == 0) {
                return "Idle or Inactive";
            }

            double ioCpuRatio = cpuDurationMillis > 0L ? (double)totalIoOperations / (double)cpuDurationMillis : Double.MAX_VALUE;
            double ioBoundThreshold = 10.0;
            double cpuBoundThreshold = 0.1;
            if (ioCpuRatio > ioBoundThreshold) {
                return "I/O bound";
            } else {
                return ioCpuRatio < cpuBoundThreshold ? "CPU bound" : "Balanced";
            }
        } else {
            return "Unknown";
        }
    }
}