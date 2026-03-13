import java.util.*;

import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;

public class RegexProcessor {
    public static List<List<Integer>> currentMainPathList = new ArrayList<>();
    public static String testStrings1;
    public static String mainPathTestStrings;
    // 静态变量来存储去重后的测试串数量
    public static int generatedTestStringsCount = 0;

    public static String smartClean(String path) {
        // 删除每个逗号后面跟的一个空格
        String partiallyCleaned = path.replaceAll(", ", ",");

        // 如果逗号数只有一个，就不处理了，直接返回格式化后的内容
        if (partiallyCleaned.chars().filter(ch -> ch == ',').count() == 1) {
            return partiallyCleaned;
        }

        StringBuilder result = new StringBuilder();
        int len = partiallyCleaned.length();
        boolean betweenElements = false;
        int commaCount = 0;

        for (int i = 0; i < len; i++) {
            char c = partiallyCleaned.charAt(i);

            if (c == ',') {
                if (betweenElements) {
                    commaCount++;
                    // 只保留偶数位的逗号（从1开始数）
                    if (commaCount % 2 == 0) {
                        result.append(',');
                    }
                }
            } else {
                // 每次遇到非逗号字符时，重置 commaCount，并标记为在“元素”之间
                commaCount = 0;
                betweenElements = true;
                result.append(c);
            }
        }

        return result.toString();
    }



