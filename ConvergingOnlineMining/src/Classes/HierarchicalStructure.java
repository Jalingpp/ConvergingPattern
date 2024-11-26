package Classes;

import Tools.TimeConversion;

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
    HashMap<String, HashMap<String,ClusterNode>> hierarchialGraph;
    String ts,te; //监测的起始时间和终止时间
    int timeNum; //层级图结构中已有的时间戳数

    //闭合汇聚模式表
    HashMap<String, ClusterNode> closedConverging;

    /**
     * 无参构造函数
     */
    public HierarchicalStructure() {
        this.hierarchialGraph = new HashMap<>();
        this.ts = null;
        this.te = null;
        this.timeNum = 0;
        this.closedConverging = new HashMap<>();
    }

    /**
     * 构造函数
     * @param clusterSet  一个快照簇集合
     * @param timestamp  对应的时间戳
     */
    public HierarchicalStructure(HashMap<String,Cluster> clusterSet, String timestamp) {
        this.hierarchialGraph = new HashMap<>();
        HashMap<String,ClusterNode> clusterNodeList = new HashMap<>();
        //为簇集合中每个簇创建图结点
        for(Map.Entry<String,Cluster> entry:clusterSet.entrySet()){
            ClusterNode _cn = new ClusterNode(entry.getValue());
            clusterNodeList.put(entry.getKey(),_cn);
        }
        this.hierarchialGraph.put(timestamp,clusterNodeList);
        this.ts = timestamp;
        this.te = timestamp;
        this.timeNum = 1;
        this.closedConverging = new HashMap<>();
    }

    /**
     * 更新层级图结构
     * @param clusterSet   当前簇集
     * @param timestamp    当前时间戳
     * @param timeInterval  时间间隔
     * @param ClusterContainment   簇包含关系
     * @param patternScale  模式规模阈值
     * @param patternDuration  模式时长阈值
     */
    public void update(HashMap<String,Cluster> clusterSet,String timestamp,int timeInterval,HashMap<String,String> ClusterContainment,int patternScale,int patternDuration){
        //为当前簇集创建簇结点列表
        creatClusterNodes(clusterSet,timestamp);
        //遍历clustercontainment列表，更新簇结点的信息
        if(ClusterContainment.size()>0)
            updateClusterNodes(ClusterContainment,timestamp,timeInterval,patternScale,patternDuration);
    }

    /**
     * 更新层级图结构的簇结点
     * @param ClusterContainment  簇包含关系
     * @param timestamp  当前时间戳
     * @param timeInterval  时间间隔
     * @param patternScale  模式规模阈值
     * @param patternDuration  模式时长阈值
     */
    private void updateClusterNodes(HashMap<String,String> ClusterContainment,String timestamp,int timeInterval,int patternScale,int patternDuration){
        //获取当前时刻的簇结点表
        HashMap<String,ClusterNode> curClusterNodes = this.hierarchialGraph.get(timestamp);
        //获取前一时刻的簇结点表
        String preTimestamp = TimeConversion.getNextKTimeString(timestamp,-1,timeInterval);
        HashMap<String,ClusterNode> preClusterNodes = this.hierarchialGraph.get(preTimestamp);
        //遍历每个簇包含关系列表：后面的String包含前面的String
        for(Map.Entry<String,String> entry:ClusterContainment.entrySet()){
            String preCNID = entry.getKey();
            String curCNID = entry.getValue();
            ClusterNode curCN = curClusterNodes.get(curCNID);
            ClusterNode preCN = preClusterNodes.get(preCNID);
            //如果preCN是快照簇cluster或非汇聚模式nonconverging，验证curCN的规模和时长：
            // 满足阈值curCN标为非闭合模式unclosed，否则curCN标为非汇聚模式nonconverging
            if(preCN.getNodeType().equals("cluster")||preCN.getNodeType().equals("nonconverging")){
                //更新curCN的起始时间：若preCN的起始时间早于curCN则更新为preCN的起始时间
                if(TimeConversion.isAfter(curCN.getTs(),preCN.getTs())){
                    curCN.setTs(preCN.getTs());
                }
                //计算curCN的总时长
                int curTimes = TimeConversion.getTimestampAmount(curCN.getTs(),curCN.getTe(),timeInterval);
                //如果时长超过阈值且规模超过阈值，则标注为非闭合模式unclosed，否则为非汇聚模式nonconverging
                if(curTimes>=patternDuration&&curCN.getCluster().getMenberPoints().size()>=patternScale){
                    curCN.setNodeType("unclosed");
                }else {
                    curCN.setNodeType("nonconverging");
                }
            }else if(preCN.getNodeType().equals("unclosed")){   //如果preCN是非闭合模式unclosed，则将curCN直接标注为非闭合模式unclosed
                curCN.setNodeType("unclosed");
            }
            //将preCN加入到curCN的子结点列表中
            curCN.getSubNodeList().add(preCN);
        }
        //遍历前一时刻的簇结点列表，将不存在簇包含关系的非闭合模式标注为闭合模式，并插入到闭合汇聚模式表中，并输出
        for(Map.Entry<String,ClusterNode> entry:preClusterNodes.entrySet()){
            ClusterNode cn = entry.getValue();
            if(cn.getNodeType().equals("unclosed")&&!ClusterContainment.containsKey(entry.getKey())){
                cn.setNodeType("closed");
                //插入到闭合模式表中
                this.closedConverging.put(entry.getKey(),cn);
                //输出闭合汇聚模式
                outputClosedConverging(cn);
            }
        }
    }

    /**
     * 为一个时间戳下的簇集创建图结点列表并插入到层级图结构中
     * @param clusterSet  快照簇集合
     * @param timestamp   时间戳
     */
    private void creatClusterNodes(HashMap<String,Cluster> clusterSet, String timestamp){
        //为集合中的每个簇创建图结点，并插入到列表中
        HashMap<String,ClusterNode> clusterNodeList = new HashMap<>();
        for(Map.Entry<String,Cluster> entry:clusterSet.entrySet()){
            ClusterNode _cn = new ClusterNode(entry.getValue());
            clusterNodeList.put(entry.getKey(),_cn);
        }
        this.hierarchialGraph.put(timestamp,clusterNodeList);
        //修改te为当前时刻（按道理，为了程序鲁棒性，应该比较一下te和timestamp哪个更迟）
        this.te = timestamp;
        this.timeNum++;
        if(timeNum==0)
            this.ts = timestamp;
    }

    /**
     * 输出闭合汇聚模式
     * @param cn 闭合汇聚模式的根结点
     */
    private void outputClosedConverging(ClusterNode cn){
        System.out.println("*");
        System.out.println("Find a converging:"+cn.getTe());
        System.out.println("Start Time:"+cn.getTs());
        System.out.println("End Time:"+cn.getTe());
        System.out.print("Members:{");
        for(int i=0;i<cn.getCluster().getMenberPoints().size()-1;i++){
            System.out.print(cn.getCluster().getMenberPoints().get(i).getId()+",");
        }
        System.out.print(cn.getCluster().getMenberPoints().get(cn.getCluster().getMenberPoints().size()-1).getId());
        System.out.println("}");
        System.out.println("*************************************");
    }
}
