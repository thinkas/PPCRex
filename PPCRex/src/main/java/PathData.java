import java.util.List;

public class PathData {
    // 存储主路径的列表
    private List<String> ppList;
    // 存储主路径数量
    private int ppCnt;
    // 存储初始状态
    private int initialState;
    // 存储最终状态
    private List<Integer> accStates; // 存储多个接受状态
    // 存储目标状态
    private int targetState;

    /**
     * 构造函数，用于初始化 PathData 对象
     *
     * @param ppList        主路径列表
     * @param ppCnt         主路径数量
     * @param initialState  自动机初始状态
     * @param accStates     自动机接受状态集合
     * @param targetState   目标状态
     */
    public PathData(List<String> ppList, int ppCnt, int initialState, List<Integer> accStates, int targetState) {
        this.ppList = ppList; // 初始化主路径列表
        this.ppCnt = ppCnt; // 主路径数量
        this.initialState = initialState; // 初始化初始状态
        this.accStates = accStates; // 初始化最终状态
        this.targetState = targetState; // 初始化目标状态
    }

    /**
     * 获取主路径列表
     *
     * @return 主路径集合（Prime Path List）
     */
    public List<String> getPPList() {
        return ppList;
    }

    /**
     * 获取自动机初始状态
     *
     * @return 初始状态编号
     */
    public int getInitialState() {
        return initialState;
    }
    /**
     * 获取自动机的接受状态集合
     *
     * @return 接受状态列表
     */
    public List<Integer> getAccStates() {
        return accStates; // 返回所有接受状态
    }

    /**
     * 获取目标状态
     *
     * @return 目标状态编号
     */
    // 获取目标状态
    public int getTargetState() {
        return targetState;
    }

}