    public static double[] processRegex(String regex) {
        try {
            System.out.println("进入了 processRegex，参数是: " + regex);
            PrimePath generator = new PrimePath(regex,true);
            generator.PrimePathGen();
            generator.buildPPRG();
            generator.removeCycles();

            List<List<Integer>> minimumTestPaths = MinimumTestPath.finalReplacedEdges;
            generator.printAutomatonGraph();
            System.out.println("最小测试路径：");
            for (List<Integer> path : minimumTestPaths) {
                System.out.println(path.toString().replaceAll("[\\[\\]]", ""));
            }

            if (!PPRG.updatedMainPaths.isEmpty()) {
                System.out.println("主路径集发生过变化，变化后的主路径集是：");
                for (String updatedPath : PPRG.updatedMainPaths) {
                    System.out.println(updatedPath);
                }
            }
            List<String> mainPaths;
            if (!PPRG.updatedMainPaths.isEmpty()) {
                System.out.println("使用更新后的主路径集进行计算。");
                mainPaths = new ArrayList<>(PPRG.updatedMainPaths);
            } else {
                mainPaths = generator.getPrimePath();
            }
            PPRG.updatedMainPaths.clear();
            System.out.println("当前使用的主路径集：");
            for (String path : mainPaths) {
                String cleanedPath = path.replaceAll("p\\d+: ", "");
                System.out.println(cleanedPath.replaceAll("[\\[\\]]", ""));
            }
            // 使用主路径集生成测试串
            List<List<Integer>> mainPathList = new ArrayList<>();
            for (String path : mainPaths) {
                String cleaned = path.replaceAll("p\\d+: ", "").replaceAll("[\\[\\]]", "").trim();
                if (!cleaned.isEmpty()) {
                    List<Integer> intPath = new ArrayList<>();
                    for (String numStr : cleaned.split(",\\s*")) {
                        intPath.add(Integer.parseInt(numStr.trim()));
                    }
                    mainPathList.add(intPath);
                }
            }
            LargeAlphabet.main(new String[]{}, mainPathList);
            // === 检查是否需要添加 “*” 对应的 0 次出现情况 ===
            if (regex.contains("*")) {
                System.out.println("\n【检测到正则包含 '*'，开始检查“0次出现”测试情况】");

                boolean hasZeroOccurrence = false;
                for (String s : LargeAlphabet.cleanedTestStrings) {
                    if (s.isEmpty()) { // 空串表示“出现0次”
                        hasZeroOccurrence = true;
                        break;
                    }
                }
                if (!hasZeroOccurrence) {
                    System.out.println("‘0次出现’的测试串尝试自动添加...");

                    // 获取自动机
                    Automaton automaton = PrimePath.automaton;
                    State initial = automaton.getInitialState();

                    // 1️⃣ 如果初始状态就是接受态，空串合法
                    if (initial.isAccept()) {
                        System.out.println("初始状态是接受态，添加空串 '' 作为0次出现测试串。");
                        LargeAlphabet.printCleanedTestString("", new ArrayList<>(), LargeAlphabet.cleanedTestStrings.size() + 1);
                    } else {
                        // 2️⃣ 否则，尝试找到从初始状态到任意接受状态的最短路径
                        Map<Integer, List<Integer>> adjacency = LargeAlphabet.buildAdjacencyMap(automaton);
                        Set<Integer> acceptStates = new HashSet<>();
                        for (State s : automaton.getStates()) {
                            if (s.isAccept()) acceptStates.add(s.getNumber());
                        }

                        List<Integer> zeroPath = null;
                        for (Integer acc : acceptStates) {
                            zeroPath = LargeAlphabet.findShortestPath(initial.getNumber(), acc, adjacency);
                            if (zeroPath != null && !zeroPath.isEmpty()) break;
                        }

                        if (zeroPath != null && !zeroPath.isEmpty()) {
                            // 在这里检查 zeroPath 是否已经存在于 MinimumTestPath.finalReplacedEdges 中
                            boolean pathExists = false;
                            for (List<Integer> existingPath : MinimumTestPath.finalReplacedEdges) {
                                if (existingPath.equals(zeroPath)) {
                                    pathExists = true;
                                    System.out.println("‘0次出现’路径已经存在，已存在的路径为: " + existingPath);
                                    break;
                                }
                            }

                            // 如果路径不存在，继续添加路径
                            if (!pathExists) {
                                List<String> zeroStrings = LargeAlphabet.buildAllPaths(zeroPath, automaton);
                                if (!zeroStrings.isEmpty()) {
                                    String zeroStr = zeroStrings.get(0);
                                    System.out.println("添加‘0次出现’对应的测试串: " + zeroStr);
                                    LargeAlphabet.printCleanedTestString(zeroStr, zeroPath, LargeAlphabet.cleanedTestStrings.size() + 1);
                                } else {
                                    System.out.println("未能基于自动机生成对应的0次出现串，跳过。");
                                }
                            } else {
                                System.out.println("‘0次出现’路径已存在，跳过添加。");
                            }
                        } else {
                            System.out.println("未能找到从初始状态到接受状态的最短路径，无法生成0次出现串。");
                        }
                    }
                } else {
                    System.out.println("检测到已有空串，无需补充。");
                }
            }

            // 调用 LargeAlphabet 逻辑展开成测试串
            List<String> generatedTestStrings = new ArrayList<>();
            for (List<Integer> path : mainPathList) {
                List<String> allPaths = LargeAlphabet.buildAllPaths(path, PrimePath.automaton,false);
                generatedTestStrings.addAll(allPaths);
            }
            // 打印测试串
            System.out.println("---------------");
            System.out.println("根据主路径集生成的字符串：");

//            // 先去重
//            List<String> uniqueList = new ArrayList<>(new LinkedHashSet<>(generatedTestStrings));
            List<String> uniqueList = new ArrayList<>(generatedTestStrings);
            // 更新静态变量
            generatedTestStringsCount = uniqueList.size();
// 保留原来的循环形式
            for (int i = 0; i < uniqueList.size(); i++) {
                System.out.println("测试串 " + (i + 1) + ": " + uniqueList.get(i));
            }
            // 清空旧数据
            LargeAlphabet.cleanedMainPathTestStrings.clear();
            // === 主路径：检查是否需要添加 “*” 对应的 0 次出现情况 ===
            if (regex.contains("*")) {
                System.out.println("\n【检测到正则包含 '*'，开始检查主路径的“0次出现”测试情况】");

                boolean hasZeroOccurrence = false;
                for (String s : LargeAlphabet.cleanedMainPathTestStrings) {
                    if (s.isEmpty()) { // 空串表示“出现0次”
                        hasZeroOccurrence = true;
                        break;
                    }
                }

                if (!hasZeroOccurrence) {
                    System.out.println("主路径 ‘0次出现’ 的测试串尝试自动添加...");

                    // 获取自动机
                    Automaton automaton = PrimePath.automaton;
                    State initial = automaton.getInitialState();

                    // 1️⃣ 初始状态是接受态 → 空串合法
                    if (initial.isAccept()) {
                        System.out.println("主路径：初始状态是接受态，添加空串 '' 作为0次出现测试串。");
                        LargeAlphabet.cleanMainPathTestString("", new ArrayList<>());
                    } else {

                        // 2️⃣ 找到从初始状态到任意接受态的最短路径
                        Map<Integer, List<Integer>> adjacency = LargeAlphabet.buildAdjacencyMap(automaton);
                        Set<Integer> acceptStates = new HashSet<>();
                        for (State s : automaton.getStates()) {
                            if (s.isAccept()) acceptStates.add(s.getNumber());
                        }

                        List<Integer> zeroPath = null;
                        for (Integer acc : acceptStates) {
                            zeroPath = LargeAlphabet.findShortestPath(initial.getNumber(), acc, adjacency);
                            if (zeroPath != null && !zeroPath.isEmpty()) break;
                        }

                        if (zeroPath != null && !zeroPath.isEmpty()) {

                            // 检查该路径是否已经在主路径集中存在
                            boolean pathExists = false;
                            for (String existingMainPath : LargeAlphabet.cleanedMainPathTestStrings) {
                                if (existingMainPath.equals(zeroPath)) {
                                    pathExists = true;
                                    System.out.println("主路径的 ‘0次出现’ 路径已经存在，为: " + existingMainPath);
                                    break;
                                }
                            }

                            // 若不存在 → 正常添加
                            if (!pathExists) {
                                List<String> zeroStrings = LargeAlphabet.buildAllPaths(zeroPath, automaton);
                                if (!zeroStrings.isEmpty()) {
                                    String zeroStr = zeroStrings.get(0);
                                    System.out.println("主路径：添加 ‘0次出现’ 对应的测试串: " + zeroStr);

                                    // 使用主路径清洗方法
                                    LargeAlphabet.cleanMainPathTestString(zeroStr, zeroPath);
                                } else {
                                    System.out.println("主路径：无法生成0次出现串，跳过。");
                                }
                            } else {
                                System.out.println("主路径：‘0次出现’ 路径已存在，跳过添加。");
                            }

                        } else {
                            System.out.println("主路径：未能找到初始到接受态的路径，无法生成 0 次出现串。");
                        }
                    }

                } else {
                    System.out.println("主路径：检测到已有空串，无需补充。");
                }
            }
            System.out.println("---------------");
            System.out.println("根据主路径集生成的字符串：");

            for (int i = 0; i < uniqueList.size(); i++) {
                String raw = uniqueList.get(i);
                List<Integer> srcMainPath = mainPathList.get(i % mainPathList.size()); // 或者实际对应关系
                LargeAlphabet.cleanMainPathTestString(raw, srcMainPath);
            }

            generatedTestStringsCount = LargeAlphabet.cleanedMainPathTestStrings.size();

            System.out.println("主路径测试串数量为 " + generatedTestStringsCount);



            Set<String> countedMainPaths = new HashSet<>();
            Set<String> countedTestPaths = new HashSet<>();

            for (String mainPath : mainPaths) {
                String cleanedMainPath = mainPath.replaceAll("p\\d+: ", "").replaceAll("[\\[\\]]", "").trim();
                countedMainPaths.add(cleanedMainPath);

                for (List<Integer> testPath : minimumTestPaths) {
                    String cleanedTestPath = testPath.toString().replaceAll("[\\[\\]]", "").trim();
                    if (cleanedTestPath.contains(cleanedMainPath)) {
                        countedTestPaths.add(cleanedTestPath);
                    }
                }
            }

// 打印全部参与统计的主路径
            System.out.println("【全部参与统计的主路径】");
            int idx = 1;
            for (String mp : countedMainPaths) {
                System.out.println("主路径 " + idx + ": " + mp);
                idx++;
            }

// 打印全部参与统计的最小测试路径
// 合并 finalReplacedEdges + addedPaths 作为最终统计使用
            List<List<Integer>> allTestPaths = new ArrayList<>();
            allTestPaths.addAll(MinimumTestPath.finalReplacedEdges);
            allTestPaths.addAll(LargeAlphabet.addedPaths);

// 打印全部参与统计的最小测试路径
            System.out.println("【全部参与统计的最小测试路径】");
            for (int i = 0; i < allTestPaths.size(); i++) {
                List<Integer> path = allTestPaths.get(i);
                System.out.println("最小测试路径 " + (i + 1) + ": " + path.toString().replaceAll("[\\[\\]]", ""));
            }

// 主路径覆盖统计
            int coveredPaths = 0;
            for (String mainPath : mainPaths) {
                String cleanedMainPath = mainPath.replaceAll("p\\d+: ", "").replaceAll("[\\[\\]]", "");
                for (List<Integer> testPath : allTestPaths) {  // 使用合并后的路径
                    String cleanedTestPath = testPath.toString().replaceAll("[\\[\\]]", "");
                    if (cleanedTestPath.contains(cleanedMainPath)) {
                        coveredPaths++;
                        break;
                    }
                }
            }

// 点和边统计
            int totalNodes = generator.getNumberOfStates();
            Set<String> allEdges = new HashSet<>();
            for (int from = 0; from < totalNodes; from++) {
                for (int to : generator.getAdj()[from]) {
                    allEdges.add(from + "->" + to);
                }
            }
            int totalEdges = allEdges.size();

// 使用合并路径统计点和边覆盖
            Set<Integer> coveredNodes = new HashSet<>();
            Set<String> coveredEdges = new HashSet<>();
            for (List<Integer> path : allTestPaths) {  // 使用合并后的路径
                for (int i = 0; i < path.size(); i++) {
                    coveredNodes.add(path.get(i));
                    if (i < path.size() - 1) {
                        coveredEdges.add(path.get(i) + "->" + path.get(i + 1));
                    }
                }
            }

            double nodeCoverage = totalNodes == 0 ? 0 : (double) coveredNodes.size() / totalNodes;
            double edgeCoverage = totalEdges == 0 ? 0 : (double) coveredEdges.size() / totalEdges;

// 对边覆盖统计（edge-pair coverage）
            Set<String> allEdgePairs = new HashSet<>();
            for (int from = 0; from < totalNodes; from++) {
                for (int mid : generator.getAdj()[from]) {
                    for (int to : generator.getAdj()[mid]) {
                        allEdgePairs.add(from + "->" + mid + "->" + to);
                    }
                }
            }
            int totalEdgePairs = allEdgePairs.size();

            Set<String> coveredEdgePairs = new HashSet<>();
            for (List<Integer> path : allTestPaths) {  // 使用合并后的路径
                for (int i = 0; i < path.size() - 2; i++) {
                    int from = path.get(i);
                    int mid = path.get(i + 1);
                    int to = path.get(i + 2);
                    coveredEdgePairs.add(from + "->" + mid + "->" + to);
                }
            }

            double edgePairCoverage = totalEdgePairs == 0 ? 1.0 : (double) coveredEdgePairs.size() / totalEdgePairs;

// 打印覆盖率
            System.out.printf("点覆盖率：%.2f%%\n", nodeCoverage * 100);
            System.out.printf("边覆盖率：%.2f%%\n", edgeCoverage * 100);
            System.out.printf("对边覆盖率：%.2f%%\n", edgePairCoverage * 100);

// 打印所有对边和被覆盖的对边
            System.out.println("所有可能的对边：");
            for (String edgePair : allEdgePairs) {
                System.out.println(edgePair);
            }
            System.out.println("被覆盖的对边：");
            for (String edgePair : coveredEdgePairs) {
                System.out.println(edgePair);
            }

// 主路径覆盖率
            double coverage = (mainPaths.size() == 0) ? 0 : ((double) coveredPaths / mainPaths.size());
            System.out.println("主路径覆盖率：" + (coverage * 100) + "%");


            List<String> cleanedUpdatedPaths = LargeAlphabet.cleanedTestStrings;

// 去重并保持原顺序
//            LinkedHashSet<String> uniquePaths = new LinkedHashSet<>(cleanedUpdatedPaths);
            List<String> uniquePaths = new ArrayList<>(cleanedUpdatedPaths);
// 打印去重后的测试串
            System.out.println("---------------");
            System.out.println("根据主路径生成的字符串：");

// ★★ 在这里检查初始状态是否是接受态，如果是则先添加一个空串 ★★
            if (LargeAlphabet.initialState.isAccept()) {
                // 避免重复添加，可以检查是否已存在
                if (!LargeAlphabet.cleanedMainPathTestStrings.contains("")) {
                    LargeAlphabet.cleanedMainPathTestStrings.add(0, "");
                    // 使用 add(0, "") 让空串排在第一位
                }
            }

            int mainIndex = 1;
            for (String s : LargeAlphabet.cleanedMainPathTestStrings) {
                System.out.println("测试串 " + mainIndex + ": " + s);
                mainIndex++;
            }

            //去重了，消融最小流时千万记得直接删除这行别忘！！！！
            uniquePaths = new ArrayList<>(new LinkedHashSet<>(uniquePaths));
            System.out.println("---------------");
            System.out.println("根据最小测试路径生成的字符串：");
            int index = 1;
            for (String s : uniquePaths) {
                System.out.println("测试串 " + index + ": " + s);
                index++;
            }
// 拼接成 mainPathTestStrings（用于表格）
            List<String> joinedMainPaths = new ArrayList<>();

            for (String s : LargeAlphabet.cleanedMainPathTestStrings) {
                joinedMainPaths.add("'" + s + "'");
            }

            RegexProcessor.mainPathTestStrings = String.join(",", joinedMainPaths);
            System.out.println("主路径拼接结果：" + mainPathTestStrings);

// 拼接成 testStrings1
            List<String> joinedPaths = new ArrayList<>();

            for (String s : uniquePaths) {
                joinedPaths.add("'" + s + "'");
            }
            testStrings1 = String.join(",", joinedPaths);
            System.out.println("最小测试路径拼接结果：" + testStrings1);
// 统计个数
            // 统计个数，去重后的
            int exampleCount = uniquePaths.size();
// 返回结果
            return new double[]{
                    LargeAlphabet.reducedFinalPathCount,
                    coverage * 100,
                    nodeCoverage * 100,
                    edgeCoverage * 100,
                    edgePairCoverage * 100,
                    LargeAlphabet.cleanedMainPathTestStrings.size(),           // 主路径测试串数量
                    exampleCount,                // 最小测试路径生成的测试串数量
                    mainPaths.size(),            // 主路径数量
                    exampleCount                 // 实际输出的测试串数量
            };

        } catch (IllegalArgumentException e) {
            System.err.println("跳过正则: " + regex);
            return new double[]{-1, -1, 0, 0, 0, 0}; // 返回生成的测试串数量为 0
        } catch (Exception e) {
            System.err.println("处理正则表达式时出错，跳过: " + regex);
            e.printStackTrace();
            return new double[]{-1, -1, 0, 0, 0, 0}; // 返回生成的测试串数量为 0
        }
    }
}
//import java.util.*;
//
//        import dk.brics.automaton.Automaton;
//        import dk.brics.automaton.State;
//
//public class RegexProcessor {
//    public static List<List<Integer>> currentMainPathList = new ArrayList<>();
//    public static String testStrings1;
//    public static String mainPathTestStrings;
//    // 静态变量来存储去重后的测试串数量
//    public static int generatedTestStringsCount = 0;
//
//    public static String smartClean(String path) {
//        // 删除每个逗号后面跟的一个空格
//        String partiallyCleaned = path.replaceAll(", ", ",");
//
//        // 如果逗号数只有一个，就不处理了，直接返回格式化后的内容
//        if (partiallyCleaned.chars().filter(ch -> ch == ',').count() == 1) {
//            return partiallyCleaned;
//        }
//
//        StringBuilder result = new StringBuilder();
//        int len = partiallyCleaned.length();
//        boolean betweenElements = false;
//        int commaCount = 0;
//
//        for (int i = 0; i < len; i++) {
//            char c = partiallyCleaned.charAt(i);
//
//            if (c == ',') {
//                if (betweenElements) {
//                    commaCount++;
//                    // 只保留偶数位的逗号（从1开始数）
//                    if (commaCount % 2 == 0) {
//                        result.append(',');
//                    }
//                }
//            } else {
//                // 每次遇到非逗号字符时，重置 commaCount，并标记为在“元素”之间
//                commaCount = 0;
//                betweenElements = true;
//                result.append(c);
//            }
//        }
//
//        return result.toString();
//    }
//
//
//
//    public static double[] processRegex(String regex) {
//        try {
//            System.out.println("进入了 processRegex，参数是: " + regex);
//            PrimePath generator = new PrimePath(regex,true);
//            generator.generateMainPaths();
//            generator.createBHT();
//            generator.delH();
//
//            List<List<Integer>> minimumTestPaths = MinimumTestPath.finalReplacedEdges;
//            generator.printAutomatonGraph();
//            System.out.println("最小测试路径：");
//            for (List<Integer> path : minimumTestPaths) {
//                System.out.println(path.toString().replaceAll("[\\[\\]]", ""));
//            }
//
//            if (!PPRG.updatedMainPaths.isEmpty()) {
//                System.out.println("主路径集发生过变化，变化后的主路径集是：");
//                for (String updatedPath : PPRG.updatedMainPaths) {
//                    System.out.println(updatedPath);
//                }
//            }
//            List<String> mainPaths;
//            if (!PPRG.updatedMainPaths.isEmpty()) {
//                System.out.println("使用更新后的主路径集进行计算。");
//                mainPaths = new ArrayList<>(PPRG.updatedMainPaths);
//            } else {
//                mainPaths = generator.getMainPaths();
//            }
//            PPRG.updatedMainPaths.clear();
//            System.out.println("当前使用的主路径集：");
//            for (String path : mainPaths) {
//                String cleanedPath = path.replaceAll("p\\d+: ", "");
//                System.out.println(cleanedPath.replaceAll("[\\[\\]]", ""));
//            }
//            // 使用主路径集生成测试串
//            List<List<Integer>> mainPathList = new ArrayList<>();
//            for (String path : mainPaths) {
//                String cleaned = path.replaceAll("p\\d+: ", "").replaceAll("[\\[\\]]", "").trim();
//                if (!cleaned.isEmpty()) {
//                    List<Integer> intPath = new ArrayList<>();
//                    for (String numStr : cleaned.split(",\\s*")) {
//                        intPath.add(Integer.parseInt(numStr.trim()));
//                    }
//                    mainPathList.add(intPath);
//                }
//            }
//            LargeAlphabet.main(new String[]{}, mainPathList);
//            // === 检查是否需要添加 “*” 对应的 0 次出现情况 ===
//            if (regex.contains("*")) {
//                System.out.println("\n【检测到正则包含 '*'，开始检查“0次出现”测试情况】");
//
//                boolean hasZeroOccurrence = false;
//                for (String s : LargeAlphabet.cleanedTestStrings) {
//                    if (s.isEmpty()) { // 空串表示“出现0次”
//                        hasZeroOccurrence = true;
//                        break;
//                    }
//                }
//                if (!hasZeroOccurrence) {
//                    System.out.println("‘0次出现’的测试串尝试自动添加...");
//
//                    // 获取自动机
//                    Automaton automaton = PrimePath.automaton;
//                    State initial = automaton.getInitialState();
//
//                    // 1️⃣ 如果初始状态就是接受态，空串合法
//                    if (initial.isAccept()) {
//                        System.out.println("初始状态是接受态，添加空串 '' 作为0次出现测试串。");
//                        LargeAlphabet.printCleanedTestString("", new ArrayList<>(), LargeAlphabet.cleanedTestStrings.size() + 1);
//                    } else {
//                        // 2️⃣ 否则，尝试找到从初始状态到任意接受状态的最短路径
//                        Map<Integer, List<Integer>> adjacency = LargeAlphabet.buildAdjacencyMap(automaton);
//                        Set<Integer> acceptStates = new HashSet<>();
//                        for (State s : automaton.getStates()) {
//                            if (s.isAccept()) acceptStates.add(s.getNumber());
//                        }
//
//                        List<Integer> zeroPath = null;
//                        for (Integer acc : acceptStates) {
//                            zeroPath = LargeAlphabet.findShortestPath(initial.getNumber(), acc, adjacency);
//                            if (zeroPath != null && !zeroPath.isEmpty()) break;
//                        }
//
//                        if (zeroPath != null && !zeroPath.isEmpty()) {
//                            // 在这里检查 zeroPath 是否已经存在于 MinimumTestPath.finalReplacedEdges 中
//                            boolean pathExists = false;
//                            for (List<Integer> existingPath : MinimumTestPath.finalReplacedEdges) {
//                                if (existingPath.equals(zeroPath)) {
//                                    pathExists = true;
//                                    System.out.println("‘0次出现’路径已经存在，已存在的路径为: " + existingPath);
//                                    break;
//                                }
//                            }
//
//                            // 如果路径不存在，继续添加路径
//                            if (!pathExists) {
//                                List<String> zeroStrings = LargeAlphabet.buildAllPaths(zeroPath, automaton);
//                                if (!zeroStrings.isEmpty()) {
//                                    String zeroStr = zeroStrings.get(0);
//                                    System.out.println("添加‘0次出现’对应的测试串: " + zeroStr);
//                                    LargeAlphabet.printCleanedTestString(zeroStr, zeroPath, LargeAlphabet.cleanedTestStrings.size() + 1);
//                                } else {
//                                    System.out.println("未能基于自动机生成对应的0次出现串，跳过。");
//                                }
//                            } else {
//                                System.out.println("‘0次出现’路径已存在，跳过添加。");
//                            }
//                        } else {
//                            System.out.println("未能找到从初始状态到接受状态的最短路径，无法生成0次出现串。");
//                        }
//                    }
//                } else {
//                    System.out.println("检测到已有空串，无需补充。");
//                }
//            }
//
//            // 调用 LargeAlphabet 逻辑展开成测试串
//            List<String> generatedTestStrings = new ArrayList<>();
//            for (List<Integer> path : mainPathList) {
//                List<String> allPaths = LargeAlphabet.buildAllPaths(path, PrimePath.automaton,false);
//                generatedTestStrings.addAll(allPaths);
//            }
//            // 打印测试串
//            System.out.println("---------------");
//            System.out.println("根据主路径集生成的字符串：");
//
////            // 先去重
////            List<String> uniqueList = new ArrayList<>(new LinkedHashSet<>(generatedTestStrings));
//            List<String> uniqueList = new ArrayList<>(generatedTestStrings);
//            // 更新静态变量
//            generatedTestStringsCount = uniqueList.size();
//// 保留原来的循环形式
//            for (int i = 0; i < uniqueList.size(); i++) {
//                System.out.println("测试串 " + (i + 1) + ": " + uniqueList.get(i));
//            }
//            // 清空旧数据
//            LargeAlphabet.cleanedMainPathTestStrings.clear();
//            // === 主路径：检查是否需要添加 “*” 对应的 0 次出现情况 ===
//            if (regex.contains("*")) {
//                System.out.println("\n【检测到正则包含 '*'，开始检查主路径的“0次出现”测试情况】");
//
//                boolean hasZeroOccurrence = false;
//                for (String s : LargeAlphabet.cleanedMainPathTestStrings) {
//                    if (s.isEmpty()) { // 空串表示“出现0次”
//                        hasZeroOccurrence = true;
//                        break;
//                    }
//                }
//
//                if (!hasZeroOccurrence) {
//                    System.out.println("主路径 ‘0次出现’ 的测试串尝试自动添加...");
//
//                    // 获取自动机
//                    Automaton automaton = PrimePath.automaton;
//                    State initial = automaton.getInitialState();
//
//                    // 1️⃣ 初始状态是接受态 → 空串合法
//                    if (initial.isAccept()) {
//                        System.out.println("主路径：初始状态是接受态，添加空串 '' 作为0次出现测试串。");
//                        LargeAlphabet.cleanMainPathTestString("", new ArrayList<>());
//                    } else {
//
//                        // 2️⃣ 找到从初始状态到任意接受态的最短路径
//                        Map<Integer, List<Integer>> adjacency = LargeAlphabet.buildAdjacencyMap(automaton);
//                        Set<Integer> acceptStates = new HashSet<>();
//                        for (State s : automaton.getStates()) {
//                            if (s.isAccept()) acceptStates.add(s.getNumber());
//                        }
//
//                        List<Integer> zeroPath = null;
//                        for (Integer acc : acceptStates) {
//                            zeroPath = LargeAlphabet.findShortestPath(initial.getNumber(), acc, adjacency);
//                            if (zeroPath != null && !zeroPath.isEmpty()) break;
//                        }
//
//                        if (zeroPath != null && !zeroPath.isEmpty()) {
//
//                            // 检查该路径是否已经在主路径集中存在
//                            boolean pathExists = false;
//                            for (String existingMainPath : LargeAlphabet.cleanedMainPathTestStrings) {
//                                if (existingMainPath.equals(zeroPath)) {
//                                    pathExists = true;
//                                    System.out.println("主路径的 ‘0次出现’ 路径已经存在，为: " + existingMainPath);
//                                    break;
//                                }
//                            }
//
//                            // 若不存在 → 正常添加
//                            if (!pathExists) {
//                                List<String> zeroStrings = LargeAlphabet.buildAllPaths(zeroPath, automaton);
//                                if (!zeroStrings.isEmpty()) {
//                                    String zeroStr = zeroStrings.get(0);
//                                    System.out.println("主路径：添加 ‘0次出现’ 对应的测试串: " + zeroStr);
//
//                                    // 使用主路径清洗方法
//                                    LargeAlphabet.cleanMainPathTestString(zeroStr, zeroPath);
//                                } else {
//                                    System.out.println("主路径：无法生成0次出现串，跳过。");
//                                }
//                            } else {
//                                System.out.println("主路径：‘0次出现’ 路径已存在，跳过添加。");
//                            }
//
//                        } else {
//                            System.out.println("主路径：未能找到初始到接受态的路径，无法生成 0 次出现串。");
//                        }
//                    }
//
//                } else {
//                    System.out.println("主路径：检测到已有空串，无需补充。");
//                }
//            }
//            System.out.println("---------------");
//            System.out.println("根据主路径集生成的字符串：");
//
//            for (int i = 0; i < uniqueList.size(); i++) {
//                String raw = uniqueList.get(i);
//                List<Integer> srcMainPath = mainPathList.get(i % mainPathList.size()); // 或者实际对应关系
//                LargeAlphabet.cleanMainPathTestString(raw, srcMainPath);
//            }
//
//            generatedTestStringsCount = LargeAlphabet.cleanedMainPathTestStrings.size();
//
//            System.out.println("主路径测试串数量为 " + generatedTestStringsCount);
//
//
//
//            Set<String> countedMainPaths = new HashSet<>();
//            Set<String> countedTestPaths = new HashSet<>();
//
//            for (String mainPath : mainPaths) {
//                String cleanedMainPath = mainPath.replaceAll("p\\d+: ", "").replaceAll("[\\[\\]]", "").trim();
//                countedMainPaths.add(cleanedMainPath);
//
//                for (List<Integer> testPath : minimumTestPaths) {
//                    String cleanedTestPath = testPath.toString().replaceAll("[\\[\\]]", "").trim();
//                    if (cleanedTestPath.contains(cleanedMainPath)) {
//                        countedTestPaths.add(cleanedTestPath);
//                    }
//                }
//            }
//
//// 打印全部参与统计的主路径
//            System.out.println("【全部参与统计的主路径】");
//            int idx = 1;
//            for (String mp : countedMainPaths) {
//                System.out.println("主路径 " + idx + ": " + mp);
//                idx++;
//            }
//
//// 打印全部参与统计的最小测试路径
//            // 打印最小测试路径（原始数据，顺序保持不变）
//            System.out.println("【全部参与统计的最小测试路径】");
//            for (int i = 0; i < MinimumTestPath.finalReplacedEdges.size(); i++) {
//                List<Integer> path = MinimumTestPath.finalReplacedEdges.get(i);
//                System.out.println("最小测试路径 " + (i + 1) + ": " + path.toString().replaceAll("[\\[\\]]", ""));
//            }
//
//            int coveredPaths = 0;
//            for (String mainPath : mainPaths) {
//                String cleanedMainPath = mainPath.replaceAll("p\\d+: ", "").replaceAll("[\\[\\]]", "");
//                for (List<Integer> testPath : MinimumTestPath.finalReplacedEdges) {
//                    String cleanedTestPath = testPath.toString().replaceAll("[\\[\\]]", "");
//                    if (cleanedTestPath.contains(cleanedMainPath)) {
//                        coveredPaths++;
//                        break;
//                    }
//                }
//
//            }
//// 统计点和边的覆盖情况
//            int totalNodes = generator.getNumberOfStates();
//            Set<String> allEdges = new HashSet<>();
//            for (int from = 0; from < totalNodes; from++) {
//                for (int to : generator.getAdj()[from]) {
//                    allEdges.add(from + "->" + to);
//                }
//            }
//            int totalEdges = allEdges.size();
//
//// 使用最小测试路径统计点和边覆盖
//            Set<Integer> coveredNodes = new HashSet<>();
//            Set<String> coveredEdges = new HashSet<>();
//            for (List<Integer> path : MinimumTestPath.finalReplacedEdges) {
//                for (int i = 0; i < path.size(); i++) {
//                    coveredNodes.add(path.get(i));
//                    if (i < path.size() - 1) {
//                        coveredEdges.add(path.get(i) + "->" + path.get(i + 1));
//                    }
//                }
//            }
//
//            double nodeCoverage = totalNodes == 0 ? 0 : (double) coveredNodes.size() / totalNodes;
//            double edgeCoverage = totalEdges == 0 ? 0 : (double) coveredEdges.size() / totalEdges;
//
//// 对边覆盖统计（edge-pair coverage）
//            Set<String> allEdgePairs = new HashSet<>();
//            for (int from = 0; from < totalNodes; from++) {
//                for (int mid : generator.getAdj()[from]) {
//                    for (int to : generator.getAdj()[mid]) {
//                        allEdgePairs.add(from + "->" + mid + "->" + to);
//                    }
//                }
//            }
//            int totalEdgePairs = allEdgePairs.size();
//            Set<String> coveredEdgePairs = new HashSet<>();
//            for (List<Integer> path : MinimumTestPath.finalReplacedEdges) {
//                for (int i = 0; i < path.size() - 2; i++) {
//                    int from = path.get(i);
//                    int mid = path.get(i + 1);
//                    int to = path.get(i + 2);
//                    coveredEdgePairs.add(from + "->" + mid + "->" + to);
//                }
//            }
//
//            double edgePairCoverage = totalEdgePairs == 0 ? 1.0 : (double) coveredEdgePairs.size() / totalEdgePairs;
//
//            System.out.printf("点覆盖率：%.2f%%\n", nodeCoverage * 100);
//            System.out.printf("边覆盖率：%.2f%%\n", edgeCoverage * 100);
//            System.out.printf("对边覆盖率：%.2f%%\n", edgePairCoverage * 100);
//
//            System.out.println("所有可能的对边：");
//            for (String edgePair : allEdgePairs) {
//                System.out.println(edgePair);
//            }
//            System.out.println("被覆盖的对边：");
//            for (String edgePair : coveredEdgePairs) {
//                System.out.println(edgePair);
//            }
//
//
//
//            double coverage = (mainPaths.size() == 0) ? 0 : ((double) coveredPaths / mainPaths.size());
//            System.out.println("主路径覆盖率：" + (coverage * 100) + "%");
//
//            List<String> cleanedUpdatedPaths = LargeAlphabet.cleanedTestStrings;
//
//// 去重并保持原顺序
//            LinkedHashSet<String> uniquePaths = new LinkedHashSet<>(cleanedUpdatedPaths);
//
//// 打印去重后的测试串
//            System.out.println("---------------");
//            System.out.println("根据主路径生成的字符串：");
//
//// ★★ 在这里检查初始状态是否是接受态，如果是则先添加一个空串 ★★
//            if (LargeAlphabet.initialState.isAccept()) {
//                // 避免重复添加，可以检查是否已存在
//                if (!LargeAlphabet.cleanedMainPathTestStrings.contains("")) {
//                    LargeAlphabet.cleanedMainPathTestStrings.add(0, "");
//                    // 使用 add(0, "") 让空串排在第一位
//                }
//            }
//
//            int mainIndex = 1;
//            for (String s : LargeAlphabet.cleanedMainPathTestStrings) {
//                System.out.println("测试串 " + mainIndex + ": " + s);
//                mainIndex++;
//            }
//
//
//            System.out.println("---------------");
//            System.out.println("根据最小测试路径生成的字符串：");
//            int index = 1;
//            for (String s : uniquePaths) {
//                System.out.println("测试串 " + index + ": " + s);
//                index++;
//            }
//// 拼接成 mainPathTestStrings（用于表格）
//            List<String> joinedMainPaths = new ArrayList<>();
//
//            for (String s : LargeAlphabet.cleanedMainPathTestStrings) {
//                joinedMainPaths.add("'" + s + "'");
//            }
//
//            RegexProcessor.mainPathTestStrings = String.join(",", joinedMainPaths);
//            System.out.println("主路径拼接结果：" + mainPathTestStrings);
//
//// 拼接成 testStrings1
//            List<String> joinedPaths = new ArrayList<>();
//
//            for (String s : uniquePaths) {
//                joinedPaths.add("'" + s + "'");
//            }
//            testStrings1 = String.join(",", joinedPaths);
//            System.out.println("最小测试路径拼接结果：" + testStrings1);
//// 统计个数
//            // 统计个数，去重后的
//            int exampleCount = uniquePaths.size();
//// 返回结果
//            return new double[]{
//                    MinimumTestPath.finalReplacedEdges.size(),
//                    coverage * 100,
//                    nodeCoverage * 100,
//                    edgeCoverage * 100,
//                    edgePairCoverage * 100,
//                    LargeAlphabet.cleanedMainPathTestStrings.size(),           // 主路径测试串数量
//                    exampleCount,                // 最小测试路径生成的测试串数量
//                    mainPaths.size(),            // 主路径数量
//                    exampleCount                 // 实际输出的测试串数量
//            };
//
//        } catch (IllegalArgumentException e) {
//            System.err.println("跳过正则: " + regex);
//            return new double[]{-1, -1, 0, 0, 0, 0}; // 返回生成的测试串数量为 0
//        } catch (Exception e) {
//            System.err.println("处理正则表达式时出错，跳过: " + regex);
//            e.printStackTrace();
//            return new double[]{-1, -1, 0, 0, 0, 0}; // 返回生成的测试串数量为 0
//        }
//    }
//}
