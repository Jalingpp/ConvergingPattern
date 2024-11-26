package Classes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 用于汇聚模式监测的层级数据结构
 * @Author JJP
 * @Date 2021/7/1 21:40
 */
public class HierarchicalStructure {
    //用于汇聚模式监测的层级图结构,String:时间戳,List:簇结点列表
    HashMap<String, List<ClusterNode>> hierarchialGraph;
    String ts,te; //监测的起始时间和终止时间
    int timeNum; //层级图结构中已有的时间戳数

    /**
     * 无参构造函数
     */
    public HierarchicalStructure() {
        this.hierarchialGraph = new HashMap<>();
        this.ts = null;
        this.te = null;
        this.timeNum = 0;
    }

    /**
     * 构造函数
     * @param clusterSet  一个快照簇集合
     * @param timestamp  对应的时间戳
     */
    public HierarchicalStructure(HashMap<String,Cluster> clusterSet, String timestamp) {
        this.hierarchialGraph = new HashMap<>();
        List<ClusterNode> clusterNodeList = new ArrayList<>();
        //为簇集合中每个簇创建图结点
        for(Map.Entry<String,Cluster> entry:clusterSet.entrySet()){
            ClusterNode _cn = new ClusterNode(entry.getValue());
            clusterNodeList.add(_cn);
        }
        this.hierarchialGraph.put(timestamp,clusterNodeList);
        this.ts = timestamp;
        this.te = timestamp;
        this.timeNum = 1;

    }

    /**
     * 为一个时间戳下的簇集创建图结点列表并插入到层级图结构中
     * @param clusterSet  快照簇集合
     * @param timestamp   时间戳
     */
    public void addClusterSet(HashMap<String,Cluster> clusterSet, String timestamp){
        //为集合中的每个簇创建图结点，并插入到列表中
        List<ClusterNode> clusterNodeList = new ArrayList<>();
        for(Map.Entry<String,Cluster> entry:clusterSet.entrySet()){
            ClusterNode _cn = new ClusterNode(entry.getValue());
            clusterNodeList.add(_cn);
        }
        this.hierarchialGraph.put(timestamp,clusterNodeList);
        //修改te为当前时刻（按道理，为了程序鲁棒性，应该比较一下te和timestamp哪个更迟）
        this.te = timestamp;
        this.timeNum++;
        if(timeNum==0)
            this.ts = timestamp;
    }
}
