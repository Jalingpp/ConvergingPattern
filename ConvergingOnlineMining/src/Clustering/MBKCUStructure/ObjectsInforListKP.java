package Clustering.MBKCUStructure;

import Classes.Cluster;
import Classes.SnapObject;
import Clustering.FVARangeQuery.RNRangeQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 移动对象位置信息边
 * @Author JJP
 * @Date 2021/11/16 9:20
 */
public class ObjectsInforListKP {
    String currTime;  //当前时间戳
    int minpts;  //密度阈值
    HashMap<String, ObjectInforNodeKP> objectsInforList;  //移动对象位置信息表
    HashMap<String,List<String>> objectListInEdges;  //边上的移动对象列表（按到起点的距离升序排序）

    /**
     * 构造函数
     * @param currTime  当前时间戳
     * @param snapshotObjectSet 移动对象位置点集合
     * @param currentClusterSet 当前簇集
     * @param rnRQ_leps  路网中的范围查询对象
     * @param minpts  密度阈值
     */
    public ObjectsInforListKP(String currTime, HashMap<String, SnapObject> snapshotObjectSet, HashMap<String, Cluster> currentClusterSet, RNRangeQuery rnRQ_leps, int minpts){
        this.currTime = currTime;
        this.minpts = minpts;
        this.objectsInforList = new HashMap<>();
        this.objectListInEdges = new HashMap<>();
        //创建边上的移动对象列表
        updateObjectsInEdges(snapshotObjectSet);
        //为边上所有对象创建位置信息记录
        for(Map.Entry<String,List<String>> entry:objectListInEdges.entrySet()){
            List<String> objectList = entry.getValue();
            //为最靠近起始顶点的移动对象创建信息记录
            String object = objectList.get(0);
            List<String> neighbors = rnRQ_leps.getResultObjects(object);
            if(neighbors.size()>=minpts){
                ObjectInforNodeKP oin = new ObjectInforNodeKP(object,"C",true,null,neighbors);
                this.objectsInforList.put(object,oin);
            }else{
                ObjectInforNodeKP oin = new ObjectInforNodeKP(object,"N",false,null,neighbors);
                this.objectsInforList.put(object,oin);
            }
            if(objectList.size()>1){
                //为靠近另一个顶点的移动对象创建信息记录
                String object2 = objectList.get(objectList.size()-1);
                List<String> neighbors2 = rnRQ_leps.getResultObjects(object2);
                if(neighbors2.size()>=minpts){
                    ObjectInforNodeKP oin2 = new ObjectInforNodeKP(object2,"C",true,null,neighbors2);
                    this.objectsInforList.put(object2,oin2);
                }else{
                    ObjectInforNodeKP oin2 = new ObjectInforNodeKP(object2,"N",false,null,neighbors2);
                    this.objectsInforList.put(object2,oin2);
                }
                //为剩余的点创建信息记录
                for(int i=1;i<objectList.size()-1;i++){
                    String objectMid = objectList.get(i);
                    List<String> neighborsMid = new ArrayList<>();
                    neighborsMid.add(objectList.get(0));
                    neighborsMid.add(objectList.get(objectList.size()-1));
                    ObjectInforNodeKP oinkp = new ObjectInforNodeKP(objectMid, "N", false, null, neighborsMid);
                    this.objectsInforList.put(objectMid,oinkp);
                }
            }
        }
        //先遍历簇中的点，设置对象信息中的簇ID
        for(Map.Entry<String,Cluster> entry:currentClusterSet.entrySet()){
            Cluster cluster = entry.getValue();
            for(int i=0;i<cluster.getMenberPoints().size();i++){
                String so = cluster.getMenberPoints().get(i).getId();
                ObjectInforNodeKP oinkp = objectsInforList.get(so);
                if(oinkp==null){
                    List<String> neighbors = rnRQ_leps.getResultObjects(so);
                    if(neighbors.size()>=minpts){
                        ObjectInforNodeKP newoin = new ObjectInforNodeKP(so,"C",false,cluster.getId(),neighbors);
                        objectsInforList.put(so,newoin);
                    }else{
                        ObjectInforNodeKP newoin = new ObjectInforNodeKP(so,"B",false,cluster.getId(),neighbors);
                        objectsInforList.put(so,newoin);
                    }
                    continue;
                }
                oinkp.setCid(cluster.getId());
                if(!oinkp.getType().equals("C"))
                    oinkp.setType("B");
                objectsInforList.put(so,oinkp);
            }
        }
    }

