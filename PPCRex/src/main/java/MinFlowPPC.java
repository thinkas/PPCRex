import java.util.*;
/**
 * MinFlowPPC
 * ----------------------------
 * 使用最小流模型求解 Prime Path Coverage 问题，
 * 从流图 G4 中找到最少数量的测试路径，使其覆盖所有主路径。
 */
public class MinFlowPPC {
    public static void main(String[] args) {
        // 记录 S 能到达的所有 Vi+ 顶点
        Set<String> reachableViPlus = new HashSet<>();
        // 获取构造好的流图 G4
        FlowGraph flowGraph = FlowBuilder.getFlowGraph();

        System.out.println("执行最小流算法的流图 G4 为：");

        // 初始化索引和数据结构
        int index = 1;
        // 记录 Vi++ 能到达的所有 T 组合路径
        Map<String, List<List<String>>> pathsFromViPlusPlusToT = new HashMap<>();

        // 1. 初始化并打印流图中的所有边
        for (Map.Entry<String, Set<Edge>> entry : flowGraph.getGraph().entrySet()) {
            String fromNode = entry.getKey();
            for (Edge edge : entry.getValue()) {
                edge.setFlow(0); // 初始化流量为 0
                System.out.println(index + ": [" + fromNode + ", " + edge.toNode + "] (l_ij=" + edge.lowerBound + ", c_ij=" + edge.capacity + ", f_ij=" + edge.getFlow() + ")");
                index++;
            }
        }

        // 2. 查找 S 能直接到达的 Vi+ 节点
        for (String vertex : flowGraph.getGraph().keySet()) {
            // 只考虑 S 到达的节点，且该节点包含 "+" 且不以 "++" 结尾
            if (!vertex.equals("S") && !vertex.equals("T") && vertex.contains("+") && !vertex.endsWith("++")) {
                // 遍历 S 的出边，检查是否能够直接到达该 Vi+ 节点
                Set<Edge> edgesFromS = flowGraph.getGraph().get("S");
                if (edgesFromS != null) {
                    for (Edge edge : edgesFromS) {
                        if (edge.toNode.equals(vertex)) {
                            reachableViPlus.add(vertex);
                            System.out.println("S 可以到达 " + vertex);
                            break; // 找到后可以跳出循环，避免重复添加
                        }
                    }
                }
            }
        }

        // 3. 对每个 Vi+，查找 Vi++ 到 T 的所有可行路径
        for (String viPlus : reachableViPlus) {
            String viPlusPlus = viPlus + "+";
            if (flowGraph.getGraph().containsKey(viPlusPlus)) {
                List<List<String>> allPaths = new ArrayList<>();
                bfs(flowGraph, viPlusPlus, "T", allPaths);
                if (!allPaths.isEmpty()) {
                    pathsFromViPlusPlusToT.put(viPlusPlus, allPaths);
                    System.out.println(viPlusPlus + " 到 T 的所有路径: " + allPaths);
                }
            }
        }


        // 4. 合并 S -> Vi+ -> Vi++ -> T 形成完整路径
        List<List<String>> allFullPaths = new ArrayList<>();
        for (String viPlus : reachableViPlus) {
            String viPlusPlus = viPlus + "+";
            if (pathsFromViPlusPlusToT.containsKey(viPlusPlus)) {
                List<List<String>> pathsFromViPlusPlus = pathsFromViPlusPlusToT.get(viPlusPlus);
                for (List<String> pathFromViPlusPlus : pathsFromViPlusPlus) {
                    List<String> fullPath = new ArrayList<>();
                    fullPath.add("S");
                    fullPath.add(viPlus);
                    fullPath.addAll(pathFromViPlusPlus);
                    allFullPaths.add(fullPath);
                    System.out.println("合并路径: " + fullPath);
                }
            }
        }

        // 5.按路径中元素数量进行排序，最少元素的路径放在最前面
        Collections.sort(allFullPaths, new Comparator<List<String>>() {
            @Override
            public int compare(List<String> path1, List<String> path2) {
                return Integer.compare(path1.size(), path2.size());
            }
        });

        // 按路径长度排序
        System.out.println("所有完整合并路径：");
        for (List<String> path : allFullPaths) {
            System.out.println(path);
        }

        // 6. 依次沿路径调整流量（满足下界约束）
        for (List<String> path : allFullPaths) {
            System.out.println("开始调整流量");
            System.out.println(path);
            // 调整流量，按排序后的路径进行流量调整
            adjustFlow(flowGraph, path);
        }

        // 打印更新后的图信息
        System.out.println("更新后的流图信息：");
        int updatedIndex = 1;
        for (Map.Entry<String, Set<Edge>> entry : flowGraph.getGraph().entrySet()) {
            String fromNode = entry.getKey();
            for (Edge edge : entry.getValue()) {
                System.out.println(updatedIndex + ": [" + fromNode + ", " + edge.toNode + "] (l_ij=" + edge.lowerBound + ", c_ij=" + edge.capacity + ", f_ij=" + edge.getFlow() + ")");
                updatedIndex++;
            }
        }

        // 输出正向边和反向边信息
        List<String> forwardEdgeInfo = new ArrayList<>();
        List<String> backwardEdgeInfo = new ArrayList<>();
        System.out.println("");
        int fanxiang = 1;
        int zhengxiang = 1;
        for (Map.Entry<String, Set<Edge>> entry : flowGraph.getGraph().entrySet()) {
            String fromNode = entry.getKey();
            for (Edge edge : entry.getValue()) {
                // 正向边信息
                int forwardResidual = edge.getFlow() - edge.lowerBound;
                String forwardInfo = zhengxiang + ": 正向边: [" + fromNode + ", " + edge.toNode + "] (l_ij=" + edge.lowerBound + ", c_ij=" + edge.capacity + ", f_ij=" + edge.getFlow() + ", r_ij=" + forwardResidual + ")";
                forwardEdgeInfo.add(forwardInfo);
                // 反向边信息
                Edge backwardEdge = new Edge(edge.toNode, fromNode, 0, (flowGraph.getVertices().size() / 2) - 1);
                int backwardResidual = backwardEdge.capacity - edge.getFlow();
                String backwardInfo = fanxiang + ": 反向边: [" + backwardEdge.fromNode + ", " + backwardEdge.toNode + "] (l_ji=" + backwardEdge.lowerBound + ", c_ji=" + backwardEdge.capacity + ", r_ij=" + backwardResidual + ")";
                backwardEdgeInfo.add(backwardInfo);

                zhengxiang++;
                fanxiang++;
            }
        }

        // 打印正向边和反向边的总结信息
        System.out.println("正向边信息总结：");
        forwardEdgeInfo.forEach(System.out::println);
        System.out.println("反向边信息总结：");
        backwardEdgeInfo.forEach(System.out::println);

        findAndPrintSTPaths(flowGraph, "S", "T", backwardEdgeInfo);

        // 打印更新后的流图信息
        printUpdatedGraphInfo(flowGraph);

        // 计算并打印从源点 S 出发的总流量
        printTotalFlowFromSource(flowGraph, "S");
        findPathP(flowGraph);
    }
    /**
     * 使用 BFS 查找 start 到 end 的所有可行路径
     * 只沿着仍有剩余容量的边进行扩展
     */
    private static void bfs(FlowGraph graph, String start, String end, List<List<String>> allPaths) {
        Queue<List<String>> queue = new LinkedList<>();
        List<String> startPath = new ArrayList<>();
        startPath.add(start);
        queue.add(startPath);

        while (!queue.isEmpty()) {
            List<String> currentPath = queue.poll();
            String currentNode = currentPath.get(currentPath.size() - 1);

            if (currentNode.equals(end)) {
                allPaths.add(new ArrayList<>(currentPath));
            } else {
                for (Edge edge : graph.getGraph().getOrDefault(currentNode, Collections.emptySet())) {
                    if (edge.capacity > edge.getFlow()) { // 确保有可用容量
                        List<String> newPath = new ArrayList<>(currentPath);
                        newPath.add(edge.toNode);
                        queue.add(newPath);
                    }
                }
            }
        }
    }

