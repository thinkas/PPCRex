import com.google.gson.Gson;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class test {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();  // 记录开始时间
        String inputExcelFile = "Re-test.xlsx";
        String outputCsvFile = "regex_test_results.csv";
        List<String[]> results = new ArrayList<>();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("程序正在退出，保存结果...");
            writeResultsToCsv(outputCsvFile, results);
        }));

        try (FileInputStream fis = new FileInputStream(inputExcelFile);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                throw new IOException("Excel 文件为空");
            }

            // ===== 读取表头 =====
            Row headerRow = rowIterator.next();
            int colCount = headerRow.getLastCellNum();
            String[] header = new String[colCount];

            for (int i = 0; i < colCount; i++) {
                Cell cell = headerRow.getCell(i);
                header[i] = (cell == null) ? "" : cell.toString().trim();
            }

            int attemptRegexIndex = -1;
            for (int i = 0; i < header.length; i++) {
                if ("attemptregex".equalsIgnoreCase(header[i])) {
                    attemptRegexIndex = i;
                    break;
                }
            }

            if (attemptRegexIndex == -1) {
                throw new RuntimeException("未找到 attemptregex 列");
            }

            // ===== 读取数据行 =====
            while (rowIterator.hasNext()) {
                Row excelRow = rowIterator.next();
                String[] row = new String[colCount];

                for (int i = 0; i < colCount; i++) {
                    Cell cell = excelRow.getCell(i);
                    row[i] = (cell == null) ? "" : cell.toString().trim();
                }

                long regexStartTime = System.currentTimeMillis();
                String regexLengthStr = "";
                String regexTimeStr = "";

                if (row.length <= attemptRegexIndex) continue;

                String originalRegex = row[0].trim();
                String regex = row[attemptRegexIndex].trim();

                System.out.println("正在处理正则表达式: " + regex);

                if ("*".equals(regex)
                        || "\\*".equals(regex)
                        || regex.replaceAll("\\\\", "").equals("*")) {
                    System.out.println("检测到不合法的星号正则，已跳过: " + regex);
                    continue;
                }

                String[] resultRow;

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
                        resultRow = new String[]{
                                originalRegex, regex,
                                "", "", "", "", "", "", "", "", "", "", "",
                                "", "", ""
                        };
                    } else {
                        try {
                            outputFuture.get(2, TimeUnit.SECONDS);
                        } catch (TimeoutException ignored) {
                        }
//打印输出全部
//                        String lastLine = null;
//                        while (!outputLines.isEmpty()) {
//                            String line = outputLines.poll();
//                            System.out.println("【RegexWorker】" + line);
//                            lastLine = line;
//                        }
                        String lastLine = null;
                        while (!outputLines.isEmpty()) {
                            lastLine = outputLines.poll();   // 必须要
                        }


                        if (lastLine == null || lastLine.trim().isEmpty()
                                || lastLine.startsWith("-1")) {

                            resultRow = new String[]{
                                    originalRegex, regex,
                                    "跳过(非法字符或错误)", "", "0",
                                    "0%", "0%", "0%", "0%",
                                    "", "0", "", "", "", "", ""
                            };
                        } else {
                            long regexEndTime = System.currentTimeMillis();
                            regexLengthStr = String.valueOf(regex.length());
                            regexTimeStr = String.valueOf(regexEndTime - regexStartTime);

                            String[] parts = lastLine.split("\\|\\|", 3);
                            String[] pathInfo = parts[0].split(",");

                            String testStrings = parts.length > 1 ? parts[1] : "";
                            String mainPathTestStrings = parts.length > 2 ? parts[2] : "";

                            double pathCount = Double.parseDouble(pathInfo[0]);
                            double coveragePercent = Double.parseDouble(pathInfo[1]);
                            double nodeCoveragePercent = Double.parseDouble(pathInfo[2]);
                            double edgeCoveragePercent = Double.parseDouble(pathInfo[3]);
                            double edgePairCoveragePercent = Double.parseDouble(pathInfo[4]);
                            double generatedTestStringsCount = Double.parseDouble(pathInfo[5]);
                            double minPathGeneratedCount = Double.parseDouble(pathInfo[6]);
                            double mainPathCount = Double.parseDouble(pathInfo[7]);
                            double exampleCount1 = Double.parseDouble(pathInfo[8]);

                            String pathScaleComparison =
                                    pathCount > mainPathCount ? "增大"
                                            : pathCount < mainPathCount ? "减少" : "相等";

                            String scaleComparison =
                                    minPathGeneratedCount > generatedTestStringsCount ? "增大"
                                            : minPathGeneratedCount < generatedTestStringsCount ? "减少" : "相等";

                            resultRow = new String[]{
                                    originalRegex,
                                    regex,
                                    String.valueOf((int) pathCount),
                                    testStrings,
                                    String.valueOf((int) exampleCount1),
                                    String.format("%.2f%%", coveragePercent),
                                    String.format("%.2f%%", nodeCoveragePercent),
                                    String.format("%.2f%%", edgeCoveragePercent),
                                    String.format("%.2f%%", edgePairCoveragePercent),
                                    String.valueOf((int) generatedTestStringsCount),
                                    scaleComparison,
                                    String.valueOf((int) mainPathCount),
                                    pathScaleComparison,
                                    mainPathTestStrings,
                                    regexLengthStr,
                                    regexTimeStr
                            };

                            MinimumTestPath.finalReplacedEdges.clear();
                        }
                    }

                    executor.shutdownNow();
                    results.add(resultRow);

                } catch (Exception e) {
                    e.printStackTrace();
                    results.add(new String[]{
                            originalRegex, regex,
                            "", "", "", "", "", "", "", "", "", "", "",
                            "", "", ""
                    });
                }
            }

            writeResultsToCsv(outputCsvFile, results);

            long endTime = System.currentTimeMillis();
            System.out.println("总运行时间: " +
                    (endTime - startTime) / 1000.0 + " 秒");

        } catch (IOException e) {
            e.printStackTrace();
            writeResultsToCsv(outputCsvFile, results);
        }
    }


    private static String escapeTestStringsField(String field) {
        if (field == null || field.isEmpty()) {
            return "\"\"";
        }

        Pattern pattern = Pattern.compile("'([^']*)'");
        Matcher matcher = pattern.matcher(field);

        StringBuffer sb = new StringBuffer();
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String unmatched = field.substring(lastEnd, matcher.start());
                sb.append(escapeUnquoted(unmatched));
            }

            String inner = matcher.group(1);
            String replacedInner = inner.replace("\\", "\\\\");
            sb.append("'" + replacedInner + "'");

            lastEnd = matcher.end();
        }

        if (lastEnd < field.length()) {
            sb.append(escapeUnquoted(field.substring(lastEnd)));
        }

        String replaced = sb.toString().replace("\"", "\"\"");
        return "\"" + replaced + "\"";
    }

    private static String escapeUnquoted(String text) {
        return text.replace("\\", "\\\\");
    }

    private static String saveToJsonFile(List<String> testStrings, String prefix) throws IOException {
        // 使用 Gson 将数据转换为 JSON 格式
        Gson gson = new Gson();
        String fileName = prefix + "_" + System.currentTimeMillis() + ".json";
        try (FileWriter writer = new FileWriter(fileName)) {
            gson.toJson(testStrings, writer);
        }
        return fileName;  // 返回文件路径
    }

    private static void writeResultsToCsv(String filePath, List<String[]> results) {
        try (OutputStream os = new FileOutputStream(filePath)) {

            // ===== 关键：写 UTF-8 BOM，Excel 才能正确识别 =====
            os.write(0xEF);
            os.write(0xBB);
            os.write(0xBF);

            Writer writer = new OutputStreamWriter(os, StandardCharsets.UTF_8);

            writer.append("\"regex\",\"正则表达式\",\"约简测试路径数量\",\"约简测试串\",\"测试串数量\",\"主路径覆盖率\",\"点覆盖率\",\"边覆盖率\",\"对边覆盖率\",\"主路径测试串(最短路径拼接非去重）\",\"主路径集测试串数量\",\"测试串数量对比\",\"主路径数量\",\"路径数量对比\",\"正则长度\",\"运行时间(ms)\"\n");

            for (String[] result : results) {
                String originalRegex = result[0];
                String[] adjusted = Arrays.copyOfRange(result, 1, result.length);

                String regexForCsv = adjusted[0].replace("\\", "\\\\").replace("\"", "\"\"");
                String regex = "\"" + regexForCsv + "\"";
                String pathCount = "\"" + adjusted[1].replace("\"", "\"\"") + "\"";
                String exampleCount1 = "\"" + adjusted[3].replace("\"", "\"\"") + "\"";
                String coverage = "\"" + adjusted[4].replace("\"", "\"\"") + "\"";
                String nodeCoverage = "\"" + adjusted[5].replace("\"", "\"\"") + "\"";
                String edgeCoverage = "\"" + adjusted[6].replace("\"", "\"\"") + "\"";
                String edgePairCoverage = "\"" + adjusted[7].replace("\"", "\"\"") + "\"";
                String testStringCount = "\"" + adjusted[8].replace("\"", "\"\"") + "\"";
                String scaleComparison = "\"" + adjusted[9].replace("\"", "\"\"") + "\"";
                String mainPathCount = "\"" + adjusted[10].replace("\"", "\"\"") + "\"";
                String pathScaleComparison = "\"" + adjusted[11].replace("\"", "\"\"") + "\"";
                String regexLength = adjusted[13];
                String regexTime = adjusted[14];

                boolean tooManyTestStrings =
                        !adjusted[3].trim().isEmpty() && Integer.parseInt(adjusted[3]) > 5000;

                String testStringsJsonFile = tooManyTestStrings
                        ? saveToJsonFile(Arrays.asList(adjusted[2].split(",")), "testStrings")
                        : escapeTestStringsField(adjusted[2]);

                boolean tooManyMainPathStrings =
                        !adjusted[8].trim().isEmpty() && Integer.parseInt(adjusted[8]) > 1500;

                String mainPathTestStringsJsonFile = tooManyMainPathStrings
                        ? saveToJsonFile(Arrays.asList(adjusted[12].split(",")), "mainPathTestStrings")
                        : escapeTestStringsField(adjusted[12]);

                writer.append("\"")
                        .append(originalRegex.replace("\\", "\\\\").replace("\"", "\"\""))
                        .append("\",")
                        .append(regex).append(",")
                        .append(pathCount).append(",")
                        .append(testStringsJsonFile).append(",")
                        .append(exampleCount1).append(",")
                        .append(coverage).append(",")
                        .append(nodeCoverage).append(",")
                        .append(edgeCoverage).append(",")
                        .append(edgePairCoverage).append(",")
                        .append(mainPathTestStringsJsonFile).append(",")
                        .append(testStringCount).append(",")
                        .append(scaleComparison).append(",")
                        .append(mainPathCount).append(",")
                        .append(pathScaleComparison).append(",")
                        .append("\"").append(regexLength).append("\",")
                        .append("\"").append(regexTime).append("\"\n");
            }

            writer.flush();
            System.out.println("结果已保存到 CSV 文件: " + filePath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
