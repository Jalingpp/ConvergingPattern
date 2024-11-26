package Clustering;

import Classes.Cluster;
import Classes.RoadNetwork;
import Classes.SnapObject;
import Clustering.FVARangeQuery.RNRangeQuery;
import Clustering.MBCCUStructure.ObjectInforNode;
import Clustering.MBCCUStructure.ObjectsInforList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description:基于最小外接核心点集的簇更新算法
 * @Author JJP
 * @Date 2021/11/15 10:21
 */
public class MBCClusterUpdating {
    RoadNetwork rn_leps;  //路网
    RNRangeQuery rnRQ_leps;  //范围查询对象
    HashMap<String, Cluster> preClusterSet;  //前一时间窗口的簇集
    HashMap<String, SnapObject> snapshotObjectSet;  //更新之后的移动对象集合
    HashMap<String, SnapObject> incrementalObjectSet; //增量的移动对象集合
    HashMap<String, SnapObject> objectsToDelete; //待删除的移动对象位置点集（被更新的位置覆盖）
    ObjectsInforList objectsInforList;  //移动对象位置点信息表
    int eps; //聚类半径
    int minpts; //聚类密度
    int mc;  ////成员数量阈值
    String curTime;  //当前时间戳
    int clusterAmount;  //记录簇的数量，用于编号

    HashMap<String, ObjectInforNode> exCores;  //前核心点集
    HashMap<String, ObjectInforNode> neoCores;  //新核心点集
    HashMap<String, ObjectInforNode> incOINs;  //用于记录所有新增点的信息

    /**
     * 构造函数
     * @param rn_leps
     * @param preClusterSet
     * @param snapshotObjectSet
     * @param incrementalObjectSet
     * @param objectsToDelete
     * @param objectsInforList
     * @param eps
     * @param minpts
     * @param mc
     * @param curTime
     */
    public MBCClusterUpdating(RoadNetwork rn_leps, RNRangeQuery rnRQ_leps, HashMap<String, Cluster> preClusterSet, HashMap<String, SnapObject> snapshotObjectSet, HashMap<String, SnapObject> incrementalObjectSet, HashMap<String, SnapObject> objectsToDelete, ObjectsInforList objectsInforList, int eps, int minpts, int mc, String curTime) {
        this.rn_leps = rn_leps;
        this.rnRQ_leps = rnRQ_leps;
        this.preClusterSet = preClusterSet;
        this.snapshotObjectSet = snapshotObjectSet;
        this.incrementalObjectSet = incrementalObjectSet;
        this.objectsToDelete = objectsToDelete;
        this.objectsInforList = objectsInforList;
        this.eps = eps;
        this.minpts = minpts;
        this.mc = mc;
        this.curTime = curTime;
        exCores = new HashMap<>();
        neoCores = new HashMap<>();
        incOINs = new HashMap<>();  //用于记录所有新增点的信息
        clusterAmount = 0;
    }

    /**
     * 核心函数，基于MBC更新簇
     * @param currentClusterSet  当前的聚类结果
     */
    public void MBCClusterUpdating(HashMap<String, Cluster> currentClusterSet){
        rnRQ_leps.update(snapshotObjectSet,minpts);
        //遍历待删点集，识别前核心点
        for(Map.Entry<String,SnapObject> entry:objectsToDelete.entrySet()){
            SnapObject delPoint = entry.getValue();
            ObjectInforNode delOIN = objectsInforList.getObjectsInforList().get(delPoint.getId());
            if(delOIN==null)
                continue;
            if(delOIN.getType().equals("C")){
                ObjectInforNode _delOIN = new ObjectInforNode(delOIN);
                exCores.put(_delOIN.getId(),_delOIN);
            }
        }
        //根据待删点集更新位置点信息
        this.objectsInforList.updatingByObjectsToDelete(objectsToDelete,exCores);
        //根据前核心点更新簇
        updateClustersByExCores(currentClusterSet);
        //遍历新增点集，为所有点创建位置信息记录，并识别新核心点
        for(Map.Entry<String,SnapObject> entry:incrementalObjectSet.entrySet()){
            SnapObject newPoint = entry.getValue();
            List<String> neighbors = rnRQ_leps.getResultObjects(newPoint.getId());
            ObjectInforNode newOIN = new ObjectInforNode(newPoint.getId(), "C",null,neighbors);
            if(neighbors.size()>=minpts){
                neoCores.put(newOIN.getId(),newOIN);
                incOINs.put(newOIN.getId(),newOIN);
            }else{
                ObjectInforNode newIncOIN = new ObjectInforNode(newOIN);
                newIncOIN.setType("unknown");
                incOINs.put(newIncOIN.getId(),newIncOIN);
            }
        }
        //根据新增点集更新位置点信息
        this.objectsInforList.updatingByIncObjects(incOINs,neoCores);
        //根据新核心点更新簇
        updateClusterByNeoCores(currentClusterSet);

    }

