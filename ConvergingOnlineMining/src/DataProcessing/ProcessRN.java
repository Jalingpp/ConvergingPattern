package DataProcessing;

import Classes.Edge;
import Classes.RoadNetwork;
import Classes.Vertex;
import Tools.DataRw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 对路网文件进行预处理
 * @Author JJP
 * @Date 2021/5/14 9:33
 */
public class ProcessRN {
    int eps;  //参数-聚类半径
    RoadNetwork rn_lowerEPS; //处理之后的路网，每条边长度不超过eps
    HashMap<String, List<String>> rn_change; //变化后的路网与原有路网的关联,String:原有路网中边id;List<String>:切分后的边id列表

    /**
     * 构造函数
     * @param eps 聚类半径
     */
    public ProcessRN(int eps) {
        this.eps = eps;
        this.rn_lowerEPS = new RoadNetwork();
        this.rn_change = new HashMap<>();
    }

    /**
     * 核心计算函数：将路网边长度处理为不超过eps
     * @param rn_original 原始路网
     * @return 处理后的路网
     */
    public RoadNetwork lowerEps(RoadNetwork rn_original){
        rn_lowerEPS = new RoadNetwork();
        //获取原始路网的边表和顶点表
        HashMap<String,Edge> edgeList_o = rn_original.getEdgeList();
        HashMap<String,Vertex> vertexList_o = rn_original.getVertexList();
        //复制顶点表到rn_lowerEPS的顶点表中
        rn_lowerEPS.setVertexList(vertexList_o);
        //遍历边表，若长度小于eps直接放入rn_lowerEPS的边表中，若大于，则切分边
        int newEdgeAmount = 0,newVertexAmount = 0;
        for(Map.Entry<String,Edge> entry: edgeList_o.entrySet()){
            Edge _e = entry.getValue();
            if(_e.getLength()<=eps)
                rn_lowerEPS.getEdgeList().put(_e.getId(),_e);
            else {
                List<String> cutfor_e = new ArrayList<>();  //记录_e切分后边的列表,用于建立新边与原边的联系
                //获取边的两个顶点
                Vertex _v1 = rn_original.getVertexList().get(_e.getSp_id());
                Vertex _v2 = rn_original.getVertexList().get(_e.getEp_id());
                //在两个顶点的邻接边中，删除本边
                _v1.getAdjEdges().remove(_e.getId());
                _v2.getAdjEdges().remove(_e.getId());
                //计算切分份数和每份的长度
                int cutAmount = (int) (_e.getLength()/eps)+1;
                double avgLength = _e.getLength()/cutAmount;
                /*将边等分为cutAmount份
                 具体做法：判断边两个端点经纬度的大小关系，判定边是/型还是\型
                         若满足lng1<lng2&lat1<lat2,则为/型，计算公式为：
                         lng_i=lng_min+(lng_max-lng_min)*i/cutAmount,
                         lat_i=lat_min+(lat_max-lat_min)*i/cutAmount;
                         若满足lng1<lng2&lat1>lat2,则为\型，则计算公式为：
                         lng_i=lng_min+(lng_max-lng_min)*i/cutAmount,
                         lat_i=lat_max-(lat_max-lat_min)*i/cutAmount;
                 */
                //第一步：比较两个顶点的经纬度大小，判断类型
                Boolean isType1 = false;  //记录是否是/类型，true为是，false为不是
                double lng_max,lng_min,lat_max,lat_min;
                if(_v1.getLng()<= _v2.getLng()&&_v1.getLat()<= _v2.getLat()){
                    //边是/型，且_v1是最小点
                    lng_max = _v2.getLng();
                    lng_min = _v1.getLng();
                    lat_max = _v2.getLat();
                    lat_min = _v1.getLat();
                    isType1 = true;
                }else if(_v2.getLng()<= _v1.getLng()&&_v2.getLat()<= _v1.getLat()) {
                    //边是/型，且_v2是最小点
                    lng_max = _v1.getLng();
                    lng_min = _v2.getLng();
                    lat_max = _v1.getLat();
                    lat_min = _v2.getLat();
                    isType1 = true;
                }else if(_v1.getLng()<= _v2.getLng()&&_v1.getLat()>_v2.getLat()){
                    //边是\型，且_v1是高点
                    lng_min = _v1.getLng();
                    lng_max = _v2.getLng();
                    lat_max = _v1.getLat();
                    lat_min = _v2.getLat();
                }else{
                    //边是\型，且_v2是高点
                    lng_min = _v2.getLng();
                    lng_max = _v1.getLng();
                    lat_max = _v2.getLat();
                    lat_min = _v1.getLat();
                }
                //第二步：计算新边和新顶点
                Vertex _newV1 = _v1;
                Vertex _newV2;
                Edge _newE;
                for(int i=1;i<cutAmount;i++){
                    _newV2 = new Vertex();
                    _newE = new Edge();
                    String newEdgeId = "new"+(++newEdgeAmount);
                    String newVertexId = "new"+(++newVertexAmount);
                    //设置新V2的ID
                    _newV2.setId(newVertexId);
                    //计算新V2的经纬度
                    if(isType1){
                        _newV2.setLng(lng_min+(lng_max-lng_min)*i/cutAmount);
                        _newV2.setLat(lat_min+(lat_max-lat_min)*i/cutAmount);
                    }else{
                        _newV2.setLng(lng_min+(lng_max-lng_min)*i/cutAmount);
                        _newV2.setLat(lat_max-(lat_max-lat_min)*i/cutAmount);
                    }
                    //设置新V2的邻接边
                    _newV2.addAdjEdge(newEdgeId);
                    _newV2.addAdjEdge("new"+(newEdgeAmount+1));
                    //将新的顶点插入到顶点表中
                    rn_lowerEPS.getVertexList().put(_newV2.getId(),_newV2);
                    //设置新边的id、长度、顶点
                    _newE.setId(newEdgeId);
                    _newE.setLength(avgLength);
                    _newE.setSp_id(_newV1.getId());
                    //将新边加入到sp的邻接表中
                    if(!_newV1.getAdjEdges().contains(_newE.getId()))
                        _newV1.addAdjEdge(_newE.getId());
                    _newE.setEp_id(_newV2.getId());
                    if(!_newV2.getAdjEdges().contains(_newE.getId()))
                        _newV2.addAdjEdge(_newE.getId());
                    //将新边插入到边表中
                    rn_lowerEPS.getEdgeList().put(_newE.getId(),_newE);
                    //将新边id插入到记录表中，用于建立与原边的关联
                    cutfor_e.add(_newE.getId());
                    //将_newV1更新为_newV2
                    _newV1 = _newV2;
                }
                //单独计算最后一条边进行处理，无新顶点，只新增一条边
                _newV2 = _v2;  //新边的两个端点分别为刚刚新加的顶点和原边的第二个顶点
                _newE = new Edge();
                _newE.setId("new"+(++newEdgeAmount));
                _newE.setLength(avgLength);
                _newE.setSp_id(_newV1.getId());
                //将新边加入到sp的邻接表中
                if(!_newV1.getAdjEdges().contains(_newE.getId()))
                    _newV1.addAdjEdge(_newE.getId());
                _newE.setEp_id(_newV2.getId());
                if(!_newV2.getAdjEdges().contains(_newE.getId()))
                    _newV2.addAdjEdge(_newE.getId());
                rn_lowerEPS.getEdgeList().put(_newE.getId(),_newE);
                //将新边id插入到记录表中，用于建立与原边的关联
                cutfor_e.add(_newE.getId());
                //将原边与其cut列表加入到关联表中
                this.rn_change.put(_e.getId(),cutfor_e);
            }
        }
        System.out.println("newEdgeAmount = "+newEdgeAmount+"  newVertexAmount = "+newVertexAmount);
        return rn_lowerEPS;
    }

    public HashMap<String, List<String>> getRn_change() {
        return rn_change;
    }

    public static void main(String[] args) throws IOException {
        //读取原始路网数据
        String rnFilePath = "E:/1 Research/data/RoadNetwork_shanghai/undigraph/";
        String edgeFN = "edges.json";
        String vertexFN = "nodes.json";
        int eps = 200;
        RoadNetwork roadNetwork = DataRw.readRNFromFile(rnFilePath,edgeFN,vertexFN);
        //处理路网数据
        ProcessRN processRN = new ProcessRN(eps);
        RoadNetwork roadNetwork_leps = processRN.lowerEps(roadNetwork);
        //输出路网数据
        String rnlepsFilePath = "E:/1 Research/data/RoadNetwork_shanghai/undigraph/";
        String edgelepsFN = "edges_lowerEps";
        String vertexlepsFN = "nodes_lowerEps";
        DataRw.writeRoadNetworkToJson(roadNetwork_leps,rnlepsFilePath,edgelepsFN,vertexlepsFN);
    }
}
