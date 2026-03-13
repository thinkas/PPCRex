import java.util.*;
/**
 * RemoveRedundancy
 *
 * 功能：
 * 对最小流算法生成的测试路径进行环路冗余消除。
 * 对于已经出现过的相同环路，仅保留一次完整展开，
 */
public class RemoveRedundancy {
    // 新增存储G2图的边信息
    private static List<String[]> g2Edges = new ArrayList<>();

    // 新增方法：用于添加边到G2
    public static void addEdgeToG2(String from, String to) {
        g2Edges.add(new String[]{from, to});
    }

    // 方法：简化路径中的环路
    private static List<String> simplifyPathWithCycles(List<String> nodes, Set<String> processedCycles) {
        List<String> simplifiedPath = new ArrayList<>(nodes);  // 初始化为当前路径
        Set<String> visited = new HashSet<>();  // 用于DFS访问的节点
        Stack<String> stack = new Stack<>();    // 用于DFS的栈

        for (int i = 0; i < nodes.size(); i++) {
            if (visited.contains(nodes.get(i))) {
                continue;  // 如果节点已经访问过，跳过
            }
            if (dfsAndSimplify(nodes, i, visited, stack, simplifiedPath, processedCycles)) {
                break;  // 找到环路后执行简化
            }
        }

        return simplifiedPath;
    }

    // DFS查找环路并简化后续路径中的重复环路
    private static boolean dfsAndSimplify(List<String> nodes, int currentIndex, Set<String> visited, Stack<String> stack,
                                          List<String> simplifiedPath, Set<String> processedCycles) {
        String currentNode = nodes.get(currentIndex);

        // 如果当前节点已经在栈中，说明我们发现了一个环路
        if (stack.contains(currentNode)) {
            // 打印并保存环路
            List<String> cycle = getCycle(stack, currentNode);
            String cycleKey = String.join(",", cycle);

//            System.out.println("发现环路: " + cycleKey);

            // 如果这个环路已经被处理过，则简化它
            if (processedCycles.contains(cycleKey)) {
                // 简化路径中所有环路为环路的第一个节点
                shrinkCycle(simplifiedPath, cycle);
            } else {
                // 第一次遇到的环路，加入已处理集合
                processedCycles.add(cycleKey);
            }

            return true;
        }

        // 将当前节点标记为已访问，并加入栈
        visited.add(currentNode);
        stack.push(currentNode);

        // 递归访问路径的下一个节点
        if (currentIndex + 1 < nodes.size()) {
            if (dfsAndSimplify(nodes, currentIndex + 1, visited, stack, simplifiedPath, processedCycles)) {
                return true;  // 如果发现了环路，停止递归
            }
        }

        // 如果没有发现环路，将当前节点从栈中移除
        stack.pop();
        return false;
    }

    // 从栈中提取环路
    private static List<String> getCycle(Stack<String> stack, String startNode) {
        List<String> cycle = new ArrayList<>();
        boolean inCycle = false;

        // 遍历栈中的节点，找到环路部分
        for (String node : stack) {
            if (node.equals(startNode)) {
                inCycle = true;  // 从环路的起点开始记录
            }
            if (inCycle) {
                cycle.add(node);  // 记录环路中的节点
            }
        }
        cycle.add(startNode);  // 补充起点，形成完整环路

        return cycle;
    }

    // 将路径中的环路简化为环路的第一个节点
    private static void shrinkCycle(List<String> path, List<String> cycle) {
        String firstNode = cycle.get(0);
        boolean simplified = false;

        // 使用迭代器来避免路径修改时索引失效
        Iterator<String> iterator = path.iterator();
        int index = 0;

        while (iterator.hasNext()) {
            String current = iterator.next();

            // 检查是否匹配环路的第一个节点
            if (current.equals(firstNode)) {
                // 检查路径中的子列表是否匹配环路
                if (index + cycle.size() <= path.size() && path.subList(index, index + cycle.size()).equals(cycle)) {
                    // 移除整个环路并替换为第一个节点
                    for (int i = 1; i < cycle.size(); i++) {
                        iterator.next();
                        iterator.remove();  // 删除环路的其他节点
                    }
                    simplified = true;
                }
            }

            // 更新索引
            index++;
        }

    }
}
