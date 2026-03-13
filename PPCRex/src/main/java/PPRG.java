import java.util.*;

/**
 * PPRG：主路径关系图（Primary Path Relation Graph）
 * 说明：
 * 1. 本类负责根据主路径集合构建变换图（主路径关系图）。
 * 2. 图中每个节点表示一条主路径，边表示路径之间可以合并。
 * 3. 该类是论文中“主路径关系图构建”的核心实现。
 */
public class PPRG {
    public static int initialState;//初始状态
    public static List<Integer> finalState;//终止状态
    public static List<String> updatedMainPaths = new ArrayList<>();
    public static List<String> remainingNonConnectedPaths = new ArrayList<>();


    //用于从 PathData 构建主路径关系图
    public static DirectedGraph buildPPRG(PathData pathData) {
        // 清空 MinimumTestPath 中的缓存，防止不同图间数据污染
        MinimumTestPath.clear();

        // 从 PathData 中读取状态信息与主路径集合
        int initialState = pathData.getInitialState();
        List<Integer> finalState = pathData.getAccStates();
        int targetState = pathData.getTargetState();
        List<String> currentMainPaths = pathData.getPPList();

        // 将初始状态与接受状态传递给后续模块
        MinimumTestPath.Finder3(initialState, finalState);

        /* ---------- 1. 主路径字符串解析 ---------- */
        List<List<String>> paths = new ArrayList<>();
        List<String> pathNames = new ArrayList<>();

        for (String pathString : currentMainPaths) {
            String name = pathString.substring(0, pathString.indexOf(":"));
            String path = pathString.substring(
                    pathString.indexOf("[") + 1,
                    pathString.indexOf("]")
            );
            List<String> pathList = Arrays.asList(path.split(", "));
            paths.add(pathList);
            pathNames.add(name);
        }

        /* ---------- 2. 构建主路径关系图 ---------- */
        DirectedGraph graph = new DirectedGraph(pathNames);
        for (int i = 0; i < paths.size(); i++) {
            for (int j = 0; j < paths.size(); j++) {
                if (i != j && canUnion(paths.get(i), paths.get(j), paths)) {
                    graph.addEdge(i, j);
                }
            }
        }

        /* ---------- 3. 添加源点 S 与汇点 T ---------- */
        Set<Integer> acceptStates = new HashSet<>(finalState);
        AddST(graph, paths, pathNames,
                initialState, finalState, targetState, acceptStates);

        /* ---------- 4. 输出当前构建的关系图 ---------- */
        graph.printGraph();

        /* ---------- 5. 连通性检查 ---------- */
        // 仅做判定
        checkPPRGConn(graph, pathNames, pathData);

        return graph; // 返回构建完成的主路径关系图
    }

    /**
     * 判断有向图中是否存在从 start 节点到 end 节点的可达路径
     *
     * 实现方式：
     * 使用广度优先搜索（BFS）进行可达性分析。
     *
     * 使用场景：
     * - 在主路径关系图 G2 中，
     *   判断某条主路径节点是否：
     *   1) 可以从源点 S 到达；
     *   2) 是否可以到达汇点 T。
     *
     * @param graph 主路径关系图 G2
     * @param start 起始节点索引
     * @param end   目标节点索引
     * @return 若存在路径则返回 true，否则返回 false
     */
    private static boolean isPathExists(DirectedGraph graph, int start, int end) {
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            int node = queue.poll();
            if (node == end) {
                return true;  // 找到了从 S 到 T 的路径
            }
            for (int neighbor : graph.getNeighbors(node)) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return false;  // S 不能到达 T
    }

