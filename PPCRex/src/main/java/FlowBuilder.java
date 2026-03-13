import java.util.*;
/**
 * FlowBuilder 用于将无环图（DAG）转换为流网络（FlowNet）
 * 以便后续进行最小流（MinFlow）计算。
 */
public class FlowBuilder {
    // 存储图的边
    private static List<String[]> dagEdges = new ArrayList<>();

    // 初始化边
    public static void initEdges(List<String[]> newEdges) {
        dagEdges.clear(); // 清空之前的边
        dagEdges.addAll(newEdges);
    }

    public static void main(String[] args) {
        // 调用最小流算法的主方法
        MinFlowPPC.main(null);
    }

    /**
     * 将无环图（DAG）转换为流图（FlowGraph）
     * 主要做法：
     * 1. 将每个顶点拆分为 vertex+ 和 vertex++
     * 2. 在 vertex+ -> vertex++ 上设置容量（capacity）
     * 3. 将 DAG 的边连接到拆分后的顶点
     *
     * @param edges DAG 的边列表
     * @return 转换后的流图（FlowGraph）
     */
    public static FlowGraph buildFlowNet(List<String[]> edges) {
        FlowGraph flowGraph = new FlowGraph();

        // 存储图中的所有顶点
        Set<String> vertices = new HashSet<>();
        for (String[] edge : edges) {
            vertices.add(edge[0]);
            vertices.add(edge[1]);
        }

        // 计算流图中顶点的数量和容量
        int v4Size = (vertices.size() - 2) * 2 + 2; // S 和 T 不计入
        int capacity = v4Size / 2 - 1;

        // 为每个顶点添加新的顶点（+和++表示）
        for (String vertex : vertices) {
            if (!vertex.equals("S") && !vertex.equals("T")) {
                String vertexPlus = vertex + "+";
                String vertexPlusPlus = vertex + "++";

                flowGraph.addVertex(vertexPlus);    // 添加顶点
                flowGraph.addVertex(vertexPlusPlus); // 添加顶点

                // 添加边，流量为1，容量为capacity
                flowGraph.addEdge(vertexPlus, vertexPlusPlus, 1, capacity);
            } else {
                flowGraph.addVertex(vertex); // S 和 T 直接添加
            }
        }

        // 为无环图中的每条边添加流图中的边
        for (String[] edge : edges) {
            String startPlusPlus = edge[0].equals("S") ? "S" : edge[0] + "++";
            String endPlus = edge[1].equals("T") ? "T" : edge[1] + "+";

            flowGraph.addEdge(startPlusPlus, endPlus, 0, capacity); // 添加边，流量为0
        }

        return flowGraph; // 返回转换后的流图
    }

    // 获取当前的流图
    public static FlowGraph getFlowGraph() {
        return buildFlowNet(dagEdges);
    }

}

// 流图类，表示流图的结构
class FlowGraph {
    private Map<String, Set<Edge>> graph = new HashMap<>();
    private String source = "S"; // 源点
    private String sink = "T";   // 汇点

    // 获取从某个节点出发的所有边
    public Set<Edge> getEdges(String from) {
        return graph.getOrDefault(from, Collections.emptySet());
    }

    // 获取所有顶点
    public Set<String> getVertices() {
        return graph.keySet();
    }

    // 添加顶点
    public void addVertex(String vertex) {
        graph.computeIfAbsent(vertex, k -> new HashSet<>());
    }

    // 添加边
    public void addEdge(String from, String to, int lowerBound, int capacity) {
        addVertex(from); // 确保起点存在
        addVertex(to);   // 确保终点存在
        Edge newEdge = new Edge(from, to, lowerBound, capacity); // 创建新边
        if (!graph.get(from).contains(newEdge)) {
            graph.get(from).add(newEdge); // 将边添加到图中
        }
    }

    // 获取图的边信息
    public Map<String, Set<Edge>> getGraph() {
        return graph;
    }

    // 获取具体的边，如果不存在则返回null
    public Edge getEdge(String from, String to) {
        Set<Edge> dagEdges = getEdges(from);
        for (Edge edge : dagEdges) {
            if (edge.toNode.equals(to)) {
                return edge; // 找到并返回边
            }
        }
        return null;  // 如果没有找到边，返回null
    }
}

// 边类，表示流图中的边
class Edge {
    String fromNode; // 起点
    String toNode;   // 终点
    int lowerBound;  // 边的下界
    int capacity;    // 边的容量
    int flow;        // 当前流量
    Edge reverseEdge;  // 反向边的引用

    // 构造函数
    public Edge(String from, String to, int lowerBound, int capacity) {
        this.fromNode = from;
        this.toNode = to;
        this.capacity = capacity;
        this.flow = 0;  // 初始化流量为0
        this.lowerBound = lowerBound; // 初始化下界
    }

    // 方法用于设置和获取流量
    public int getFlow() {
        return flow;
    }

    // 设置流量，并更新反向边的流量
    public void setFlow(int flow) {
        this.flow = flow;
        if (this.reverseEdge != null) {
            this.reverseEdge.flow = this.capacity - this.flow; // 更新反向边的流量
        }
    }
}
