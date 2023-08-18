package Clusters;

import RoadNetWork.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public class Cluster {

    String Cid;     //簇编号
    int Scale;      //簇大小
    String Time;    //簇所在的时间
    int Tier;       //可以表示该簇属于哪一个层
    List<String> E = new ArrayList<>();   //簇包含的边集合
    List<Clusters.Objects> Objects = new ArrayList<>();    //簇包含的运动对象集合

    String FatherId;                     //父簇的id
    String eventId;    //簇所在的汇聚事件id
    String eventStartTime;    //当前簇的最早叶节点的时刻
    List<String> childClusters = new ArrayList<>();   //簇的直接子簇列表,String为子簇所在的Tc中时间序号i+簇id

    public Cluster() {

    }

    public String getEventStartTime() {
        return eventStartTime;
    }

    public void setEventStartTime(String eventStartTime) {
        this.eventStartTime = eventStartTime;
    }

    public void setCid(String cid) {
        Cid = cid;
    }

    public List<String> getChildClusters() {
        return childClusters;
    }

    public String getFatherId() {
        return FatherId;
    }

    public void setFatherId(String fatherId) {
        FatherId = fatherId;
    }

    public String getCid() {
        return Cid;
    }

    public int getScale() {
        return Scale;
    }

    public String getTime() {
        return Time;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public List<Clusters.Objects> getObjects() {
        return Objects;
    }

    public boolean isInclude(String s, TimeClusters tcl) {   //该簇是否包含在id为s的簇中
        int pL = Objects.size();
        Cluster c = tcl.getClusters().get(s);
        for(int i = 0; i < pL; i++) {
            if (this.Objects.get(i).isInCluster(c) == false) {
                return false;
            }
        }
        return true;
    }

    public List<String> getEdge() {                //得到该簇所在的边的序号
        return this.E;
    }

    public List<String> getCandiClusters(RoadNetWork road, TimeClusters nextclu, HashMap<String,ArrayList<String>> edg_clu) {      //得到候选簇的序列,待完成


        /**
         * @ Methond: getCandiClusters
         * @ Description: Find candidate clusters in target cluster's edges which it includes
         * @ Date 2019/10/22 19:59
         * @ Created by bridge
         * @ return java.util.List<java.lang.String>
        **/

        List<String> candiClusters = new ArrayList<String>();
        ArrayList<String> searchedge = new ArrayList<String>();
        //read clusters in the next time
        Time time = new Time(this.Time);
//        String path = "D:/TestConverging/Clusters" + time.getYear() + time.getMonth() + time.getDate() + time.getHour() + time.Operadd(time.getMinute()) + time.getSecond() + "0.json";
        //first expand
        searchedge = firstAddPossibleClusters(road,nextclu,candiClusters,edg_clu);
        //second expand
        searchedge = moreAddPossibleClusters(road,nextclu,candiClusters,searchedge,edg_clu);
        return candiClusters;
    }

    public ArrayList<String> firstAddPossibleClusters(RoadNetWork road, TimeClusters nextclu, List<String> candiClusters,HashMap<String,ArrayList<String>> edg_clu) {
        ArrayList<String> allnearedges = new ArrayList<String>();
        for(int i = 0; i < E.size(); i++) {                         //遍历当前簇所在边集
            HashMap<String,Boolean> Iscandicluster = new HashMap<String, Boolean>();   //标识下一时刻的簇集合中各个簇是否已经加入候选簇
            for(int k = 0; k < nextclu.ClusterSize; k++) {                    //置默认值
                Iscandicluster.put(nextclu.Clusters.get(k).Cid, false);
            }
            Edges temp = road.getEdges().get(E.get(i));
            ArrayList<String> nearedge = new ArrayList<String>();    //可能出现的簇的临边集合
            nearedge.add(temp.getId());                       //把此路先加入集合
            String start = temp.getEp();
            for(Edges e : road.getEdges().values()) {        //将临近的边加入临边集合
                if(start.equals(e.getSp())) {
                    nearedge.add(e.getId());
                    allnearedges.add(e.getId());
                }
            }
            for(String eid : nearedge) {                             //遍历临边集合
                ArrayList<String> clusters = edg_clu.get(eid);
                if(clusters != null) {
                    for(int j = 3; j < clusters.size(); j++) {
                        if (Iscandicluster.get(clusters.get(j)) == false) {
                            Iscandicluster.remove(clusters.get(j)) ;
                            Iscandicluster.put(clusters.get(j),true);
                            candiClusters.add(clusters.get(j));
                        }
                    }
                }
            }
        }
        return allnearedges;
    }

    public ArrayList<String> moreAddPossibleClusters(RoadNetWork road, TimeClusters nextclu, List<String> candiClusters, ArrayList<String> searchedge,HashMap<String,ArrayList<String>> edg_clu) {
        ArrayList<String> allnearedges = new ArrayList<String>();
        for (int i = 0; i < searchedge.size(); i++) {                         //遍历当前簇所在边集
            HashMap<String,Boolean> Iscandicluster = new HashMap<String, Boolean>();   //标识下一时刻的簇集合中各个簇是否已经加入候选簇
            for(int k = 0; k < nextclu.ClusterSize; k++) {                    //置默认值
                Iscandicluster.put(nextclu.Clusters.get(k).Cid,false);
            }
            Edges temp = road.getEdges().get(searchedge.get(i));
            ArrayList<String> nearedge = new ArrayList<String>();    //可能出现的簇的临边集合
            String start = temp.getEp();
            for (Edges e : road.getEdges().values()) {        //将临近的边加入临边集合
                if (start.equals(e.getSp())) {
                    nearedge.add(e.getId());
                    allnearedges.add(e.getId());
                }
            }
            for(String eid : nearedge) {                             //遍历临边集合
                ArrayList<String> clusters = edg_clu.get(eid);
                if(clusters != null) {
                    for(int j = 3; j < clusters.size(); j++) {
                        if (Iscandicluster.get(clusters.get(j)) == false) {
                            Iscandicluster.remove(clusters.get(j)) ;
                            Iscandicluster.put(clusters.get(j),true);
                            candiClusters.add(clusters.get(j));
                        }
                    }
                }
            }
        }
        return allnearedges;
    }
}
