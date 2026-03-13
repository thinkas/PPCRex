/*完整的约简*/
import dk.brics.automaton.Automaton;
import dk.brics.automaton.State;
import dk.brics.automaton.Transition;
import ua.edu.lp.asu.pairwise.Case;
import ua.edu.lp.asu.pairwise.Pairwise;
import ua.edu.lp.asu.pairwise.Parameter;
import java.util.*;
import java.util.stream.Collectors;
/**
 * LargeAlphabet
 *
 * 本类用于解决【大字母表正则表达式】下的测试用例生成问题，
 * 重点处理：
 * 1. 自动机路径组合爆炸
 * 2. 最小测试路径的修复与补全
 * 3. 基于 Pairwise Testing 的测试串约简
 */
public class LargeAlphabet {
    public static List<List<Integer>> addedPaths = new ArrayList<>();
    // 新增：用于统计的约简测试路径数量
    public static int reducedFinalPathCount = 0;
    public static State initialState;
    // 基于成对测试（Pairwise Testing）的分支因子上限
    private static final int MAX_BRANCH = 2;

    public static List<String> cleanedMainPathTestStrings = new ArrayList<>();
    public static List<String> cleanedTestStrings = new ArrayList<>();
    private static final Random random = new Random();
    public static List<String> getUpdatedPaths(List<List<Integer>> finalReplacedEdges, Automaton automaton) {
        List<String> updatedPaths = new ArrayList<>();
        for (List<Integer> path : finalReplacedEdges) {
            List<String> allPaths = buildAllPaths(path, automaton);
            updatedPaths.addAll(allPaths);
        }
        return updatedPaths;
    }
    /**
     * 仅统计一条路径在【全组合情况下】可生成的测试串数量
     * 不生成字符串，避免指数爆炸
     */
    public static long countAllCombinations(List<Integer> path, Automaton automaton) {
        Map<Integer, State> numberToState = new HashMap<>();
        for (State state : automaton.getStates()) {
            numberToState.put(state.getNumber(), state);
        }

        long total = 1;

        for (int i = 0; i < path.size() - 1; i++) {
            int from = path.get(i);
            int to = path.get(i + 1);

            int edgeCount = 0;
            State fromState = numberToState.get(from);

            for (Transition t : fromState.getTransitions()) {
                if (t.getDest().getNumber() == to) {
                    // ⚠️ 注意：一个 transition 表示一个“字符集合”，
                    // 在组合数量上算作 1 个“可选边”
                    edgeCount++;
                }
            }

            if (edgeCount == 0) {
                return 0; // 路径不合法
            }

            // 防止 long 溢出
            if (total > Long.MAX_VALUE / edgeCount) {
                return Long.MAX_VALUE;
            }

            total *= edgeCount;
        }

        return total;
    }
    public static void main(String[] args, List<List<Integer>> mainPathList) {
        System.out.println("正则表达式：" + PrimePath.regExp1);
        System.out.println("---------------");
        System.out.println("原始自动机：");
        System.out.println(PrimePath.automaton);
        // 修复 | 解析问题
        fixOrOperatorTransitions(PrimePath.automaton, PrimePath.regExp1);
        // 打印初始状态编号
        initialState = PrimePath.automaton.getInitialState();
        System.out.println("初始状态编号: " + initialState.getNumber());
        // 打印所有状态
        System.out.println("所有状态编号:");
        for (State state : PrimePath.automaton.getStates()) {
            System.out.print(state.getNumber() + " ");
        }
        System.out.println();
        // 打印接受状态
        System.out.print("接受状态编号: ");
        for (State state : PrimePath.automaton.getStates()) {
            if (state.isAccept()) {
                System.out.print(state.getNumber() + " ");
            }
        }
        System.out.println();
        // 打印每条边（转换）
        System.out.println("状态转换边:");
        for (State state : PrimePath.automaton.getStates()) {
            for (Transition transition : state.getTransitions()) {
                int from = state.getNumber();
                int to = transition.getDest().getNumber();
                char min = transition.getMin();
                char max = transition.getMax();
                String label = (min == max) ? String.valueOf(min) : (min + "-" + max);

                System.out.println("边 " + from + " 到 " + to);
            }
        }
        // 打印剩余非连通路径
        System.out.println("\n剩余的非连通路径：");
        if (PPRG.remainingNonConnectedPaths.isEmpty()) {
            System.out.println("无剩余非连通路径");
        } else {
            for (String path : PPRG.remainingNonConnectedPaths) {
                System.out.println(path);
            }
        }
        System.out.println("\n修复非连通路径后的新路径：");
        for (String pathStr : PPRG.remainingNonConnectedPaths) {
            // 解析字符串 "[0, 9, 5, 8, 0, 6]" => List<Integer>
            // 提取冒号后的部分，只要数字数组
            int colonIndex = pathStr.indexOf(":");
            String numberPart = (colonIndex != -1) ? pathStr.substring(colonIndex + 1) : pathStr;
// 去掉中括号和空格，留下纯数字部分
            String cleaned = numberPart.replaceAll("[\\[\\]\\s]", "");
            String[] tokens = cleaned.split(",");
            List<Integer> brokenPath = new ArrayList<>();
            for (String token : tokens) {
                if (!token.isEmpty()) {
                    brokenPath.add(Integer.parseInt(token));
                }
            }
            List<Integer> fixedPath = repairBrokenPath(brokenPath, PrimePath.automaton);
            if (!fixedPath.isEmpty()) {
                System.out.println("原路径: " + brokenPath);
                System.out.println("修复为: " + fixedPath);
            } else {
                System.out.println("原路径: " + brokenPath + " 无法修复");
            }
        }
        Set<String> seenFixedPaths = new HashSet<>();
        System.out.println("\n修复非连通路径后的新路径（去重后）：");
        for (String pathStr : PPRG.remainingNonConnectedPaths) {
            try {
                int colonIndex = pathStr.indexOf(":");
                String numberPart = (colonIndex != -1) ? pathStr.substring(colonIndex + 1) : pathStr;

                String cleaned = numberPart.replaceAll("[\\[\\]\\s]", "");
                String[] tokens = cleaned.split(",");
                List<Integer> brokenPath = new ArrayList<>();
                for (String token : tokens) {
                    if (!token.isEmpty()) {
                        brokenPath.add(Integer.parseInt(token));
                    }
                }
                List<Integer> fixedPath = repairBrokenPath(brokenPath, PrimePath.automaton);
                if (!fixedPath.isEmpty()) {
                    String fixedPathStr = fixedPath.toString();
                    // 如果修复路径没出现过，打印并加入finalReplacedEdges
                    if (seenFixedPaths.add(fixedPathStr)) {
                        System.out.println("修复为: " + fixedPath);
                        // 追加到最小测试路径列表末尾
                        MinimumTestPath.finalReplacedEdges.add(fixedPath);
                        // 去除被包含的路径
                        List<List<Integer>> filteredPaths = new ArrayList<>();
                        for (int i = 0; i < MinimumTestPath.finalReplacedEdges.size(); i++) {
                            List<Integer> pathA = MinimumTestPath.finalReplacedEdges.get(i);
                            boolean isSubPath = false;
                            for (int j = 0; j < MinimumTestPath.finalReplacedEdges.size(); j++) {
                                if (i == j) continue;
                                List<Integer> pathB = MinimumTestPath.finalReplacedEdges.get(j);
                                if (isSubPath(pathA, pathB)) {
                                    isSubPath = true;
                                    break;
                                }
                            }
                            if (!isSubPath) {
                                filteredPaths.add(pathA);
                            }
                        }
                        MinimumTestPath.finalReplacedEdges = filteredPaths;
                    }
                } else {
                    System.out.println("原路径: " + brokenPath + " 无法修复");
                }

            } catch (Exception e) {
                System.out.println("修复路径失败: " + pathStr + "，原因: " + e.getMessage());
            }
        }
// 修复最小测试路径中可能不存在的边
        List<List<Integer>> fullyRepairedPaths = new ArrayList<>();
        for (List<Integer> path : MinimumTestPath.finalReplacedEdges) {
            List<Integer> repaired = repairPathWithShortestConnection(path, PrimePath.automaton);
            fullyRepairedPaths.add(repaired);
        }
        System.out.println("修复前最小测试路径：");
        for (int i = 0; i < MinimumTestPath.finalReplacedEdges.size(); i++) {
            System.out.println("路径 " + (i + 1) + ": " + MinimumTestPath.finalReplacedEdges.get(i));
        }
// 用修复后的路径替换原来的最小测试路径
        MinimumTestPath.finalReplacedEdges = fullyRepairedPaths;

// 继续打印修复后的路径
        System.out.println("修复后最小测试路径：");
        for (int i = 0; i < MinimumTestPath.finalReplacedEdges.size(); i++) {
            System.out.println("路径 " + (i + 1) + ": " + MinimumTestPath.finalReplacedEdges.get(i));
        }


        // === 新增逻辑：打印主路径信息，并检查补充以结束状态结尾的主路径 ===
        System.out.println("\n＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝");
        System.out.println("当前主路径检查与补充过程：");
        System.out.println("＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝");

// 1️⃣ 获取当前主路径集
        List<List<Integer>> currentMainPaths = mainPathList;


// 打印当前主路径
        System.out.println("\n【当前使用的主路径集】");
        if (currentMainPaths == null || currentMainPaths.isEmpty()) {
            System.out.println("（无主路径）");
        } else {
            for (int i = 0; i < currentMainPaths.size(); i++) {
                System.out.println("主路径 " + (i + 1) + ": " + currentMainPaths.get(i));
            }
        }

// 2️⃣ 打印以接受状态结尾的主路径
        Set<Integer> acceptStates = new HashSet<>();
        for (State s : PrimePath.automaton.getStates()) {
            if (s.isAccept()) acceptStates.add(s.getNumber());
        }

        System.out.println("\n【以结束状态结尾的主路径】");
        List<List<Integer>> endAtAcceptPaths = new ArrayList<>();
        if (currentMainPaths != null) {
            for (List<Integer> mp : currentMainPaths) {
                if (!mp.isEmpty() && acceptStates.contains(mp.get(mp.size() - 1))) {
                    endAtAcceptPaths.add(mp);
                    System.out.println(mp);
                }
            }
        }
        if (endAtAcceptPaths.isEmpty()) {
            System.out.println("（没有以结束状态结尾的主路径）");
        }

// 3️⃣ 检查这些主路径是否都被修复后的最小测试路径覆盖或作为结尾
        System.out.println("\n【检查以结束状态结尾的主路径是否已被包含 / 作为结尾】");
        Map<Integer, List<Integer>> adjacency = buildAdjacencyMap(PrimePath.automaton);
        int initialStateId = PrimePath.automaton.getInitialState().getNumber();

        for (List<Integer> mainPath : endAtAcceptPaths) {
            boolean found = false;
            for (List<Integer> repaired : MinimumTestPath.finalReplacedEdges) {
                if (isSubPathAtEnd(mainPath, repaired)) {
                    System.out.println("主路径 " + mainPath + " 是路径结尾: " + repaired);
                    found = true;
                    break;
                }
            }

            // 如果没作为结尾出现，则需要补充
            if (!found) {
                System.out.println("主路径 " + mainPath + " 未作为结尾出现，开始补充...");

                // 如果主路径不是从初始状态开始，需要拼上最短路径
                if (mainPath.get(0) != initialStateId) {
                    List<Integer> prefix = findShortestPath(initialStateId, mainPath.get(0), adjacency);
                    if (prefix == null || prefix.isEmpty()) {
                        System.out.println("❌ 无法找到从初始状态到 " + mainPath.get(0) + " 的路径，跳过。");
                        continue;
                    }
                    prefix.remove(prefix.size() - 1);
                    List<Integer> merged = new ArrayList<>(prefix);
                    merged.addAll(mainPath);
                    addedPaths.add(merged);
                    MinimumTestPath.finalReplacedEdges.add(merged);
                    System.out.println("已补充主路径（带前缀）：" + merged);
                } else {
                    MinimumTestPath.finalReplacedEdges.add(new ArrayList<>(mainPath));
                    System.out.println("已补充主路径：" + mainPath);
                }
            }
        }


// 4️⃣ 打印最终修复后的最小测试路径
        System.out.println("\n【补充后修复的最小测试路径】");
        for (int i = 0; i < MinimumTestPath.finalReplacedEdges.size(); i++) {
            System.out.println("路径 " + (i + 1) + ": " + MinimumTestPath.finalReplacedEdges.get(i));
        }
        System.out.println("＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝＝\n");
// === 新增逻辑：严格检查以接受状态结尾的主路径是否存在于最终最小测试路径中 ===
        System.out.println("\n=====================================================");
        System.out.println("新增检查：确保所有以接受状态结尾的主路径在最终修复后最小测试路径中");
        System.out.println("=====================================================");

// 1️⃣ 打印所有以接受状态结尾的主路径
        System.out.println("\n【以结束状态结尾的主路径】");
        for (List<Integer> path : endAtAcceptPaths) {
            System.out.println("主路径: " + path);
        }

// 2️⃣ 打印当前补充后的最小测试路径
        System.out.println("\n【当前补充后的最小测试路径】");
        for (int i = 0; i < MinimumTestPath.finalReplacedEdges.size(); i++) {
            System.out.println("路径 " + (i + 1) + ": " + MinimumTestPath.finalReplacedEdges.get(i));
        }

// 3️⃣ 检查并补充
        List<List<Integer>> finalPaths = new ArrayList<>(MinimumTestPath.finalReplacedEdges);

        for (List<Integer> mainPath : endAtAcceptPaths) {
            boolean exists = false;

            // 3.1 检查是否已经存在于最终最小测试路径
            for (List<Integer> repaired : finalPaths) {
                if (mainPath.equals(repaired)) {
                    exists = true;
                    System.out.println("主路径 " + mainPath + " 已在最终最小测试路径中，无需添加。");
                    break;
                }
            }

            if (!exists) {
                System.out.println("主路径 " + mainPath + " 不在最终最小测试路径中，需要进一步处理。");

                List<Integer> pathToAdd = new ArrayList<>(mainPath);

                // 3.2 如果首节点不是初始状态，需要拼接最短路径前缀
                if (!mainPath.isEmpty() && mainPath.get(0) != initialStateId) {
                    List<Integer> prefix = findShortestPath(initialStateId, mainPath.get(0), adjacency);
                    if (prefix == null || prefix.isEmpty()) {
                        System.out.println("无法找到从初始状态到主路径首节点 " + mainPath.get(0) + " 的最短路径，跳过添加此主路径。");
                        continue;
                    }
                    prefix.remove(prefix.size() - 1); // 去掉重复节点
                    pathToAdd = new ArrayList<>(prefix);
                    pathToAdd.addAll(mainPath);
                    System.out.println("拼接前缀后的路径: " + pathToAdd);
                } else {
                    System.out.println("主路径首节点是初始状态，无需拼接前缀。");
                }

                // 3.3 拼接后再次检查是否已存在
                boolean existsAfterPrefix = false;
                for (List<Integer> repaired : finalPaths) {
                    if (pathToAdd.equals(repaired)) {
                        existsAfterPrefix = true;
                        System.out.println("拼接前缀后的路径 " + pathToAdd + " 已在最终最小测试路径中，无需添加。");
                        break;
                    }
                }

                // 3.4 最终添加路径
                if (!existsAfterPrefix) {
                    MinimumTestPath.finalReplacedEdges.add(pathToAdd);
                    addedPaths.add(pathToAdd);
                    finalPaths.add(pathToAdd);
                    System.out.println("主路径 " + mainPath + " 被添加到最终最小测试路径中: " + pathToAdd);
                }
            }
        }
// 先打印最终最小测试路径（包括新增路径）
        System.out.println("\n【最终最小测试路径（新增补充后）】");
        for (int i = 0; i < MinimumTestPath.finalReplacedEdges.size(); i++) {
            System.out.println("路径 " + (i + 1) + ": " + MinimumTestPath.finalReplacedEdges.get(i));
        }
        System.out.println("=====================================================\n");

// 从最终最小测试路径中移除所有新增补充的路径 addedPaths
        MinimumTestPath.finalReplacedEdges.removeAll(addedPaths);
        reducedFinalPathCount = MinimumTestPath.finalReplacedEdges.size();
// 打印移除后的最小测试路径
        System.out.println("\n【最终最小测试路径（移除新增补充路径后）】");
        for (int i = 0; i < MinimumTestPath.finalReplacedEdges.size(); i++) {
            System.out.println("路径 " + (i + 1) + ": " + MinimumTestPath.finalReplacedEdges.get(i));
        }

        System.out.println("\n【移除 75% 分位前：路径 → 全组合测试串数量统计】");

        int idx = 1;
        for (List<Integer> path : MinimumTestPath.finalReplacedEdges) {
            long count = countAllCombinations(path, PrimePath.automaton);

            if (count == Long.MAX_VALUE) {
                System.out.println("路径 " + idx + ": " + path + " → 测试串数量：极大（已溢出）");
            } else {
                System.out.println("路径 " + idx + ": " + path + " → 测试串数量：" + count);
            }

            idx++;
        }

        System.out.println("=====================================================\n");
// ------------------ 新增百分位逻辑 ------------------
// 计算所有路径长度
        List<Integer> lengths = new ArrayList<>();
        for (List<Integer> path : MinimumTestPath.finalReplacedEdges) {
            lengths.add(path.size());
        }

// 排序
        Collections.sort(lengths);
        int n = lengths.size();

// 定义需要打印的百分位
        double[] percentiles = {0.0, 0.25, 0.50, 0.75, 1.0};
        System.out.println("路径长度关键百分位统计：");
        for (double p : percentiles) {
            int index = (int) Math.ceil(p * n) - 1;
            index = Math.max(index, 0);       // 避免负索引
            index = Math.min(index, n - 1);   // 避免越界
            int value = lengths.get(index);
            System.out.println((int) (p * 100) + "% 分位数: " + value);
        }

// 取 0% 和 75% 分位数
        int length0 = lengths.get(0);
        int length75 = lengths.get((int) Math.ceil(0.75 * n) - 1);
        int length100 = lengths.get(n - 1); // 最大值，用于判断所有路径是否相同

// 判断是否需要移除路径
        if (length0 == length100) {
            // 所有路径长度相同，不移除任何路径
            System.out.println("所有路径长度相同，长度>=75%分位数的路径将不被移除。");
        } else {

            // ------------------ 新增：统计长路径/短路径 ------------------
            int longCount = 0;
            int shortCount = 0;
            for (List<Integer> path : MinimumTestPath.finalReplacedEdges) {
                if (path.size() >= length75) longCount++;
                else shortCount++;
            }

            double ratio = (shortCount == 0) ? Double.MAX_VALUE : (double) longCount / shortCount;
            System.out.println("长路径数量 = " + longCount + "，短路径数量 = " + shortCount + "，长/短占比 = " + ratio);

            // 误差范围，避免浮点误差导致 ratio==1 的判断失败
            double EPS = 1e-9;
            boolean isRatioEqualOne = Math.abs(ratio - 1.0) < EPS;

            // 移除长度大于等于 75% 分位数的路径，并加入 addedPaths
            List<List<Integer>> longPaths = new ArrayList<>();
            for (Iterator<List<Integer>> it = MinimumTestPath.finalReplacedEdges.iterator(); it.hasNext(); ) {
                List<Integer> path = it.next();

                int pathLength = path.size();
                long count = countAllCombinations(path, PrimePath.automaton);
                long linearThreshold = (long) pathLength * MAX_BRANCH;

                boolean isExploding =
                        (count == Long.MAX_VALUE) || count > linearThreshold;

                System.out.println(
                        "路径 " + path
                                + " | 长度 = " + pathLength
                                + " | 组合数 = " + (count == Long.MAX_VALUE ? "溢出" : count)
                                + " | pairwise 阈值 = " + linearThreshold
                                + " | 是否爆炸 = " + isExploding
                );
                if (pathLength >= length75) {

                    if (ratio > 0.25 && !isRatioEqualOne) {
                        System.out.println("→ 长路径占比 > 25% 且不为1，全部长路径走随机，移除该路径");
                        longPaths.add(path);
                        it.remove();
                    } else {
                        if (isExploding) {
                            System.out.println("→ 移除该路径（长度>=75%分位 且 组合爆炸）");
                            longPaths.add(path);
                            it.remove();
                        } else {
                            System.out.println("→ 保留该路径（可通过 pairwise 覆盖）");
                        }
                    }

                } else {
                    System.out.println("→ 短路径，保留");
                }
            }

            // 将这些长路径加入 addedPaths
            addedPaths.addAll(longPaths);
        }

        System.out.println("=====================================================\n");


// 打印移除长路径后的最小测试路径
        System.out.println("\n【最终最小测试路径（移除新增补充路径和长度>=75%分位数且超出阈值路径后）】");
        for (int i = 0; i < MinimumTestPath.finalReplacedEdges.size(); i++) {
            System.out.println("路径 " + (i + 1) + ": " + MinimumTestPath.finalReplacedEdges.get(i));
        }
        System.out.println("=====================================================\n");


// 假设在方法最开始，声明一个方法内可见的变量
        List<String> savedNewTestStrings = new ArrayList<>(); // 保存新增路径测试串

// 打印所有通过策略补充的新路径，同时生成随机测试串
        System.out.println("\n【所有新增补充路径生成的测试串】");
        if (!addedPaths.isEmpty()) {
            int tmpStrIndex = 1;
            for (int i = 0; i < addedPaths.size(); i++) {
                List<Integer> path = addedPaths.get(i);
                System.out.println("新增路径 " + (i + 1) + ": " + path);

                List<String> generatedStrings = buildAllPaths(path, PrimePath.automaton, true);
                if (!generatedStrings.isEmpty()) {
                    printCleanedTestString(generatedStrings.get(0), path, tmpStrIndex++);
                    // 保存到方法外变量
                    savedNewTestStrings.add(LargeAlphabet.cleanedTestStrings.get(LargeAlphabet.cleanedTestStrings.size() - 1));
                }
            }
            System.out.println("\n【统一打印所有新增补充的测试串】");
            System.out.println(savedNewTestStrings);
        }
        System.out.println("=====================================================\n");


// 对修复后的最小测试路径去重
        Set<String> uniquePaths = new HashSet<>();
        List<List<Integer>> deduplicatedPaths = new ArrayList<>();
        for (List<Integer> path : MinimumTestPath.finalReplacedEdges) {
            String pathStr = path.toString(); // 用字符串做唯一性标识
            if (uniquePaths.add(pathStr)) {
                deduplicatedPaths.add(path);
            }
        }
// 替换原始路径列表为去重后的版本
        MinimumTestPath.finalReplacedEdges = deduplicatedPaths;
        // 打印未展开的原始路径
        System.out.println("最小测试路径：");
        for (int i = 0; i < MinimumTestPath.finalReplacedEdges.size(); i++) {
            System.out.println("路径 " + (i + 1) + ": " + MinimumTestPath.finalReplacedEdges.get(i));
        }


        printAutomatonStructure(PrimePath.automaton);
        isParametersPrinted = false;
        isPrinted = false;
        // 构建最小测试路径到展开字符串的映射
        Map<List<Integer>, List<String>> pathToStrings = new LinkedHashMap<>();
        for (List<Integer> path : MinimumTestPath.finalReplacedEdges) {
            List<String> allPaths = buildAllPaths(path, PrimePath.automaton);
            pathToStrings.put(path, allPaths);
        }

// === BEGIN: 输出空串测试用例（如果初始状态是接受态），放最前面，不参与分组 ===
        int strIndex = 1;
        LargeAlphabet.cleanedTestStrings.clear();
        if (initialState.isAccept()) {
            printCleanedTestString("", Collections.emptyList(), strIndex++);
        }



// === BEGIN: 分组与输出逻辑（宽松：连通分量）===
        Random random = new Random();

        List<List<List<Integer>>> groups = new ArrayList<>();
        List<List<Integer>> allPaths = new ArrayList<>(pathToStrings.keySet());

// 原来的 grouped 改成 visited
        boolean[] visited = new boolean[allPaths.size()];

        System.out.println("\n=== 构造无向图并求连通分量（宽松分组）===");

        for (int i = 0; i < allPaths.size(); i++) {
            if (visited[i]) continue;

            // 发现一个新的连通分量
            System.out.println("\n发现未访问路径，作为新连通分量起点: " + allPaths.get(i));

            List<List<Integer>> group = new ArrayList<>();
            Queue<Integer> queue = new LinkedList<>();

            // BFS 初始化
            queue.add(i);
            visited[i] = true;

            while (!queue.isEmpty()) {
                int curr = queue.poll();
                List<Integer> currPath = allPaths.get(curr);

                group.add(currPath);
                System.out.println("  访问路径: " + currPath);

                // 扫描所有其他路径，找相似的
                for (int j = 0; j < allPaths.size(); j++) {
                    if (visited[j]) continue;

                    List<Integer> otherPath = allPaths.get(j);

                    if (onlyOneElementDiff(currPath, otherPath)) {
                        System.out.println("    相似 → 加边: " + currPath + " —— " + otherPath);
                        visited[j] = true;
                        queue.add(j);
                    }
                }
            }

            System.out.println("连通分量完成，包含路径数量: " + group.size());
            groups.add(group);
        }

// === 输出阶段：逻辑与原来完全一致 ===
        System.out.println("\n=== 按分组输出测试串 ===");

        for (List<List<Integer>> group : groups) {
            boolean isFirstInGroup = true;
            System.out.println("\n处理分组: " + group);

            for (List<Integer> path : group) {
                List<String> strings = pathToStrings.get(path);
                if (strings == null || strings.isEmpty()) continue;

                if (isFirstInGroup) {
                    System.out.println("  代表路径（全保留）: " + path);
                    for (String original : strings) {
                        printCleanedTestString(original, path, strIndex++);
                    }
                    isFirstInGroup = false;
                } else {
                    String original = strings.get(random.nextInt(strings.size()));
                    System.out.println("  非代表路径（随机保留一条）: " + path);
                    printCleanedTestString(original, path, strIndex++);
                }
            }
        }

// 在清空后，把之前保存的新增测试串追加回来
        LargeAlphabet.cleanedTestStrings.addAll(savedNewTestStrings);
// === END: 分组与输出逻辑（宽松）===
    }
    /**
     * 判断 small 是否是 big 的连续子路径（严格连续匹配）
     */
    public static boolean isContainedSubPath(List<Integer> small, List<Integer> big) {
        if (small.size() > big.size()) return false;
        for (int i = 0; i <= big.size() - small.size(); i++) {
            boolean match = true;
            for (int j = 0; j < small.size(); j++) {
                if (!small.get(j).equals(big.get(i + j))) {
                    match = false;
                    break;
                }
            }
            if (match) return true; // 找到了连续匹配
        }
        return false;
    }

