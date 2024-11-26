package Clustering;

import Classes.*;
import Clustering.FVARangeQuery.RNRangeQuery;
import Tools.TimeConversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 基于同簇验证的簇更新算法
 * @Author JJP
 * @Date 2021/11/5 18:23
 */
public class SCVClusterUpdating {
    RoadNetwork rn_leps;
    HashMap<String, Cluster> preClusterSet;
    HashMap<String, SnapObject> snapshotObjectSet;
    HashMap<String, List<String>> edgeObjectIndex; //边-移动对象索引
    RNRangeQuery rnRQ;  //路网范围查询索引
    int eps; //聚类半径
    int minpts; //聚类密度
    int mc;  ////成员数量阈值
    String curTime;  //当前时间戳

    /**
     * 构造函数
     * @param rn_leps
     * @param preClusterSet
     * @param snapshotObjectSet
     * @param rnRQ
     * @param eps
     * @param minpts
     * @param mc
     * @param curTime
     */
    public SCVClusterUpdating(RoadNetwork rn_leps, HashMap<String, Cluster> preClusterSet, HashMap<String, SnapObject> snapshotObjectSet,RNRangeQuery rnRQ, int eps, int minpts,int mc,String curTime) {
        this.rn_leps = rn_leps;
        this.preClusterSet = preClusterSet;
        this.snapshotObjectSet = new HashMap<>();
        for(Map.Entry<String,SnapObject> entry:snapshotObjectSet.entrySet()){
            this.snapshotObjectSet.put(entry.getKey(),entry.getValue());
        }
        this.rnRQ = rnRQ;
        this.eps = eps;
        this.minpts = minpts;
        this.mc = mc;
        this.curTime = curTime;
        edgeObjectIndexConstruct();
    }
    /**
     * 路网边和移动对象的索引，用于查询边上的移动对象列表，该列表从边起点侧开始，以pos值从小到大排列
     * @return 该索引
     */
    private void edgeObjectIndexConstruct(){
        this.edgeObjectIndex = new HashMap<>();
        //遍历移动对象，放到相应的边表中
        for(Map.Entry<String,SnapObject> entry:snapshotObjectSet.entrySet()){
            SnapObject _so = entry.getValue();
            //如果对象所在的边表已存在，则加入到其中；否则，创建边表加入其中
            List<String> _edgeObjectList = new ArrayList<>();
            if(edgeObjectIndex.containsKey(_so.getEdgeid())){
                _edgeObjectList = edgeObjectIndex.get(_so.getEdgeid());
                for(int i=0;i<_edgeObjectList.size();i++){
                    if(_so.getPos()<=snapshotObjectSet.get(_edgeObjectList.get(i)).getPos()){
                        _edgeObjectList.add(i,_so.getId());
                        break;
                    }
                }
            }else{
                _edgeObjectList.add(_so.getId());
            }
            edgeObjectIndex.put(_so.getEdgeid(),_edgeObjectList);
        }
    }