    private static void findAndPrintSTPaths(FlowGraph graph, String start, String end, List<String> backwardEdgeInfo) {
        List<List<String>> allPaths = new ArrayList<>();
        bfs(graph, start, end, allPaths);

        // 打印包含正向和反向边的路径
        for (List<String> path : allPaths) {
            System.out.println("找到路径P: [" + String.join(", ", path) + "]，构成的边有：");
            List<String> forwardEdges = new ArrayList<>();
            List<String> reverseEdges = new ArrayList<>();

            for (int i = 0; i < path.size() - 1; i++) {
                String fromNode = path.get(i);
                String toNode = path.get(i + 1);
                Edge edge = graph.getEdge(fromNode, toNode);

                if (edge != null) {
                    int forwardResidual = edge.getFlow() - edge.lowerBound;
                    String forwardEdge = "[" + fromNode + ", " + toNode + "] (l_ij=" + edge.lowerBound +
                            ", c_ij=" + edge.capacity + ", f_ij=" + edge.getFlow() +
                            ", r_ij=" + forwardResidual + ")";
                    forwardEdges.add(forwardEdge);
                } else {
                    forwardEdges.add("无正向边: [" + fromNode + ", " + toNode + "]");
                }

                // 查找反向边信息
                String reverseEdgeKey = toNode + ", " + fromNode;
                boolean reverseEdgeFound = false;

                for (String backwardInfo : backwardEdgeInfo) {
                    if (backwardInfo.contains(reverseEdgeKey)) {
                        String[] parts = backwardInfo.split(": 反向边: ", 2);
                        if (parts.length > 1) {
                            reverseEdges.add(parts[1]);
                        } else {
                            reverseEdges.add(backwardInfo);
                        }
                        reverseEdgeFound = true;
                        break;
                    }
                }

                if (!reverseEdgeFound) {
                    reverseEdges.add("无反向边: [" + toNode + ", " + fromNode + "]");
                }
            }

            System.out.println("正向边: ");
            for (String edge : forwardEdges) {
                System.out.println(edge);
            }

            System.out.println("反向边: ");
            for (String edge : reverseEdges) {
                System.out.println(edge);
            }
            System.out.println();


        }

        // 打印所有路径P
        System.out.println("所有路径P:");
        for (List<String> path : allPaths) {
            System.out.println("[" + String.join(", ", path) + "]");
        }
        System.out.println();

//         打印所有r_ij > 0的路径P及其边属性
        System.out.println("所有r_ij > 0的路径P及其边属性:");
        for (List<String> path : allPaths) {
            List<String> forwardEdges = new ArrayList<>();
            List<String> reverseEdges = new ArrayList<>();
            boolean allResidualsPositive = true;
            int rmin = Integer.MAX_VALUE;
            List<String> comparedEdges = new ArrayList<>();
            List<Integer> comparedValues = new ArrayList<>();

            for (int i = 0; i < path.size() - 1; i++) {
                String fromNode = path.get(i);
                String toNode = path.get(i + 1);
                Edge edge = graph.getEdge(fromNode, toNode);

                if (edge != null) {
                    int forwardResidual = edge.getFlow() - edge.lowerBound;
                    if (forwardResidual <= 0) {
                        allResidualsPositive = false;
                        break;
                    }
                    String forwardEdge = "[" + fromNode + ", " + toNode + "] (l_ij=" + edge.lowerBound +
                            ", c_ij=" + edge.capacity + ", f_ij=" + edge.getFlow() +
                            ", r_ij=" + forwardResidual + ")";
                    forwardEdges.add(forwardEdge);

                    // 比较更新rmin
                    rmin = Math.min(rmin, forwardResidual);
                    comparedEdges.add(forwardEdge);
                    comparedValues.add(forwardResidual);

                } else {
                    allResidualsPositive = false;
                    break;
                }

                // 查找反向边信息
                String reverseEdgeKey = toNode + ", " + fromNode;
                boolean reverseEdgeFound = false;

                for (String backwardInfo : backwardEdgeInfo) {
                    if (backwardInfo.contains(reverseEdgeKey)) {
                        String[] parts = backwardInfo.split(": 反向边: ", 2);
                        if (parts.length > 1) {
                            reverseEdges.add(parts[1]);
                        } else {
                            reverseEdges.add(backwardInfo);
                        }
                        reverseEdgeFound = true;
                        break;
                    }
                }

                if (!reverseEdgeFound) {
                    reverseEdges.add("无反向边: [" + toNode + ", " + fromNode + "]");
                }
            }

            if (allResidualsPositive) {
                System.out.println("路径P: [" + String.join(", ", path) + "]");
                System.out.println("正向边属性: ");
                for (String edge : forwardEdges) {
                    System.out.println(edge);
                }
                System.out.println("反向边属性: ");
                for (String edge : reverseEdges) {
                    System.out.println(edge);
                }
                System.out.println("参与rmin比较的边: ");
                for (String edge : comparedEdges) {
                    System.out.println(edge);
                }
                System.out.println("比较的r_ij值: " + comparedValues);
                System.out.println("最小r_ij (rmin): " + rmin);
                System.out.println();
                // 减去rmin值
                for (int i = 0; i < path.size() - 1; i++) {
                    String fromNode = path.get(i);
                    String toNode = path.get(i + 1);
                    Edge edge = graph.getEdge(fromNode, toNode);

                    if (edge != null) {
                        edge.setFlow(edge.getFlow() - rmin);
                    }
                }
            }
        }
    }