    /**
     * 判断 small 是否是 big 的结尾子路径
     */
    public static boolean isSubPathAtEnd(List<Integer> small, List<Integer> big) {
        if (small.size() > big.size()) return false;
        int offset = big.size() - small.size();
        for (int i = 0; i < small.size(); i++) {
            if (!small.get(i).equals(big.get(offset + i))) {
                return false;
            }
        }
        return true;
    }

// === END: 分组与输出逻辑 ===

    /**
     * 辅助方法：判断两个路径只有一个元素不同
     */
    private static boolean onlyOneElementDiff (List < Integer > a, List < Integer > b){
        if (a.size() != b.size()) return false;
        int diff = 0;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).equals(b.get(i))) diff++;
            if (diff > 1) return false;
        }
        return diff == 1;
    }

    /**
     * 辅助方法：打印并清洗测试串
     */
    public static void printCleanedTestString (String original, List < Integer > srcPath,int strIndex){
        String[] tokens = original.split(", ");
        for (int t = 0; t < tokens.length; t++) {
            tokens[t] = tokens[t].replace(" ", "\\u0020");
        }
        String visibleOriginal = String.join(", ", tokens);
        String cleaned = String.join("", tokens);
        cleaned = cleaned.replace("\\u0020", " ");
        StringBuilder cleanedReplaced = new StringBuilder();
        for (char c : cleaned.toCharArray()) {
            if (Character.isISOControl(c) || (Character.isWhitespace(c) && c != ' ')) {
                cleanedReplaced.append('中');
            } else {
                cleanedReplaced.append(c);
            }
        }
        String finalCleaned = cleanedReplaced.toString();

        System.out.println("原始路径字符串: [" + visibleOriginal + "] 来自路径: " + srcPath);
        System.out.println("清洗后测试串: [" + finalCleaned + "]");
        LargeAlphabet.cleanedTestStrings.add(finalCleaned);
        System.out.println("测试串 " + strIndex + ": " + finalCleaned);
    }
    /**
     * 主路径专用测试串清洗方法（无 strIndex）
     * original = 原始测试串（由 LargeAlphabet.buildAllPaths 产生）
     * srcMainPath = 来源的主路径（List<Integer>）
     */
    public static void cleanMainPathTestString(String original, List<Integer> srcMainPath) {

        // 1. 切分
        String[] tokens = original.split(", ");
        for (int t = 0; t < tokens.length; t++) {
            tokens[t] = tokens[t].replace(" ", "\\u0020");
        }

        // 2. 还原可见形式
        String visibleOriginal = String.join(", ", tokens);

        // 3. 拼接成连续串
        String cleaned = String.join("", tokens);
        cleaned = cleaned.replace("\\u0020", " ");

        // 4. 清洗非法字符
        StringBuilder cleanedReplaced = new StringBuilder();
        for (char c : cleaned.toCharArray()) {
            if (Character.isISOControl(c) || (Character.isWhitespace(c) && c != ' ')) {
                cleanedReplaced.append('中');
            } else {
                cleanedReplaced.append(c);
            }
        }

        String finalCleaned = cleanedReplaced.toString();

        // 5. 打印（主路径专用）
        System.out.println("原始主路径字符串: [" + visibleOriginal + "] 来自主路径: " + srcMainPath);
        System.out.println("清洗后主路径测试串: [" + finalCleaned + "]");

        // 6. 保存到主路径专用的静态变量（在 LargeAlphabet 中新增）
        LargeAlphabet.cleanedMainPathTestStrings.add(finalCleaned);
    }


    public static List<Integer> repairPathWithShortestConnection(List<Integer> path, Automaton automaton) {
        List<Integer> repaired = new ArrayList<>();
        repaired.add(path.get(0));
        Map<Integer, List<Integer>> adj = buildAdjacencyMap(automaton);

        for (int i = 1; i < path.size(); i++) {
            int from = path.get(i - 1);
            int to = path.get(i);

            // 🐞 DEBUG：打印当前from和to，以及邻接表中from的邻居
            System.out.println("检查路径: " + from + " -> " + to);
            System.out.println("邻接表中" + from + "的邻居: " + adj.get(from));

            // 如果from到to是合法边，直接加to
            if (adj.containsKey(from) && adj.get(from).contains(to)) {
                repaired.add(to);
            } else {
                // 不存在边，找最短路径连接
                List<Integer> shortestPath = findShortestPath(from, to, adj);

                // 🐞 DEBUG：打印修复路径
                System.out.println("找不到直接边，尝试修复路径: " + from + " -> " + to);
                System.out.println("修复得到的路径: " + shortestPath);

                if (shortestPath == null || shortestPath.isEmpty()) {
                    repaired.add(to);  // 或考虑抛异常
                } else {
                    for (int j = 1; j < shortestPath.size(); j++) {
                        repaired.add(shortestPath.get(j));
                    }
                }
            }
        }
        return repaired;
    }
    // 构建邻接表，key是状态编号，value是状态编号列表（邻居）
    public static Map<Integer, List<Integer>> buildAdjacencyMap(Automaton automaton) {
        Map<Integer, List<Integer>> adj = new HashMap<>();
        Set<State> states = automaton.getStates();

        for (State state : states) {
            int stateId = state.getNumber();
            adj.putIfAbsent(stateId, new ArrayList<>());

            for (Transition t : state.getTransitions()) {
                int destId = t.getDest().getNumber();
                adj.get(stateId).add(destId);
            }
        }
        return adj;
    }
    // BFS 找最短路径，从 start 状态编号到 end 状态编号
    public static List<Integer> findShortestPath(int start, int end, Map<Integer, List<Integer>> adj) {
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> parent = new HashMap<>();
        queue.add(start);
        parent.put(start, null);

        while (!queue.isEmpty()) {
            int curr = queue.poll();
            if (curr == end) break;

            List<Integer> neighbors = adj.get(curr);
            if (neighbors == null) continue;

            for (int next : neighbors) {
                if (!parent.containsKey(next)) {
                    parent.put(next, curr);
                    queue.add(next);
                }
            }
        }

        if (!parent.containsKey(end)) return null; // 找不到路径

        List<Integer> path = new ArrayList<>();
        for (Integer at = end; at != null; at = parent.get(at)) {
            path.add(at);
        }
        Collections.reverse(path);
        return path;
    }
    public static boolean isSubPath(List<Integer> small, List<Integer> big) {
        if (small.size() > big.size()) return false;
        for (int i = 0; i <= big.size() - small.size(); i++) {
            boolean match = true;
            for (int j = 0; j < small.size(); j++) {
                if (!small.get(j).equals(big.get(i + j))) {
                    match = false;
                    break;
                }
            }
            if (match) return true;
        }
        return false;
    }

    public static void printAutomatonStructure(Automaton automaton) {
        System.out.println("------ 当前使用的自动机结构 ------");

        // 打印初始状态
        State initial = automaton.getInitialState();
        System.out.println("初始状态编号: " + initial.getNumber());

        // 打印所有状态编号
        System.out.print("所有状态编号: ");
        for (State state : automaton.getStates()) {
            System.out.print(state.getNumber() + " ");
        }
        System.out.println();

        // 打印接受状态编号
        System.out.print("接受状态编号: ");
        for (State state : automaton.getStates()) {
            if (state.isAccept()) {
                System.out.print(state.getNumber() + " ");
            }
        }
        System.out.println();

        // 打印每条边
        System.out.println("状态转换边:");
        for (State state : automaton.getStates()) {
            for (Transition t : state.getTransitions()) {
                int from = state.getNumber();
                int to = t.getDest().getNumber();
                char min = t.getMin();
                char max = t.getMax();
                String label = (min == max) ? String.valueOf(min) : (min + "-" + max);
                System.out.println("边 " + from + " → " + to + " [label: " + label + "]");
            }
        }

        System.out.println("------ 自动机结构结束 ------\n");
    }
    private static List<Integer> findShortestPathByNumber(int startNum, int goalNum, Map<Integer, List<Integer>> adjacency) {
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> prev = new HashMap<>();
        Set<Integer> visited = new HashSet<>();

        queue.add(startNum);
        visited.add(startNum);

        while (!queue.isEmpty()) {
            int current = queue.poll();
            if (current == goalNum) break;

            for (int next : adjacency.getOrDefault(current, Collections.emptyList())) {
                if (!visited.contains(next)) {
                    visited.add(next);
                    prev.put(next, current);
                    queue.add(next);
                }
            }
        }

        if (!prev.containsKey(goalNum) && startNum != goalNum) return Collections.emptyList();

        List<Integer> path = new LinkedList<>();
        Integer curr = goalNum;
        while (curr != null && curr != startNum) {
            path.add(0, curr);
            curr = prev.get(curr);
        }
        path.add(0, startNum);
        return path;
    }

    public static List<Integer> repairBrokenPath(List<Integer> brokenPath, Automaton automaton) {
        // 构建邻接表
        Map<Integer, List<Integer>> adjacency = new HashMap<>();
        for (State state : automaton.getStates()) {
            int from = state.getNumber();
            adjacency.putIfAbsent(from, new ArrayList<>());
            for (Transition t : state.getTransitions()) {
                int to = t.getDest().getNumber();
                adjacency.get(from).add(to);
            }
        }

        List<Integer> fixed = new ArrayList<>();
        int startIndex = MinimumTestPath.a;  // 假设a和b是状态编号
        List<Integer> endIndices = MinimumTestPath.b;

        // Step 1: a -> brokenPath[0]
        List<Integer> prefix = findShortestPathByNumber(startIndex, brokenPath.get(0), adjacency);
        if (prefix.isEmpty()) return Collections.emptyList();
        fixed.addAll(prefix);

        // Step 2: 修复 brokenPath 中中间节点的连通性
        for (int i = 0; i < brokenPath.size() - 1; i++) {
            int from = brokenPath.get(i);
            int to = brokenPath.get(i + 1);

            if (from == to) {
                fixed.add(to);
                continue;
            }
            if (adjacency.getOrDefault(from, Collections.emptyList()).contains(to)) {
                fixed.add(to);
            } else {
                List<Integer> midPath = findShortestPathByNumber(from, to, adjacency);
                if (midPath.isEmpty()) return Collections.emptyList();
                midPath.remove(0); // 避免重复添加 from
                fixed.addAll(midPath);
            }
        }
        // Step 3: brokenPath最后一个节点 -> 最近终点
        int lastNode = fixed.get(fixed.size() - 1);
        int nearestEnd = -1;
        List<Integer> suffix = null;
        int minLen = Integer.MAX_VALUE;
        for (int end : endIndices) {
            List<Integer> tempPath = findShortestPathByNumber(lastNode, end, adjacency);
            if (!tempPath.isEmpty() && tempPath.size() < minLen) {
                minLen = tempPath.size();
                nearestEnd = end;
                suffix = tempPath;
            }
        }

        if (suffix == null) return Collections.emptyList();

        suffix.remove(0);
        fixed.addAll(suffix);

        return fixed;
    }


    /**
     * 处理正则表达式中的 |（或）运算符，修正自动机的转换关系
     *
     * @param automaton 自动机
     * @param regex     正则表达式
     */
    private static void fixOrOperatorTransitions(Automaton automaton, String regex) {
        if (!regex.contains("|")) {
            return;
        }

        for (State state : automaton.getStates()) {
            List<Transition> originalTransitions = new ArrayList<>(state.getTransitions());
            state.getTransitions().clear(); // 清空旧转移

            for (Transition transition : originalTransitions) {
                char min = transition.getMin();
                char max = transition.getMax();

                if (min == max) {
                    // 单字符情况
                    if (isRegexCharacter(min, regex)) {
                        // 保留原始 transition 对象
                        state.addTransition(transition);
                    } else {
                        state.addTransition(transition); // 非正则字符照样保留
                    }
                } else if (isRange(min, max)) {
                    if (isRegexRange(min, max, regex)) {
                        state.addTransition(transition); // 保留整个范围
                    } else {
                        boolean allSplit = true;
                        for (char ch = min; ch <= max; ch++) {
                            if (!isRegexCharacter(ch, regex)) {
                                allSplit = false;
                                break;
                            }
                        }

                        if (allSplit) {
                            // 全部在正则中，拆分范围，但保留原始终点
                            for (char ch = min; ch <= max; ch++) {
                                state.addTransition(new Transition(ch, transition.getDest()));
                            }
                        } else {
                            // 否则保留原始范围
                            state.addTransition(transition);
                        }
                    }
                } else {
                    // 其他类型的 transition 直接保留
                    state.addTransition(transition);
                }
            }
        }

        // 初始状态
        State initialState = automaton.getInitialState();
        System.out.printf("initial state: %d\n", initialState.getNumber());
// 再打印所有状态
        List<State> states = new ArrayList<>(automaton.getStates());
        states.sort(Comparator.comparingInt(State::getNumber));
        for (State state : states) {
            System.out.printf("state %d [%s]:\n", state.getNumber(), state.isAccept() ? "accept" : "reject");
            for (Transition t : state.getTransitions()) {
                if (t.getMin() == t.getMax()) {
                    System.out.printf("  %c -> %d\n", t.getMin(), t.getDest().getNumber());
                } else {
                    System.out.printf("  %c-%c -> %d\n", t.getMin(), t.getMax(), t.getDest().getNumber());
                }
            }
        }


    }


    /**
     * 判断该字符是否属于正则表达式中的字符
     */
    private static boolean isRegexCharacter(char ch, String regex) {
        // 判断字符是否是正则表达式中的字符
        return regex.contains(String.valueOf(ch));
    }

    /**
     * 判断是否是正则表达式中的范围，如a-z, A-Z等
     */
    private static boolean isRegexRange(char min, char max, String regex) {
        // 如果是有效范围，保留该范围
        return regex.contains(String.valueOf(min) + "-" + String.valueOf(max));
    }
    /**
     * 判断是否是一个字符范围（a-z, A-Z, 0-9）
     */
    private static boolean isRange(char min, char max) {
        return (min >= 'a' && max <= 'z') ||
                (min >= 'A' && max <= 'Z') ||
                (min >= '0' && max <= '9');
    }
    /**
     * 根据最小测试路径构造所有可能的输入字符串。
     *
     * @param path      最小测试路径，包含状态编号的列表
     * @param automaton 目标自动机
     * @return 生成的所有路径字符串列表
     */
    private static boolean isPrinted;  // 标志位
    public static List<String> buildAllPaths(List<Integer> path, Automaton automaton) {
        Map<Integer, State> numberToState = new HashMap<>();
        for (State state : automaton.getStates()) {
            numberToState.put(state.getNumber(), state);
        }

        List<List<String>> allTransitions = new ArrayList<>();

        for (int i = 0; i < path.size() - 1; i++) {
            int from = path.get(i);
            int to = path.get(i + 1);

            List<String> edgeLabels = new ArrayList<>();

            State fromState = numberToState.get(from);
            for (Transition transition : fromState.getTransitions()) {
                if (transition.getDest().getNumber() == to) {
                    char min = transition.getMin();
                    char max = transition.getMax();

                    String selectedChar;

                    if (min == max) {
                        selectedChar = String.valueOf(min);
                    } else if (min == '\u0000' || max == '\uffff' || (min == '\u0000' && max == '\uffff')) {
                        selectedChar = String.valueOf(getValidNegativeChar(min, max));
                    } else {
                        List<String> characterList = getCharacterFromRange(min, max);
                        selectedChar = characterList.get(random.nextInt(characterList.size()));
                    }

                    edgeLabels.add(selectedChar);
                }
            }
            allTransitions.add(edgeLabels);
        }

        if (!isPrinted) {
//                System.out.println("所有转换路径:");
//                for (int i = 0; i < allTransitions.size(); i++) {
//                    System.out.println("从状态 " + path.get(i) + " 到状态 " + path.get(i + 1) + ": " + allTransitions.get(i));
//                }
            isPrinted = true;
        }

        return generatePathCombinations(allTransitions);
    }


    public static List<String> buildAllPaths(List<Integer> path, Automaton automaton, boolean usePairwiseTesting) {
        // 打印传进来的路径
        System.out.println("传入的路径: " + path);
// ---------- 补路径逻辑开始 ----------
        State initialStateX = automaton.getInitialState();

// 1. 若首节点不是初始状态，则补齐前导路径
        if (!path.isEmpty() && path.get(0) != initialStateX.getNumber()) {
            List<Integer> prefix = bfsShortestPath(automaton, initialStateX.getNumber(), path.get(0));
            if (prefix != null && prefix.size() > 1) {
                prefix.remove(prefix.size() - 1);   // 去掉重复的首节点
                prefix.addAll(path);
                path = prefix;
            }
        }

// 2. 若尾节点不是接受状态，则补齐末尾路径
        int last = path.get(path.size() - 1);
        int nearestAccept = findNearestAcceptState(automaton, last);

        if (nearestAccept != -1 && nearestAccept != last) {
            List<Integer> suffix = bfsShortestPath(automaton, last, nearestAccept);
            if (suffix != null && suffix.size() > 1) {
                suffix.remove(0);    // 去掉重复的尾节点
                path.addAll(suffix);
            }
        }

        System.out.println("补齐后的实际使用路径: " + path);
// ---------- 补路径逻辑结束 ----------
        Map<Integer, State> numberToState = new HashMap<>();
        for (State state : automaton.getStates()) {
            numberToState.put(state.getNumber(), state);
        }

        List<List<String>> allTransitions = new ArrayList<>();

        // 遍历路径中的每一跳，获取转移标签
        for (int i = 0; i < path.size() - 1; i++) {
            int from = path.get(i);
            int to = path.get(i + 1);

            List<String> edgeLabels = new ArrayList<>();

            State fromState = numberToState.get(from);
            for (Transition transition : fromState.getTransitions()) {
                if (transition.getDest().getNumber() == to) {
                    char min = transition.getMin();
                    char max = transition.getMax();

                    String selectedChar;

                    if (min == max) {
                        selectedChar = String.valueOf(min);
                    } else if (min == '\u0000' || max == '\uffff' || (min == '\u0000' && max == '\uffff')) {
                        selectedChar = String.valueOf(getValidNegativeChar(min, max));
                    } else {
                        List<String> characterList = getCharacterFromRange(min, max);
                        selectedChar = characterList.get(random.nextInt(characterList.size()));
                    }

                    edgeLabels.add(selectedChar); // 将选中的字符添加到跳转标签中
                }
            }
            allTransitions.add(edgeLabels);
        }

        // 可选：打印每一步的转换信息（也可以根据需要放开注释）
        // System.out.println("每一步的转换路径:");
        // for (int i = 0; i < allTransitions.size(); i++) {
        //     System.out.println("从状态 " + path.get(i) + " 到状态 " + path.get(i + 1) + ": " + allTransitions.get(i));
        // }

        // 打印路径的转换标签
        System.out.println("路径的转换标签: " + allTransitions);

        // 返回路径组合（根据是否使用成对测试决定）
        return generatePathCombinations(allTransitions, usePairwiseTesting);
    }
    public static List<Integer> bfsShortestPath(Automaton automaton, int start, int target) {
        Map<Integer, State> map = new HashMap<>();
        for (State s : automaton.getStates()) {
            map.put(s.getNumber(), s);
        }

        Queue<List<Integer>> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();
        List<Integer> begin = new ArrayList<>();
        begin.add(start);

        queue.offer(begin);
        visited.add(start);

        while (!queue.isEmpty()) {
            List<Integer> path = queue.poll();
            int last = path.get(path.size() - 1);

            if (last == target) return path;

            for (Transition t : map.get(last).getTransitions()) {
                int next = t.getDest().getNumber();
                if (!visited.contains(next)) {
                    visited.add(next);
                    List<Integer> newPath = new ArrayList<>(path);
                    newPath.add(next);
                    queue.offer(newPath);
                }
            }
        }
        return null;
    }
    public static int findNearestAcceptState(Automaton automaton, int start) {
        Map<Integer, State> map = new HashMap<>();
        for (State s : automaton.getStates()) {
            map.put(s.getNumber(), s);
        }

        Queue<Integer> queue = new LinkedList<>();
        Set<Integer> visited = new HashSet<>();

        queue.offer(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            int cur = queue.poll();
            if (map.get(cur).isAccept()) return cur;

            for (Transition t : map.get(cur).getTransitions()) {
                int next = t.getDest().getNumber();
                if (!visited.contains(next)) {
                    visited.add(next);
                    queue.offer(next);
                }
            }
        }
        return -1;
    }
    /**
     * 从给定的字符范围（min-max）中随机选择一个字符，并满足以下规则：
     * 1. 如果范围的左边界不是 \u0000 或 \uffff，则生成的字符中必须包含左边界字符。
     * 2. 如果范围的右边界不是 \u0000 或 \uffff，则生成的字符中必须包含右边界字符。
     * 3. 保留原本处理大写字母、小写字母和数字的逻辑。
     * 4. 如果左边界和右边界都是 \u0000 或 \uffff，则保留原有的逻辑。
     *
     * @param min 范围的最小字符
     * @param max 范围的最大字符
     * @return 选中的字符列表，确保包含符合条件的边界字符。
     */
    private static List<String> getCharacterFromRange(char min, char max) {
        List<String> result = new ArrayList<>();

        // 如果左边界不是 \u0000 或 \uffff，则必须包含左边界字符
        if (min != '\u0000' && min != '\uffff') {
            result.add(String.valueOf(min));
        }

        // 如果右边界不是 \u0000 或 \uffff，则必须包含右边界字符
        if (max != '\u0000' && max != '\uffff') {
            result.add(String.valueOf(max));
        }

        // 情况 1: 处理整个 Unicode 范围（\u0000-\uffff）
        if (min == '\u0000' && max == '\uffff') {
            result.add(String.valueOf(getValidNegativeChar(min, max)));
            return result;
        }

        // 情况 2: 处理大写字母范围 (A-Z)
        if (min >= 'A' && max <= 'Z') {
            for (char c = min; c <= max; c++) {
                result.add(String.valueOf(c));  // 直接添加范围内的字符
            }
            return result;
        }

        // 情况 3: 处理小写字母范围 (a-z)
        if (min >= 'a' && max <= 'z') {
            for (char c = min; c <= max; c++) {
                result.add(String.valueOf(c));  // 直接添加范围内的字符
            }
            return result;
        }

        // 情况 4: 处理数字范围 (0-9)
        if (min >= '0' && max <= '9') {
            for (char c = min; c <= max; c++) {
                result.add(String.valueOf(c));  // 直接添加范围内的字符
            }
            return result;
        }

        // 情况 5: 处理其他范围
        if (min < max) {
            // 生成在 min 到 max 之间的随机字符
            char randomChar = (char) (min + random.nextInt(max - min + 1));
            // 确保随机生成的字符是可打印字符
            if (isPrintable(randomChar)) {
                result.add(String.valueOf(randomChar));
            }
        }

        return result;
    }

    private static List<String> generatePathCombinations(List<List<String>> edgeLists) {
        List<String> result = new ArrayList<>();

        if (edgeLists.isEmpty()) {
            return result;
        }

        // 只有一跳的路径，不要做成对优化
        if (edgeLists.size() == 1) {
            return new ArrayList<>(edgeLists.get(0));
        }

        // 正常多跳路径，先组合
        generateCombinationsHelper(edgeLists, 0, "", result);

        // 再判断是否优化
//        if (result.size() > 5) {
//            result = generatePairwiseCombinations(edgeLists);
//        }
        result = generatePairwiseCombinations(edgeLists);
        return result;
    }
    private static List<String> generatePathCombinations(List<List<String>> edgeLists, boolean usePairwiseTesting) {
        List<String> result = new ArrayList<>();

        if (edgeLists.isEmpty()) {
            return result;
        }

        // 只有一跳的路径，不做成对优化
        if (edgeLists.size() == 1) {
            return new ArrayList<>(edgeLists.get(0));
        }

        // 正常多跳路径，先组合
        generateCombinationsHelper(edgeLists, 0, "", result);

        return result;
    }

    private static void generateCombinationsHelper(List<List<String>> edgeLists, int index, String currentPath, List<String> result) {
        // 如果已遍历到最后一个转换，添加当前路径到结果集
        if (index == edgeLists.size()) {
            result.add(currentPath);
            return;
        }

        // 遍历当前索引对应的转换字符集
        for (String edge : edgeLists.get(index)) {
            // 构造新的路径
            String newPath = currentPath.isEmpty() ? edge : currentPath + ", " + edge;
            generateCombinationsHelper(edgeLists, index + 1, newPath, result); // 递归生成组合
        }
    }
    private static boolean isParametersPrinted;

    private static List<String> generatePairwiseCombinations(List<List<String>> edgeLists) {
        System.out.println("正在成对测试");
        List<Parameter<?>> parameters = new ArrayList<>();

        // 确保每次调用时打印参数的取值集合，只打印一次
        if (!isParametersPrinted) {
            System.out.println("参数的取值集合:");
            for (int i = 0; i < edgeLists.size(); i++) {
                System.out.println("P" + (i + 1) + "：" + edgeLists.get(i));
            }
            isParametersPrinted = true;
        }

        // 生成 Pairwise 组合
        for (int i = 0; i < edgeLists.size(); i++) {
            parameters.add(new Parameter<>("edge_" + (i + 1), edgeLists.get(i)));
        }

        Pairwise pairwise = new Pairwise.Builder()
                .withParameters(parameters)
                .build();

        //  成对后：Pairwise 组合
        System.out.println("\n【成对后：Pairwise 组合结果】");

        List<String> pairwiseCombinations = new ArrayList<>();
        int index = 1;

        for (Case pair : pairwise) {
            String combination = pair.values().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));

            System.out.println("用例 " + (index++) + "： " + combination);
            pairwiseCombinations.add(combination);
        }

        System.out.println("\n【成对测试完成】生成用例数 = " + pairwiseCombinations.size());
        System.out.println("=================================\n");



        return pairwiseCombinations;
    }



    // 一次性空格生成标记
    private static boolean spaceGenerated = false;

    private static char getValidNegativeChar(char min, char max) {
        Random random = new Random();

        // ===== ⭐ 一次性空格优先规则 =====
        // 条件：
        // 1. 范围包含空格
        // 2. 不包含数字 / 大写字母 / 小写字母
        // 3. 空格尚未生成过
        if (!spaceGenerated
                && min <= ' ' && max >= ' '
                && !(min <= '0' && max >= '9')
                && !(min <= 'A' && max >= 'Z')
                && !(min <= 'a' && max >= 'z')) {

            spaceGenerated = true;
            return ' ';
        }

        // ===== 原有逻辑：整个 Unicode 范围 =====
        if (min == '\u0000' && max == '\uffff') {
            double digitProbability = 0.5; // 50% 概率生成数字
            if (random.nextDouble() < digitProbability) {
                return (char) ('0' + random.nextInt(10));
            } else {
                char ch;
                do {
                    ch = (char) random.nextInt(65536);
                } while (!isPrintable(ch));
                return ch;
            }
        }

        // ===== 原有逻辑：优先返回左边界 =====
        if (min != '\u0000' && min != '\uffff') {
            return min;
        }

        // ===== 原有逻辑：否则返回右边界 =====
        if (max != '\u0000' && max != '\uffff') {
            return max;
        }

        // ===== 原有逻辑：随机可打印字符 =====
        char ch;
        do {
            ch = (char) (min + random.nextInt(max - min + 1));
        } while (!isPrintable(ch));

        return ch;
    }


    /**
     * 检查字符是否是可打印的
     */
    private static boolean isPrintable(char ch) {
        return (ch >= 32 && ch <= 126) || (ch >= '\u4E00' && ch <= '\u9FFF') ||  // 支持ASCII可打印字符以及汉字
                (ch >= '\u00A0' && ch <= '\u00FF') || // 其他常见的可打印字符
                (ch >= '\u2010' && ch <= '\u2027') || // 标点符号范围
                (ch >= '\u2100' && ch <= '\u214F');   // 字母数字范围等其他可打印符号
    }
}