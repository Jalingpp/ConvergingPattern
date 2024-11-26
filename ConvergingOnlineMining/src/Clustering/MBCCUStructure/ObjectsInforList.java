package Clustering.MBCCUStructure;

import Classes.Cluster;
import Classes.SnapObject;
import Clustering.FVARangeQuery.RNRangeQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: �ƶ�����λ����Ϣ��
 * @Author JJP
 * @Date 2021/11/16 9:20
 */
public class ObjectsInforList {
    String currTime;  //��ǰʱ���
    int minpts;  //�ܶ���ֵ
    HashMap<String,ObjectInforNode> objectsInforList;  //�ƶ�����λ����Ϣ��

    /**
     * ���캯��
     * @param currTime  ��ǰʱ���
     * @param snapshotObjectSet �ƶ�����λ�õ㼯��
     * @param currentClusterSet ��ǰ�ؼ�
     * @param rnRQ_leps  ·���еķ�Χ��ѯ����
     * @param minpts  �ܶ���ֵ
     */
    public ObjectsInforList(String currTime, HashMap<String, SnapObject> snapshotObjectSet, HashMap<String, Cluster> currentClusterSet, RNRangeQuery rnRQ_leps,int minpts){
        this.currTime = currTime;
        this.minpts = minpts;
        this.objectsInforList = new HashMap<>();
        //�ȱ������еĵ㣬����������Ϣ�ڵ�
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
        //�����ƶ�����㼯����ʣ��㴴��Ϊ���������λ�õ���Ϣ����
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
     * ���ݴ�ɾ���ĵ����λ�õ���Ϣ���ʣ������Ϣ����ǰ���ĵ㼯������
     * @param objectsToDelete
     * @param exCores
     */
    public void updatingByObjectsToDelete(HashMap<String, SnapObject> objectsToDelete, HashMap<String, ObjectInforNode> exCores) {
        //�Ѵ�ɾ�������Ϣɾ��
        for (Map.Entry<String, SnapObject> entry : objectsToDelete.entrySet()) {
            this.objectsInforList.remove(entry.getKey());
        }
        //��ʣ������Ϣ��������
        HashMap<String, ObjectInforNode> restOIN = new HashMap<>();
        for (Map.Entry<String, ObjectInforNode> entry : objectsInforList.entrySet()) {
            restOIN.put(entry.getKey(), new ObjectInforNode(entry.getValue()));
        }
        //�ڴ�ɾ����ھӵ�������ɾ����ɾ�㣬������µ�ǰ���ĵ�
        for (Map.Entry<String, ObjectInforNode> entry : objectsInforList.entrySet()) {
            ObjectInforNode oin = entry.getValue();
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
    public void updatingByIncObjects(HashMap<String,ObjectInforNode> incOINs,HashMap<String, ObjectInforNode> neoCores){
        //��������λ�õ���Ϣ���뵽λ����Ϣ����
        for(Map.Entry<String,ObjectInforNode> entry:incOINs.entrySet()){
            objectsInforList.put(entry.getKey(),entry.getValue());
        }
        //��������������򣬽����ھӵ������м����������
        for(Map.Entry<String,ObjectInforNode> entry:incOINs.entrySet()){
            ObjectInforNode oin = entry.getValue();
            for(int i=0;i<oin.getNeighbors().size();i++){
                String neighborID = oin.getNeighbors().get(i);
                ObjectInforNode neighborIN = objectsInforList.get(neighborID);
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
