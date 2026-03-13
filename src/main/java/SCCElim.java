import java.util.*;
/**
 * SCCElim 用于对主路径关系图中环进行消解，最终将主路径关系图转换为无环有向图（DAG）。
 *
 * 在本文方法中，该类主要用于：
 * 1. 对 Prime Path Relation Graph 中的环路进行处理
 * 2. 通过逐步消除环性，隐式地将每个强连通分量压缩为一个抽象顶点
 * 3. 为后续最小流构造无环图结构
 *
 * 本实现采用“逐环检测 + 逐步替换”的方式，
 * 而非一次性计算所有 SCC，但其语义等价于 SCC消解 因为 SCC 最终都会被压缩掉。图一定是 DAG
 */
public class SCCElim {
    // 存储全局的边信息
    private static List<String[]> edgeList = new ArrayList<>();
    // 记录生成的顶点数量
    private static int vCnt = 1;
    // 记录每个生成的顶点 Vi 和其替代的环路
    private static Map<String, List<String>> vertexToCycleMap = new HashMap<>();

    // 添加新的边到全局边列表
    public static void initEdges(List<String[]> newEdges) {
        edgeList.clear(); // 清空之前的边
        vCnt = 1;
        vertexToCycleMap.clear(); // 清空环路信息
        edgeList.addAll(newEdges);
    }

    public static void main(String[] args) {
        // 创建图的邻接表
        Map<String, Set<String>> graph = new HashMap<>();
        for (String[] edge : edgeList) {
            graph.computeIfAbsent(edge[0], k -> new HashSet<>()).add(edge[1]);
        }

        // 设定起始节点为 S，并优先遍历新顶点路径
        List<String> starts = Arrays.asList("S");

        // 检测并处理环路，直到没有环路为止
        graph = elimAllScc(graph, starts);

        // 打印并传输最终无环图的结构到 FlowBuilder
        buildDAG(graph);

    }

    public static Map<String, Set<String>> elimAllScc(Map<String, Set<String>> graph, List<String> starts) {
        List<List<String>> allCycles = new ArrayList<>(); // 用于存储所有检测到的环路

        while (true) {
            // 查找当前图中的环路
            List<List<String>> sccList = findCycles(graph, starts);

            /**
             * 若未检测到任何环路，
             * 说明当前图已经是 DAG，可直接返回
             */
            if (sccList.isEmpty()) {
                return graph;
            }

            // 对第一个找到的环路生成一个新的顶点并替换环路
            List<String> cycle = sccList.get(0); // 每次只处理第一个环路
            allCycles.add(cycle); // 将环路添加到 allCycles 中
            String newVertex = "V" + vCnt++;

            // 将环路替换为新顶点
            graph = contractSCC(graph, cycle, newVertex);

            // 存储环路信息到 UnrollLoop 中
            UnrollLoop.addCycle(newVertex, cycle);

            // 继续从起点 S 出发，优先处理与新顶点相关的边
            List<String> newStartNodes = new ArrayList<>();
            newStartNodes.add("S"); // 始终从 S 开始查找
            newStartNodes.add(newVertex); // 优先处理新生成的顶点
        }
    }

    /**
     * 在图中查找环路（基于 DFS 的回边检测）
     *
     * @param graph  当前图结构
     * @param starts DFS 起始节点集合
     * @return       检测到的环路列表
     */
    public static List<List<String>> findCycles(Map<String, Set<String>> graph, List<String> starts) {
        List<List<String>> sccList = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Stack<String> stack = new Stack<>();
        Set<String> stackSet = new HashSet<>();

        for (String startNode : starts) {
            if (!visited.contains(startNode)) {
                dfs(startNode, graph, visited, stack, stackSet, sccList);
            }
        }

        return sccList;
    }