    /**
     * 更新每条边上的移动对象列表
     * @param snapshotObjectSet  移动对象位置点集合
     */
    public void updateObjectsInEdges(HashMap<String,SnapObject> snapshotObjectSet){
        objectListInEdges.clear();
        for(Map.Entry<String,SnapObject> entry:snapshotObjectSet.entrySet()){
            SnapObject so = entry.getValue();
            String edge = so.getEdgeid();
            //如果so所在的边已经存在，则将so插入到该边的对象列表中
            if(objectListInEdges.containsKey(edge)){
                List<String> objectList = objectListInEdges.get(edge);
                for(int i=0;i<objectList.size();i++){
                    if(so.getPos()<=snapshotObjectSet.get(objectList.get(i)).getPos()){
                        objectList.add(i,so.getId());
                        break;
                    }
                }
                objectListInEdges.put(edge,objectList);
            }else{
                //如果不存在该边，则创建该边的记录插入到表中
                List<String> edgeList = new ArrayList<>();
                edgeList.add(so.getId());
                objectListInEdges.put(edge,edgeList);
            }
        }
    }

    /**
     * 根据待删除的点更新位置点信息表和剩余点的信息，对前核心点集作补充
     * @param objectsToDelete
     * @param exCores
     */
    public void updatingByObjectsToDelete(HashMap<String, SnapObject> objectsToDelete, HashMap<String, ObjectInforNodeKP> exCores) {
        //把待删除点的信息删掉
        for (Map.Entry<String, SnapObject> entry : objectsToDelete.entrySet()) {
            this.objectsInforList.remove(entry.getKey());
        }
        //把剩余点的信息做个备份
        HashMap<String, ObjectInforNodeKP> restOIN = new HashMap<>();
        for (Map.Entry<String, ObjectInforNodeKP> entry : objectsInforList.entrySet()) {
            restOIN.put(entry.getKey(), new ObjectInforNodeKP(entry.getValue()));
        }
        //在待删点的邻居的邻域中删除已删点，并标记新的前核心点
        for (Map.Entry<String, ObjectInforNodeKP> entry : objectsInforList.entrySet()) {
            ObjectInforNodeKP oin = entry.getValue();
            //遍历所有邻居
            for (int i = oin.getNeighbors().size()-1; i >=0; i--) {
                String neighbor = oin.getNeighbors().get(i);
                //若该邻居出现在待删子集中，予以删除
                if (objectsToDelete.containsKey(neighbor)) {
                    oin.getNeighbors().remove(i);
                }
            }
            //如果删除邻居后由核心点变为非核心点，则将其加入前核心点集中
            if (oin.getType().equals("C") && oin.getNeighbors().size() < minpts) {
                oin.setType("uncertain");
                oin.setCid(null);
                objectsInforList.put(oin.getId(), oin);
                exCores.put(oin.getId(), restOIN.get(oin.getId()));
            } else if (oin.getNeighbors().size() == 0) {
                oin.setType("N");
                oin.setCid(null);
                objectsInforList.put(oin.getId(), oin);
            }
        }

    }

    /**
     * 根据新增点位置信息更新位置信息表，并补充新核心点
     * @param incOINs 新增点的位置信息
     * @param neoCores 新核心点集
     */
    public void updatingByIncObjects(HashMap<String, ObjectInforNodeKP> incOINs, HashMap<String, ObjectInforNodeKP> neoCores){
        //把新增的位置点信息插入到位置信息表中
        for(Map.Entry<String, ObjectInforNodeKP> entry:incOINs.entrySet()){
            objectsInforList.put(entry.getKey(),entry.getValue());
        }
        //遍历新增点的邻域，将其邻居的邻域中加入该新增点
        for(Map.Entry<String, ObjectInforNodeKP> entry:incOINs.entrySet()){
            ObjectInforNodeKP oin = entry.getValue();
            for(int i=0;i<oin.getNeighbors().size();i++){
                String neighborID = oin.getNeighbors().get(i);
                ObjectInforNodeKP neighborIN = objectsInforList.get(neighborID);
                if(neighborIN==null)
                    continue;
                //如果邻居的邻域中不存在该点，则插入该点
                if(!neighborIN.getNeighbors().contains(oin.getId())){
                    neighborIN.addNeighbor(oin.getId());
                    objectsInforList.put(neighborIN.getId(),neighborIN);
                }
            }
        }
        //遍历剩余的点，若类型为非核心点，但邻域超过阈值，则添加到新核心点集中
        for(Map.Entry<String, ObjectInforNodeKP> entry:objectsInforList.entrySet()){
            ObjectInforNodeKP oin = entry.getValue();
            if(oin.getNeighbors().size()>=minpts&&!oin.getType().equals("C")){
                oin.setType("C");
                objectsInforList.put(oin.getId(),oin);
                neoCores.put(oin.getId(),oin);
            }
        }
    }

    public HashMap<String, ObjectInforNodeKP> getObjectsInforList() {
        return objectsInforList;
    }

    public HashMap<String, List<String>> getObjectListInEdges() {
        return objectListInEdges;
    }
}
