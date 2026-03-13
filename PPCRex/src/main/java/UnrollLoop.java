import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * UnrollLoop
 * --------------------------------------------------
 * 该类负责对“最小测试路径”中出现的循环体 Vi 进行展开（Unroll）。
 *
 * 核心作用：
 * 1. 收集并管理图中的环路信息（Vi → Cycle）
 * 2. 区分 G2（原有向图）与 G3（去环后的无环图）
 * 3. 对包含 Vi 的最小测试路径进行循环体替换
 * 4. 将所有路径展开为不含 Vi 的最终测试路径
 *
 */
public class UnrollLoop {
    private static int finalTestPathCount = 0; // 全局变量存储路径数量

    // 新增存储G3图的边信息
    private static List<String[]> g3Edges = new ArrayList<>();
    // 新增存储G2图的边信息
    private static List<String[]> g2Edges = new ArrayList<>();

    private static Map<String, List<String>> cycleMap = new HashMap<>();

    // 创建一个列表来存储所有路径
    private static List<String> allPaths = new ArrayList<>();
    public static void clear() {
        g2Edges.clear();       // 清空 G2 的边
        g3Edges.clear();       // 清空 G3 的边
        cycleMap.clear();      // 清空环路数据
        allPaths.clear();      // 清空所有路径
    }

    public static void main(String[] args) {
//        // 1. 先处理环路并输出环路信息
        printAllCyclePaths();  // 输出所有环路

        // 2. 输出存储的最小测试路径
        printAllStoredPaths();

        // 4. 输出 G3 和 G2 的图结构
        printGraphEdgesG3(); // 打印无环图G3的边
        printGraphEdgesG2(); // 打印有向图G2的边

        // 5. 获取最终版的最小测试路径
        getFinalTestPath();

        expandPathWithVi();
        UnrollLoop.clear(); // 在每次处理正则表达式前清空状态

    }

    public static void printSinglePathString(String path) {
        allPaths.add(path);

    }

//     打印所有存储的路径
    public static void printAllStoredPaths() {
        System.out.println("所有存储的最小测试路径：");
        for (String storedPath : allPaths) {
            System.out.println(storedPath);
        }
    }

//     打印所有存储的环路路径
    public static void printAllCyclePaths() {
        System.out.println("所有存储的环路路径：");
        for (Map.Entry<String, List<String>> entry : cycleMap.entrySet()) {
            System.out.println(entry.getKey() + " 存储了环路: " + entry.getValue());
        }
    }

    // 新增的 addCycle 方法，用于 GraphCycleFinder 类存储环路信息
    public static void addCycle(String vertex, List<String> cycle) {
        cycleMap.put(vertex, cycle);  // 将 Vi 顶点和对应的环路存储在 cycleMap 中
    }

//     新增方法：存储并打印G2图的边
    public static void printGraphEdgesG2() {
        for (String[] edge : g2Edges) {
            RemoveRedundancy.addEdgeToG2(edge[0], edge[1]);  // 直接调用添加到G2的方法
        }
    }

//     新增方法：存储并打印G2图的边
    public static void printGraphEdgesG3() {
        System.out.println("无换图图 G3 的边有：");
        for (String[] edge : g3Edges) {
            System.out.println("(" + edge[0] + ", " + edge[1] + ")");
        }
    }

    // 新增方法：用于添加边到G2
    public static void addEdgeToG2(String from, String to) {
        g2Edges.add(new String[]{from, to});
    }

    // 新增方法：用于添加边到G3
    public static void addEdgeToG3(String from, String to) {
        g3Edges.add(new String[]{from, to});
    }

    public static void getFinalTestPath() {
        Pattern viPattern = Pattern.compile("V\\d+");

        for (String path : allPaths) {
            // 确保 path 中没有额外的描述性文本，比如 "最小测试路径: "
            path = path.trim(); // 去掉前后的空格
            if (path.startsWith("最小测试路径: ")) {
                path = path.replace("最小测试路径: ", ""); // 去掉多余的前缀
            }

            String[] nodes = path.replace("[", "").replace("]", "").split(",\\s*");
            boolean containsVi = false;
            String foundVi = "";
            int viIndex = -1;  // 用于记录Vi的下标

            // 遍历路径中的所有节点，找到Vi
            for (int i = 0; i < nodes.length; i++) {
                Matcher matcher = viPattern.matcher(nodes[i]);
                if (matcher.find()) {
                    containsVi = true;
                    foundVi = matcher.group();
                    viIndex = i;  // 记录Vi的下标
                    break;
                }
            }

            // 如果路径中含有Vi，则处理它
            if (containsVi) {
                System.out.println("路径 " + path + " 含有 " + foundVi + "，正在展开该环路。");

                // 判断Vi的前一个元素
                if (viIndex > 0) {
                    String previousNode = nodes[viIndex - 1];  // 获取Vi前面的元素
                }
            }
        }
    }

    static List<String> SharedPaths_paths = new ArrayList<>(); // 最终路径