    /**
     * 通过前核心点集更新簇
     */
    private void updateClustersByExCores(HashMap<String, Cluster> currentClusterSet){
        //获取前核心点集中所有的前密度可达核心点集
        List<List<String>> esList = getES();
        //遍历前密度可达核心点集列表的每个前密度可达核心点集
        for(int i=0;i<esList.size();i++){
            List<String> es = esList.get(i);
            //获取es的最小外接核心点集
            List<String> mbc4es = getMBC_ES(es);
            //如果es的mbc不为空，则找mbc4es的连通分量
            if(mbc4es.size()>0){
                HashMap<String,List<String>> nccs4mbc = getNCC(mbc4es);
                //每一个连通分量可以成为一个簇（由原始簇分裂而来）
                getNewClusters(nccs4mbc,currentClusterSet);
            }
        }
    }
    /**
     * 获取所有的前密度可达核心点集
     * @return List<List<String>> 前密度可达核心点集的列表，前密度可达核心点集为List<String>
     */
    private List<List<String>> getES(){
        List<List<String>> esList = new ArrayList<>();
        List<String> visitedExCores = new ArrayList<>();
        for(Map.Entry<String,ObjectInforNode> entry:exCores.entrySet()){
            if(visitedExCores.contains(entry.getKey()))
                continue;
            ObjectInforNode oin = entry.getValue();
            List<String> ESofOin = new ArrayList<>();
            ESofOin.add(oin.getId());
            int pointer = 0; //指针，用于指示ESofOin中访问到的对象
            while(pointer<ESofOin.size()){
                ObjectInforNode pointedExcore = exCores.get(ESofOin.get(pointer));
                //访问其邻居，将其中的前核心点加入到该前密度可达核心点集中
                for(int i=0;i<pointedExcore.getNeighbors().size();i++){
                    if(exCores.containsKey(pointedExcore.getNeighbors().get(i))&&!ESofOin.contains(pointedExcore.getNeighbors().get(i))) {
                        ESofOin.add(pointedExcore.getNeighbors().get(i));
                        visitedExCores.add(pointedExcore.getNeighbors().get(i));
                    }
                }
                pointer++;
            }
            esList.add(ESofOin);
        }
        return esList;
    }
    /**
     * 获取前密度可达核心点集的MBR
     * @param es   前密度可达核心点集
     * @return
     */
    private List<String> getMBC_ES(List<String> es){
        List<String> mbc = new ArrayList<>();
        for(int i=0;i<es.size();i++){
            ObjectInforNode ec = exCores.get(es.get(i));
            //遍历其邻居，将类型为C的放入mbc中
            for(int j=0;j<ec.getNeighbors().size();j++){
                ObjectInforNode neighbor = objectsInforList.getObjectsInforList().get(ec.getNeighbors().get(j));
                if(neighbor==null)
                    continue;
                if(neighbor.getType().equals("C")){
                    if(!mbc.contains(neighbor.getId())){
                        mbc.add(neighbor.getId());
                    }
                }
            }
        }
        return mbc;
    }
    /**
     * 计算mbc中的连通分量
     * @param mbc
     * @return HashMap<String,List<String>> MBC中与String连通的点集
     */
    private HashMap<String,List<String>> getNCC(List<String> mbc){
        HashMap<String,List<String>> reachablePoints = new HashMap<>();  //用于记录点密度可达的点集
        for(int i=0;i<mbc.size();i++){
            ObjectInforNode oin = this.objectsInforList.getObjectsInforList().get(mbc.get(i));
            boolean isIn = false;//判断oin是否在某个连通分量的列表中
            for(Map.Entry<String,List<String>> entry:reachablePoints.entrySet()){
                List<String> rps = entry.getValue();
                if(rps.contains(oin.getId())){
                    isIn = true;
                    break;
                }
            }
            //如果不属于任何一个连通分量，则以之为起点，创建一个新的连通分量
            if(!isIn){
                List<String> rps4oin = new ArrayList<>();
                int pointer = 0; //指针指示访问到的点在rps4oin中的位置
                List<String> queue = new ArrayList<>();
                queue.add(oin.getId());
                while(pointer<queue.size()){
                    ObjectInforNode poin = this.objectsInforList.getObjectsInforList().get(queue.get(pointer));
                    if(!rps4oin.contains(poin.getId()))
                        rps4oin.add(poin.getId());
                    //验证其邻居是否可以继续扩展，若为核心点放入队列中待扩展，若为非核心点，设定类型为B，放入可达点列表中
                    for(int j=0;j<poin.getNeighbors().size();j++){
                        ObjectInforNode neighbor = this.objectsInforList.getObjectsInforList().get(poin.getNeighbors().get(j));
                        if(neighbor==null)
                            continue;
                        if(neighbor.getType().equals("C")){
                            if(!queue.contains(neighbor.getId()))
                                queue.add(neighbor.getId());
                        }else {
                            neighbor.setType("B");
                            objectsInforList.getObjectsInforList().put(neighbor.getId(),neighbor);
                            if(!rps4oin.contains(neighbor.getId()))
                                rps4oin.add(neighbor.getId());
                        }
                    }
                    pointer++;
                }
                reachablePoints.put(oin.getId(),rps4oin);
            }
        }
        return reachablePoints;
    }
    /**
     * 根据各连通分量创建新簇，一个连通分量为一个新簇
     * @param nccs  连通分量表
     */
    private void getNewClusters(HashMap<String,List<String>> nccs,HashMap<String, Cluster> currentClusterSet){
        for(Map.Entry<String,List<String>> entry:nccs.entrySet()){
            List<String> memberList = entry.getValue();
            Cluster cluster = new Cluster(String.valueOf(clusterAmount),curTime);
            clusterAmount++;
            for(int i=0;i<memberList.size();i++){
                SnapObject so = snapshotObjectSet.get(memberList.get(i));
                objectsInforList.getObjectsInforList().get(memberList.get(i)).setCid(cluster.getId());
                cluster.addPoint(so);
            }
            currentClusterSet.put(cluster.getId(),cluster);
        }
    }


