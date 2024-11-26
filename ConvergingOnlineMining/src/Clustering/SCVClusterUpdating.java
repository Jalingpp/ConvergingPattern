package Clustering;

import Classes.*;
import Clustering.FVARangeQuery.RNRangeQuery;
import Tools.TimeConversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: ����ͬ����֤�Ĵظ����㷨
 * @Author JJP
 * @Date 2021/11/5 18:23
 */
public class SCVClusterUpdating {
    RoadNetwork rn_leps;
    HashMap<String, Cluster> preClusterSet;
    HashMap<String, SnapObject> snapshotObjectSet;
    HashMap<String, List<String>> edgeObjectIndex; //��-�ƶ���������
    RNRangeQuery rnRQ;  //·����Χ��ѯ����
    int eps; //����뾶
    int minpts; //�����ܶ�
    int mc;  ////��Ա������ֵ
    String curTime;  //��ǰʱ���

    /**
     * ���캯��
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
     * ·���ߺ��ƶ���������������ڲ�ѯ���ϵ��ƶ������б����б�ӱ����࿪ʼ����posֵ��С��������
     * @return ������
     */
    private void edgeObjectIndexConstruct(){
        this.edgeObjectIndex = new HashMap<>();
        //�����ƶ����󣬷ŵ���Ӧ�ı߱���
        for(Map.Entry<String,SnapObject> entry:snapshotObjectSet.entrySet()){
            SnapObject _so = entry.getValue();
            //����������ڵı߱��Ѵ��ڣ�����뵽���У����򣬴����߱��������
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
     * ����ͬ����֤�Ĵظ����㷨
     * @param currentClusterSet  ��ǰ�ؼ�
     * @param clusterContainment  ��ǰ�ذ�����ϵ��
     */
    public void SCVClusterUpdating(HashMap<String, Cluster> currentClusterSet,HashMap<String, String> clusterContainment){
        rnRQ.update(snapshotObjectSet,minpts);
        int clusterNum = 0;  //���ֵĴصĸ���
        //����ǰһʱ�̿��մؼ��е�ÿ���أ������Ƿ������չ
        for(Map.Entry<String,Cluster> preCluster:this.preClusterSet.entrySet()){
            Cluster _cluster = preCluster.getValue();
//            System.out.print("Verifyinf "+_cluster.getId()+" ...");
            //��ȡ��ʱ�����ƶ����󼯺����ڵ�·����ͼ
            RoadNetwork subgraph = getSubgraphOfObjects(_cluster.getMenberPoints());
            //��֤�ƶ����󼯺��Ƿ�ͬ��
            HashMap<String,String> objectsInOneCluster = new HashMap<>();
            boolean isInOneCluster = isInOneCluster(_cluster.getMenberPoints(),subgraph,objectsInOneCluster);
            //��ͬ�أ�����չ�أ������´��еĶ�����snapshotObjectSet��ɾ��
            if(isInOneCluster){
                //��չ��:��ѯobjectsInOneCluster�в�ȷ�������������µĵ㣬��Ǻ��ĵ㣬�߽�㣬ֱ��ȫ��ȷ��
                HashMap<String,String> certainPoints = new HashMap<>();//���ڷ����Ѿ�ȷ���ĵ�,core��border
                List<String> uncertainPoins = new ArrayList<>();//���ڷ��ò�ȷ���ĵ�
                for(Map.Entry<String,String> entry:objectsInOneCluster.entrySet()){
                    if(entry.getValue().equals("core")&&snapshotObjectSet.containsKey(entry.getKey()))
                        certainPoints.put(entry.getKey(),entry.getValue());
                    else{
                        uncertainPoins.add(entry.getKey());
                    }
                }
                List<String> visited = new ArrayList<>();  //���ڼ�¼��uncertainPoints���ʹ��ĵ�
                //���ϲ�ѯ��ȷ���������ֱ�����в����ڲ�ȷ���ĵ�
                while(uncertainPoins.size()>0){
                    String oid = uncertainPoins.get(0);
                    visited.add(oid);
                    //��ѯ��ȷ���������
                    List<String> nnOfUnObject = rnRQ.getResultObjects(oid);
                    //������������ֵ���������ڲ�ȷ���ĵ���벻ȷ���㼯�У����õ����ȷ�����У����򣬽����õ����ȷ������
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
                    //��ȷ������ɾ���õ�
                    uncertainPoins.remove(0);
                }
                //���´ز��뵽�´ؼ��У��������еĶ�����snapshotObjectSet��ɾ��
                Cluster newCluster = new Cluster(""+clusterNum,curTime);
                clusterNum++;
                for(Map.Entry<String,String> entry:certainPoints.entrySet()){
                    newCluster.addPoint(snapshotObjectSet.get(entry.getKey()));
                    snapshotObjectSet.remove(entry.getKey());
                }
//                System.out.print(", get newCluster "+newCluster.getId());
                currentClusterSet.put(newCluster.getId(),newCluster);
                //���ذ�����ϵ���뵽�ذ�����ϵ����
//                System.out.println(", get cluster containment ("+_cluster.getId()+","+newCluster.getId()+")");
                clusterContainment.put(_cluster.getId(),newCluster.getId());
            }
        }
        //��snapshotObjectSet��ʣ��ĵ���ԭʼ��������
        HashMap<String,Cluster> newClusters = DBSCAN_RoadNetwork.getClusters(snapshotObjectSet,rnRQ,minpts,curTime,mc);
        System.out.print(curTime+": Clustering left "+snapshotObjectSet.size()+" objects...");
        //����ÿ���ص�id���뵽��ǰ�ؼ���
        for(Map.Entry<String,Cluster> entry:newClusters.entrySet()){
            Cluster cluster = entry.getValue();
            cluster.setId(""+clusterNum);
            clusterNum++;
            currentClusterSet.put(cluster.getId(), cluster);
        }
        System.out.println("Total clusters:"+currentClusterSet.size()+", Total Containments:"+clusterContainment.size());
    }

    /**
     * ��ȡ�ƶ����󼯺����ڵ�·����ͼ���ڱ߳��Ȳ�����eps��·���У�
     * @param objects �ƶ����󼯺�
     * @return ·����ͼ
     */
    private RoadNetwork getSubgraphOfObjects(List<SnapObject> objects){
        RoadNetwork subgraph = new RoadNetwork();
        //�����ƶ������б��������ڵı߲���߱����ߵĶ�����붥���
        for(int i=0;i<objects.size();i++){
            SnapObject _o = objects.get(i);
            if(!subgraph.getEdgeList().containsKey(_o.getEdgeid())){
                Edge _e = rn_leps.getEdgeList().get(_o.getEdgeid());
                subgraph.getEdgeList().put(_o.getEdgeid(),_e);
                //���ߵĶ�����뵽���㼯
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
     * �ж�һ���ƶ�����λ�õ㼯�Ƿ�ͬ��
     * @param locationSet �ƶ�����λ�õ㼯
     * @param subgraph λ�õ㼯���ڵ���ͼ
     * @param objectsInOneCluster ���ڼ�¼��ͼ��ͬ�صĵ�,���ĵ���Ϊcore,�Ǻ��ĵ���Ϊnotsure
     * @return ͬ�ط���true����ͬ�ط���false
     */
    private boolean isInOneCluster(List<SnapObject> locationSet,RoadNetwork subgraph,HashMap<String,String> objectsInOneCluster){
        boolean isIn = false;
        //���������ĳ���㲻�ڶ����У���˵���õ��Ѽ��������أ��򷵻�false
        for(int i=0;i<locationSet.size();i++){
            if(!snapshotObjectSet.containsKey(locationSet.get(i).getId()))
                return false;
        }
        //������ͼÿ�����㣬�ж�0.5eps�������Ƿ�����minpts+1��Ҫ�󣬱�Ǻ��ĵ�
        HashMap<String,Vertex> vertexList = subgraph.getVertexList();
        for(Map.Entry<String,Vertex> entry:vertexList.entrySet()){
            Vertex _v = entry.getValue();
            //��ȡ�����0.5eps����
             List<String> halfEps = rnRQ.getNNOfNode(_v,this.eps/2);
            //������Ϊ�գ������ö����ڽӶ������������С���룬����С�������eps����ͬ��
            if(halfEps.size()==0){
                double minDist = getMinDistForVertexs(_v);
                if(minDist>=this.eps)
                    return false;
            }
            //�������ڵ��ƶ�������������(minpts+1)���Ϊ���ĵ㣬������ͼ���ĵ㼯�У������жϽ��ڽӱߵĵ�ӽ����Ƿ����(minpts+1)
            if(halfEps.size()>=minpts+1){
                for(int i=0;i<halfEps.size();i++){
                    SnapObject so = snapshotObjectSet.get(halfEps.get(i));
                    if(so==null)
                        continue;
                    objectsInOneCluster.put(so.getId(),"core");
                    //��soͬ�ߵĵ����ͬ�ص㼯�У���עΪnotsure
                    List<String> objectInOneEdge = this.edgeObjectIndex.get(so.getEdgeid());
                    for(int j=0;j<objectInOneEdge.size();j++){
                        if(!objectsInOneCluster.containsKey(objectInOneEdge.get(j))&&snapshotObjectSet.containsKey(objectInOneEdge.get(j)))
                            objectsInOneCluster.put(objectInOneEdge.get(j),"notsure");
                    }
                }
            }else{
                //�жϽ��ڽӱߵĵ�ӽ����Ƿ����(minpts+1)
//                for(int i=0;i<_v.getAdjEdges().size();i++){
//                    //��ȡһ���ڽӱ��ϵ������ƶ�����
//                    List<String> objectInEdge = this.edgeObjectIndex.get(_v.getAdjEdges().get(i));
//                    if(objectInEdge==null)
//                        continue;
//                    //����ñ��ƶ�������half�����ڵ��ƶ������ܺ�
//                    int count = halfEps.size();
//                    for(int j=0;j<objectInEdge.size();j++){
//                        if(!halfEps.contains(objectInEdge.get(j)))
//                            count++;
//                    }
//                    //���ܺͳ���(minpts+1)����ñ߼������ڵ��ƶ�����ͬ�أ��ñ����������ڵĶ���Ϊ���ĵ�
//                    if(count>=minpts+1){
//                        for(int j=0;j<halfEps.size();j++){
//                            SnapObject so = snapshotObjectSet.get(halfEps.get(j));
//                            //���so�ڱ��ϣ���Ϊ���ĵ�
//                            if(so.getEdgeid().equals(_v.getAdjEdges().get(i)))
//                                objectsInOneCluster.put(so.getId(),"core");
//                            else if(!objectsInOneCluster.containsKey(so.getId()))
//                                objectsInOneCluster.put(so.getId(),"notsure");
//                        }
//                        //�������ϵĵ㣬������뵽ͬ�ر���
//                        for(int j=0;j<objectInEdge.size();j++){
//                            if(!objectsInOneCluster.containsKey(objectInEdge.get(j)))
//                                objectsInOneCluster.put(objectInEdge.get(j),"notsure");
//                        }
//                    }
//                }
                //��ѯÿ�����Ƿ�Ϊ���ĵ㣬��Ϊ���ĵ㣬���õ㼰�������ڵĵ����ͬ�ر���
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
        //�жϸ������ƶ����󼯺��Ƿ���ͬ�ر��У�������ͬ��
        for(int i=0;i<locationSet.size();i++){
            if(!objectsInOneCluster.containsKey(locationSet.get(i).getId()))
                break;
            if(i==locationSet.size()-1)
                isIn = true;
        }

        return isIn;
    }


    /**
     * ��ȡ�����ڽӶ��������ֱ�߾��루ŷ�Ͼ��룩
     * @param v ·������
     * @return ���ڽӶ�������ֱ�߾���
     */
    private double getMinDistForVertexs(Vertex v){
        double minDist = Double.MAX_VALUE;
        //��ȡ������ڽӶ����б�
        List<Vertex> adjVertexList = new ArrayList<>();
        for(int i=0;i<v.getAdjEdges().size();i++){
            Edge adjE = this.rn_leps.getEdgeList().get(v.getAdjEdges().get(i));
            if(adjE.getSp_id().equals(v.getId()))
                adjVertexList.add(this.rn_leps.getVertexList().get(adjE.getEp_id()));
            else
                adjVertexList.add(this.rn_leps.getVertexList().get(adjE.getSp_id()));
        }
        //˫��ѭ�����㶥���ľ��룬ȡ���
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
