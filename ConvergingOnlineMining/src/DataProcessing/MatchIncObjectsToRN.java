package DataProcessing;

import Classes.Edge;
import Classes.RoadNetwork;
import Classes.SnapObject;
import Tools.DataRw;
import Tools.TimeConversion;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 将路网处理为边长不超过eps的路网，并将增量的轨迹数据匹配到边长不超过eps的路网上
 * @Author jjp
 * @Date 2021/11/5 18:48
 */
public class MatchIncObjectsToRN {
    static RoadNetwork originRN;   //原始路网
    static RoadNetwork lepsRN;    //边长不超过eps的路网
    static HashMap<String, SnapObject> originIncObjectSet = new HashMap<>();  //每片增量的移动对象位置数据
    static HashMap<String, SnapObject> lepsIncObjectSet = new HashMap<>();  //每片匹配到边长不超过eps的路网上的位置数据

    public static void main(String[] args) throws IOException {
        //读取原始路网数据
        String rnFilePath = "E:/1 Research/data/RoadNetwork_shanghai/undigraph/";
        String edgeFN = "edges.json";
        String vertexFN = "nodes.json";
        int eps = 200;
        originRN = DataRw.readRNFromFile(rnFilePath,edgeFN,vertexFN);
        //处理路网数据
        ProcessRN processRN = new ProcessRN(eps);
        lepsRN = processRN.lowerEps(originRN);
        //输出路网数据
        String rnlepsFilePath = "E:/1 Research/data/RoadNetwork_shanghai/undigraph/";
        String edgelepsFN = "edges_lowerEps";
        String vertexlepsFN = "nodes_lowerEps";
        DataRw.writeRoadNetworkToJson(lepsRN,rnlepsFilePath,edgelepsFN,vertexlepsFN);
        //逐片读入增量数据，进行匹配
        String incObjectsFilePath = "E:/1 Research/data/TestDataForConvergingMonitor/IncrementalData/";
        String startTime = "2015-04-11 00:00:00.0";
        String endTime = "2015-04-11 00:05:00.0";
        int timeInterval = 60000;
        int timeAmount = TimeConversion.getTimestampAmount(startTime,endTime,timeInterval);
        for(int i=0;i<=timeAmount;i++){
            //读入增量数据
            String incObjectsFN = TimeConversion.timeToOutpath(TimeConversion.getNextKTimeString(startTime,i,timeInterval));
            String incObjectsFP = incObjectsFilePath+incObjectsFN+".txt";
            System.out.print(incObjectsFN+": Reading...");
            DataRw.readObjectsFromFile(incObjectsFP,originIncObjectSet);
            //更新增量数据在边长不超过eps的路网中的位置
            System.out.print("Updating...");
            for(Map.Entry<String,SnapObject> entry:originIncObjectSet.entrySet()){
                SnapObject _so = entry.getValue();
                //如果当前边所在边发生过cut,则将其匹配至新的cut边上
                if(processRN.getRn_change().containsKey(_so.getEdgeid())){
                    //判断新的位置点属于哪个cut
                    //切分时是根据边长和切分份数决定的,因此需要先获取一份的长度
                    Edge _e = originRN.getEdgeList().get(_so.getEdgeid()); //原始边
                    int cutAmount = (int) (_e.getLength()/eps)+1;  //计算切分数
                    double avgLength = _e.getLength()/cutAmount;   //计算每个cut的长度
                    //计算点位于哪个cut上
                    int cutNo=1;
                    if(_so.getPos()%avgLength==0){
                        cutNo = (int)(_so.getPos()/avgLength);
                    }else{
                        cutNo = (int)(_so.getPos()/avgLength)+1;
                    }
                    if(cutNo>cutAmount)
                        cutNo = cutAmount;
//                    System.out.println("cutAmount="+cutAmount+"  cutNo="+cutNo);
                    //获取新cut边的id
                    String newEdgeId = processRN.getRn_change().get(_so.getEdgeid()).get(cutNo-1);
                    //获取在新cut边上的偏移量
                    double newPos = _so.getPos()%avgLength;
                    //更新位置点匹配的边和偏移量
                    _so.setEdgeid(newEdgeId);
                    _so.setPos(newPos);
                }
                lepsIncObjectSet.put(_so.getId(),_so);
            }
            //输出新的位置点
            String lepsIncObjectsFilePath = "E:/1 Research/data/TestDataForConvergingMonitor/LepsIncrementalData/";
            String lepsIncObjectsFP = lepsIncObjectsFilePath+incObjectsFN+".txt";
            System.out.print("Writing...");
            DataRw.writeObjectsToFile(lepsIncObjectsFP,lepsIncObjectSet);
            //情空位置点集
            originIncObjectSet.clear();
            lepsIncObjectSet.clear();
            System.out.println("finish!");
        }

    }

}
