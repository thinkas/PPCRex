import java.util.*;
/**
 * PathReduce
 *
 * 对最小流算法生成的测试路径进行：
 * 1) 非连通路径修复
 * 2) 环路冗余消除
 */
public class PathReduce {
    /** Prime Path Relation Graph 的边集 */
    private static List<String[]> E = new ArrayList<>();
    /** 邻接表表示的关系图 G */
    private static Map<String, List<String>> G = new HashMap<>();
    /** 修复后的路径集合 */
    private static List<List<String>> R = new ArrayList<>(); // 添加 R 来存储修正后的路径

    // 添加新的边到全局边列表
    public static void init(List<String[]> newEdges) {
        E.clear(); // 清空之前的边
        E.addAll(newEdges);
        buildG(); // 构建邻接表
    }

    // 构建邻接表表示的图
    private static void buildG() {
        G.clear();
        for (String[] edge : E) {
            G.computeIfAbsent(edge[0], k -> new ArrayList<>()).add(edge[1]);
        }
    }

    public static void main(String[] args) {
        // 打印全局存储的边
        System.out.println("全局存储的主路径关系图边为：");
        int index = 1;
        for (String[] edge : E) {
            System.out.println(index + ": " + Arrays.toString(edge));
            index++;
        }

        System.out.println("读取到的最小测试路径为：");
        if (UnrollLoop.SharedPaths_paths != null && !UnrollLoop.SharedPaths_paths.isEmpty()) {
            // 先打印所有原始路径
            List<String> allPaths = UnrollLoop.SharedPaths_paths;
            for (String path : allPaths) {
                System.out.println("原始路径：" + path);
            }

            // 一次性扫描所有原始路径不连通的边，并修复它们
            Set<String[]> nonConnectedEdges = new HashSet<>();
            for (String path : allPaths) {
                List<String> correctedPath = correctPath(path);
                if (correctedPath == null) {
                    System.out.println("路径 " + path + " 不连通，无法修复。");
                } else {
                    R.add(correctedPath); // 保存修正后的路径
                }
            }

            // 输出修正后的路径
            System.out.println("\n修正后的路径：");
            for (List<String> path : R) {
                System.out.println(path);
            }

            // 统一打印修正后的路径
            printAndClearCorrectedPaths();
        } else {
            System.out.println("没有路径数据！");
        }
    }

    /**
     * 统一打印修正后的路径并清理存储。
     */
    private static void printAndClearCorrectedPaths() {
        System.out.println("\n统一打印所有修正连通后的路径：");
        for (List<String> path : R) {
            System.out.println("修正后的最小测试连通路径：" + path);
        }
        // 传递 R 给 processPaths 方法
        List<String> allPaths = new ArrayList<>();
        for (List<String> path : R) {
            allPaths.add(String.join(",", path));  // 将路径转换为字符串格式
        }
        // 调用处理方法
        processPaths(allPaths);  // 调用处理方法，传递修正后的路径
        R.clear(); // 清空修正路径存储
        System.out.println("修正后的路径已清理。");
    }

    // 用于记录每个环路出现的次数
    private static Map<String, Integer> cycleCountMap = new HashMap<>();
    private static Map<String, Integer> subPathCountMap = new HashMap<>();

