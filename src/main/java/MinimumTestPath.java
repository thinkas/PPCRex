import java.util.*;
/**
 * MinimumTestPath
 *
 * 将最小测试路径（基于主路径关系图）
 * 还原为原始控制流图上的测试路径
 */
public class MinimumTestPath {
    // 定义一个静态变量，用于存储所有最终结果
    public static List<List<Integer>> finalReplacedEdges = new ArrayList<>();

    public static int a;//a是初始节点
    public static List<Integer> b;//b是终止节点

    private static List<List<String>> edges = new ArrayList<>();
    private static List<String> edges1 = new ArrayList<>();
    private static Map<String, List<Integer>> pathMappings = new HashMap<>();

    public static void Finder3(int a, List<Integer> b) {
        MinimumTestPath.a = a;//a是初始节点
        MinimumTestPath.b = b;//b是终止节点
    }

    public static void Finder1(List<String> paths) {
        edges1.clear();
        edges1.addAll(paths);
    }

    public static void Finder(List<List<String>> newEdges) {
        edges.clear();
        edges.addAll(newEdges);
    }

    public static void main(String[] args) {
        System.out.println("S是初始状态，S的值是" + MinimumTestPath.a);
        System.out.println("T是终止状态，T的值是" + MinimumTestPath.b);
        System.out.println("原始图MinimumTestPath主路径集：");
        for (String path : edges1) {
            System.out.println(path);
        }
        System.out.println("-------------------");
        System.out.println("全局存储的所有简化最小测试路径边为：");
        int index1 = 1;
        for (List<String> edge : edges) {
            System.out.println(index1 + ": " + edge);
            index1++;
        }
        System.out.println("还原回原始图的最小测试路径为：");
        int index = 1;
        for (List<String> edge : edges) {
            List<Integer> replacedEdge = replaceAndSimplify(edge);
            System.out.println(index + ": " + replacedEdge);
            index++;
        }

    }

    // 判断 replacedEdge 的后缀是否与 replacement 的前缀完全相等
    private static boolean isFullyOverlapping(List<Integer> replacedEdge, List<Integer> replacement) {
        int edgeSize = replacedEdge.size();
        int repSize = replacement.size();

        // 如果 replacedEdge 的后缀长度小于 replacement，不可能完全匹配
        if (edgeSize < repSize) {
            return false;
        }

        // 取 replacedEdge 的后缀，与 replacement 完全比对
        List<Integer> suffix = replacedEdge.subList(edgeSize - repSize, edgeSize);
        return suffix.equals(replacement);
    }

    private static List<Integer> replaceAndSimplify(List<String> edge) {
        List<Integer> replacedEdge = new ArrayList<>();
        System.out.println("处理路径: " + edge);

        for (int i = 0; i < edge.size(); i++) {
            String node = edge.get(i);

            if (node.equals("S")) {
                replacedEdge.add(MinimumTestPath.a);
                System.out.println("S 表示初始状态 " + MinimumTestPath.a + "，替换后为: " + replacedEdge);
            }
            else if (node.equals("T")) {
                List<Integer> endStates = MinimumTestPath.b;

                // 检查 replacedEdge 的最后一个元素是否已经是终止状态
                if (!replacedEdge.isEmpty() && endStates.contains(replacedEdge.get(replacedEdge.size() - 1))) {
                    System.out.println("发现路径最后一个节点是终止状态，终止状态已存在，不再添加 T");
                    continue;  // 继续循环，不添加 T
                }

                // 如果没有终止状态，则添加
                replacedEdge.addAll(endStates);
                System.out.println("T 表示终止状态 " + endStates + "，替换后为: " + replacedEdge);
            }
            else {
                // 从 edges1 中查找对应的主路径集
                List<Integer> replacement = findReplacementInEdges1(node);
                if (replacement != null) {
                    System.out.println(node + " 对应主路径集: " + replacement);

                    // 检查重叠部分
                    int overlapIndex = findOverlapIndex(replacedEdge, replacement);

                    // 额外检查 replacedEdge 的后缀是否与 replacement 完全一致
                    if (isFullyOverlapping(replacedEdge, replacement)) {
                        System.out.println("replacedEdge 的后缀完全等于 replacement，不进行添加");
                        continue; // 跳过添加
                    }

                    if (overlapIndex != -1) {
                        replacedEdge.addAll(replacement.subList(overlapIndex, replacement.size()));
                        System.out.println("有重叠部分，从索引 " + overlapIndex + " 开始添加，替换后为: " + replacedEdge);
                    } else {
                        replacedEdge.addAll(replacement);
                        System.out.println("无重叠部分，直接添加，替换后为: " + replacedEdge);
                    }
                } else {
                    System.out.println("未找到 " + node + " 的映射，跳过");
                }
            }
        }

        // **终止状态检查，防止重复**
        List<Integer> endStates = MinimumTestPath.b;
        if (!replacedEdge.isEmpty() && endStates.contains(replacedEdge.get(replacedEdge.size() - 1))) {
            System.out.println("终止状态已存在，保留最后一个终止状态");
        } else {
            replacedEdge.addAll(endStates);
            System.out.println("添加终止状态 " + endStates + "，替换后为: " + replacedEdge);
        }

        // 将最终结果存入静态变量（存储多条路径）
        MinimumTestPath.finalReplacedEdges.add(replacedEdge);
        return replacedEdge;
    }



    // 从 edges1 中查找对应的主路径集
    private static List<Integer> findReplacementInEdges1(String node) {
        for (String path : edges1) {
            if (path.startsWith(node + ":")) {
                // 提取路径数据并转成 List<Integer>
                String[] parts = path.split(":");
                if (parts.length > 1) {
                    String[] values = parts[1].trim().replace("[", "").replace("]", "").split(",");
                    List<Integer> result = new ArrayList<>();
                    for (String value : values) {
                        result.add(Integer.parseInt(value.trim()));
                    }
                    return result;
                }
            }
        }
        return null; // 未找到匹配路径
    }
    // 清空所有静态数据，防止多个对象之间数据干扰
    public static void clear() {
        finalReplacedEdges.clear();
        pathMappings.clear();
        a = 0;
        b = new ArrayList<>();
    }

    // 检查前后路径的重叠部分，返回重叠部分在 replacement 中的起始索引
    private static int findOverlapIndex(List<Integer> replacedEdge, List<Integer> replacement) {
        for (int i = 1; i <= replacedEdge.size(); i++) {
            // 获取 replacedEdge 的后缀
            List<Integer> suffix = replacedEdge.subList(replacedEdge.size() - i, replacedEdge.size());
            // 获取 replacement 的前缀
            List<Integer> prefix = replacement.subList(0, Math.min(i, replacement.size()));

            if (suffix.equals(prefix)) {
                return i; // 找到重叠部分，返回 replacement 的起始索引
            }
        }
        return -1; // 没有重叠
    }
}