    /**
     * 通过新核心点集更新簇
     */
    private void updateClusterByNeoCores(HashMap<String, Cluster> currentClusterSet){
        //获取新核心点集中所有的新密度可达核心点集
        List<List<String>> nsList = getNS();
        //遍历每个新密度可达核心点集
        for(int i=0;i<nsList.size();i++){
            List<String> ns = nsList.get(i);
            //获取ns的最小外接核心点集
            List<String> mbc4ns = getMBC_NS(ns);
            //若最小外接核心点集为空，则新增簇仅包含ns;否则验证mbc中的对象是否来自同一个簇：同簇，则将ns加入到该簇中，否则，将不同的簇合并
            if(mbc4ns.size()==0){
                addCluster(ns,currentClusterSet);
            }else {
                List<String> clusterList = getClustersInMBC(mbc4ns);
                //如果mbc中的点属于同一个簇，则将ns加入该簇中
                if(clusterList.size()==1){
                    Cluster cluster = currentClusterSet.get(clusterList.get(0));
                    if(cluster==null)
                        continue;
                    for(int j=0;j<ns.size();j++){
                        SnapObject nsso = snapshotObjectSet.get(ns.get(j));
                        cluster.addPoint(nsso);
                    }
                    currentClusterSet.put(cluster.getId(),cluster);
                }else if(clusterList.size()>1){
                    //若来自不同的簇，则将这些簇合并成一个新的簇，并将ns加入其中
                    Cluster cluster = new Cluster(String.valueOf(clusterAmount),curTime);
                    clusterAmount++;
                    for(int j=0;j<clusterList.size();j++){
                        Cluster c1 = currentClusterSet.get(clusterList.get(j));
                        if(c1==null)
                            continue;
                        for(int k=0;k<c1.getMenberPoints().size();k++){
                            cluster.addPoint(c1.getMenberPoints().get(k));
                            objectsInforList.getObjectsInforList().get(c1.getMenberPoints().get(k).getId()).setCid(cluster.getId());
                        }
                    }
                    //将ns加入其中
                    for(int j=0;j<ns.size();j++){
                        SnapObject nsso = snapshotObjectSet.get(ns.get(j));
                        cluster.addPoint(nsso);
                        objectsInforList.getObjectsInforList().get(ns.get(j)).setCid(cluster.getId());
                    }
                }
            }

        }
    }
    /**
     * 获取所有的前密度可达核心点集
     * @return List<List<String>> 前密度可达核心点集的列表，前密度可达核心点集为List<String>
     */
    private List<List<String>> getNS(){
        List<List<String>> nsList = new ArrayList<>();
        List<String> visitedNeoCores = new ArrayList<>();
        for(Map.Entry<String,ObjectInforNode> entry:neoCores.entrySet()){
            if(visitedNeoCores.contains(entry.getKey()))
                continue;
            ObjectInforNode oin = entry.getValue();
            List<String> NSofOin = new ArrayList<>();
            NSofOin.add(oin.getId());
            int pointer = 0; //指针，用于指示NSofOin中访问到的对象
            while(pointer<NSofOin.size()){
                ObjectInforNode pointedNeocore = neoCores.get(NSofOin.get(pointer));
                //访问其邻居，将其中的新核心点加入到该前密度可达核心点集中
                for(int i=0;i<pointedNeocore.getNeighbors().size();i++){
                    if(neoCores.containsKey(pointedNeocore.getNeighbors().get(i))&&!NSofOin.contains(pointedNeocore.getNeighbors().get(i))) {
                        NSofOin.add(pointedNeocore.getNeighbors().get(i));
                        visitedNeoCores.add(pointedNeocore.getNeighbors().get(i));
                    }
                }
                pointer++;
            }
            nsList.add(NSofOin);
        }
        return nsList;
    }
    /**
     * 获取新密度可达核心点集的MBR
     * @param ns   新密度可达核心点集
     * @return
     */
    private List<String> getMBC_NS(List<String> ns){
        List<String> mbc = new ArrayList<>();
        for(int i=0;i<ns.size();i++){
            ObjectInforNode nc = neoCores.get(ns.get(i));
            //遍历其邻居，将类型为C的放入mbc中
            for(int j=0;j<nc.getNeighbors().size();j++){
                //如果邻居为新增的核心点，则不予处理
                if(incOINs.containsKey(nc.getNeighbors().get(j))||neoCores.containsKey(nc.getNeighbors().get(j)))
                    continue;
                ObjectInforNode neighbor = objectsInforList.getObjectsInforList().get(nc.getNeighbors().get(j));
                if(neighbor==null)
                    continue;
                if(neighbor.getType().equals("C")){
                    if(!mbc.contains(neighbor.getId())){
                        mbc.add(neighbor.getId());
                    }
                }
            }
        }
        return mbc;
    }
    /**
     * 用新密度可达核心点集新增一个簇
     * @param ns 新密度可达核心点集
     */
    private void addCluster(List<String> ns,HashMap<String, Cluster> currentClusterSet){
        Cluster cluster = new Cluster(String.valueOf(clusterAmount),curTime);
        clusterAmount++;
        for(int i=0;i<ns.size();i++){
            SnapObject so = snapshotObjectSet.get(ns.get(i));
            cluster.addPoint(so);
            objectsInforList.getObjectsInforList().get(so.getId()).setCid(cluster.getId());
        }
        currentClusterSet.put(cluster.getId(),cluster);
    }
    /**
     * 获取MBC中点所在的簇的列表
     * @param mbc
     * @return
     */
    private List<String> getClustersInMBC(List<String> mbc){
        List<String> clusters = new ArrayList<>();
        for(int i=0;i<mbc.size();i++){
            ObjectInforNode oin = objectsInforList.getObjectsInforList().get(mbc.get(i));
            if(!clusters.contains(oin.getCid()))
                clusters.add(oin.getCid());
        }
        return clusters;
    }

}