    public static void processPaths(List<String> paths) {
        System.out.println("\n开始对每条路径查找环路（首尾相同的有向回路）...");

        cycleCountMap.clear();
        subPathCountMap.clear();

        // 第一次查找环路（用于简化）
        for (String pathStr : paths) {
            List<String> path = new ArrayList<>(Arrays.asList(pathStr.split(",")));
            System.out.println("\n处理路径：" + path);

            for (int i = 0; i < path.size(); i++) {
                findCyclesFromNode(path, i, true, null); // 统计环路次数
            }
        }

        // 简化路径
        System.out.println("\n开始简化路径...");
        Map<String, Integer> cycleSeenCount = new HashMap<>();
        List<List<String>> simplifiedPaths = new ArrayList<>();

        for (String pathStr : paths) {
            List<String> path = new ArrayList<>(Arrays.asList(pathStr.split(",")));
            List<String> simplifiedPath = new ArrayList<>();
            int i = 0;

            while (i < path.size()) {
                boolean cycleMatched = false;

                for (String cycleKey : cycleCountMap.keySet()) {
                    List<String> cycleNodes = Arrays.asList(cycleKey.split("->"));
                    int len = cycleNodes.size();

                    if (i + len <= path.size() && path.subList(i, i + len).equals(cycleNodes)) {
                        int seen = cycleSeenCount.getOrDefault(cycleKey, 0);
                        if (seen == 0) {
                            simplifiedPath.addAll(cycleNodes); // 保留完整环路
                        } else {
                            simplifiedPath.add(cycleNodes.get(0)); // 只保留起点
                        }
                        cycleSeenCount.put(cycleKey, seen + 1);
                        i += len;
                        cycleMatched = true;
                        break;
                    }
                }

                if (!cycleMatched) {
                    simplifiedPath.add(path.get(i));
                    i++;
                }
            }

            simplifiedPaths.add(simplifiedPath);
            System.out.println("原始路径：" + path);
            System.out.println("简化后路径：" + simplifiedPath);
        }
// 在简化路径后添加
        Map<String, Integer> globalCycleCountMap = new HashMap<>();
        Map<String, Boolean> firstOccurrenceMap = new HashMap<>();

        System.out.println("\n简化后路径中统计全局合法环路出现次数...");
        for (List<String> simplifiedPath : simplifiedPaths) {
            for (int i = 0; i < simplifiedPath.size(); i++) {
                String startNode = simplifiedPath.get(i);
                List<String> currentPath = new ArrayList<>();
                Set<String> visited = new HashSet<>();

                for (int j = i; j < simplifiedPath.size(); j++) {
                    String currentNode = simplifiedPath.get(j);
                    if (visited.contains(currentNode) && !currentNode.equals(startNode)) break;

                    currentPath.add(currentNode);
                    visited.add(currentNode);

                    if (j != i && currentNode.equals(startNode) && currentPath.size() > 2) {
                        String cycleKey = String.join("->", currentPath);
                        int count = globalCycleCountMap.getOrDefault(cycleKey, 0) + 1;
                        globalCycleCountMap.put(cycleKey, count);
                        if (!firstOccurrenceMap.containsKey(cycleKey)) {
                            firstOccurrenceMap.put(cycleKey, true); // 先标记第一次出现
                        }
                        System.out.println("全局合法环路：" + currentPath + " 当前出现次数：" + count);
                        break;
                    }
                }
            }
        }

// 替换所有简化路径中的频繁环路（只替换第二次及之后）
        System.out.println("\n开始根据全局频率替换环路为起点（只替换重复出现的）...");
        for (List<String> path : simplifiedPaths) {
            for (Map.Entry<String, Integer> entry : globalCycleCountMap.entrySet()) {
                String cycleKey = entry.getKey();
                List<String> loop = Arrays.asList(cycleKey.split("->"));
                String start = loop.get(0);

                int i = 0;
                while (i <= path.size() - loop.size()) {
                    if (path.subList(i, i + loop.size()).equals(loop)) {
                        if (firstOccurrenceMap.getOrDefault(cycleKey, false)) {
                            // 首次出现，不替换
                            firstOccurrenceMap.put(cycleKey, false); // 标记为已处理
                            System.out.println("首次出现环路不替换：" + loop);
                            i += loop.size();
                            continue;
                        }

                        // 替换
                        System.out.println("替换环路：" + loop + " -> 起点：" + start);
                        System.out.println("替换前路径：" + path);
                        for (int k = 0; k < loop.size(); k++) path.remove(i);
                        path.add(i, start);
                        System.out.println("替换后路径：" + path);
                    } else {
                        i++;
                    }
                }
            }
        }

// 输出最终路径
        System.out.println("\n最终处理后的路径：");
        for (List<String> path : simplifiedPaths) {
            System.out.println(path);
        }

        // 第二次查找环路（不统计环路次数，而是提取中间子路径）
        System.out.println("\n在简化路径中查找环路...");
        for (List<String> simplifiedPath : simplifiedPaths) {
            for (int i = 0; i < simplifiedPath.size(); i++) {
                findCyclesFromNode(simplifiedPath, i, false, simplifiedPaths);
            }
        }
        // 查找出现频率大于1的子路径对应的环路，并替换为起点
        System.out.println("\n开始根据子路径频率替换环路为起点...");
        Map<List<String>, String> loopsToReplace = new HashMap<>();

        boolean hasFrequentSubPath = true;

        while (hasFrequentSubPath) {
            hasFrequentSubPath = false;

            // 第三次遍历简化路径，识别频繁子路径对应的环路
            for (List<String> path : simplifiedPaths) {
                for (int i = 0; i < path.size(); i++) {
                    String startNode = path.get(i);
                    List<String> currentPath = new ArrayList<>();
                    Set<String> visited = new HashSet<>();

                    for (int j = i; j < path.size(); j++) {
                        String currentNode = path.get(j);
                        if (visited.contains(currentNode) && !currentNode.equals(startNode)) break;

                        currentPath.add(currentNode);
                        visited.add(currentNode);

                        if (j != i && currentNode.equals(startNode)) {
                            if (currentPath.size() > 2) {
                                List<String> subPath = currentPath.subList(1, currentPath.size() - 1);
                                int freq = countSubPathOccurrences(simplifiedPaths, subPath);
                                if (freq > 1) {
                                    loopsToReplace.put(new ArrayList<>(currentPath), startNode);
                                }
                            }
                            break;
                        }
                    }
                }
            }

            // 执行替换
            for (List<String> path : simplifiedPaths) {
                for (Map.Entry<List<String>, String> entry : loopsToReplace.entrySet()) {
                    List<String> loop = entry.getKey();
                    String start = entry.getValue();
                    List<String> subPath = loop.subList(1, loop.size() - 1);

                    int i = 0;
                    while (i <= path.size() - loop.size()) {
                        if (path.subList(i, i + loop.size()).equals(loop)) {
                            int freq = countSubPathOccurrences(simplifiedPaths, subPath);
                            if (freq > 1) {
                                System.out.println("替换环路：" + loop + " -> 起点：" + start);
                                System.out.println("替换前路径：" + path);
                                for (int k = 0; k < loop.size(); k++) path.remove(i);
                                path.add(i, start);
                                System.out.println("替换后路径：" + path);
                                hasFrequentSubPath = true; // 标记继续循环
                                // 不自增 i，当前位置可能还有下一个环路
                            } else {
                                i++;
                            }
                        } else {
                            i++;
                        }
                    }
                }
            }
        }

// 循环结束，所有子路径出现次数 ≤ 1

        System.out.println("\n最终处理后的路径：");
        for (List<String> path : simplifiedPaths) {
            System.out.println(path);
        }

        MinimumTestPath.Finder(simplifiedPaths);
        MinimumTestPath.main(null);
    }

