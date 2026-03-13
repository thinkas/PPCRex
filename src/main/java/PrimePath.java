import dk.brics.automaton.*;
import java.util.*;
/**
 * PrimePath
 *
 * 本类负责实现 Prime Path Coverage (PPC) 中的“主路径生成”阶段。
 *
 * 主要功能包括：
 * 1. 将输入的正则表达式转换为最小化 DFA；
 * 2. 将 DFA 建模为有向图；
 * 3. 枚举图中的所有简单路径；
 * 4. 根据 Prime Path 的定义，过滤非主路径；
 * 5. 输出最终的主路径集合，并为后续的主路径关系图（PPRG）构建提供数据。
 *
 */
public class PrimePath {
    public static String regExp1;//原始正则表达式
    public static Automaton automaton; // 自动机对象
    private int numberOfStates; // 状态数量
    private int V; // 顶点数量
    private List<Integer>[] adj; // 邻接表
    private String[] vertexNames; // 顶点名称数组
    private List<List<String>> allPaths = new ArrayList<>(); // 存储所有路径
    private PPRG.DirectedGraph directedGraph; // 主路径关系图
    private int primePathCount; // 主路径数量
    private int start; // 初始状态编号（开始顶点）
    private List<Integer> acceptStates; // 接受状态编号（结束顶点）
    private int targetStates1; // 开始顶点的目标状态顶点
    private List<Integer> targetStates = new ArrayList<>(); // 目标状态列表
    private List<String> p = new ArrayList<>(); // 最终路径字符串列表

    // 构造函数
    public PrimePath(String regex, boolean useComplement) {
        RegExp regexp = new RegExp(ExtendedRegex.extendRegex(regex));
        this.acceptStates = new ArrayList<>();
        regExp1 = regex;
        if (useComplement) {
            automaton = regexp.toAutomaton();
        } else {
            automaton = regexp.toAutomaton().complement();
        }
        automaton.determinize();
        automaton.minimize();
        System.out.println(automaton);

        /**
         * 如果 DFA 是 singleton（只接受一个固定字符串），
         * 则该正则不需要再运行代码，直接终止。
         */
        String singletonStr = automaton.getSingleton();
        if (singletonStr != null) {
            System.out.println("当前正则表达式 \"" + regex + "\" 只接受字符串: \"" + singletonStr + "\"，跳过处理。");
            throw new SingletonRegexException(singletonStr);
        }

        State initialState = automaton.getInitialState(); // 获取初始状态
        start = initialState.getNumber(); // 记录初始状态编号

        for (Transition transition : initialState.getTransitions()) { // 遍历初始状态转换
            targetStates.add(transition.getDest().getNumber()); // 添加目标状态编号
        }
        targetStates1 = targetStates.isEmpty() ? -1 : targetStates.get(0); // 如果目标状态为空，设为-1

        for (State state : automaton.getStates()) { // 遍历自动机状态
            if (state.isAccept()) { // 如果是接受状态
                acceptStates.add(state.getNumber()); // 记录接受状态编号
            }
        }

        numberOfStates = automaton.getNumberOfStates(); // 获取状态数量
        V = numberOfStates; // 设置顶点数量
        adj = new ArrayList[V]; // 初始化邻接表数组
        for (int i = 0; i < V; i++) {
            adj[i] = new ArrayList<>(); // 初始化每个邻接表
        }

        vertexNames = new String[V]; // 初始化顶点名称数组

        for (int i = 0; i < V; i++) {
            vertexNames[i] = "" + i; // 设置顶点名称为编号字符串
        }

        for (State state : automaton.getStates()) { // 遍历状态
            for (Transition transition : state.getTransitions()) { // 遍历转换
                addEdge(state.getNumber(), transition.getDest().getNumber()); // 添加边
            }
        }
    }
    public class SingletonRegexException extends RuntimeException {
        public SingletonRegexException(String singletonStr) {
            super("只接受字符串 \"" + singletonStr + "\"，跳过处理。");
        }
    }