    /**
     * 根据完整路径计算最小 k = min(f_ij - l_ij)
     * 当 k < 0 时，对路径上的所有边增加流量
     */
    private static void adjustFlow(FlowGraph graph, List<String> fullPath) {
        boolean updated;
        do {
            updated = false;  // 标记是否在本轮循环中更新了流量

            // 初始化k为最大整数值
            int k = Integer.MAX_VALUE;

            // 存放路径上的边
            List<Edge> pathEdges = new ArrayList<>();

            // 遍历路径上的边，计算k的值
            for (int i = 0; i < fullPath.size() - 1; i++) {
                String from = fullPath.get(i);
                String to = fullPath.get(i + 1);
                Edge edge = graph.getEdge(from, to);
                if (edge != null) {
                    pathEdges.add(edge);
                    int currentK = edge.flow - edge.lowerBound;  // 计算当前边的f_{mn} - l_{mn}
                    k = Math.min(k, currentK);  // 更新k为最小值
                }
            }

//             输出最小值k
            System.out.println("路径上的最小流量差值 k: " + k);
//             如果k小于0，则增加路径上每条边的流量
            if (k < 0) {
                for (Edge updateEdge : pathEdges) {
                    updateEdge.flow += 1;  // 增加流量
                    System.out.println("更新边 [" + updateEdge.fromNode + " -> " + updateEdge.toNode + "] 的流量从 " + (updateEdge.flow - 1) + " 变更为 " + updateEdge.flow);
                    updated = true;  // 标记已进行更新
                }
            }
        } while (updated);  // 循环直到没有更多更新为止
    }
    /**
     * 打印当前流图中所有边的状态
     */
    private static void printUpdatedGraphInfo(FlowGraph graph) {
        System.out.println("更新后的流图信息：");
        for (Map.Entry<String, Set<Edge>> entry : graph.getGraph().entrySet()) {
            String fromNode = entry.getKey();
            for (Edge edge : entry.getValue()) {
                System.out.println("[" + fromNode + ", " + edge.toNode + "] (l_ij=" + edge.lowerBound + ", c_ij=" + edge.capacity + ", f_ij=" + edge.flow + ")");
            }
        }
    }
    /**
     * 计算并输出从源点 S 发出的总流量
     * 即最小测试路径的数量
     */
    private static void printTotalFlowFromSource(FlowGraph graph, String source) {
        int totalFlow = 0;
        Set<Edge> edgesFromSource = graph.getGraph().get(source);
        if (edgesFromSource != null) {
            for (Edge edge : edgesFromSource) {
                totalFlow += edge.flow;
            }
        }
        System.out.println("从源点 " + source + " 出发的最小流量为: " + totalFlow);
    }