    /**
     * 深度优先搜索过程，用于检测回边并构造环路
     *
     * @param node      当前访问节点
     * @param graph     图结构
     * @param visited   已访问节点集合
     * @param stack     DFS 递归栈
     * @param stackSet  栈中节点集合（用于 O(1) 判断）
     */
    private static void dfs(String node, Map<String, Set<String>> graph, Set<String> visited, Stack<String> stack, Set<String> stackSet, List<List<String>> sccList) {
        visited.add(node);
        stack.push(node);
        stackSet.add(node);

        if (graph.containsKey(node)) {
            // 将邻接节点按照一定顺序排列，优先处理新生成的顶点
            List<String> neighbors = new ArrayList<>(graph.get(node));
            Collections.sort(neighbors); // 按照字典顺序排序，保证新顶点优先被处理

            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    dfs(neighbor, graph, visited, stack, stackSet, sccList);
                } else if (stackSet.contains(neighbor)) {
                    // 只有在 neighbor 确实在 stack 中时才继续处理
                    int startIndex = stack.indexOf(neighbor);
                    if (startIndex != -1) {
                        List<String> cycle = new ArrayList<>();
                        for (int i = startIndex; i < stack.size(); i++) {
                            cycle.add(stack.get(i));
                        }
                        cycle.add(neighbor);
                        sccList.add(cycle);
                        return; // 检测到环路后立刻返回，停止深度优先搜索
                    }
                }
            }
        }

        stack.pop();
        stackSet.remove(node);
    }

    /**
     * 将指定环路从图中移除，并用新抽象顶点进行替换
     *
     * @param graph     原始图
     * @param cycle     待消解的环路节点集合
     * @param newVertex 新生成的抽象顶点
     * @return          更新后的图结构
     */
    private static Map<String, Set<String>> contractSCC(Map<String, Set<String>> graph, List<String> cycle, String newVertex) {
        Map<String, Set<String>> newGraph = new HashMap<>(graph);

        Set<String> cycleNodes = new HashSet<>(cycle);

        // 记录新顶点的入边和出边
        Set<String> incomingEdges = new HashSet<>();
        Set<String> outgoingEdges = new HashSet<>();

        // 处理每个环路节点的入边和出边
        for (String node : cycle) {
            if (graph.containsKey(node)) {
                for (String neighbor : graph.get(node)) {
                    if (!cycleNodes.contains(neighbor)) {
                        outgoingEdges.add(newVertex + " -> " + neighbor);
                    }
                }
            }

            for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
                if (entry.getValue().contains(node) && !cycleNodes.contains(entry.getKey())) {
                    incomingEdges.add(entry.getKey() + " -> " + newVertex);
                }
            }
        }

        // 移除原来的环路节点
        for (String node : cycleNodes) {
            newGraph.remove(node);
        }
        for (String node : newGraph.keySet()) {
            newGraph.get(node).removeIf(cycleNodes::contains);
        }

        // **递归替换子路径**
        for (String vertex : cycleNodes) {
            if (newGraph.containsKey(vertex)) {
                newGraph.get(newVertex).addAll(newGraph.get(vertex));
                newGraph.remove(vertex); // 删除所有子路径的顶点
            }
        }

        // 添加新顶点及其入边和出边
        newGraph.put(newVertex, new HashSet<>());

        for (String edge : incomingEdges) {
            String[] parts = edge.split(" -> ");
            String from = parts[0];
            newGraph.computeIfAbsent(from, k -> new HashSet<>()).add(newVertex);
        }

        for (String edge : outgoingEdges) {
            String[] parts = edge.split(" -> ");
            String to = parts[1];
            newGraph.get(newVertex).add(to);
        }

        return newGraph;
    }

    // 打印最终无环图的边，并传输到 FlowBuilder
    private static void buildDAG(Map<String, Set<String>> graph) {
        List<String[]> dagEdges = new ArrayList<>();
        int index = 1;
        for (Map.Entry<String, Set<String>> entry : graph.entrySet()) {
            String fromNode = entry.getKey();
            for (String toNode : entry.getValue()) {
//                System.out.println(index + "：（" + fromNode + ", " + toNode + "）");
                dagEdges.add(new String[]{fromNode, toNode});  // 将边加入 dagEdges 列表
                index++;
                UnrollLoop.addEdgeToG3(fromNode, toNode);
            }
        }

        // 将最终无环图的边传输到 FlowBuilder
        FlowBuilder.initEdges(dagEdges);
        FlowBuilder.main(null);

    }


}