    // 添加边的方法
    private void addEdge(int from, int to) {
        if (!adj[from].contains(to)) { // 检查是否已经有边指向目标状态
            adj[from].add(to); // 如果没有，则添加边
        }
    }


    public void printAutomatonGraph() {
        System.out.println("自动机顶点：");
        for (int i = 0; i < V; i++) {
            System.out.println("顶点: " + i);
        }
        System.out.println("自动机边：");
        for (int from = 0; from < V; from++) {
            for (int to : adj[from]) {
                System.out.println("边: " + from + " -> " + to);
            }
        }
    }
    public int getNumberOfStates() {
        return V;
    }
    public List<Integer>[] getAdj() {
        return adj;
    }
    //  current：当前递归中正在处理的顶点编号。
//  path：当前路径，即从起点到目前为止走过的顶点序列。
//  visited：一个布尔数组，用于标记哪些顶点已经访问过。
    // 查找所有简单路径的方法
    /**
     * 使用深度优先搜索（DFS）枚举从某一顶点出发的所有简单路径。
     *
     * 约束：
     * - 路径中不允许出现重复中间节点；
     * - 允许回到起点以形成简单环（用于处理循环结构）。
     *
     * @param current 当前访问的顶点
     * @param path    当前路径（顶点序列）
     * @param visited 顶点访问标记数组
     */
    private void dfsPaths(int current, List<Integer> path, boolean[] visited) {
        List<Integer> newPath = new ArrayList<>(path); // 复制当前路径
        boolean[] newVisited = Arrays.copyOf(visited, visited.length); // 复制访问数组

        newPath.add(current); // 添加当前节点到路径
        newVisited[current] = true; // 标记当前节点已访问

        if (newPath.size() > 1 || (V == 1 && adj[current].contains(current))) {
            List<String> currentPath = new ArrayList<>();
            for (int vertex : newPath) {
                currentPath.add(vertexNames[vertex]);
            }
            // 如果是单个顶点的自环情况，手动添加两次0
            if (V == 1 && adj[current].contains(current)) {
                currentPath.add(vertexNames[current]);
            }
            allPaths.add(currentPath);
        }


        if (newPath.size() == V) { // 如果路径长度等于顶点数，返回
            return;
        }

        for (int neighbor : adj[current]) { // 遍历邻居节点
            if (!newVisited[neighbor] || neighbor == newPath.get(0)) {
                if (!repEnd(newPath, neighbor)) { // 检查中间路径是否与结束顶点有重复
                    dfsPaths(neighbor, newPath, newVisited); // 递归查找路径
                }
            }
        }
    }

    /**
     * 生成满足 Prime Path Coverage 的主路径集合。
     *
     * 步骤：
     * 1. 对 DFA 图中每个顶点执行 DFS，枚举所有简单路径；
     * 2. 删除中间节点重复的非法路径；
     * 3. 删除被其他路径包含的非主路径；
     * 4. 按路径长度排序，形成最终主路径集。
     *
     * @return 主路径集合（字符串表示）
     */
    public List<String> PrimePathGen() {
        for (int i = 0; i < V; i++) { // 遍历所有顶点
            dfsPaths(i, new ArrayList<>(), new boolean[V]); // 查找所有路径
        }

        rmMidRep(); // 删除中间路径的顶点与起始顶点重复的路径
        rmSub(); // 删除被其他路径包含的路径
        allPaths.sort(Comparator.comparingInt(List::size)); // 按路径长度升序排序

        for (int i = 0; i < allPaths.size(); i++) { // 构建路径字符串
            List<String> path = allPaths.get(i);
            p.add("p" + i + ": " + path);
        }
        primePathCount = p.size(); // 记录主路径数量

//        System.out.println("主路径集："); // 打印主路径集
        for (String path : p) {
//            System.out.println(path);
            MinimumTestPath.Finder1(p);
        }
        return p; // 返回主路径列表

    }

