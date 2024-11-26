package Classes;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 层级数据结构(HierarchicalStructure)的结点
 * @Author JJP
 * @Date 2021/7/1 21:42
 */
public class ClusterNode {
    Cluster cluster;  //快照簇信息
    String nodeType;  //结点类型，包括cluster(快照簇-默认类型),nonconverging(非汇聚模式),unclosed(非闭合模式),closed(闭合模式)
    List<ClusterNode> subNodeList; //子结点列表
    String ts; //模式生命期-起始时间,为子结点中最早的起始时间
    String te; //模式生命期-终止时间,为簇当前的时间

    /**
     * 为簇创建图结点
     * @param cluster 簇
     */
    public ClusterNode(Cluster cluster) {
        this.cluster = new Cluster(cluster);
        this.nodeType = "cluster"; //默认为快照簇
        this.subNodeList = new ArrayList<>();
        this.ts = cluster.getTimestamp();
        this.te = cluster.getTimestamp();
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public List<ClusterNode> getSubNodeList() {
        return subNodeList;
    }

    public void setSubNodeList(List<ClusterNode> subNodeList) {
        this.subNodeList = subNodeList;
    }

    public String getTs() {
        return ts;
    }

    public void setTs(String ts) {
        this.ts = ts;
    }

    public String getTe() {
        return te;
    }

    public void setTe(String te) {
        this.te = te;
    }
}
