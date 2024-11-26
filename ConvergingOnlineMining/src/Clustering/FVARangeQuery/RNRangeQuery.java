package Clustering.FVARangeQuery;

import Classes.*;
import Tools.*;

import java.io.IOException;
import java.util.*;

public class RNRangeQuery {

    private RoadNetwork roadNetwok;   //路网
    private HashMap<String,SnapObject> snapObjectSet; //移动对象集
    private int eps;
    private HashMap<String, List<SnapObject>> objectSequenceOnEdges;  //边id-对象序列列表，表示边上的移动对象序列
    private HashMap<String,List<String>> edgeMap;  //处理前的边与处理后的边的映射，处理前边ID-处理后子边的id列表
    private HashMap<String,List<EdgeInRange>> rangeOfNodes;  //路网节点的邻域边集,节点id-邻域内的边集
    private HashMap<String,Boolean> edgesWithoutCorePoint;   //过滤掉的一定不含核心点的边
    private HashMap<String,List<String>> nnOfNodes;  //已经查询过的路网节点的范围近邻
    private HashMap<String,List<String>> nnOfObjects;  //已经查询过的路网节点的范围近邻

    /**
     * 构造函数
     * @param roadNetwork 原始路网
     * @param eps  范围半径
     */
    public RNRangeQuery(RoadNetwork roadNetwork, int eps){
        this.roadNetwok = new RoadNetwork(roadNetwork);
        this.snapObjectSet = new HashMap<>();
        this.eps = eps;
        this.objectSequenceOnEdges = new HashMap<>();
        this.edgeMap = new HashMap<>();
        this.rangeOfNodes = new HashMap<>();
        this.edgesWithoutCorePoint = new HashMap<>();
        this.nnOfNodes = new HashMap<>();
        this.nnOfObjects = new HashMap<>();
        //将路网边处理为边长不超过2倍eps的边
        rnProcessing();
        //为每个节点创建eps范围近邻边集
        getRangeForNodes();
    }
    /**
     * 将路网边处理为不大于2倍eps的边
     */
    private void rnProcessing(){
        int virtualNodeAmount = 0;  //新增的虚拟节点数目，便于给新增节点编号
        HashMap<String,Edge> newEdges = new HashMap<>();  //暂存新增的边
        for(Map.Entry<String, Edge> edgeEntry:this.roadNetwok.getEdgeList().entrySet()) {
            Edge edge = edgeEntry.getValue();
            if (edge.getLength() >= 2 * eps) {
                List<String> subEdges = new ArrayList<>();  //记录划分出来的子段的id
                int splitAmount = (int) edge.getLength() / (2 * eps) + 1;  //计算须划分的段数
                Vertex sp = this.roadNetwok.getVertexList().get(edge.getSp_id());  //原边起点
                Vertex ep = this.roadNetwok.getVertexList().get(edge.getEp_id());  //原边终点
                Vertex newsp = sp;  //用来指示新加的点，作为新增边的起点
                //划分成splitAmount段
                for (int i = 0; i < splitAmount - 1; i++) {
                    Vertex newNode = new Vertex("v" + virtualNodeAmount++, sp.getLng() + ((ep.getLng() - sp.getLng()) * (i + 1)) / splitAmount, sp.getLat() + ((ep.getLat() - sp.getLat()) * (i + 1)) / splitAmount);
                    char idSuffix = (char) ('a' + i);
                    String newEdgeId = edge.getId().concat(String.valueOf(idSuffix));
                    Edge newEdge = new Edge(newEdgeId, edge.getLength() / splitAmount, newsp.getId(), newNode.getId());
                    newNode.getAdjEdges().add(newEdgeId);   //新增边终点的邻接边集中加入新增边id
                    newsp.getAdjEdges().add(newEdgeId);   //新增边起点的邻接边集中加入新增边id
                    this.roadNetwok.getVertexList().put(newsp.getId(), newsp);  //将新增边的起点放入路网节点集中
                    newsp = newNode;
                    newEdges.put(newEdge.getId(), newEdge);  //将新增边放入新增队列
                    subEdges.add(newEdge.getId());  //将新增边的id放入子段的id序列
                }
                char idSuffix = (char) ('a' + splitAmount - 1);
                String newEdgeId = edge.getId().concat(String.valueOf(idSuffix));
                Edge newEdge = new Edge(newEdgeId, edge.getLength() / splitAmount, newsp.getId(), ep.getId());
                newEdges.put(newEdge.getId(), newEdge);    //将新增边放入新增队列
                subEdges.add(newEdge.getId());   //将新增边的id放入子段的id序列
                newsp.getAdjEdges().add(newEdgeId);    //新增边起点的邻接边集中加入新增边id
                this.roadNetwok.getVertexList().put(newsp.getId(), newsp);   //将新增边的起点放入路网节点集中
                ep.getAdjEdges().add(newEdgeId);   //新增边终点的邻接边集中加入新增边id
                ep.getAdjEdges().remove(edge.getId());  //原边终点的邻接边集中删除原边id
                sp.getAdjEdges().remove(edge.getId());  //原边起点的邻接边集中删除原边id
                this.roadNetwok.getVertexList().put(sp.getId(), sp);
                this.roadNetwok.getVertexList().put(ep.getId(), ep);
                edgeMap.put(edge.getId(),subEdges);  //记录原边与新增边的映射
            }
        }
        //将原边从路网边集中删除
        for(Map.Entry<String,List<String>> originEdges:edgeMap.entrySet()){
            String originEdge = originEdges.getKey();  //获取原边id
            this.roadNetwok.getEdgeList().remove(originEdge);  //将原边从路网边集中删除
        }
        //将新增边加入路网边集中
        this.roadNetwok.getEdgeList().putAll(newEdges);
    }
    /**
     * 获取每个节点的邻域范围内的边集
     */
    private void getRangeForNodes(){
        for(Map.Entry<String,Vertex> nodeEntry:roadNetwok.getVertexList().entrySet()){
            Vertex node = nodeEntry.getValue();
            List<EdgeInRange> rangeEdges = new ArrayList<>();
            for(int i=0;i<node.getAdjEdges().size();i++){
                Edge edge = roadNetwok.getEdgeList().get(node.getAdjEdges().get(i));  //获取一条邻接边
                EdgeInRange edgeInRange = new EdgeInRange(edge.getId());
                edgeInRange.referenceNode = node.getId();
                if(edge.getLength()<=eps){
                    edgeInRange.setLength(edge.getLength());
                    edgeInRange.setComplete(true);
                    if(edge.getLength()<eps)
                        edgeInRange.calAdjaxEdgesInRange(roadNetwok,eps-edge.getLength());
                }else {
                    edgeInRange.setLength(eps);
                    edgeInRange.setComplete(false);
                }
                rangeEdges.add(edgeInRange);
            }
            rangeOfNodes.put(node.getId(),rangeEdges);
        }
    }