    // 检查中间路径是否有与结束顶点重复的情况
    private boolean repEnd(List<Integer> path, int end) {
        for (int i = 1; i < path.size(); i++) { // 从第二个顶点开始检查
            if (path.get(i).equals(end)) { // 如果有重复顶点
                return true; // 如果中间有顶点与结束顶点重复，返回true
            }
        }
        return false; // 否则返回false
    }

    // 检查中间路径是否有与起始顶点重复的情况
    private boolean midRep(List<String> path) {
        String start = path.get(0); // 获取起始顶点
        String end = path.get(path.size() - 1); // 获取结束顶点
        for (int i = 1; i < path.size() - 1; i++) { // 从第二个顶点检查到倒数第二个顶点
            if (path.get(i).equals(start) && !path.get(i).equals(end)) { // 如果中间顶点与起始顶点重复且不是结束顶点
                return true; // 返回true
            }
        }
        return false; // 否则返回false
    }

    // 删除中间节点与起始节点重复的路径
    private void rmMidRep() {
        List<List<String>> validPaths = new ArrayList<>(); // 存放有效路径
        for (List<String> path : allPaths) {
            if (!midRep(path)) { // 检查路径是否有中间节点与起始节点重复
                validPaths.add(path); // 添加有效路径
            }
        }
        allPaths = validPaths; // 更新所有路径
    }

    /**
     * 判断 path1 是否是 path2 的连续子路径。
     * 若是，则 path1 不构成 Prime Path。
     */
    // 检查路径是否被包含的方法
    private boolean isSubPath(List<String> path1, List<String> path2) {
        if (path1.size() > path2.size()) { // 如果path1比path2长
            return false; // 返回false
        }

        for (int i = 0; i <= path2.size() - path1.size(); i++) { // 遍历path2
            boolean matched = true;
            for (int j = 0; j < path1.size(); j++) { // 比较path1和path2的子路径
                if (!path1.get(j).equals(path2.get(i + j))) { // 如果不匹配
                    matched = false; // 标记为不匹配
                    break;
                }
            }
            if (matched) { // 如果匹配
                return true; // 返回true
            }
        }

        return false; // 否则返回false
    }

    /**
     * 删除被其他路径完全包含的路径，
     * 仅保留 Prime Path。
     */
    private void rmSub() {
        List<List<String>> finalPaths = new ArrayList<>(allPaths); // 复制所有路径

        for (int i = 0; i < allPaths.size(); i++) {
            for (int j = 0; j < allPaths.size(); j++) {
                if (i != j) { // 如果不是同一条路径
                    List<String> path1 = allPaths.get(i);
                    List<String> path2 = allPaths.get(j);

                    if (isSubPath(path1, path2)) { // 如果path1被path2包含
                        finalPaths.remove(path1); // 移除path1
                        break;
                    }
                }
            }
        }

        allPaths = finalPaths; // 更新所有路径
    }

    // Getter 方法用于获取状态信息
    public int getPathCount() {
        return primePathCount; // 返回主路径数量
    }

    public int getInitialState() {
        return start; // 返回初始状态编号
    }

    public List<Integer> getAcceptStates() { // 修改此方法，返回多个接受状态
        return acceptStates;
    }

    public int getNextStates() {
        return targetStates1; // 返回第一个目标状态编号
    }

    public List<String> getPrimePath() {
        return p; // 返回主路径列表
    }


    public PathData createPathData() {
        return new PathData(
                getPrimePath(), // 主路径集
                getPathCount(),        // 主路径数量
                getInitialState(), // 初始状态编号
                getAcceptStates(),  // 修改为获取多个接受状态
                getNextStates()  // 目标状态
        );
    }

    /**
     * 根据主路径集构建 Prime Path Relation Graph (PPRG)。
     */
    public void buildPPRG() {
        PathData pathData = createPathData();
        // 使用主路径集构建主路径关系图，并获取构建的图对象
        directedGraph =  PPRG.buildPPRG(pathData);

    }

    public void removeCycles() {
        // 去除环路的方法
        PPRG.RemoveSCC(directedGraph);
    }
}