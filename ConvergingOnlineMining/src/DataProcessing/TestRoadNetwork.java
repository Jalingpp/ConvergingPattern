package DataProcessing;

import Classes.RoadNetwork;
import Classes.Vertex;
import Tools.DataRw;

import java.io.IOException;
import java.util.Map;

/**
 * @Description: ����·�������Ƿ���ȷ
 * @Author JJP
 * @Date 2021/11/10 8:59
 */
public class TestRoadNetwork {

    public static void main(String[] args) throws IOException {
        String rnPath = "E:/1 Research/data/RoadNetwork_shanghai/undigraph/";
        String edgeFN = "edges_lowerEps.json";
        String nodeFN = "nodes_lowerEps.json";
        RoadNetwork roadNetwork = DataRw.readRNFromFile(rnPath,edgeFN,nodeFN);
        //��֤·��������ڽӱ��Ƿ���
        for(Map.Entry<String, Vertex> entry:roadNetwork.getVertexList().entrySet()){
            Vertex v = entry.getValue();
            for(int i=0;i<v.getAdjEdges().size();i++){
                String edgeId = v.getAdjEdges().get(i);
                if(!roadNetwork.getEdgeList().containsKey(edgeId))
                    System.out.println("Edge "+edgeId+" of Node "+v.getId()+" is not found!");
            }
        }
    }

}