    /**
     * 更新当前索引结构
     * @param snapshotObjectSet  新的移动对象集合
     */
    public void update(HashMap<String,SnapObject> snapshotObjectSet,int minpts){
        //把边-对象序列表清空
        objectSequenceOnEdges.clear();
        edgesWithoutCorePoint.clear();
        nnOfNodes.clear();
        snapObjectSet.clear();
        nnOfObjects.clear();
        //重新放入当前时刻的对象
        for(Map.Entry<String,SnapObject> objectEntry:snapshotObjectSet.entrySet()){
            snapObjectSet.put(objectEntry.getKey(),new SnapObject(objectEntry.getValue()));
        }
//        long ts = System.currentTimeMillis();
        //构建边上的对象序列
        for(Map.Entry<String,SnapObject> objectEntry:snapObjectSet.entrySet()){
            SnapObject object = objectEntry.getValue();
            //调整对象点所在的边
            if(this.roadNetwok.getEdgeList().get(object.getEdgeid())==null){
                String currentEdge = getCurrentEdge(object);
                object.setEdgeid(currentEdge);
            }
            //将对象按序插入到边上
            List<SnapObject> objectList;
            if(objectSequenceOnEdges.get(object.getEdgeid())!=null) {
                objectList = objectSequenceOnEdges.get(object.getEdgeid());
                boolean isInserted = false;
                for(int i=0;i<objectList.size();i++){
                    if(object.getPos()<=objectList.get(i).getPos()){
                        objectList.add(i,object);
                        isInserted = true;
                        break;
                    }
                }
                if(!isInserted)
                    objectList.add(object);
            }
            else{
                objectList = new ArrayList<>();
                objectList.add(object);
                objectSequenceOnEdges.put(object.getEdgeid(),objectList);
            }
            snapObjectSet.replace(object.getId(),object);
        }
//        long te = System.currentTimeMillis();
//        System.out.println("EdgeObjectMap constructs in "+(te-ts)+" ms");
//        ts = System.currentTimeMillis();
        //采用上界对边进行过滤
        filter(minpts);
//        System.out.println("There are "+edgesWithoutCorePoint.size()+"/"+roadNetwok.getEdgeList().size()+" without core point.");
//        te = System.currentTimeMillis();
//        System.out.println("Filter runs in "+(te-ts)+" ms");
        //全对象查询，遍历每条边，对边上的对象进行判断
        int calReduceAmount = 0;
//        ts = System.currentTimeMillis();
        for(Map.Entry<String,List<SnapObject>> entry:objectSequenceOnEdges.entrySet()){
            Edge edge = roadNetwok.getEdgeList().get(entry.getKey());
            List<SnapObject> objectList = entry.getValue();
            //如果边上无核心点，则边上所有移动对象的近邻为null
            if(edgesWithoutCorePoint.get(edge.getId())!=null){
                for(int i=0;i<objectList.size();i++){
                    List<String> nn = new ArrayList<>();
                    nnOfObjects.put(objectList.get(i).getId(),nn);
                    calReduceAmount++;
                }
                continue;
            }
            //如果边上有核心点，进行验证
            //如果边两个节点的范围近邻树达到阈值
            if(nnOfNodes.get(edge.getSp_id()).size()>minpts&&nnOfNodes.get(edge.getEp_id()).size()>minpts){
                if(edge.getLength()<=eps){  //如果边长度小于eps，则边上每个对象的近邻都为端点近邻的并集
                    for(int i=0;i<objectList.size();i++){
                        List<String> nn = new ArrayList<>();
                        nn.addAll(nnOfNodes.get(edge.getSp_id()));
                        nn.addAll(nnOfNodes.get(edge.getEp_id()));
                        nn.remove(objectList.get(i).getId());
                        nnOfObjects.put(objectList.get(i).getId(),nn);
                        calReduceAmount++;
                    }
                }else {  //如果边长度大于eps，小于2eps，则验证中间移动对象是否为核心点
                    SnapObject midObject = objectList.get(objectList.size()/2);
                    List<String> nnOfMidObject = getResultObjects(midObject.getId(),eps);
                    nnOfObjects.put(midObject.getId(),nnOfMidObject);
                    if(nnOfMidObject.size()>=minpts){  //如果中间节点是核心点，则边上所有移动对象均为核心点,以两个端点近邻的并集为范围近邻
                        for(int i=0;i<objectList.size();i++){
                            List<String> nn = new ArrayList<>();
                            nn.addAll(nnOfNodes.get(edge.getSp_id()));
                            nn.addAll(nnOfNodes.get(edge.getEp_id()));
                            nn.remove(objectList.get(i).getId());
                            nnOfObjects.put(objectList.get(i).getId(),nn);
                            calReduceAmount++;
                        }
                    }else{  //如果中间节点不是核心点，则需要挨个验证
                        for(int i=0;i<objectList.size();i++){
                            if(i!=objectList.size()/2){
                                List<String> nnOfObject = getResultObjects(objectList.get(i).getId(),eps);
                                nnOfObjects.put(objectList.get(i).getId(),nnOfObject);
                            }
                        }
                    }
                }
            }else if(nnOfNodes.get(edge.getSp_id()).size()>minpts){  //如果只是起点的范围近邻大于阈值
                //倒序验证，找到一个核心点，起点与该点中间的点均为核心点，近邻为两个近邻集的并集
                for(int i=objectList.size()-1;i>=0;i--){
                    List<String> nnOfObject = getResultObjects(objectList.get(i).getId(),eps);
                    nnOfObjects.put(objectList.get(i).getId(),nnOfObject);
                    if(nnOfObject.size()>=minpts){ //若该点为核心点，起点到该点的所有点均为核心点
                        for(int j=0;j<i;j++){
                            List<String> nn = new ArrayList<>();
                            nn.addAll(nnOfNodes.get(edge.getSp_id()));
                            nn.addAll(nnOfObject);
                            nn.remove(objectList.get(i).getId());
                            nnOfObjects.put(objectList.get(i).getId(),nn);
                            calReduceAmount++;
                        }
                        break;
                    }
                }
            }else if(nnOfNodes.get(edge.getEp_id()).size()>minpts){  //如果只是终点的范围近邻大于阈值
                //正序验证，找到一个核心点，该点与终点之间的点均为核心点，近邻为两个近邻集的并集
                for(int i=0;i<objectList.size();i++){
                    List<String> nnOfObject = getResultObjects(objectList.get(i).getId(),eps);
                    nnOfObjects.put(objectList.get(i).getId(),nnOfObject);
                    if(nnOfObject.size()>=minpts){ //若该点为核心点，起点到该点的所有点均为核心点
                        for(int j=i+1;j<objectList.size();j++){
                            List<String> nn = new ArrayList<>();
                            nn.addAll(nnOfNodes.get(edge.getSp_id()));
                            nn.addAll(nnOfObject);
                            nn.remove(objectList.get(i).getId());
                            nnOfObjects.put(objectList.get(i).getId(),nn);
                            calReduceAmount++;
                        }
                        break;
                    }
                }
            }else{  //如果起点与终点的范围近邻均小于阈值，则挨个验证
                for(int i=0;i<objectList.size();i++){
                    List<String> nnOfObject = getResultObjects(objectList.get(i).getId(),eps);
                    nnOfObjects.put(objectList.get(i).getId(),nnOfObject);
                }
            }
        }
//        te = System.currentTimeMillis();
//        System.out.println("ANN query runs in "+(te-ts)+" ms");
//        System.out.println("FVARangeQuery: Reduce range query for "+calReduceAmount+"/"+snapObjectSet.size()+" objects in ANNQ!");
    }
    /**
     * 获取对象点当前所在的边,同时调整偏移量
     * @param snapObject
     * @return
     */
    private String getCurrentEdge(SnapObject snapObject){
        List<String> currentEdges = edgeMap.get(snapObject.getEdgeid());
        double pos = snapObject.getPos();
        for(int i=0;i<currentEdges.size();i++){
            Edge currentEdge = this.roadNetwok.getEdgeList().get(currentEdges.get(i));
            if(currentEdge.getLength()>=pos){
                snapObject.setPos(pos);
                return currentEdge.getId();
            }
            pos = pos - currentEdge.getLength();
        }
        //如果移动对象偏移量超过了原边长度，则将其对应为最后一段路段上，以最后一段路段的长度为偏移量
        snapObject.setPos(this.roadNetwok.getEdgeList().get(currentEdges.get(currentEdges.size()-1)).getLength());
        return currentEdges.get(currentEdges.size()-1);
    }

