package Clustering.FVARangeQuery;

import Classes.*;

import java.util.*;

public class EdgeInRange {

    String id;
    Boolean isComplete;  //是否整条边都在范围里
    String referenceNode;  //标识在范围里的是哪一端
    double length;  //标识在范围里的长度
    List<EdgeInRange> adjaxEdgesInRange;  //范围内该边邻接的边（或路段）

    /**
     * 构造函数
     * @param id  边id
     * @param isComplete
     * @param referenceNode
     * @param length
     */
    public EdgeInRange(String id, Boolean isComplete, String referenceNode, double length) {
        this.id = id;
        this.isComplete = isComplete;
        this.referenceNode = referenceNode;
        this.length = length;
        this.adjaxEdgesInRange = new ArrayList<>();
    }
    public EdgeInRange(String id) {
        this.id = id;
        this.isComplete = false;
        this.length = 0;
        this.referenceNode = null;
        this.adjaxEdgesInRange = new ArrayList<>();
    }

    /**
     * 计算范围内该边邻接的边（或路段）(递归构建）
     * @param roadNetwok  路网
     * @param restEps eps值

    public void calAdjaxEdgesInRange(RoadNetwok roadNetwok, double eps){
        double restLength = eps-this.length;  //剩余的长度额度
        if(restLength>0){
            String anotherNodeId = roadNetwok.getEdgeList().get(id).getSp().equals(referenceNode)?roadNetwok.getEdgeList().get(id).getEp():roadNetwok.getEdgeList().get(id).getSp();
            Node anotherNode = roadNetwok.getNodeList().get(anotherNodeId);
            for(int i=0;i<anotherNode.getsEdges().size();i++){
                if(anotherNode.getsEdges().get(i).equals(id))
                    continue;
                Edge adjoinEdge = roadNetwok.getEdgeList().get(anotherNode.getsEdges().get(i));
                EdgeInRange adjoinEdgeInRange = new EdgeInRange(adjoinEdge.getId());
                adjoinEdgeInRange.referenceNode = anotherNodeId;
                if(adjoinEdge.getLength()<=restLength){
                    adjoinEdgeInRange.setLength(adjoinEdge.getLength());
                    adjoinEdgeInRange.setComplete(true);
                    if(adjoinEdge.getLength()<restLength)
                        adjoinEdgeInRange.calAdjaxEdgesInRange(roadNetwok,restLength);
                }else {
                    adjoinEdgeInRange.setLength(restLength);
                    adjoinEdgeInRange.setComplete(false);
                }
                this.adjaxEdgesInRange.add(adjoinEdgeInRange);
            }
        }
    }
    */
    /**
    * 计算范围内该边邻接的边（或路段）(非递归构建，用队列）
    * @param roadNetwork  路网
    * @param restEps eps值
    */
    public void calAdjaxEdgesInRange(RoadNetwork roadNetwork, double restEps){
        HashMap<String,Boolean> edgesInRange = new HashMap<>();  //用于记录已经扩展过的完全边，边id-是否为完全边
        Queue<EdgeInRange> edgeIRQueue = new LinkedList<>();
        Queue<Double> restLengthQueue = new LinkedList<>();
        edgeIRQueue.add(this);
        restLengthQueue.add(restEps);
        edgesInRange.put(this.id,true);
        while(!edgeIRQueue.isEmpty()){
            EdgeInRange edgeIR = edgeIRQueue.poll();
            double restLength = restLengthQueue.poll();  //剩余的长度额度
            String anotherNodeId = roadNetwork.getEdgeList().get(edgeIR.id).getSp_id().equals(edgeIR.referenceNode)?roadNetwork.getEdgeList().get(edgeIR.id).getEp_id():roadNetwork.getEdgeList().get(edgeIR.id).getSp_id();
            Vertex anotherNode = roadNetwork.getVertexList().get(anotherNodeId);
            for(int i=0;i<anotherNode.getAdjEdges().size();i++){
                if(anotherNode.getAdjEdges().get(i).equals(edgeIR.id))
                    continue;
                Edge adjoinEdge = roadNetwork.getEdgeList().get(anotherNode.getAdjEdges().get(i));
                EdgeInRange adjoinEdgeInRange = new EdgeInRange(adjoinEdge.getId());
                adjoinEdgeInRange.referenceNode = anotherNodeId;
                if(adjoinEdge.getLength()<=restLength){
                    adjoinEdgeInRange.setLength(adjoinEdge.getLength());
                    adjoinEdgeInRange.setComplete(true);
                    if(adjoinEdge.getLength()<restLength&&edgesInRange.get(adjoinEdge.getId())==null){//扩展条件：该边长度比剩余长度短且该边未被扩展过
                        edgeIRQueue.add(adjoinEdgeInRange);
                        restLengthQueue.add(restLength-adjoinEdge.getLength());
                        edgesInRange.put(adjoinEdge.getId(),true);
                    }
                }else {
                    adjoinEdgeInRange.setLength(restLength);
                    adjoinEdgeInRange.setComplete(false);
                }
                edgeIR.adjaxEdgesInRange.add(adjoinEdgeInRange);
            }
        }

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Boolean getComplete() {
        return isComplete;
    }

    public void setComplete(Boolean complete) {
        isComplete = complete;
    }

    public String getReferenceNode() {
        return referenceNode;
    }

    public void setReferenceNode(String referenceNode) {
        this.referenceNode = referenceNode;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public List<EdgeInRange> getAdjaxEdgesInRange() {
        return adjaxEdgesInRange;
    }

    public void setAdjaxEdgesInRange(List<EdgeInRange> adjaxEdgesInRange) {
        this.adjaxEdgesInRange = adjaxEdgesInRange;
    }
}
