import java.util.*;

public class Cluster {

    String Cid;     //簇编号
    int Scale;      //簇大小
    String Time;    //簇所在的时间
    int Tier;       //可以表示该簇属于哪一个层
    List<String> E = new ArrayList<>();   //簇包含的边集合
    List<Objects> Objects = new ArrayList<>();    //簇包含的运动对象集合

    String FatherId;                     //父簇的id
    String eventId;    //簇所在的汇聚事件id
    String eventStartTime;    //当前簇的最早叶节点的时刻
    List<String> childClusters = new ArrayList<>();   //簇的直接子簇列表,String为子簇所在的Tc中时间序号i+簇id

    public Cluster() {
        FatherId = null;
    }

    /**
     * 得到该簇到根节点的路径，并按概率排序
     * @param road
     * @param eventEdges_CA
     * @param eventRoot
     * @param edgeturnlist
     * @return
     */
    public ArrayList<Path> getPathOrder(RoadNetWork road, HashMap<String, Integer> eventEdges_CA, Cluster eventRoot,HashMap<String, HashMap<String, Double>> edgeturnlist){

        /**
         * @ Methond: getPathOrder
         * @ Description: 找到该节点到根节点的所有路径，根据转向概率、簇的个数、路径长度进行路径优先排序（唯一和簇有关系的地方在于限定了搜索边的集合）
         * @ Date 2019/10/29 20:53
         * @ Created by bridge
         * @ return java.util.ArrayList<java.util.ArrayList<java.lang.String>>
         **/

        ArrayList<Path> ret = new ArrayList<Path>();

        //建立搜索边集中各个边的邻边索引关系
        for(Map.Entry<String,Integer> edgeEntry1:eventEdges_CA.entrySet()){
            Edges e1 = road.getEdges().get(edgeEntry1.getKey());
            for(Map.Entry<String,Integer> edgeEntry2:eventEdges_CA.entrySet()){
                Edges e2 = road.getEdges().get(edgeEntry2.getKey());
                if(!e2.getId().equals(e1.getId())){
                    if(road.getNodes().get(e1.getEp()).getsEdges().contains(e2.getId())){
                        if(!e1.getAccedge().contains(e2)){
                            e1.getAccedge().add(e2);
                        }
                    }
                    if(road.getNodes().get(e1.getSp()).geteEdges().contains(e2.getId())){
                        if(!e2.getAccedge().contains(e1)){
                            e2.getAccedge().add(e1);
                        }
                    }
                }
            }
        }

//        System.out.println("当前事件中的边有：");
//        for(Map.Entry<String,Integer> entry:eventEdges_CA.entrySet()){
//            System.out.print("("+entry.getKey()+","+entry.getValue()+"):");
//            for(int i=0;i<road.getEdges().get(entry.getKey()).getAccedge().size();i++){
//                System.out.print(road.getEdges().get(entry.getKey()).getAccedge().get(i).getId()+",");
//            }
//            System.out.println();
//        }

        //生成所有的路径
        for (int i = 0; i < this.E.size(); i++) {
            for (int j = 0; j < eventRoot.E.size(); j++) {
                PathSearch ps = new PathSearch();
                ps.getPaths(road.getEdges().get(this.E.get(i)), null, road.getEdges().get(this.E.get(i)), road.getEdges().get(eventRoot.E.get(j)));
                ret.addAll(ps.ret);
            }
        }

        //计算优先级
//        System.out.println("i-ret.size() = "+ret.size());
        for (int i = 0; i < ret.size(); i++) {  //计算每一条路径的优先级别
//            System.out.println("ret.size() = "+ret.size());
            double p = 1.0;
            double length = 0.0;
            int num = 0;
//            System.out.println("j-ret.get(i).path.size() = "+ret.get(i).path.size());
            for (int j = 0; j < ret.get(i).path.size(); j++) {  //路径中的每一条边
//                System.out.println("ret.get(i).path.size() = "+ret.get(i).path.size());
                if (j < ret.get(i).path.size() - 1) {
                    if (ret.get(i).path.size() == 1)
                        p = 1.0;
                    else{
                        if(edgeturnlist.get(ret.get(i).path.get(j)).containsKey(ret.get(i).path.get(j + 1))){
                            p = p * edgeturnlist.get(ret.get(i).path.get(j)).get(ret.get(i).path.get(j + 1));
                        }else{
                            p = p * 0.0;
                        }
                    }
                }
//                System.out.println("road.getEdges().containsKey(ret.get(i).path.get(j)) = "+road.getEdges().containsKey("43545a"));
                length = length + road.getEdges().get(ret.get(i).path.get(j)).getLength();
//                System.out.println("eventEdges_CA.size() = "+eventEdges_CA.size());
//                System.out.println("ret.get(i).path.get(j) = "+ret.get(i).path.get(j));
                if(eventEdges_CA.containsKey(ret.get(i).path.get(j))){
                    num = num + eventEdges_CA.get(ret.get(i).path.get(j));
                }
            }
            ret.get(i).priority = (p * num) / length;
        }

//        System.out.println("当前簇到根节点簇的路径有："+ret.size()+"条！");

//        for(int i=0;i<ret.size();i++){
//            System.out.print("路径"+i+"的边序列为:");
//            for(int j=0;j<ret.get(i).path.size();j++){
//                System.out.print(ret.get(i).path.get(j)+"-->");
//            }
//            System.out.println();
//            System.out.println("路径"+i+"的优先级为："+ret.get(i).priority);
//        }

        //按优先级排序
        Collections.sort(ret, new Comparator<Path>() {
            @Override
            public int compare(Path o1, Path o2) {
                if (o1.priority < o2.priority)
                    return 1;
                return -1;
            }
        });

        return ret;
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

    public List<Objects> getObjects() {
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
        String time = this.Time;
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
            HashMap<String,Boolean> Iscandicluster = new HashMap<>();   //标识下一时刻的簇集合中各个簇是否已经加入候选簇
            for(Map.Entry<String,Cluster> nextCluEntry:nextclu.Clusters.entrySet()){
                Iscandicluster.put(nextCluEntry.getValue().Cid, false);
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
            for(Map.Entry<String,Cluster> nextCluEntry:nextclu.Clusters.entrySet()){
                Iscandicluster.put(nextCluEntry.getValue().Cid, false);
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

class Objects {

    String OId;
    double Lng;
    double Lat;
    String e;
    double pos;

    public Objects() {

    }

    public void setOId(String OId) {
        this.OId = OId;
    }

    public void setLng(double lng) {
        Lng = lng;
    }

    public void setLat(double lat) {
        Lat = lat;
    }

    public double getLng() {
        return Lng;
    }

    public double getLat() {
        return Lat;
    }

    public String getOId() {
        return OId;
    }

    public boolean isInCluster(Cluster C) {              //是不是在簇中
        for(int i = 0; i < C.Objects.size(); i++) {
            if(this.OId.equals(C.Objects.get(i).OId)) {
                return true;
            }
        }
        return false;
    }
}