    /**
     * 根据上界函数过滤掉一定不含核心点的边
     * @param minpts  近邻数量阈值
     */
    private void filter(int minpts){
        //过滤掉一定不含核心点的边
        for(Map.Entry<String,Edge> edgeEntry:roadNetwok.getEdgeList().entrySet()){
            Edge edge = edgeEntry.getValue();
            List<String> upBound = getUpperBound(edge);
            if(upBound.size()<minpts){
                edgesWithoutCorePoint.put(edge.getId(),true);
            }
        }
    }
    /**
     * 计算边的范围近邻上界
     * @param edge
     * @return
     */
    private List<String> getUpperBound(Edge edge){
        List<String> ubNN = new ArrayList<>();
        Vertex sp = this.roadNetwok.getVertexList().get(edge.getSp_id());
        List<String> spNN;
        if(nnOfNodes.get(sp.getId())!=null)
            spNN = nnOfNodes.get(sp.getId());
        else {
            spNN = getNNOfNode(sp,eps);
            nnOfNodes.put(sp.getId(),spNN);
        }
        ubNN.addAll(spNN);
        Vertex ep = this.roadNetwok.getVertexList().get(edge.getEp_id());
        List<String> epNN ;
        if(nnOfNodes.get(ep.getId())!=null)
            epNN = nnOfNodes.get(ep.getId());
        else {
            epNN = getNNOfNode(ep,eps);
            nnOfNodes.put(ep.getId(),epNN);
        }
        for(int i=0;i<epNN.size();i++){
            if(!ubNN.contains(epNN.get(i)))
                ubNN.add(epNN.get(i));
        }
        return ubNN;
    }
    /**
     * 获取以node为中心，eps范围内的近邻集，eps非固定，可指定
     * @param node
     * @param eps
     * @return  node的eps范围近邻集
     */
    public List<String> getNNOfNode(Vertex node,double eps){
        List<String> nn = new ArrayList<>();
        List<EdgeInRange> range = rangeOfNodes.get(node.getId());
        Queue<EdgeInRange> edgeIRQueue = new LinkedList<>();
        Queue<Double> restEpsQueue = new LinkedList<>();
        List<String> visitedEdgeIR = new ArrayList<>();  //用于记录已经入过队列的边，避免重复遍历
        for(int i=0;i<range.size();i++) {
            edgeIRQueue.add(range.get(i));
            restEpsQueue.add((double)eps);
            visitedEdgeIR.add(range.get(i).getId());
        }
        while (!edgeIRQueue.isEmpty()){
            EdgeInRange edgeIR = edgeIRQueue.poll();
            double restEps = restEpsQueue.poll();
            List<SnapObject> snapObjects = objectSequenceOnEdges.get(edgeIR.getId());  //获取边上的移动对象序列
            if(snapObjects!=null){
                if(edgeIR.getLength()<=restEps){  //如果该边为完全边，则将该边上的所有移动对象放入
                    for(int i=0;i<snapObjects.size();i++){
                        if(!nn.contains(snapObjects.get(i).getId()))
                            nn.add(snapObjects.get(i).getId());
                    }
                }else {  //如果该边为不完全边
                    //判断是正序还是逆序
                    boolean isReverse = false;  //false为正序，true为逆序
                    Edge edge = this.roadNetwok.getEdgeList().get(edgeIR.getId());
                    if(edgeIR.referenceNode.equals(edge.getEp_id()))
                        isReverse = true;
                    if(!isReverse){ //正序遍历
                        for(int i=0;i<snapObjects.size();i++){
                            if(snapObjects.get(i).getPos()<=restEps){
                                if(!nn.contains(snapObjects.get(i).getId()))
                                    nn.add(snapObjects.get(i).getId());
                            }else {
                                break;
                            }
                        }
                    }else {  //逆序
                        for(int i=snapObjects.size()-1;i>=0;i--){
                            if((edge.getLength()-snapObjects.get(i).getPos())<=restEps){
                                if(!nn.contains(snapObjects.get(i).getId()))
                                    nn.add(snapObjects.get(i).getId());
                            }else {
                                break;
                            }
                        }
                    }
                }
            }
            restEps = restEps-edgeIR.getLength();
            if(restEps<=0)   //如果剩余的长度小于等于零则不再继续扩展
                continue;
            //将该edgeIR的邻接边集加入到队列中
            for(int i=0;i<edgeIR.getAdjaxEdgesInRange().size();i++){
                if(!visitedEdgeIR.contains(edgeIR.getAdjaxEdgesInRange().get(i))){
                    edgeIRQueue.add(edgeIR.getAdjaxEdgesInRange().get(i));
                    restEpsQueue.add(restEps);
                    visitedEdgeIR.add(edgeIR.getAdjaxEdgesInRange().get(i).getId());
                }
            }
        }
        return nn;
    }