    /**
     * 基于同簇验证的簇更新算法
     * @param currentClusterSet  当前簇集
     * @param clusterContainment  当前簇包含关系集
     */
    public void SCVClusterUpdating(HashMap<String, Cluster> currentClusterSet,HashMap<String, String> clusterContainment){
        rnRQ.update(snapshotObjectSet,minpts);
        int clusterNum = 0;  //发现的簇的个数
        //遍历前一时刻快照簇集中的每个簇，考虑是否可以扩展
        for(Map.Entry<String,Cluster> preCluster:this.preClusterSet.entrySet()){
            Cluster _cluster = preCluster.getValue();
//            System.out.print("Verifyinf "+_cluster.getId()+" ...");
            //获取新时刻下移动对象集合所在的路网子图
            RoadNetwork subgraph = getSubgraphOfObjects(_cluster.getMenberPoints());
            //验证移动对象集合是否同簇
            HashMap<String,String> objectsInOneCluster = new HashMap<>();
            boolean isInOneCluster = isInOneCluster(_cluster.getMenberPoints(),subgraph,objectsInOneCluster);
            //若同簇，则扩展簇，并将新簇中的对象在snapshotObjectSet中删除
            if(isInOneCluster){
                //扩展簇:查询objectsInOneCluster中不确定点的邻域，添加新的点，标记核心点，边界点，直到全部确定
                HashMap<String,String> certainPoints = new HashMap<>();//用于放置已经确定的点,core或border
                List<String> uncertainPoins = new ArrayList<>();//用于放置不确定的点
                for(Map.Entry<String,String> entry:objectsInOneCluster.entrySet()){
                    if(entry.getValue().equals("core")&&snapshotObjectSet.containsKey(entry.getKey()))
                        certainPoints.put(entry.getKey(),entry.getValue());
                    else{
                        uncertainPoins.add(entry.getKey());
                    }
                }
                List<String> visited = new ArrayList<>();  //用于记录被uncertainPoints访问过的点
                //不断查询不确定点的邻域，直到其中不存在不确定的点
                while(uncertainPoins.size()>0){
                    String oid = uncertainPoins.get(0);
                    visited.add(oid);
                    //查询不确定点的邻域
                    List<String> nnOfUnObject = rnRQ.getResultObjects(oid);
                    //如果邻域大于阈值，将邻域内不确定的点放入不确定点集中，将该点放入确定点中，否则，仅将该点放入确定点中
                    if(nnOfUnObject.size()>=minpts){
                        if(snapshotObjectSet.containsKey(oid))
                            certainPoints.put(oid,"core");
                        for(int i=0;i<nnOfUnObject.size();i++)
                            if((!certainPoints.containsKey(nnOfUnObject.get(i)))&&(!uncertainPoins.contains(nnOfUnObject.get(i)))&&(!visited.contains(nnOfUnObject.get(i))))
                                uncertainPoins.add(nnOfUnObject.get(i));
                    }else {
                        if(snapshotObjectSet.containsKey(oid))
                            certainPoints.put(oid,"border");
                    }
                    //不确定点中删除该点
                    uncertainPoins.remove(0);
                }
                //将新簇插入到新簇集中，并将其中的对象在snapshotObjectSet中删除
                Cluster newCluster = new Cluster(""+clusterNum,curTime);
                clusterNum++;
                for(Map.Entry<String,String> entry:certainPoints.entrySet()){
                    newCluster.addPoint(snapshotObjectSet.get(entry.getKey()));
                    snapshotObjectSet.remove(entry.getKey());
                }
//                System.out.print(", get newCluster "+newCluster.getId());
                currentClusterSet.put(newCluster.getId(),newCluster);
                //将簇包含关系加入到簇包含关系集中
//                System.out.println(", get cluster containment ("+_cluster.getId()+","+newCluster.getId()+")");
                clusterContainment.put(_cluster.getId(),newCluster.getId());
            }
        }
        //对snapshotObjectSet中剩余的点用原始方法聚类
        HashMap<String,Cluster> newClusters = DBSCAN_RoadNetwork.getClusters(snapshotObjectSet,rnRQ,minpts,curTime,mc);
        System.out.print(curTime+": Clustering left "+snapshotObjectSet.size()+" objects...");
        //调整每个簇的id加入到当前簇集中
        for(Map.Entry<String,Cluster> entry:newClusters.entrySet()){
            Cluster cluster = entry.getValue();
            cluster.setId(""+clusterNum);
            clusterNum++;
            currentClusterSet.put(cluster.getId(), cluster);
        }
        System.out.println("Total clusters:"+currentClusterSet.size()+", Total Containments:"+clusterContainment.size());
    }

    /**
     * 获取移动对象集合所在的路网子图（在边长度不超过eps的路网中）
     * @param objects 移动对象集合
     * @return 路网子图
     */
    private RoadNetwork getSubgraphOfObjects(List<SnapObject> objects){
        RoadNetwork subgraph = new RoadNetwork();
        //遍历移动对象列表，将其所在的边插入边表，将边的顶点插入顶点表
        for(int i=0;i<objects.size();i++){
            SnapObject _o = objects.get(i);
            if(!subgraph.getEdgeList().containsKey(_o.getEdgeid())){
                Edge _e = rn_leps.getEdgeList().get(_o.getEdgeid());
                subgraph.getEdgeList().put(_o.getEdgeid(),_e);
                //将边的顶点插入到顶点集
                if(!subgraph.getVertexList().containsKey(_e.getSp_id())){
                    Vertex _v = rn_leps.getVertexList().get(_e.getSp_id());
                    subgraph.getVertexList().put(_e.getSp_id(),_v);
                }
                if(!subgraph.getVertexList().containsKey(_e.getEp_id())){
                    Vertex _v = rn_leps.getVertexList().get(_e.getEp_id());
                    subgraph.getVertexList().put(_e.getEp_id(),_v);
                }
            }
        }
        return subgraph;
    }