    /**
     * 合并图中的节点和边，生成一个简化的路径图。
     * 该方法会移除特殊标记（如 "+" 和 "++"）的节点，并将其合并为普通节点。
     * 同时会保留边的流量信息、容量等数据。
     *
     * @param graph 原始流图（FlowGraph 对象），包含节点和边的完整信息
     */
    private static void findPathP(FlowGraph graph) {
        // 获取图的邻接表，originalGraph 是一个 Map，键是节点名，值是与该节点相连的边集合
        Map<String, Set<Edge>> originalGraph = graph.getGraph();
        // 用于存储合并后的图结构（邻接表形式），键是合并后的节点名，值是与该节点相连的边集合
        Map<String, Set<Edge>> mergedGraph = new HashMap<>();
        // 遍历原始图的每个节点及其相连的边
        for (Map.Entry<String, Set<Edge>> entry : originalGraph.entrySet()) {
            String fromNode = entry.getKey();// 当前节点（起点）
            Set<Edge> edges = entry.getValue();// 与当前节点相连的边集合
            // 遍历当前节点的所有边
            for (Edge edge : edges) {
                String toNode = edge.toNode;// 当前边的终点节点
                // 跳过特殊情况：如果起点以 "+" 结尾，且终点以 "++" 结尾，这条边将被忽略
                if (fromNode.endsWith("+") && toNode.endsWith("++")) {
                    continue;// 跳过该边
                }
                // 合并节点名称：
                // 去掉起点和终点名称中的 "+" 和 "++" 标记，形成新的节点名称
                String mergedFromNode = fromNode.replace("+", "").replace("++", "");
                String mergedToNode = toNode.replace("+", "").replace("++", "");
                // 如果合并后的起点节点尚未存在于 mergedGraph 中，则初始化一个空的边集合
                mergedGraph.putIfAbsent(mergedFromNode, new HashSet<>());
                // 创建一条新的边对象，边的起点和终点为合并后的节点名称，保留原始边的属性
                Edge newEdge = new Edge(mergedFromNode, mergedToNode, edge.lowerBound, edge.capacity);
                // 设置新边的流量值，继承原始边的流量信息
                newEdge.setFlow(edge.flow);
                // 将新边添加到合并图的邻接表中
                mergedGraph.get(mergedFromNode).add(newEdge);
            }
        }

        System.out.println("合并后的流图:");
        for (Map.Entry<String, Set<Edge>> entry : mergedGraph.entrySet()) {
            String fromNode = entry.getKey();
            for (Edge edge : entry.getValue()) {
                System.out.println("[" + fromNode + ", " + edge.toNode + "] (l_ij=" + edge.lowerBound + ", c_ij=" + edge.capacity + ", f_ij=" + edge.flow + ")");
            }
        }
        // 通过 BFS 查找从 s 到 t 的路径
        String source = "S";  // 假设源点为 S
        String sink = "T";    // 假设汇点为 T
        // 存储已找到的路径
        List<List<Edge>> foundPaths = new ArrayList<>();

        while (true) {
            // 移除所有流量为 0 的边
            removeZeroFlowEdges(mergedGraph);

            // 找到从源点到汇点的路径
            List<Edge> path = findPathBFS(mergedGraph, source, sink);

            // 如果找不到路径，结束循环
            if (path == null || path.isEmpty()) {
                System.out.println("未找到更多路径");
                break;
            }

            // 输出找到的路径
            System.out.print("找到的路径: ");
            for (Edge edge : path) {
                System.out.print("[" + edge.fromNode + " -> " + edge.toNode + "] ");
            }
            System.out.println();

            // 将找到的路径添加到已找到的路径列表中
            foundPaths.add(path);

            // 对路径中的每条边，将流量减 1
            for (Edge edge : path) {
                edge.flow--;
                System.out.println("更新后的边: [" + edge.fromNode + " -> " + edge.toNode + "]，流量减少1，当前流量: " + edge.flow);
            }
        }


//         输出所有已找到的路径
        System.out.println("找到的最小测试路径:");
        for (List<Edge> p : foundPaths) {
            List<String> nodes = new ArrayList<>();
            nodes.add(p.get(0).fromNode);  // 添加起始节点

            for (Edge edge : p) {
                nodes.add(edge.toNode);  // 添加每条边的目标节点
            }

            System.out.print("最小测试路径: [");
            System.out.print(String.join(", ", nodes));  // 使用逗号连接节点
            System.out.println("]");
        }

        // 生成和传递路径字符串
        for (List<Edge> p : foundPaths) {
            List<String> nodes = new ArrayList<>();
            nodes.add(p.get(0).fromNode);
            for (Edge edge : p) {
                nodes.add(edge.toNode);
            }

            String pathString = "最小测试路径: [" + String.join(", ", nodes) + "]";
            UnrollLoop.printSinglePathString(pathString);  // 调用 UnrollLoop 类的方法打印路径

        }
        UnrollLoop.main(new String[0]);

    }