    /**
     * 查询单个移动对象的近邻
     * @param snapObjectID
     * @param eps
     * @return
     */
    private List<String> getResultObjects(String snapObjectID,double eps){
        List<String> reslutNN = new ArrayList<>();
        SnapObject snapObject = snapObjectSet.get(snapObjectID);
        Edge currentEdge = this.roadNetwok.getEdgeList().get(snapObject.getEdgeid());
        //如果当前边是无核心点的边，直接返回
        if(edgesWithoutCorePoint.get(currentEdge.getId())!=null)
            return reslutNN;
        else {
            //如果该点到边起点的距离小于eps，则计算sp的（eps-pos)范围内的近邻
            if(snapObject.getPos()<eps){
                Vertex sp = this.roadNetwok.getVertexList().get(currentEdge.getSp_id());
                List<String> spNN = getNNOfNode(sp,eps-snapObject.getPos());
                reslutNN.addAll(spNN);
            }
            //将边上正序与pos值差在eps范围内的点加入nn中
            List<SnapObject> objectList = objectSequenceOnEdges.get(currentEdge.getId());
            for(int i=0;i<objectList.size();i++){
                if(objectList.get(i).getId().equals(snapObject.getId()))
                    break;
                if((snapObject.getPos()-objectList.get(i).getPos())<=eps)
                    if(!reslutNN.contains(objectList.get(i).getId()))
                        reslutNN.add(objectList.get(i).getId());
            }
            //如果该点到边终点的距离小于eps，则计算ep的（eps-(length-pos))范围内的近邻
            if((currentEdge.getLength()-snapObject.getPos())<eps){
                Vertex ep = this.roadNetwok.getVertexList().get(currentEdge.getEp_id());
                List<String> epNN = getNNOfNode(ep,eps-(currentEdge.getLength()-snapObject.getPos()));
                for(int i=0;i<epNN.size();i++)
                    if(!reslutNN.contains(epNN.get(i)))
                        reslutNN.add(epNN.get(i));
            }
            //将边上逆序与pos值差在eps范围内的点加入nn中
            for(int i=objectList.size()-1;i>=0;i--){
                if(objectList.get(i).getId().equals(snapObject.getId()))
                    break;
                if((objectList.get(i).getPos()-snapObject.getPos())<=eps)
                    if(!reslutNN.contains(objectList.get(i).getId()))
                        reslutNN.add(objectList.get(i).getId());
            }
        }
        //将本对象从结果集中剔除
        reslutNN.remove(snapObject.getId());
        return reslutNN;
    }

