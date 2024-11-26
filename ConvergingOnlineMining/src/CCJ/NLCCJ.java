package CCJ;

import Classes.Cluster;

import java.util.HashMap;
import java.util.Map;

public class NLCCJ {

    /**
     * 获取此次连接后产生的新的汇聚树
     * @param clusterSet1  簇集1
     * @param clusterSet2  簇集2
     * @return 簇包含匹配
     */
    public static HashMap<String,String> getConverging(HashMap<String, Cluster> clusterSet1, HashMap<String, Cluster> clusterSet2){
        HashMap<String,String> clusterMatches = new HashMap<>();
        int disMatchClusterAmount = 0;
        //循环嵌套做包含关系判断
        for(Map.Entry<String, Cluster> clusterEntry1:clusterSet1.entrySet()) {
            Cluster cluster1 = clusterEntry1.getValue();
            boolean isFind = false;
            for (Map.Entry<String, Cluster> clusterEntry2 : clusterSet2.entrySet()) {
                Cluster cluster2 = clusterEntry2.getValue();
                //簇1被簇2包含
                if (cluster1.isCluded(cluster2)) {
                    clusterMatches.put(cluster1.getId(), cluster2.getId());
                    isFind = true;
                }
            }
            if(isFind==false)
                disMatchClusterAmount++;
        }
//        System.out.println("NLCCJ: There are "+disMatchClusterAmount+"/"+clusterSet1.size()+" disMatch clusters in clusterSet1!");
        return clusterMatches;
    }
}