    /**
     * 检查主路径关系图 G2 的连通性
     *
     * 连通性判定标准（论文定义）：
     * 对于任意一条主路径顶点 v：
     * - v 必须可以从源点 S 到达；
     * - 且 v 必须可以到达汇点 T。
     *
     * 功能说明：
     * 1. 对每一条主路径节点进行 S → v 和 v → T 的可达性检查；
     * 2. 收集所有不满足连通性条件的主路径；
     * 3. 将不连通主路径保存，用于后续“独立路径扩展生成测试用例”。
     *
     * @param graph     主路径关系图 G2
     * @param pathNames 图中节点对应的主路径名称
     * @param pathData  原始主路径数据
     * @return 原始主路径集合
     */
    public static List<String> checkPPRGConn(DirectedGraph graph, List<String> pathNames, PathData pathData) {
        List<String> pathStrings = pathData.getPPList();
        int sIndex = pathNames.indexOf("S");
        int tIndex = pathNames.indexOf("T");

        List<String> nonConnectedPaths = new ArrayList<>();
        for (int i = 0; i < pathNames.size(); i++) {
            if (i != sIndex && i != tIndex) {
                boolean canReachFromS = isPathExists(graph, sIndex, i);
                boolean canReachTFromNode = isPathExists(graph, i, tIndex);

                if (!canReachFromS || !canReachTFromNode) {
                    nonConnectedPaths.add(pathStrings.get(i));
                }
            }
        }

        if (!nonConnectedPaths.isEmpty()) {
            System.out.println("主路径关系图是不连通的");
            System.out.println("当前主路径集：");
            for (String pathString : pathStrings) {
                System.out.println(pathString);
            }

            System.out.println("无法从 S 到 T 的路径：");
            for (String nonConnectedPath : nonConnectedPaths) {
                System.out.println(nonConnectedPath);
            }
            // 把当前的非连通路径赋值给静态变量
            PPRG.remainingNonConnectedPaths = nonConnectedPaths;
//            //原本主路径集不连通的情况下会有一个特殊的处理修复，现在已经删掉。改成论文中提到的后期对独立的主路径向前后延伸成为一条独立的路径生成测试用例。
//            return analyzeCommonSubPaths(graph, nonConnectedPaths, pathStrings);
        } else {
            System.out.println("主路径关系图是可连通的");
            PPRG.remainingNonConnectedPaths.clear();
        }

        return pathStrings; // 返回原始路径集
    }

    /**
     * RemoveSCC：消除主路径关系图中的强连通分量（SCC）
     *
     * 方法目的：
     * 在主路径关系图 G2 中，不同主路径之间可能形成环（强连通分量），
     * 这会导致后续测试路径生成出现冗余或无限扩展。
     *
     * 本方法通过调用 SCCElim 与 PathReduce 模块：
     * 1. 识别图中的环结构；
     * 2. 消除图中的环性。
     *
     * 说明：
     * - 本方法只负责“检测与触发处理”，
     * - 不直接修改 G2主路径关系图的结构，
     * - 具体的环处理策略由 SCCElim / PathReduce 实现。
     */
    public static void RemoveSCC(DirectedGraph graph) {
        // 访问并使用全局边信息
        SCCElim.initEdges(graph.getEdges()); // 查找图的环路
        PathReduce.init(graph.getEdges()); // 传递数据

        SCCElim.main(new String[]{}); // 执行主方法
    }