    public static void expandPathWithVi() {
        Pattern viPattern = Pattern.compile("V\\d+");
        Map<String, List<String>> g2Cache = buildG2Cache(g2Edges);

        List<String> newAllPaths = new ArrayList<>();

        for (String pathStr : allPaths) {
            pathStr = pathStr.replace("最小测试路径: ", "");
            String[] nodes = pathStr.replace("[", "").replace("]", "").split(",\\s*");
            List<String> nodeList = new ArrayList<>(Arrays.asList(nodes));

            boolean expanded = true;
            while (expanded) {
                expanded = false;
                for (int i = 0; i < nodeList.size(); i++) {
                    Matcher matcher = viPattern.matcher(nodeList.get(i));
                    if (matcher.matches()) {
                        String vi = nodeList.get(i);
                        String prev = (i > 0) ? nodeList.get(i - 1) : null;
                        if (prev != null) {
                            List<String> cycle = cycleMap.get(vi);
                            if (cycle == null) continue;

                            System.out.println("当前处理路径: " + nodeList);
                            System.out.println("发现循环体 " + vi + "，前驱节点为 " + prev);
                            System.out.println("循环体 " + vi + " 存储的环路: " + cycle);

                            List<String> g2Targets = g2Cache.getOrDefault(prev, Collections.emptyList());
                            String directTarget = findDirectReachable(prev, cycle, g2Cache);

                            if (directTarget != null) {
                                System.out.println("找到前驱节点 " + prev + " 可到达的节点: " + directTarget);
                                List<String> reordered = reorderCyclePath(cycle, directTarget);
                                System.out.println("重排序后的循环体: " + reordered);

                                List<String> newPath = new ArrayList<>();
                                newPath.addAll(nodeList.subList(0, i));
                                newPath.addAll(reordered);
                                newPath.addAll(nodeList.subList(i + 1, nodeList.size()));

                                System.out.println("展开前: " + nodeList);
                                System.out.println("展开后: " + newPath);
                                nodeList = newPath;
                                expanded = true;
                                break;
                            } else {
                                // Direct target is null, so expand the cycle body anyway
                                System.out.println("前驱节点 " + prev + " 无法直接或递归到达循环体内节点，直接展开该循环体。");

                                List<String> reordered = reorderCyclePath(cycle, prev);
                                List<String> newPath = new ArrayList<>();
                                newPath.addAll(nodeList.subList(0, i));
                                newPath.addAll(reordered);
                                newPath.addAll(nodeList.subList(i + 1, nodeList.size()));

                                System.out.println("展开前: " + nodeList);
                                System.out.println("展开后: " + newPath);
                                nodeList = newPath;
                                expanded = true;
                                break;
                            }
                        } else {
                            System.out.println("循环体 " + vi + " 是路径第一个节点，无法展开。");
                        }
                    }
                }
            }
            // 完全展开
            newAllPaths.add("[" + String.join(", ", nodeList) + "]");
            System.out.println("最终展开结果: " + newAllPaths.get(newAllPaths.size() - 1));
            System.out.println("-------------------------------------------------------");
        }

        allPaths.clear();
        allPaths.addAll(newAllPaths);
        System.out.println("所有路径中循环体已完全展开✅");
        System.out.println("最终所有展开的路径：");
        for (String finalPath : newAllPaths) {
            System.out.println(finalPath);
        }

        SharedPaths_paths = new ArrayList<>(allPaths);

        finalTestPathCount = allPaths.size();
        System.out.println("总路径数量: " + finalTestPathCount);
        PathReduce.main(new String[]{});
    }


    /**
     * 递归寻找循环体中可以被前驱到达的节点
     */
    public static String findDirectReachable(String prev, List<String> cycle, Map<String, List<String>> g2Cache) {
        Pattern viPattern = Pattern.compile("V\\d+");
        List<String> g2Targets = g2Cache.getOrDefault(prev, Collections.emptyList());

        for (String node : cycle) {
            if (viPattern.matcher(node).matches()) {
                List<String> innerCycle = cycleMap.get(node);
                if (innerCycle != null) {
                    String foundInInner = findDirectReachable(prev, innerCycle, g2Cache);
                    if (foundInInner != null) {
                        return node; // 返回外层循环体节点名（比如V2）
                    }
                }
            } else {
                if (g2Targets.contains(node)) {
                    return node; // 找到普通节点
                }
            }
        }
        return null; // 没找到
    }

    /**
     * 重排序循环体，确保startNode作为起点，且顺序符合原环路连通关系
     */
    public static List<String> reorderCyclePath(List<String> cyclePath, String startNode) {
        int size = cyclePath.size();
        // 如果已经闭环，去掉最后一个元素
        if (size > 1 && cyclePath.get(0).equals(cyclePath.get(size - 1))) {
            cyclePath = new ArrayList<>(cyclePath.subList(0, size - 1));
            size = cyclePath.size();
        }

        int idx = cyclePath.indexOf(startNode);
        if (idx == -1) {
            // 加闭环
            List<String> fallback = new ArrayList<>(cyclePath);
            fallback.add(fallback.get(0));
            return fallback;
        }

        List<String> reordered = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            reordered.add(cyclePath.get((idx + i) % size));
        }

        // 最后补上首节点，形成新的闭环
        reordered.add(reordered.get(0));
        return reordered;
    }

    public static Map<String, List<String>> buildG2Cache(List<String[]> g2Edges) {
        Map<String, List<String>> cache = new HashMap<>();
        for (String[] edge : g2Edges) {
            cache.computeIfAbsent(edge[0], k -> new ArrayList<>()).add(edge[1]);
        }
        return cache;
    }

}
