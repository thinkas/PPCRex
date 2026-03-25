import com.google.gson.Gson;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test {
    public static void main(String[] args) {
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("请输入正则表达式: ");
            String regex = scanner.nextLine().trim();

            String originalRegex = regex;

            long regexStartTime = System.currentTimeMillis();
            String regexLengthStr = "";
            String regexTimeStr = "";

            System.out.println("正在处理正则表达式: " + regex);

            if ("*".equals(regex)
                    || "\\*".equals(regex)
                    || regex.replaceAll("\\\\", "").equals("*")) {
                System.out.println("检测到不合法的星号正则，已跳过: " + regex);
                return;
            }

            try {
                String classpath = "target/classes" + File.pathSeparator + "lib/dk.jar";

                ProcessBuilder pb = new ProcessBuilder(
                        "java",
                        "-Dfile.encoding=UTF-8",
                        "-cp", classpath,
                        "RegexWorker"
                );

                pb.redirectErrorStream(true);
                Process proc = pb.start();

                try (Writer writer = new OutputStreamWriter(
                        proc.getOutputStream(), StandardCharsets.UTF_8)) {
                    writer.write(regex);
                    writer.write("\n");
                    writer.flush();
                }

                ExecutorService executor = Executors.newSingleThreadExecutor();
                BlockingQueue<String> outputLines = new LinkedBlockingQueue<>();

                Future<?> outputFuture = executor.submit(() -> {
                    try (BufferedReader reader1 =
                                 new BufferedReader(new InputStreamReader(
                                         proc.getInputStream(), StandardCharsets.UTF_8))) {
                        String lineOut;
                        while ((lineOut = reader1.readLine()) != null) {
                            outputLines.offer(lineOut);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                boolean finished = proc.waitFor(60, TimeUnit.SECONDS);
                if (!finished) {
                    System.out.println("超时未完成，已跳过: " + regex);
                    proc.destroyForcibly();
                } else {
                    try {
                        outputFuture.get(2, TimeUnit.SECONDS);
                    } catch (TimeoutException ignored) {
                    }

                    String lastLine = null;
                    while (!outputLines.isEmpty()) {
                        lastLine = outputLines.poll();
                    }

                    if (lastLine == null || lastLine.trim().isEmpty()
                            || lastLine.startsWith("-1")) {

                        System.out.println("未生成测试串（请检查正则是否非法）");

                    } else {
                        long regexEndTime = System.currentTimeMillis();
                        regexLengthStr = String.valueOf(regex.length());
                        regexTimeStr = String.valueOf(regexEndTime - regexStartTime);

                        String[] parts = lastLine.split("\\|\\|", 3);

                        String testStrings = parts.length > 1 ? parts[1] : "";

                        System.out.println("====== 测试串 ======");

                        if (!testStrings.isEmpty()) {
                            Pattern pattern = Pattern.compile("'([^']*)'");
                            Matcher matcher = pattern.matcher(testStrings);

                            while (matcher.find()) {
                                String s = matcher.group(1); // 拿到引号里的完整内容
                                System.out.println(s);
                            }
                        } else {
                            System.out.println("无测试串");
                        }

                        MinimumTestPath.finalReplacedEdges.clear();
                    }
                }

                executor.shutdownNow();

            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}