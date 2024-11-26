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
 * @Description: ��·������Ϊ�߳�������eps��·�������������Ĺ켣����ƥ�䵽�߳�������eps��·����
 * @Author jjp
 * @Date 2021/11/5 18:48
 */
public class MatchIncObjectsToRN {
    static RoadNetwork originRN;   //ԭʼ·��
    static RoadNetwork lepsRN;    //�߳�������eps��·��
    static HashMap<String, SnapObject> originIncObjectSet = new HashMap<>();  //ÿƬ�������ƶ�����λ������
    static HashMap<String, SnapObject> lepsIncObjectSet = new HashMap<>();  //ÿƬƥ�䵽�߳�������eps��·���ϵ�λ������

    public static void main(String[] args) throws IOException {
        //��ȡԭʼ·������
        String rnFilePath = "E:/1 Research/data/RoadNetwork_shanghai/undigraph/";
        String edgeFN = "edges.json";
        String vertexFN = "nodes.json";
        int eps = 200;
        originRN = DataRw.readRNFromFile(rnFilePath,edgeFN,vertexFN);
        //����·������
        ProcessRN processRN = new ProcessRN(eps);
        lepsRN = processRN.lowerEps(originRN);
        //���·������
        String rnlepsFilePath = "E:/1 Research/data/RoadNetwork_shanghai/undigraph/";
        String edgelepsFN = "edges_lowerEps";
        String vertexlepsFN = "nodes_lowerEps";
        DataRw.writeRoadNetworkToJson(lepsRN,rnlepsFilePath,edgelepsFN,vertexlepsFN);
        //��Ƭ�����������ݣ�����ƥ��
        String incObjectsFilePath = "E:/1 Research/data/TestDataForConvergingMonitor/IncrementalData/";
        String startTime = "2015-04-11 00:00:00.0";
        String endTime = "2015-04-11 00:05:00.0";
        int timeInterval = 60000;
        int timeAmount = TimeConversion.getTimestampAmount(startTime,endTime,timeInterval);
        for(int i=0;i<=timeAmount;i++){
            //������������
            String incObjectsFN = TimeConversion.timeToOutpath(TimeConversion.getNextKTimeString(startTime,i,timeInterval));
            String incObjectsFP = incObjectsFilePath+incObjectsFN+".txt";
            System.out.print(incObjectsFN+": Reading...");
            DataRw.readObjectsFromFile(incObjectsFP,originIncObjectSet);
            //�������������ڱ߳�������eps��·���е�λ��
            System.out.print("Updating...");
            for(Map.Entry<String,SnapObject> entry:originIncObjectSet.entrySet()){
                SnapObject _so = entry.getValue();
                //�����ǰ�����ڱ߷�����cut,����ƥ�����µ�cut����
                if(processRN.getRn_change().containsKey(_so.getEdgeid())){
                    //�ж��µ�λ�õ������ĸ�cut
                    //�з�ʱ�Ǹ��ݱ߳����зַ���������,�����Ҫ�Ȼ�ȡһ�ݵĳ���
                    Edge _e = originRN.getEdgeList().get(_so.getEdgeid()); //ԭʼ��
                    int cutAmount = (int) (_e.getLength()/eps)+1;  //�����з���
                    double avgLength = _e.getLength()/cutAmount;   //����ÿ��cut�ĳ���
                    //�����λ���ĸ�cut��
                    int cutNo=1;
                    if(_so.getPos()%avgLength==0){
                        cutNo = (int)(_so.getPos()/avgLength);
                    }else{
                        cutNo = (int)(_so.getPos()/avgLength)+1;
                    }
                    if(cutNo>cutAmount)
                        cutNo = cutAmount;
//                    System.out.println("cutAmount="+cutAmount+"  cutNo="+cutNo);
                    //��ȡ��cut�ߵ�id
                    String newEdgeId = processRN.getRn_change().get(_so.getEdgeid()).get(cutNo-1);
                    //��ȡ����cut���ϵ�ƫ����
                    double newPos = _so.getPos()%avgLength;
                    //����λ�õ�ƥ��ıߺ�ƫ����
                    _so.setEdgeid(newEdgeId);
                    _so.setPos(newPos);
                }
                lepsIncObjectSet.put(_so.getId(),_so);
            }
            //����µ�λ�õ�
            String lepsIncObjectsFilePath = "E:/1 Research/data/TestDataForConvergingMonitor/LepsIncrementalData/";
            String lepsIncObjectsFP = lepsIncObjectsFilePath+incObjectsFN+".txt";
            System.out.print("Writing...");
            DataRw.writeObjectsToFile(lepsIncObjectsFP,lepsIncObjectSet);
            //���λ�õ㼯
            originIncObjectSet.clear();
            lepsIncObjectSet.clear();
            System.out.println("finish!");
        }

    }

}
