package Clustering.MBKCUStructure;

import Classes.Cluster;
import Classes.SnapObject;
import Clustering.FVARangeQuery.RNRangeQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: �ƶ�����λ����Ϣ��
 * @Author JJP
 * @Date 2021/11/16 9:20
 */
public class ObjectsInforListKP {
    String currTime;  //��ǰʱ���
    int minpts;  //�ܶ���ֵ
    HashMap<String, ObjectInforNodeKP> objectsInforList;  //�ƶ�����λ����Ϣ��
    HashMap<String,List<String>> objectListInEdges;  //���ϵ��ƶ������б��������ľ�����������

    /**
     * ���캯��
     * @param currTime  ��ǰʱ���
     * @param snapshotObjectSet �ƶ�����λ�õ㼯��
     * @param currentClusterSet ��ǰ�ؼ�
     * @param rnRQ_leps  ·���еķ�Χ��ѯ����
     * @param minpts  �ܶ���ֵ
     */
    public ObjectsInforListKP(String currTime, HashMap<String, SnapObject> snapshotObjectSet, HashMap<String, Cluster> currentClusterSet, RNRangeQuery rnRQ_leps, int minpts){
        this.currTime = currTime;
        this.minpts = minpts;
        this.objectsInforList = new HashMap<>();
        this.objectListInEdges = new HashMap<>();
        //�������ϵ��ƶ������б�
        updateObjectsInEdges(snapshotObjectSet);
        //Ϊ�������ж��󴴽�λ����Ϣ��¼
        for(Map.Entry<String,List<String>> entry:objectListInEdges.entrySet()){
            List<String> objectList = entry.getValue();
            //Ϊ�����ʼ������ƶ����󴴽���Ϣ��¼
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
                //Ϊ������һ��������ƶ����󴴽���Ϣ��¼
                String object2 = objectList.get(objectList.size()-1);
                List<String> neighbors2 = rnRQ_leps.getResultObjects(object2);
                if(neighbors2.size()>=minpts){
                    ObjectInforNodeKP oin2 = new ObjectInforNodeKP(object2,"C",true,null,neighbors2);
                    this.objectsInforList.put(object2,oin2);
                }else{
                    ObjectInforNodeKP oin2 = new ObjectInforNodeKP(object2,"N",false,null,neighbors2);
                    this.objectsInforList.put(object2,oin2);
                }
                //Ϊʣ��ĵ㴴����Ϣ��¼
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
        //�ȱ������еĵ㣬���ö�����Ϣ�еĴ�ID
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
     * ����ÿ�����ϵ��ƶ������б�
     * @param snapshotObjectSet  �ƶ�����λ�õ㼯��
     */
    public void updateObjectsInEdges(HashMap<String,SnapObject> snapshotObjectSet){
        objectListInEdges.clear();
        for(Map.Entry<String,SnapObject> entry:snapshotObjectSet.entrySet()){
            SnapObject so = entry.getValue();
            String edge = so.getEdgeid();
            //���so���ڵı��Ѿ����ڣ���so���뵽�ñߵĶ����б���
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
                //��������ڸñߣ��򴴽��ñߵļ�¼���뵽����
                List<String> edgeList = new ArrayList<>();
                edgeList.add(so.getId());
                objectListInEdges.put(edge,edgeList);
            }
        }
    }

    /**
     * ���ݴ�ɾ���ĵ����λ�õ���Ϣ���ʣ������Ϣ����ǰ���ĵ㼯������
     * @param objectsToDelete
     * @param exCores
     */
    public void updatingByObjectsToDelete(HashMap<String, SnapObject> objectsToDelete, HashMap<String, ObjectInforNodeKP> exCores) {
        //�Ѵ�ɾ�������Ϣɾ��
        for (Map.Entry<String, SnapObject> entry : objectsToDelete.entrySet()) {
            this.objectsInforList.remove(entry.getKey());
        }
        //��ʣ������Ϣ��������
        HashMap<String, ObjectInforNodeKP> restOIN = new HashMap<>();
        for (Map.Entry<String, ObjectInforNodeKP> entry : objectsInforList.entrySet()) {
            restOIN.put(entry.getKey(), new ObjectInforNodeKP(entry.getValue()));
        }
        //�ڴ�ɾ����ھӵ�������ɾ����ɾ�㣬������µ�ǰ���ĵ�
        for (Map.Entry<String, ObjectInforNodeKP> entry : objectsInforList.entrySet()) {
            ObjectInforNodeKP oin = entry.getValue();
            //���������ھ�
            for (int i = oin.getNeighbors().size()-1; i >=0; i--) {
                String neighbor = oin.getNeighbors().get(i);
                //�����ھӳ����ڴ�ɾ�Ӽ��У�����ɾ��
                if (objectsToDelete.containsKey(neighbor)) {
                    oin.getNeighbors().remove(i);
                }
            }
            //���ɾ���ھӺ��ɺ��ĵ��Ϊ�Ǻ��ĵ㣬�������ǰ���ĵ㼯��
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
     * ����������λ����Ϣ����λ����Ϣ���������º��ĵ�
     * @param incOINs �������λ����Ϣ
     * @param neoCores �º��ĵ㼯
     */
    public void updatingByIncObjects(HashMap<String, ObjectInforNodeKP> incOINs, HashMap<String, ObjectInforNodeKP> neoCores){
        //��������λ�õ���Ϣ���뵽λ����Ϣ����
        for(Map.Entry<String, ObjectInforNodeKP> entry:incOINs.entrySet()){
            objectsInforList.put(entry.getKey(),entry.getValue());
        }
        //��������������򣬽����ھӵ������м����������
        for(Map.Entry<String, ObjectInforNodeKP> entry:incOINs.entrySet()){
            ObjectInforNodeKP oin = entry.getValue();
            for(int i=0;i<oin.getNeighbors().size();i++){
                String neighborID = oin.getNeighbors().get(i);
                ObjectInforNodeKP neighborIN = objectsInforList.get(neighborID);
                if(neighborIN==null)
                    continue;
                //����ھӵ������в����ڸõ㣬�����õ�
                if(!neighborIN.getNeighbors().contains(oin.getId())){
                    neighborIN.addNeighbor(oin.getId());
                    objectsInforList.put(neighborIN.getId(),neighborIN);
                }
            }
        }
        //����ʣ��ĵ㣬������Ϊ�Ǻ��ĵ㣬�����򳬹���ֵ������ӵ��º��ĵ㼯��
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