    private static void findCyclesFromNode(List<String> path, int startIndex, boolean countCycles, List<List<String>> allPaths) {
        String startNode = path.get(startIndex);
        List<String> currentPath = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (int i = startIndex; i < path.size(); i++) {
            String currentNode = path.get(i);

            if (visited.contains(currentNode) && !currentNode.equals(startNode)) {
                return;
            }

            currentPath.add(currentNode);
            visited.add(currentNode);

            if (i != startIndex && currentNode.equals(startNode)) {
                String cycleKey = String.join("->", currentPath);

                if (countCycles) {
                    // 第一次统计完整环路次数
                    int cycleCount = cycleCountMap.getOrDefault(cycleKey, 0) + 1;
                    cycleCountMap.put(cycleKey, cycleCount);
                    System.out.println("发现合法环路：" + currentPath + " 当前出现" + cycleCount + "次");
                } else {
                    // 第二次仅打印中间子路径，并统计频率
                    if (currentPath.size() > 2) {
                        List<String> subPath = currentPath.subList(1, currentPath.size() - 1);
                        int freq = countSubPathOccurrences(allPaths, subPath);
                        System.out.println("发现合法环路：" + currentPath + " 中间子路径：" + subPath + " 出现次数" + freq + "次");
                    } else {
                        System.out.println("发现合法环路：" + currentPath + " 无中间子路径");
                    }
                }
                return;
            }
        }
    }

    private static int countSubPathOccurrences(List<List<String>> allPaths, List<String> subPath) {
        int count = 0;
        int len = subPath.size();

        for (List<String> path : allPaths) {
            for (int i = 0; i <= path.size() - len; i++) {
                if (path.subList(i, i + len).equals(subPath)) {
                    count++;
                }
            }
        }
        return count;
    }



    public static List<String> correctPath(String path) {
        path = path.replaceAll("\\[|\\]|\\s", "");
        String[] nodes = path.split(",");
        List<String> correctedPath = new ArrayList<>();
        correctedPath.add(nodes[0]); // 添加起点

        for (int i = 0; i < nodes.length - 1; i++) {
            String currentNode = nodes[i];
            String nextNode = nodes[i + 1];

            if (isDirectlyConnected(currentNode, nextNode)) {
                correctedPath.add(nextNode); // 直接连通
            } else {
                // 尝试修复路径
                List<String> intermediatePath = findIntermediatePath(currentNode, nextNode);
                if (intermediatePath == null) {
                    return null; // 无法修复
                }
                correctedPath.addAll(intermediatePath);
                correctedPath.add(nextNode);
            }
        }
        return correctedPath;
    }


    private static boolean isDirectlyConnected(String node1, String node2) {
        return G.containsKey(node1) && G.get(node1).contains(node2);
    }

    private static List<String> findIntermediatePath(String start, String target) {
        if (start.equals(target)) {
            System.out.println("跳过自环: (" + start + " -> " + target + ")");
            return new ArrayList<>(); // 直接返回空路径，避免死循环
        }
        Queue<List<String>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        queue.add(Collections.singletonList(start));
        visited.add(start);

        while (!queue.isEmpty()) {
            List<String> path = queue.poll();
            String lastNode = path.get(path.size() - 1);

            if (G.containsKey(lastNode)) {
                for (String neighbor : G.get(lastNode)) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        List<String> newPath = new ArrayList<>(path);
                        newPath.add(neighbor);

                        if (neighbor.equals(target)) {
                            addEdge(start, neighbor); // 添加新边
                            return newPath.subList(1, newPath.size() - 1); // 返回中间节点
                        }
                        queue.add(newPath);
                    }
                }
            }
        }
        return null; // 无法连通
    }

    private static final List<String[]> repairedEdges = new ArrayList<>();

    private static void addEdge(String from, String to) {
        repairedEdges.add(new String[]{from, to});
        System.out.println("记录修复边: (" + from + ", " + to + ")");
    }


}