    /**
     * 判断两条主路径是否可以在主路径关系图中建立连接关系
     *
     * 建立边的含义：
     * 若 path1 可以在语义上“延伸”到 path2，
     * 且合并后不会覆盖其他主路径，
     * 则在 G2 中建立一条从 path1 到 path2 的有向边。
     *
     * 判定规则包括两种情况：
     * 1. 有重叠情况：
     *    - path1 的后缀与 path2 的前缀相同；
     *    - 合并后路径不包含第三条主路径。
     *
     * 2. 无重叠情况：
     *    - 通过 BFS 寻找 path1 终点到 path2 起点的最短连接路径；
     *    - 合并后路径仍不包含其他主路径。
     *
     * 3.其他情况，
     *    - 说明存在不连通的路径，既没有跟其他路径重叠，又无法通过拼接最短路径进行合并。因此后续生成测试用例时会在单独处理
     * @param path1     第一条主路径
     * @param path2     第二条主路径
     * @param allPaths  当前所有主路径集合
     * @return 若可以建立连接关系返回 true，否则返回 false
     */
    private static boolean canUnion(List<String> path1, List<String> path2, List<List<String>> allPaths) {
        boolean isOverlapProcessed = false; // 用于标记是否处理了重叠情况

        int minLength = Math.min(path1.size(), path2.size()); // 取两条路径的最小长度
        for (int len = 1; len < minLength; len++) {
            List<String> suffix = path1.subList(path1.size() - len, path1.size()); // 取路径1的后缀
            List<String> prefix = path2.subList(0, len); // 取路径2的前缀

            if (suffix.equals(prefix)) {
                // 处理有重叠的路径对
                isOverlapProcessed = true;

                // 合并路径1和路径2
                List<String> unionPath = new ArrayList<>(path1);
                unionPath.addAll(path2.subList(len, path2.size()));

                System.out.println("检查路径 " + path1 + " 和 " + path2 + " 的并集");
                System.out.println("后缀: " + suffix + " 前缀: " + prefix);
                System.out.println("并集路径: " + unionPath);

                // 检查合并路径是否包含其他路径的子路径
                boolean containsOtherPath = false;
                for (List<String> path : allPaths) {
                    if (!path.equals(path1) && !path.equals(path2) && IsSubPath(unionPath, path)) {
                        containsOtherPath = true;
                        System.out.println("并集路径 " + unionPath + " 包含其他路径 " + path);
                        break;
                    }
                }

                // 如果不包含其他路径，则可以建立边
                if (!containsOtherPath) {
                    System.out.println("路径 " + path1 + " 和 " + path2 + " 之间可以建立边");
                    return true;
                } else {
                    System.out.println("路径 " + path1 + " 和 " + path2 + " 之间不能建立边，因为合并路径包含其他路径");
                }
            }
        }

        // 如果没有处理任何重叠的情况，处理无重叠的路径对
        if (!isOverlapProcessed) {
            System.out.println("路径 " + path1 + " 和 " + path2 + " 没有重叠，尝试寻找最短路径");

            // 获取路径1的最后一个顶点和路径2的第一个顶点
            String lastNodePath1 = path1.get(path1.size() - 1);
            String firstNodePath2 = path2.get(0);

            // 使用 BFS 寻找最短路径
            List<String> shortestPath = BFSPath(lastNodePath1, firstNodePath2, allPaths);

            if (!shortestPath.isEmpty()) {
                // 合并路径1、最短路径和路径2
                List<String> unionPath = new ArrayList<>(path1);

                // 遍历最短路径，跳过与路径1最后一个节点重复的部分
                for (int i = 1; i < shortestPath.size(); i++) {
                    if (!shortestPath.get(i).equals(lastNodePath1)) {
                        unionPath.add(shortestPath.get(i));
                    }
                }

                // 添加路径2中没有重复的部分
                for (int i = 0; i < path2.size(); i++) {
                    if (i == 0 && path2.get(i).equals(firstNodePath2)) {
                        continue; // 跳过与最短路径重复的起始节点
                    }
                    unionPath.add(path2.get(i));
                }

                // 打印调试信息
                System.out.println("路径1: " + path1);
                System.out.println("路径2: " + path2);
                System.out.println("最短路径: " + shortestPath);
                System.out.println("合并后的路径: " + unionPath);

                // 检查合并后的路径是否包含路径1和路径2之外的其他路径
                boolean containsOtherPath = false;
                for (List<String> path : allPaths) {
                    if (!path.equals(path1) && !path.equals(path2) && IsSubPath(unionPath, path)) {
                        containsOtherPath = true;
                        System.out.println("合并路径 " + unionPath + " 包含其他路径 " + path);
                        break;
                    }
                }

                // 如果不包含其他路径，则可以建立边
                if (!containsOtherPath) {
                    System.out.println("路径 " + path1 + " 和 " + path2 + " 之间可以建立边");
                    return true;
                } else {
                    System.out.println("路径 " + path1 + " 和 " + path2 + " 之间不能建立边，因为合并路径包含其他路径");
                }
            } else {
                System.out.println("路径1的尾节点 " + lastNodePath1 + " 和路径2的头节点 " + firstNodePath2 + " 之间没有有效的最短路径");
            }
        }

        return false; // 不能建立边
    }

