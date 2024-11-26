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
 * @Description:������С��Ӻ��ĵ㼯�Ĵظ����㷨
 * @Author JJP
 * @Date 2021/11/15 10:21
 */
public class MBCClusterUpdating {
    RoadNetwork rn_leps;  //·��
    RNRangeQuery rnRQ_leps;  //��Χ��ѯ����
    HashMap<String, Cluster> preClusterSet;  //ǰһʱ�䴰�ڵĴؼ�
    HashMap<String, SnapObject> snapshotObjectSet;  //����֮����ƶ����󼯺�
    HashMap<String, SnapObject> incrementalObjectSet; //�������ƶ����󼯺�
    HashMap<String, SnapObject> objectsToDelete; //��ɾ�����ƶ�����λ�õ㼯�������µ�λ�ø��ǣ�
    ObjectsInforList objectsInforList;  //�ƶ�����λ�õ���Ϣ��
    int eps; //����뾶
    int minpts; //�����ܶ�
    int mc;  ////��Ա������ֵ
    String curTime;  //��ǰʱ���
    int clusterAmount;  //��¼�ص����������ڱ��

    HashMap<String, ObjectInforNode> exCores;  //ǰ���ĵ㼯
    HashMap<String, ObjectInforNode> neoCores;  //�º��ĵ㼯
    HashMap<String, ObjectInforNode> incOINs;  //���ڼ�¼�������������Ϣ

    /**
     * ���캯��
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
        incOINs = new HashMap<>();  //���ڼ�¼�������������Ϣ
        clusterAmount = 0;
    }

    /**
     * ���ĺ���������MBC���´�
     * @param currentClusterSet  ��ǰ�ľ�����
     */
    public void MBCClusterUpdating(HashMap<String, Cluster> currentClusterSet){
        rnRQ_leps.update(snapshotObjectSet,minpts);
        //������ɾ�㼯��ʶ��ǰ���ĵ�
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
        //���ݴ�ɾ�㼯����λ�õ���Ϣ
        this.objectsInforList.updatingByObjectsToDelete(objectsToDelete,exCores);
        //����ǰ���ĵ���´�
        updateClustersByExCores(currentClusterSet);
        //���������㼯��Ϊ���е㴴��λ����Ϣ��¼����ʶ���º��ĵ�
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
        //���������㼯����λ�õ���Ϣ
        this.objectsInforList.updatingByIncObjects(incOINs,neoCores);
        //�����º��ĵ���´�
        updateClusterByNeoCores(currentClusterSet);

    }

