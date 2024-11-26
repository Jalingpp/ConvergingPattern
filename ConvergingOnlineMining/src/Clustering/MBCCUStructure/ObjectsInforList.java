package Clustering.MBCCUStructure;

import Classes.Cluster;
import Classes.SnapObject;
import Clustering.FVARangeQuery.RNRangeQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 移动对象位置信息边
 * @Author JJP
 * @Date 2021/11/16 9:20
 */
public class ObjectsInforList {
    String currTime;  //当前时间戳
    int minpts;  //密度阈值
    HashMap<String,ObjectInforNode> objectsInforList;  //移动对象位置信息表

    /**
     * 构造函数
     * @param currTime  当前时间戳
     * @param snapshotObjectSet 移动对象位置点集合
     * @param currentClusterSet 当前簇集
     * @param rnRQ_leps  路网中的范围查询对象
     * @param minpts  密度阈值
     */
    public ObjectsInforList(String currTime, HashMap<String, SnapObject> snapshotObjectSet, HashMap<String, Cluster> currentClusterSet, RNRangeQuery rnRQ_leps,int minpts){
        this.currTime = currTime;
        this.minpts = minpts;
        this.objectsInforList = new HashMap<>();
        //先遍历簇中的点，创建对象信息节点
        for(Map.Entry<String,Cluster> entry:currentClusterSet.entrySet()){
            Cluster cluster = entry.getValue();
            for(int i=0;i<cluster.getMenberPoints().size();i++){
                String so = cluster.getMenberPoints().get(i).getId();
                if(objectsInforList.containsKey(so))
                    continue;
                List<String> neighbors = rnRQ_leps.getResultObjects(so);
                if(neighbors.size()>=minpts){
                    ObjectInforNode oin = new ObjectInforNode(so,"C",cluster.getId(),neighbors);
                    this.objectsInforList.put(so,oin);
                }else{
                    ObjectInforNode oin = new ObjectInforNode(so,"B",cluster.getId(),neighbors);
                    this.objectsInforList.put(so,oin);
                }
            }
        }
        //遍历移动对象点集，将剩余点创建为噪音点插入位置点信息表中
        for(Map.Entry<String,SnapObject> entry:snapshotObjectSet.entrySet()){
            SnapObject so = entry.getValue();
            if(!objectsInforList.containsKey(so.getId())){
                List<String> neighbors = rnRQ_leps.getResultObjects(so.getId());
                ObjectInforNode oin = new ObjectInforNode(so.getId(),"N",null,neighbors);
                this.objectsInforList.put(so.getId(),oin);
            }
        }
    }

    /**
     * 根据待删除的点更新位置点信息表和剩余点的信息，对前核心点集作补充
     * @param objectsToDelete
     * @param exCores
     */
    public void updatingByObjectsToDelete(HashMap<String, SnapObject> objectsToDelete, HashMap<String, ObjectInforNode> exCores) {
        //把待删除点的信息删掉
        for (Map.Entry<String, SnapObject> entry : objectsToDelete.entrySet()) {
            this.objectsInforList.remove(entry.getKey());
        }
        //把剩余点的信息做个备份
        HashMap<String, ObjectInforNode> restOIN = new HashMap<>();
        for (Map.Entry<String, ObjectInforNode> entry : objectsInforList.entrySet()) {
            restOIN.put(entry.getKey(), new ObjectInforNode(entry.getValue()));
        }
        //在待删点的邻居的邻域中删除已删点，并标记新的前核心点
        for (Map.Entry<String, ObjectInforNode> entry : objectsInforList.entrySet()) {
            ObjectInforNode oin = entry.getValue();
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
    public void updatingByIncObjects(HashMap<String,ObjectInforNode> incOINs,HashMap<String, ObjectInforNode> neoCores){
        //把新增的位置点信息插入到位置信息表中
        for(Map.Entry<String,ObjectInforNode> entry:incOINs.entrySet()){
            objectsInforList.put(entry.getKey(),entry.getValue());
        }
        //遍历新增点的邻域，将其邻居的邻域中加入该新增点
        for(Map.Entry<String,ObjectInforNode> entry:incOINs.entrySet()){
            ObjectInforNode oin = entry.getValue();
            for(int i=0;i<oin.getNeighbors().size();i++){
                String neighborID = oin.getNeighbors().get(i);
                ObjectInforNode neighborIN = objectsInforList.get(neighborID);
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
        for(Map.Entry<String,ObjectInforNode> entry:objectsInforList.entrySet()){
            ObjectInforNode oin = entry.getValue();
            if(oin.getNeighbors().size()>=minpts&&!oin.getType().equals("C")){
                oin.setType("C");
                objectsInforList.put(oin.getId(),oin);
                neoCores.put(oin.getId(),oin);
            }
        }
    }

    public HashMap<String, ObjectInforNode> getObjectsInforList() {
        return objectsInforList;
    }
}
