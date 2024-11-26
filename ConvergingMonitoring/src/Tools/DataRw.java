package Tools;

import Classes.Edge;
import Classes.RoadNetwork;
import Classes.SnapObject;
import Classes.Vertex;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

/**
 * @Description: 数据读写类
 * @Author JJP
 * @Date 2021/5/14 9:49
 */
public class DataRw {
    /**
     * 读取路网文件
     * @param path 文件路径
     * @param edgeFN 路网边文件名
     * @param vertexFN 路网顶点文件名
     * @return 路网类对象
     */
    public static RoadNetwork readRNFromFile(String path, String edgeFN, String vertexFN){
        HashMap<String, Edge> edgeList = readELFromFile(path,edgeFN);
        HashMap<String, Vertex> vertexList = readVLFromFile(path,vertexFN);
        RoadNetwork rn = new RoadNetwork(edgeList,vertexList);
        return rn;
    }

    /**
     * 从文件中读取边表
     * @param path 文件路径
     * @param edgeFN 路网边文件名
     * @return 路网边表 hashmap<String,edge>
     */
    public static HashMap<String, Edge> readELFromFile(String path, String edgeFN){
        HashMap<String, Edge> edgeList = new HashMap<>();
        JSONObject edgesObject = JSONRW.readJson(path+edgeFN);
        JSONArray edgesArray = edgesObject.getJSONArray("edges");
        Edge edge;   //用于指向一条边
        for(int i=0;i<edgesArray.size();i++){
            edge = new Edge();
            edge.setId(edgesArray.getJSONObject(i).getString("id"));
            edge.setSp_id(edgesArray.getJSONObject(i).getString("sp"));
            edge.setEp_id(edgesArray.getJSONObject(i).getString("ep"));
            edge.setLength(edgesArray.getJSONObject(i).getDouble("length"));
            edgeList.put(edge.getId(),edge);
        }
        return edgeList;
    }

    /**
     * 从文件中读取顶点表
     * @param path 文件路径
     * @param vertexFN 顶点文件名
     * @return 顶点表 Hashmap<String, Vertex>
     */
    public static HashMap<String, Vertex> readVLFromFile(String path, String vertexFN){
        HashMap<String, Vertex> vertexList = new HashMap<>();
        JSONObject vertexesObject = JSONRW.readJson(path+vertexFN);
        JSONArray vertexesArray = vertexesObject.getJSONArray("nodes");
        Vertex vertex;   //用于指向一个顶点
        for(int i=0;i<vertexesArray.size();i++){
            vertex = new Vertex();
            vertex.setId(vertexesArray.getJSONObject(i).getString("id"));
            vertex.setLng(vertexesArray.getJSONObject(i).getDouble("lng"));
            vertex.setLat(vertexesArray.getJSONObject(i).getDouble("lat"));
            JSONArray sedgesArray = vertexesArray.getJSONObject(i).getJSONArray("sEdges");
            for(int j=0;j<sedgesArray.size();j++)
                vertex.getAdjEdges().add(sedgesArray.getString(j));
            vertexList.put(vertex.getId(),vertex);
        }
        return vertexList;
    }


    /**
     * 根据文件路径读取一个时间戳下的移动对象到HashMap<String,SnapObject> allObjects中
     * @param inPath
     * @throws IOException
     */
    public static void readObjectsFromFile(String inPath,HashMap<String, SnapObject> objectSet) throws IOException {
        BufferedReader br=new BufferedReader(new FileReader(inPath));
        String readLine=br.readLine();
        String[] line;
        SnapObject snapObject;
        while(readLine!=null) {
            line = readLine.split(",");
            snapObject = new SnapObject();
            snapObject.setId(line[0]);
            snapObject.setLng(Double.parseDouble(line[1]));
            snapObject.setLat(Double.parseDouble(line[2]));
            snapObject.setEdgeid(line[3]);
            snapObject.setPos(Double.parseDouble(line[4]));
            snapObject.setTime(line[5]);
            objectSet.put(line[0],snapObject);    //读入一个移动对象点，放入列表中
            readLine = br.readLine();
        }
    }



}
