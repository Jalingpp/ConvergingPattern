package Classes;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 路网类
 * @Author JJP
 * @Date 2021/5/14 9:35
 */
public class RoadNetwork {
    private HashMap<String, Edge> edgeList; //路网边表
    private HashMap<String, Vertex> vertexList; //路网顶点表

    /**
     * 无参构造函数
     */
    public RoadNetwork() {
        edgeList = new HashMap<>();
        vertexList = new HashMap<>();
    }
    public RoadNetwork(RoadNetwork roadNetwork){
        this.edgeList = new HashMap<>();
        this.edgeList.putAll(roadNetwork.edgeList);
        this.vertexList = new HashMap<>();
        for(Map.Entry<String,Vertex> nodeEntry:roadNetwork.getVertexList().entrySet()){
            this.vertexList.put(nodeEntry.getKey(),new Vertex(nodeEntry.getValue()));
        }
    }

    public RoadNetwork(HashMap<String, Edge> edges,HashMap<String,Vertex> vertexes){
//        edgeList = (HashMap<String, Edge>) edges.clone();
//        vertexList = (HashMap<String, Vertex>) vertexes.clone();
        edgeList = edges;
        vertexList = vertexes;
    }

    public HashMap<String, Edge> getEdgeList() {
        return edgeList;
    }

    public void setEdgeList(HashMap<String, Edge> edgeList) {
        this.edgeList = (HashMap<String, Edge>) edgeList.clone();
    }

    public HashMap<String, Vertex> getVertexList() {
        return vertexList;
    }

    public void setVertexList(HashMap<String, Vertex> vertexList) {
        this.vertexList = (HashMap<String, Vertex>) vertexList.clone();
    }
}
