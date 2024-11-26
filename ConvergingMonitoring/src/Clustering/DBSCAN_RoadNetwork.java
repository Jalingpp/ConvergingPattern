package Clustering;

import Classes.*;
import Clustering.FVARangeQuery.RNRangeQuery;

import java.util.*;

public class DBSCAN_RoadNetwork {

    /**
     * 单一时刻下，路网上的聚类算法(FVA)
     * @param snapshotObjectSet  快照簇集，即单一时刻下的移动对象点集
     * @param rnRQ  路网范围查询索引
     * @param minpts 聚类参数
     * @param timeStamp 当前时间戳
     * @param i    当前时间在时间窗口内的序号
     * @param eps  聚类参数
     * @param mc  聚类参数
     * @return
     */
    public static HashMap<String, Cluster> getClusters(HashMap<String, SnapObject> snapshotObjectSet, RNRangeQuery rnRQ, int minpts, String timeStamp, int i, int eps, int mc) {
        int idnum = 0;   //表示簇的id
        Boolean flag = true;
        HashMap<String, Cluster> curClusterSet = new HashMap<>();  //当前时间戳下的簇集
        List<String> nearPoints = new ArrayList<>();   //点的近邻集合，容器，可多次使用
        HashMap<String, Boolean> visitedFlag = new HashMap<String, Boolean>();   //是否访问的集合

        //更新RNRQT索引
//        long ts = System.currentTimeMillis();
        rnRQ.update(snapshotObjectSet,minpts);
//        long te = System.currentTimeMillis();
//        System.out.println("rnRQ update in "+ (te-ts)+" ms");

//        long ts_query,te_query,timeForQuery=0;

        for (String key : snapshotObjectSet.keySet()) {
            visitedFlag.put(key, false);
        }
//        ts = System.currentTimeMillis();
        do {
            flag = false;
            idnum++;
            Cluster cluster = new Cluster(String.valueOf(idnum), timeStamp, String.valueOf(i));
            Queue<String> queue = new LinkedList<>();
            HashMap<String,Boolean> queued = new HashMap<>();  //放置已经入过队列的点
            //随机选择一点，设为访问，入栈
            SnapObject onePoint;
            for (String key : visitedFlag.keySet()) {
                if (visitedFlag.get(key) == false) {
                    visitedFlag.put(key, true);
                    onePoint = snapshotObjectSet.get(key);
                    queue.add(onePoint.getId());
                    queued.put(onePoint.getId(),true);
                    flag = true;
                    break;
                }
            }
            //flag标识是否取得一个未被标记的点
            if (flag == true) {
                while (!queue.isEmpty()) {
                    onePoint = snapshotObjectSet.get(queue.poll());  //点出队列
                    cluster.addPoint(onePoint);  //将该点加入簇中
                    visitedFlag.put(onePoint.getId(), true);   //将该点设为已访问

//                    ts_query = System.currentTimeMillis();
                    //FVA范围查询
                    nearPoints = rnRQ.getResultObjects(onePoint.getId());
//                    te_query = System.currentTimeMillis();
//                    timeForQuery+=te_query-ts_query;

                    if (nearPoints.size() >= minpts) {   //若为核心点，则将其近邻入队列
                        for (int j=0;j<nearPoints.size();j++) {
                            if(queued.get(nearPoints.get(j))==null&&!visitedFlag.get(nearPoints.get(j))&&!queue.contains(nearPoints.get(j))) {
                                queue.add(nearPoints.get(j));
                                queued.put(nearPoints.get(j),true);
                            }
                        }
                    }
                }
                if(cluster.getMenberPoints().size()>=mc)
                    curClusterSet.put(cluster.getId(), cluster);   //簇不能继续扩展时，将簇加入到簇集中
            }
        } while (flag);

//        te = System.currentTimeMillis();
//        System.out.println("All queries runs "+timeForQuery+" ms");
//        System.out.println("DBSCAN runs "+(te-ts)+" ms");
        return curClusterSet;
    }


}