    /**
     * 查询单个移动对象范围近邻
     * @param snpObjectID
     * @return
     */
    public List<String> getResultObjects(String snpObjectID){
        if(nnOfObjects.get(snpObjectID)!=null)
            return nnOfObjects.get(snpObjectID);
        else
            return new ArrayList<>();
    }

    public static void main(String[] args) throws IOException {
       /*String rnFilePath = "E:/data/testData/testUndirRN/";
        String nodesFilename = "nodes_part_udir.json";
        String edgesFilename = "edges_part_udir.json";

        String rnFilePath = "E:/data/MatchingDataInShanghai/MatchingData/UndirRN/";
        String nodesFilename = "nodes_udir.json";
        String edgesFilename = "edges_udir.json";
*/
        String rnFilePath = "E:/1 Research/data/testData_Fzr/testData2/";
        String nodesFilename = "testNodes.json";
        String edgesFilename = "testEdges.json";

        RoadNetwork roadNetwork = DataRw.readRNFromFile(rnFilePath,nodesFilename,edgesFilename);

        RNRangeQuery rnRQ = new RNRangeQuery(roadNetwork,5);

        //输出边长为0的边
//        for(Map.Entry<String,Edge> edgeEntry: rnRQ.roadNetwok.getEdgeList().entrySet()){
//            if(edgeEntry.getValue().getLength()<=0){
//                System.out.println(edgeEntry.getKey()+"-["+edgeEntry.getValue().getSp()+","+edgeEntry.getValue().getEp()+"]:"+edgeEntry.getValue().getLength());
//                Node sp = rnRQ.roadNetwok.getNodeList().get(edgeEntry.getValue().getSp());
//                System.out.println(sp.getId()+":["+sp.getLng()+","+sp.getLat()+"]");
//                Node ep = rnRQ.roadNetwok.getNodeList().get(edgeEntry.getValue().getEp());
//                System.out.println(ep.getId()+":["+ep.getLng()+","+ep.getLat()+"]");
//            }
//        }

        /*
        HashMap<String, List<SnapObject>> objectSequenceOnEdges = new HashMap<>();
        SnapObject snapObject = new SnapObject("object1");
        List<SnapObject> objectList = new ArrayList<>();
        objectList.add(snapObject);
        objectSequenceOnEdges.put("edge1",objectList);
        objectSequenceOnEdges.clear();
        System.out.println(objectSequenceOnEdges.get("edge1"));
        objectSequenceOnEdges.put("edge1",objectList);
*/

        //读取移动对象数据
        HashMap<String,SnapObject> snapObjectSet1 = new HashMap<>();
        String objectInpath1 = "E:/1 Research/data/testData_Fzr/testData2/object.txt";
//        String objectInpath1 = "E:/data/MatchingDataInShanghai/MatchingData/11/000000.txt";
        DataRw.readObjectsFromFile(objectInpath1,snapObjectSet1);
        rnRQ.update(snapObjectSet1,0);

        int i=0;
        for(Map.Entry<String,SnapObject> entry:snapObjectSet1.entrySet()){
            List<String> result = rnRQ.getResultObjects(entry.getKey(),5);
            System.out.println(i+"-"+entry.getKey()+":"+result.size()+"-"+result);
            i++;
        }

        int a=1;
    }

}