    /**
     * 判断一个移动对象位置点集是否同簇
     * @param locationSet 移动对象位置点集
     * @param subgraph 位置点集所在的子图
     * @param objectsInOneCluster 用于记录子图中同簇的点,核心点标记为core,非核心点标记为notsure
     * @return 同簇返回true，不同簇返回false
     */
    private boolean isInOneCluster(List<SnapObject> locationSet,RoadNetwork subgraph,HashMap<String,String> objectsInOneCluster){
        boolean isIn = false;
        //如果集合中某个点不在对象集中，则说明该点已加入其它簇，则返回false
        for(int i=0;i<locationSet.size();i++){
            if(!snapshotObjectSet.containsKey(locationSet.get(i).getId()))
                return false;
        }
        //遍历子图每个顶点，判断0.5eps邻域内是否满足minpts+1的要求，标记核心点
        HashMap<String,Vertex> vertexList = subgraph.getVertexList();
        for(Map.Entry<String,Vertex> entry:vertexList.entrySet()){
            Vertex _v = entry.getValue();
            //获取顶点的0.5eps邻域
             List<String> halfEps = rnRQ.getNNOfNode(_v,this.eps/2);
            //若集合为空，则计算该顶点邻接顶点两两间的最小距离，若最小距离大于eps，则不同簇
            if(halfEps.size()==0){
                double minDist = getMinDistForVertexs(_v);
                if(minDist>=this.eps)
                    return false;
            }
            //若集合内的移动对象数量大于(minpts+1)则均为核心点，插入子图核心点集中；否则，判断将邻接边的点加进来是否大于(minpts+1)
            if(halfEps.size()>=minpts+1){
                for(int i=0;i<halfEps.size();i++){
                    SnapObject so = snapshotObjectSet.get(halfEps.get(i));
                    if(so==null)
                        continue;
                    objectsInOneCluster.put(so.getId(),"core");
                    //与so同边的点放入同簇点集中，标注为notsure
                    List<String> objectInOneEdge = this.edgeObjectIndex.get(so.getEdgeid());
                    for(int j=0;j<objectInOneEdge.size();j++){
                        if(!objectsInOneCluster.containsKey(objectInOneEdge.get(j))&&snapshotObjectSet.containsKey(objectInOneEdge.get(j)))
                            objectsInOneCluster.put(objectInOneEdge.get(j),"notsure");
                    }
                }
            }else{
                //判断将邻接边的点加进来是否大于(minpts+1)
//                for(int i=0;i<_v.getAdjEdges().size();i++){
//                    //获取一条邻接边上的所有移动对象
//                    List<String> objectInEdge = this.edgeObjectIndex.get(_v.getAdjEdges().get(i));
//                    if(objectInEdge==null)
//                        continue;
//                    //计算该边移动对象与half邻域内的移动对象总和
//                    int count = halfEps.size();
//                    for(int j=0;j<objectInEdge.size();j++){
//                        if(!halfEps.contains(objectInEdge.get(j)))
//                            count++;
//                    }
//                    //若总和超过(minpts+1)，则该边及邻域内的移动对象同簇，该边上在邻域内的对象为核心点
//                    if(count>=minpts+1){
//                        for(int j=0;j<halfEps.size();j++){
//                            SnapObject so = snapshotObjectSet.get(halfEps.get(j));
//                            //如果so在边上，则为核心点
//                            if(so.getEdgeid().equals(_v.getAdjEdges().get(i)))
//                                objectsInOneCluster.put(so.getId(),"core");
//                            else if(!objectsInOneCluster.containsKey(so.getId()))
//                                objectsInOneCluster.put(so.getId(),"notsure");
//                        }
//                        //遍历边上的点，将点插入到同簇表中
//                        for(int j=0;j<objectInEdge.size();j++){
//                            if(!objectsInOneCluster.containsKey(objectInEdge.get(j)))
//                                objectsInOneCluster.put(objectInEdge.get(j),"notsure");
//                        }
//                    }
//                }
                //查询每个点是否为核心点，若为核心点，将该点及其邻域内的点放入同簇表中
                for(int i=0;i<halfEps.size();i++){
                    if(objectsInOneCluster.containsKey(halfEps.get(i)))
                        if(objectsInOneCluster.get(halfEps.get(i)).equals("core"))
                            continue;
                    List<String> nnOfObject = rnRQ.getResultObjects(halfEps.get(i));
                    if(nnOfObject.size()>=minpts){
                        if(snapshotObjectSet.containsKey(halfEps.get(i)))
                            objectsInOneCluster.put(halfEps.get(i),"core");
                        for(int j=0;j<nnOfObject.size();j++)
                            if(!objectsInOneCluster.containsKey(nnOfObject.get(j))&&snapshotObjectSet.containsKey(nnOfObject.get(j)))
                                objectsInOneCluster.put(nnOfObject.get(j),"notsure");
                    }
                }
            }
        }
        //判断给定的移动对象集合是否都在同簇表中，都在则同簇
        for(int i=0;i<locationSet.size();i++){
            if(!objectsInOneCluster.containsKey(locationSet.get(i).getId()))
                break;
            if(i==locationSet.size()-1)
                isIn = true;
        }

        return isIn;
    }


    /**
     * 获取顶点邻接顶点间的最短直线距离（欧氏距离）
     * @param v 路网顶点
     * @return 其邻接顶点的最短直线距离
     */
    private double getMinDistForVertexs(Vertex v){
        double minDist = Double.MAX_VALUE;
        //获取顶点的邻接顶点列表
        List<Vertex> adjVertexList = new ArrayList<>();
        for(int i=0;i<v.getAdjEdges().size();i++){
            Edge adjE = this.rn_leps.getEdgeList().get(v.getAdjEdges().get(i));
            if(adjE.getSp_id().equals(v.getId()))
                adjVertexList.add(this.rn_leps.getVertexList().get(adjE.getEp_id()));
            else
                adjVertexList.add(this.rn_leps.getVertexList().get(adjE.getSp_id()));
        }
        //双重循环计算顶点间的距离，取最短
        for(int i=0;i<adjVertexList.size();i++){
            for(int j=i+1;j<adjVertexList.size();j++){
                Vertex v1 = adjVertexList.get(i);
                Vertex v2 = adjVertexList.get(j);
                double dist = TimeConversion.getDistance(v1.getLat(),v1.getLng(),v2.getLat(),v2.getLng());
                if(dist<minDist)
                    minDist = dist;
            }
        }
        return minDist;
    }
}
