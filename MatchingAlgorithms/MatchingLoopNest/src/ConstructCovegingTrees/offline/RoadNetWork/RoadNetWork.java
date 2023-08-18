package RoadNetWork;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import Tools.JSONRW;
import com.alibaba.fastjson.JSONObject;

/**
 * @ Class: RoadNetWork
 * @ Description:
 * @ Date 2019/10/16 20:37
 * @ Created by bridge
**/

public class RoadNetWork {
    private HashMap<String,Nodes> nodes = new HashMap<String,Nodes>();   //存储路网的点信息 key:node id, value:nodes
    private HashMap<String,Edges> edges = new HashMap<String,Edges>();   //存储路网的边信息 key:edge id, value:edges

    public RoadNetWork(String path) {
        System.out.println("正在读取路网数据...  "+new Date(System.currentTimeMillis()));
        String nodespath = path+"nodes.json";
        String edgespath = path+"edges.json";
        this.readNodes(nodespath);
        this.readEdegs(edgespath);
        System.out.println("路网数据加载完毕！  "+new Date(System.currentTimeMillis()));
    }

    public void readNodes(String path) {
        JSONObject rec = JSONRW.readJson(path);
        for(int i = 0; i < rec.getJSONArray("nodes").size(); i++) {
            JSONObject temp = (JSONObject) rec.getJSONArray("nodes").get(i);
            Nodes nod = new Nodes();
            nod.setId(temp.get("id").toString());
            nod.setLat(Double.parseDouble(temp.get("lat").toString()));
            nod.setLng(Double.parseDouble(temp.get("lng").toString()));
            ArrayList<String> array1 = new ArrayList<String>();
            ArrayList<String> array2 = new ArrayList<String>();
            for(int j = 0; j < temp.getJSONArray("sEdges").size(); j++) {
                array1.add(temp.getJSONArray("sEdges").get(j).toString());
            }
            nod.setsEdges(array1);
            for(int j = 0; j < temp.getJSONArray("eEdges").size(); j++) {
                array2.add(temp.getJSONArray("eEdges").get(j).toString());
            }
            nod.seteEdges(array2);
            nodes.put(nod.getId(),nod);
        }
    }

    public void readEdegs(String path) {
        JSONObject rec = JSONRW.readJson(path);
        for(int i = 0; i < rec.getJSONArray("edges").size(); i++) {
            JSONObject temp = (JSONObject) rec.getJSONArray("edges").get(i);
            Edges edg = new Edges();
            edg.setId(temp.get("id").toString());
            edg.setSp(temp.get("sp").toString());
            edg.setEp(temp.get("ep").toString());
            edg.setLength(Double.parseDouble(temp.get("length").toString()));
            edges.put(edg.getId(),edg);
        }
    }

    public HashMap<String, Nodes> getNodes() {
        return nodes;
    }

    public HashMap<String, Edges> getEdges() {
        return edges;
    }
}