    private static void removeZeroFlowEdges(Map<String, Set<Edge>> graph) {
        for (Set<Edge> edges : graph.values()) {
            edges.removeIf(edge -> edge.flow == 0);  // 移除流量为0的边
        }
    }

    /**
     * 使用广度优先搜索 (BFS) 在图中查找从源点到汇点的路径。
     * BFS 会沿着剩余流量大于 0 的边进行搜索，并回溯找到完整路径。
     *
     * @param graph  图的邻接表，键是节点名，值是从该节点出发的边集合
     * @param source 源点节点名
     * @param sink   汇点节点名
     * @return 如果找到路径，则返回从源点到汇点的边的列表；如果找不到路径，则返回 null
     */
    private static List<Edge> findPathBFS(Map<String, Set<Edge>> graph, String source, String sink) {
        // 使用 LinkedHashMap 保证遍历顺序与输入图一致
        LinkedHashMap<String, Set<Edge>> orderedGraph = new LinkedHashMap<>();

        // 复制输入图的结构到 orderedGraph，保持遍历顺序稳定
        for (Map.Entry<String, Set<Edge>> entry : graph.entrySet()) {
            orderedGraph.put(entry.getKey(), entry.getValue());
        }

        // 使用 LinkedList 实现广度优先搜索的队列 (FIFO)
        Queue<String> queue = new LinkedList<>();

        // parentMap 用于记录路径中的父边，用于回溯找到完整路径
        Map<String, Edge> parentMap = new LinkedHashMap<>();

        // visited 集合记录已访问的节点，防止重复访问
        Set<String> visited = new LinkedHashSet<>();

        // 初始化 BFS，起点入队并标记为已访问
        queue.offer(source);
        visited.add(source);

        // BFS 主循环
        while (!queue.isEmpty()) {
            // 从队列中取出当前节点
            String currentNode = queue.poll();

            // 如果到达汇点，回溯路径并返回
            if (currentNode.equals(sink)) {
                List<Edge> path = new ArrayList<>(); // 用于存储路径中的边
                String node = sink;

                // 回溯路径，从汇点向源点追踪
                while (!node.equals(source)) {
                    Edge edge = parentMap.get(node); // 获取当前节点对应的父边
                    path.add(0, edge); // 将边插入路径的开头
                    node = edge.fromNode; // 更新当前节点为父节点
                }
                return path; // 返回完整路径
            }

            // 获取当前节点的所有出边
            Set<Edge> edges = orderedGraph.get(currentNode);

            // 遍历所有出边
            if (edges != null) {
                // 将边转换为列表并排序：toNode 中的数字大的排前面
                List<Edge> sortedEdges = new ArrayList<>(edges);
                sortedEdges.sort((e1, e2) -> extractPiIndex(e1.toNode) - extractPiIndex(e2.toNode));

                for (Edge edge : sortedEdges) {
                    if (!visited.contains(edge.toNode) && edge.flow > 0) {
                        parentMap.put(edge.toNode, edge);
                        queue.offer(edge.toNode);
                        visited.add(edge.toNode);
                    }
                }

            }
        }

        // 如果队列为空且未找到汇点，返回 null 表示无路径
        return null;
    }
    private static int extractPiIndex(String nodeName) {
        try {
            if (nodeName.matches("p\\d+")) {
                return Integer.parseInt(nodeName.substring(1));
            }
        } catch (NumberFormatException ignored) {}
        return -1; // 不符合格式的节点放后面
    }

}