    /**
     * 使用 BFS 搜索从 startNode 到 endNode 的最短路径
     *
     * 用途：
     * - 当两条主路径不存在直接重叠时，
     *   尝试通过中间状态将其连接；
     * - 仅作为 canUnion 方法中的辅助判断。
     *
     * @param startNode 起始节点
     * @param endNode   目标节点
     * @param allPaths  所有主路径，用于构建隐式状态转移图
     * @return 若存在路径则返回节点序列，否则返回空列表
     */
    private static List<String> BFSPath(String startNode, String endNode, List<List<String>> allPaths) {
        // 构建图的邻接表
        Map<String, List<String>> adjacencyList = new HashMap<>();
        for (List<String> path : allPaths) {
            for (int i = 0; i < path.size() - 1; i++) {
                adjacencyList.computeIfAbsent(path.get(i), k -> new ArrayList<>()).add(path.get(i + 1));
            }
        }

        // BFS 搜索最短路径
        Queue<List<String>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        // 初始化队列
        queue.offer(Collections.singletonList(startNode));
        visited.add(startNode);

        while (!queue.isEmpty()) {
            List<String> currentPath = queue.poll();
            String currentNode = currentPath.get(currentPath.size() - 1);

            // 如果找到目标节点，返回路径
            if (currentNode.equals(endNode)) {
                return currentPath;
            }

            // 遍历当前节点的邻居
            List<String> neighbors = adjacencyList.getOrDefault(currentNode, Collections.emptyList());
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);

                    // 构建新的路径并加入队列
                    List<String> newPath = new ArrayList<>(currentPath);
                    newPath.add(neighbor);
                    queue.offer(newPath);
                }
            }
        }

        // 如果找不到路径，返回空列表
        return Collections.emptyList();
    }

    /**
     * 判断一条路径是否为另一条路径的连续子路径
     *
     * 判定方式：
     * - 要求 path2 在 path1 中是连续出现的；
     * - 非连续出现不视为子路径。
     *
     * 使用场景：
     * - 防止主路径合并后覆盖或包含第三条主路径，
     *   从而破坏主路径的独立性。
     *
     * @param path1 被检查的较长路径
     * @param path2 待匹配的子路径
     * @return 若 path2 是 path1 的连续子路径则返回 true
     */
    private static boolean IsSubPath(List<String> path1, List<String> path2) {
        // 判断 path2 是否在 path1 中
        if (path2.size() > path1.size()) {
            return false; // path2 长度大于 path1，返回 false
        }
        for (int i = 0; i <= path1.size() - path2.size(); i++) {
            boolean matched = true; // 标记是否匹配
            for (int j = 0; j < path2.size(); j++) {
                if (!path1.get(i + j).equals(path2.get(j))) { // 判断路径节点是否相等
                    matched = false; // 不匹配
                    break; // 跳出循环
                }
            }
            if (matched) {
                return true; // 匹配成功
            }
        }
        return false; // 未找到匹配
    }

    /**
     * 主路径关系图的数据结构
     */
    static class DirectedGraph {
        private Map<Integer, List<Integer>> graph = new HashMap<>(); // 存储图的邻接表
        private List<String> pathNames; // 存储路径名称
        private List<String[]> edgesList = new ArrayList<>(); // 存储边的信息

        public DirectedGraph(List<String> pathNames) {
            this.pathNames = pathNames; // 初始化路径名称
        }

        public List<Integer> getNeighbors(int node) {
            return graph.getOrDefault(node, new ArrayList<>());
        }

        public void addEdge(int from, int to) {
            graph.computeIfAbsent(from, k -> new ArrayList<>()).add(to); // 添加边
            edgesList.add(new String[]{pathNames.get(from), pathNames.get(to)}); // 添加边的信息
        }

        public List<String[]> getEdges() {
            return edgesList; // 返回边的信息
        }

        public void printGraph() {
            System.out.println("构建成功");
            System.out.println("主路径关系图 G2 的边有：");
            int index = 1; // 边的索引
            for (String[] edge : edgesList) {
                System.out.println(index + "：" + "(" + edge[0] + ", " + edge[1] + ")"); // 打印边
                index++;
                UnrollLoop.addEdgeToG2(edge[0], edge[1]);
            }
        }
        // 清空图的方法
        public void clear() {
            graph.clear(); // 清空邻接表
            edgesList.clear(); // 清空边的信息
        }


    }

    private static void AddST(DirectedGraph graph, List<List<String>> paths, List<String> pathNames, int initialState, List<Integer> finalState, int targetState, Set<Integer> acceptStates) {
        String S = "S"; // 源点
        String T = "T"; // 汇点
        int sIndex = pathNames.size(); // 源点的索引
        int tIndex = pathNames.size() + 1; // 汇点的索引

        pathNames.add(S); // 添加源点到路径名称列表
        pathNames.add(T); // 添加汇点到路径名称列表

        Set<Integer> possibleFinalStates = new HashSet<>();  // 存储所有可能的终止状态
        Set<Integer> statesWithOutgoingEdges = new HashSet<>();  // 存储所有有出边的状态

        // 遍历所有路径，找到所有的终止状态和起点
        for (List<String> path : paths) {
            if (!path.isEmpty()) {
                int startState = Integer.parseInt(path.get(0));  // 获取起点
                int endState = Integer.parseInt(path.get(path.size() - 1));  // 获取终点

                // 只有终点是 accept 状态才加入 possibleFinalStates
                if (acceptStates.contains(endState)) {
                    possibleFinalStates.add(endState);  // 只收集 accept 状态
                }

                statesWithOutgoingEdges.add(startState);  // 收集路径起点
            }
        }

        // 只使用 acceptStates 作为最终的终止状态
        finalState.clear();
        finalState.addAll(possibleFinalStates);

        // 打印所有终止状态
        System.out.println("所有终止状态：");
        for (Integer state : finalState) {
            System.out.println(state);
        }

        // 遍历路径，为 S 和 T 建立边
        for (int i = 0; i < paths.size(); i++) {
            List<String> path = paths.get(i);

            if (!path.isEmpty()) {
                int startState = Integer.parseInt(path.get(0));
                int endState = Integer.parseInt(path.get(path.size() - 1));

                // 如果路径的起点是初始状态或目标状态，连接到 S
                if (startState == initialState || startState == targetState) {
                    graph.addEdge(sIndex, i);
                }

                // 如果路径的终点是最终的 accept 状态，连接到 T
                if (finalState.contains(endState)) { // 只连接到 accept 状态
                    graph.addEdge(i, tIndex);
                    System.out.println("建立边: (" + pathNames.get(i) + ", " + T + ")，因为路径到达最终终止状态 " + endState + "。");
                }
            }
        }
    }
}