    /**
     * ͨ��ǰ���ĵ㼯���´�
     */
    private void updateClustersByExCores(HashMap<String, Cluster> currentClusterSet){
        //��ȡǰ���ĵ㼯�����е�ǰ�ܶȿɴ���ĵ㼯
        List<List<String>> esList = getES();
        //����ǰ�ܶȿɴ���ĵ㼯�б��ÿ��ǰ�ܶȿɴ���ĵ㼯
        for(int i=0;i<esList.size();i++){
            List<String> es = esList.get(i);
            //��ȡes����С��Ӻ��ĵ㼯
            List<String> mbc4es = getMBC_ES(es);
            //���es��mbc��Ϊ�գ�����mbc4es����ͨ����
            if(mbc4es.size()>0){
                HashMap<String,List<String>> nccs4mbc = getNCC(mbc4es);
                //ÿһ����ͨ�������Գ�Ϊһ���أ���ԭʼ�ط��Ѷ�����
                getNewClusters(nccs4mbc,currentClusterSet);
            }
        }
    }
    /**
     * ��ȡ���е�ǰ�ܶȿɴ���ĵ㼯
     * @return List<List<String>> ǰ�ܶȿɴ���ĵ㼯���б�ǰ�ܶȿɴ���ĵ㼯ΪList<String>
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
            int pointer = 0; //ָ�룬����ָʾESofOin�з��ʵ��Ķ���
            while(pointer<ESofOin.size()){
                ObjectInforNode pointedExcore = exCores.get(ESofOin.get(pointer));
                //�������ھӣ������е�ǰ���ĵ���뵽��ǰ�ܶȿɴ���ĵ㼯��
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
     * ��ȡǰ�ܶȿɴ���ĵ㼯��MBR
     * @param es   ǰ�ܶȿɴ���ĵ㼯
     * @return
     */
    private List<String> getMBC_ES(List<String> es){
        List<String> mbc = new ArrayList<>();
        for(int i=0;i<es.size();i++){
            ObjectInforNode ec = exCores.get(es.get(i));
            //�������ھӣ�������ΪC�ķ���mbc��
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
     * ����mbc�е���ͨ����
     * @param mbc
     * @return HashMap<String,List<String>> MBC����String��ͨ�ĵ㼯
     */
    private HashMap<String,List<String>> getNCC(List<String> mbc){
        HashMap<String,List<String>> reachablePoints = new HashMap<>();  //���ڼ�¼���ܶȿɴ�ĵ㼯
        for(int i=0;i<mbc.size();i++){
            ObjectInforNode oin = this.objectsInforList.getObjectsInforList().get(mbc.get(i));
            boolean isIn = false;//�ж�oin�Ƿ���ĳ����ͨ�������б���
            for(Map.Entry<String,List<String>> entry:reachablePoints.entrySet()){
                List<String> rps = entry.getValue();
                if(rps.contains(oin.getId())){
                    isIn = true;
                    break;
                }
            }
            //����������κ�һ����ͨ����������֮Ϊ��㣬����һ���µ���ͨ����
            if(!isIn){
                List<String> rps4oin = new ArrayList<>();
                int pointer = 0; //ָ��ָʾ���ʵ��ĵ���rps4oin�е�λ��
                List<String> queue = new ArrayList<>();
                queue.add(oin.getId());
                while(pointer<queue.size()){
                    ObjectInforNode poin = this.objectsInforList.getObjectsInforList().get(queue.get(pointer));
                    if(!rps4oin.contains(poin.getId()))
                        rps4oin.add(poin.getId());
                    //��֤���ھ��Ƿ���Լ�����չ����Ϊ���ĵ��������д���չ����Ϊ�Ǻ��ĵ㣬�趨����ΪB������ɴ���б���
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
     * ���ݸ���ͨ���������´أ�һ����ͨ����Ϊһ���´�
     * @param nccs  ��ͨ������
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
     * ͨ���º��ĵ㼯���´�
     */
    private void updateClusterByNeoCores(HashMap<String, Cluster> currentClusterSet){
        //��ȡ�º��ĵ㼯�����е����ܶȿɴ���ĵ㼯
        List<List<String>> nsList = getNS();
        //����ÿ�����ܶȿɴ���ĵ㼯
        for(int i=0;i<nsList.size();i++){
            List<String> ns = nsList.get(i);
            //��ȡns����С��Ӻ��ĵ㼯
            List<String> mbc4ns = getMBC_NS(ns);
            //����С��Ӻ��ĵ㼯Ϊ�գ��������ؽ�����ns;������֤mbc�еĶ����Ƿ�����ͬһ���أ�ͬ�أ���ns���뵽�ô��У����򣬽���ͬ�Ĵغϲ�
            if(mbc4ns.size()==0){
                addCluster(ns,currentClusterSet);
            }else {
                List<String> clusterList = getClustersInMBC(mbc4ns);
                //���mbc�еĵ�����ͬһ���أ���ns����ô���
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
                    //�����Բ�ͬ�Ĵأ�����Щ�غϲ���һ���µĴأ�����ns��������
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
                    //��ns��������
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
     * ��ȡ���е�ǰ�ܶȿɴ���ĵ㼯
     * @return List<List<String>> ǰ�ܶȿɴ���ĵ㼯���б�ǰ�ܶȿɴ���ĵ㼯ΪList<String>
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
            int pointer = 0; //ָ�룬����ָʾNSofOin�з��ʵ��Ķ���
            while(pointer<NSofOin.size()){
                ObjectInforNode pointedNeocore = neoCores.get(NSofOin.get(pointer));
                //�������ھӣ������е��º��ĵ���뵽��ǰ�ܶȿɴ���ĵ㼯��
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
     * ��ȡ���ܶȿɴ���ĵ㼯��MBR
     * @param ns   ���ܶȿɴ���ĵ㼯
     * @return
     */
    private List<String> getMBC_NS(List<String> ns){
        List<String> mbc = new ArrayList<>();
        for(int i=0;i<ns.size();i++){
            ObjectInforNode nc = neoCores.get(ns.get(i));
            //�������ھӣ�������ΪC�ķ���mbc��
            for(int j=0;j<nc.getNeighbors().size();j++){
                //����ھ�Ϊ�����ĺ��ĵ㣬���账��
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
     * �����ܶȿɴ���ĵ㼯����һ����
     * @param ns ���ܶȿɴ���ĵ㼯
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
     * ��ȡMBC�е����ڵĴص��б�
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
