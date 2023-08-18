import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//一个时间下的簇的集合
public class TimeClusters {

    //key:簇id  value:簇
    HashMap<String,Cluster> Clusters = new HashMap<>();
    int ClusterSize;
    String Time;

    public TimeClusters() {

    }

    public String getTime() {
        return Time;
    }

    public void setTime(String time) {
        Time = time;
    }

    public HashMap<String, Cluster> getClusters() {
        return Clusters;
    }

    public int getClusterSize() {
        return ClusterSize;
    }

    public TimeClusters(String path, int Itier) throws IOException {
        //读入该时间片中的所有簇的信息
        JSONObject js = JSONRW.readJson(path);
        this.ClusterSize = js.getInteger("ClusterSize");
        this.Time = js.getString("Time");
        for(int i = 0; i < this.ClusterSize; i++) {   //遍历簇集合
            Cluster clu = new Cluster();
            JSONObject js1 = (JSONObject)js.getJSONArray("Clusters").get(i);  //每个簇的内容
            clu.Cid = js1.getString("CId");
            clu.Scale = js1.getInteger("Scale");
            clu.Time = js.getString("Time");
            clu.eventStartTime = clu.Time;
            clu.Tier =Itier;
            for(int j = 0; j < js1.getJSONArray("E").size(); j++) {
                String eid = js1.getJSONArray("E").getString(j);
                clu.E.add(eid);
            }
            for(int j = 0; j < clu.Scale; j++) {
                JSONObject js2= (JSONObject)((JSONObject)js.getJSONArray("Clusters").get(i)).getJSONArray("Objects").get(j);
                Objects p = new Objects();
                p.OId = js2.getString("OId");
                p.e = js2.getString("e");
                p.Lat = js2.getDouble("Lat");
                p.Lng = js2.getDouble("Lng");
                p.pos = js2.getDouble("pos");
                clu.Objects.add(p);
            }
            this.Clusters.put(clu.Cid,clu);
        }
    }

//    public Cluster getCluster(String Cid) {           //根据簇的id返回簇
//        for(int i = 0; i < this.Clusters.size(); i++) {
//            if(Clusters.get(i).Cid.equals(Cid)) {
//                return Clusters.get(i);
//            }
//        }
//        return new Cluster();
//    }

}
