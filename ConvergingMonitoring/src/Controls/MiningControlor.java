package Controls;

import Classes.*;
import Clustering.DBSCAN_RoadNetwork;
import Clustering.FVARangeQuery.RNRangeQuery;
import DataProcessing.ProcessRN;
import Tools.DataRw;
import Tools.TimeConversion;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @Description: 汇聚模式挖掘控制器
 * @Author JJP
 * @Date 2021/5/14 10:37
 */
public class MiningControlor {
    //配置文件路径
    String settings;

    //数据成员
    RoadNetwork roadNetwork;  //路网
    RoadNetwork roadNetwork_leps; //边不超过eps的路网
    HashMap<String, SnapObject> snapshotObjectSet; //每次读取的移动对象集合
    HashMap<String, SnapObject> snapshotObjectSet_leps; //上述移动对象集合在不超过eps的路网中的位置
    HashMap<String, List<String>> edgeObjectIndex; //边-移动对象索引，于每次读数后创建，于每次计算完成后清空
    HashMap<String, Cluster> initialClusterSet; //初始时刻的聚类结果（也用于存放前一时刻的聚类结果）
    HashMap<String, Cluster> currentClusterSet; //当前时刻的聚类结果
    HashMap<String, String> clusterContainment; //两个簇集合之间的簇包含关系

    //工具类
    ProcessRN processRN;  //路网预处理类
    RNRangeQuery rnRQ;  //路网范围查询索引
    HierarchicalStructure hierStructure; //汇聚模式监测层级图结构

    //数据路径
    String rnFilePath,edgeFN,vertexFN;//路网文件路径，边表文件名，顶点表文件名
    String trajFilePath; //轨迹数据文件路径

    //时间参数
    String startstamp,endstamp; //轨迹数据起止时间
    int timeInterval; //采样频率
    int curTimestampNum; //当前已经完成计算的时间片数，初始为0

    //阈值参数
    int eps; //聚类半径
    int minpts; //聚类密度
    int mc; //成员数量阈值

    //************************************************************************

    /**
     * 核心计算函数：挖掘汇聚模式
     */
    public void miningConverging() throws IOException {
        //处理路网数据
        roadNetwork_leps = processRN.lowerEps(roadNetwork);
        //读取初始时刻移动对象数据
        loadObjectData();
        //初始时刻移动对象聚类
        originClustering();
        //在线监测汇聚模式挖掘更新
        convergingMonitor();

        int a=1;
    }


    /**
     * 构造函数
     * @param s 配置文件路径
     */
    public MiningControlor(String s) {
        try{
            this.settings = s;
            //加载参数
            loadConf(s);
            //加载路网数据
            loadRNData();
            //初始化工具类
            processRN = new ProcessRN(eps);
            //构建范围近邻查询索引
            this.rnRQ = new RNRangeQuery(roadNetwork,eps);
            //创建汇聚模式监测层级图结构
            this.hierStructure = new HierarchicalStructure();
            //初始化数据类
            this.snapshotObjectSet = new HashMap<>();
            this.snapshotObjectSet_leps = new HashMap<>();
            this.initialClusterSet = new HashMap<>();
            this.currentClusterSet = new HashMap<>();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 配置文件参数加载
     * @param s  配置文件路径
     * @throws IOException
     */
    private void loadConf(String s) throws IOException {
        InputStream in = new FileInputStream(new File(s));
        Properties properties=new Properties();
        properties.load(in);
        //加载路网数据文件路径和文件名
        this.rnFilePath = properties.getProperty("rnFilePath");
        this.edgeFN = properties.getProperty("edgeFN");
        this.vertexFN = properties.getProperty("vertexFN");
        this.trajFilePath = properties.getProperty("trajFilePath");
        this.startstamp = properties.getProperty("startstamp");
        this.endstamp = properties.getProperty("endstamp");
        this.timeInterval = Integer.parseInt(properties.getProperty("timeInterval"));
        this.curTimestampNum = 0;
        this.eps = Integer.parseInt(properties.getProperty("eps"));
        this.minpts = Integer.parseInt(properties.getProperty("minpts"));
        this.mc=Integer.parseInt(properties.getProperty("mc"));
    }

    /**
     * 加载路网数据
     */
    private void loadRNData(){
        roadNetwork = DataRw.readRNFromFile(rnFilePath,edgeFN,vertexFN);
    }

    /**
     * 读取一个时刻下的移动对象位置点集合(快照点集)
     * @throws IOException
     */
    private void loadObjectData() throws IOException {
        String timeFileName = TimeConversion.timeToOutpath(TimeConversion.getNextKTimeString(startstamp,curTimestampNum,timeInterval));
        DataRw.readObjectsFromFile(this.trajFilePath+timeFileName+".txt",this.snapshotObjectSet);
    }

    /**
     * 初始聚类
     */
    private void originClustering(){
        if(curTimestampNum==0){
            this.initialClusterSet = DBSCAN_RoadNetwork.getClusters(snapshotObjectSet,rnRQ,minpts,startstamp,1,eps,mc);
        }else{
            //采用FRV快照式算法聚类
            String timestamp = TimeConversion.getNextKTimeString(startstamp,curTimestampNum,timeInterval);
            this.currentClusterSet = DBSCAN_RoadNetwork.getClusters(snapshotObjectSet,rnRQ,minpts,timestamp,1,eps,mc);
        }
    }

    private void incrementalClustering(){
        rnRQ.update(snapshotObjectSet,minpts);
        //遍历前一时刻快照簇集中的每个簇，考虑是否可以扩展
        for(Map.Entry<String,Cluster> preCluster:this.initialClusterSet.entrySet()){
            Cluster _cluster = preCluster.getValue();
            //更新簇中移动对象的位置：第一，在当前时刻的位置；第二：在处理过的路网中的匹配.
            List<SnapObject> newLocationsOf_cluster = updateLocations(_cluster.getMenberPoints());
            //获取新时刻下移动对象集合所在的路网子图
            RoadNetwork subgraph = getSubgraphOfObjects(newLocationsOf_cluster);
            //验证移动对象集合是否同簇
            HashMap<String,String> objectsInOneCluster = new HashMap<>();
            boolean isInOneCluster = isInOneCluster(newLocationsOf_cluster,subgraph,objectsInOneCluster);
            //若同簇，则扩展簇，并将新簇中的对象在snapshotObjectSet中删除
            if(isInOneCluster){
                //扩展簇:查询objectsInOneCluster中不确定点的邻域，添加新的点，标记核心点，边界点，直到全部确定
                HashMap<String,String> certainPoints = new HashMap<>();//用于放置已经确定的点,core或border
                List<String> uncertainPoins = new ArrayList<>();//用于放置不确定的点
                for(Map.Entry<String,String> entry:objectsInOneCluster.entrySet()){
                    if(entry.getValue().equals("core"))
                        certainPoints.put(entry.getKey(),entry.getValue());
                    else{
                        //查询不确定点的邻域

                    }
                }

                //删除点
            }
        }
        //对snapshotObjectSet中剩余的点用原始方法聚类
    }
    /**
     * 更新簇中移动对象的位置：第一，在当前时刻的位置；第二：在处理过的路网中的匹配.
     * @param menberPoints 簇中的移动对象位置点集
     * @return 更新后的移动对象位置点集
     */
    private List<SnapObject> updateLocations(List<SnapObject> menberPoints){
        List<SnapObject> snapObjects = new ArrayList<>();
        //获取移动对象在当前时刻的位置
        for(int i=0;i<menberPoints.size();i++){
            SnapObject _so = menberPoints.get(i);
            SnapObject _newso = this.snapshotObjectSet_leps.get(_so.getId());
            snapObjects.add(_newso);
        }
        return snapObjects;
    }
    /**
     * 更新当前所有移动对象在不超过eps的路网中的位置
     */
    private void updateLocations(){
        for(Map.Entry<String,SnapObject> entry:snapshotObjectSet.entrySet()){
            SnapObject _so = entry.getValue();
            //如果当前边所在边发生过cut,则将其匹配至新的cut边上
            if(this.processRN.getRn_change().containsKey(_so.getEdgeid())){
                //判断新的位置点属于哪个cut
                //切分时是根据边长和切分份数决定的,因此需要先获取一份的长度
                Edge _e = this.roadNetwork.getEdgeList().get(_so.getEdgeid()); //原始边
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
                String newEdgeId = this.processRN.getRn_change().get(_so.getEdgeid()).get(cutNo-1);
                //获取在新cut边上的偏移量
                double newPos = _so.getPos()%avgLength;
                //更新位置点匹配的边和偏移量
                _so.setEdgeid(newEdgeId);
                _so.setPos(newPos);
            }
            this.snapshotObjectSet_leps.put(_so.getId(),_so);
        }
    }

    /**
     * 获取移动对象集合所在的路网子图（在边长度不超过eps的路网中）
     * @param objects 移动对象集合
     * @return 路网子图
     */
    private RoadNetwork getSubgraphOfObjects(List<SnapObject> objects){
        RoadNetwork subgraph = new RoadNetwork();
        //遍历移动对象列表，将其所在的边插入边表，将边的顶点插入顶点表
        for(int i=0;i<objects.size();i++){
            SnapObject _o = objects.get(i);
            if(!subgraph.getEdgeList().containsKey(_o.getEdgeid())){
                Edge _e = roadNetwork_leps.getEdgeList().get(_o.getEdgeid());
                subgraph.getEdgeList().put(_o.getEdgeid(),_e);
                //将边的顶点插入到顶点集
                if(!subgraph.getVertexList().containsKey(_e.getSp_id())){
                    Vertex _v = roadNetwork_leps.getVertexList().get(_e.getSp_id());
                    subgraph.getVertexList().put(_e.getSp_id(),_v);
                }
                if(!subgraph.getVertexList().containsKey(_e.getEp_id())){
                    Vertex _v = roadNetwork_leps.getVertexList().get(_e.getEp_id());
                    subgraph.getVertexList().put(_e.getEp_id(),_v);
                }
            }
        }
        return subgraph;
    }

    /**
     * 判断一个移动对象位置点集是否同簇
     * @param locationSet 移动对象位置点集
     * @param subgraph 位置点集所在的子图
     * @param objectsInOneCluster 用于记录子图中同簇的点,核心点标记为core,非核心点标记为notsure
     * @return 同簇返回true，不同簇返回false
     */
    private boolean isInOneCluster(List<SnapObject> locationSet,RoadNetwork subgraph,HashMap<String,String> objectsInOneCluster){
        boolean isIn = false;
        //遍历子图每个顶点，判断0.5eps邻域内是否满足minpts+1的要求，标记核心点
        HashMap<String,Vertex> vertexList = subgraph.getVertexList();
        for(Map.Entry<String,Vertex> entry:vertexList.entrySet()){
            Vertex _v = entry.getValue();
            //获取顶点的0.5eps邻域
            HashMap<String,SnapObject> halfEps = getHalfEpsOfVertex(_v);
            //若集合为空，则计算该顶点邻接顶点两两间的最小距离，若最小距离大于eps，则不同簇
            if(halfEps.size()==0){
                double minDist = getMinDistForVertexs(_v);
                if(minDist>=this.eps)
                    return false;
            }
            //若集合内的移动对象数量大于(minpts+1)则均为核心点，插入子图核心点集中；否则，判断将邻接边的点加进来是否大于(minpts+1)
            if(halfEps.size()>=minpts+1){
                for(Map.Entry<String,SnapObject> entry1:halfEps.entrySet()){
                    SnapObject so = entry1.getValue();
                    objectsInOneCluster.put(so.getId(),"core");
                    //与so同边的点放入同簇点集中，标注为notsure
                    List<String> objectInOneEdge = this.edgeObjectIndex.get(so.getEdgeid());
                    for(int j=0;j<objectInOneEdge.size();j++){
                        if(!objectsInOneCluster.containsKey(objectInOneEdge.get(j)))
                            objectsInOneCluster.put(objectInOneEdge.get(j),"notsure");
                    }
                }
            }else{
                //判断将邻接边的点加进来是否大于(minpts+1)
                for(int i=0;i<_v.getAdjEdges().size();i++){
                    //获取一条邻接边上的所有移动对象
                    List<String> objectInEdge = this.edgeObjectIndex.get(_v.getAdjEdges().get(i));
                    //计算该边移动对象与half邻域内的移动对象总和
                    int count = halfEps.size();
                    for(int j=0;j<objectInEdge.size();j++){
                        if(!halfEps.containsKey(objectInEdge.get(j)))
                            count++;
                    }
                    //若总和超过(minpts+1)，则该边及邻域内的移动对象同簇，该边上在邻域内的对象为核心点
                    if(count>=minpts+1){
                        for(Map.Entry<String,SnapObject> entry1:halfEps.entrySet()){
                            SnapObject so = entry1.getValue();
                            //如果so在边上，则为核心点
                            if(so.getEdgeid().equals(_v.getAdjEdges().get(i)))
                                objectsInOneCluster.put(so.getId(),"core");
                            else if(!objectsInOneCluster.containsKey(so.getId()))
                                objectsInOneCluster.put(so.getId(),"notsure");
                        }
                        //遍历边上的点，将点插入到同簇表中
                        for(int j=0;j<objectInEdge.size();j++){
                            if(!objectsInOneCluster.containsKey(objectInEdge.get(j)))
                                objectsInOneCluster.put(objectInEdge.get(j),"notsure");
                        }
                    }
                }
            }
        }
        //判断给定的移动对象集合是否都在同簇表中，都在则同簇，否则不同簇
        for(int i=0;i<locationSet.size();i++){
            if(!objectsInOneCluster.containsKey(locationSet.get(i)))
                break;
            if(i==locationSet.size()-1)
                isIn = true;
        }
        return isIn;
    }

    /**
     * 获取顶点邻接顶点间的最短直线距离（欧氏距离）
     * @param v 路网顶点
     * @return 其邻接顶点的最短直线距离
     */
    private double getMinDistForVertexs(Vertex v){
        double minDist = Double.MAX_VALUE;
        //获取顶点的邻接顶点列表
        List<Vertex> adjVertexList = new ArrayList<>();
        for(int i=0;i<v.getAdjEdges().size();i++){
            Edge adjE = this.roadNetwork_leps.getEdgeList().get(v.getAdjEdges().get(i));
            if(adjE.getSp_id().equals(v.getId()))
                adjVertexList.add(this.roadNetwork_leps.getVertexList().get(adjE.getEp_id()));
            else
                adjVertexList.add(this.roadNetwork_leps.getVertexList().get(adjE.getSp_id()));
        }
        //双重循环计算顶点间的距离，取最短
        for(int i=0;i<adjVertexList.size();i++){
            for(int j=i+1;j<adjVertexList.size();j++){
                Vertex v1 = adjVertexList.get(i);
                Vertex v2 = adjVertexList.get(j);
                double dist = TimeConversion.getDistance(v1.getLat(),v1.getLng(),v2.getLat(),v2.getLng());
                if(dist<minDist)
                    minDist = dist;
            }
        }
        return minDist;
    }

    /**
     * 获取路网顶点1/2eps邻域内的移动对象集合
     * @param v 路网顶点
     * @return 1/2eps邻域内的移动对象集合
     */
    private HashMap<String,SnapObject> getHalfEpsOfVertex(Vertex v){
        HashMap<String,SnapObject> halfEps = new HashMap<>();
        //遍历顶点的邻接边，取邻接边上0.5eps聚类内的移动对象
        for(int i=0;i<v.getAdjEdges().size();i++){
            //获取邻接边
            Edge _adjE = roadNetwork_leps.getEdgeList().get(v.getAdjEdges().get(i));
            //获取邻接边上所有的移动对象
            List<String> objects = this.edgeObjectIndex.get(_adjE.getId());
            //判断当前节点是该邻接边的起点还是终点
            //若为起点，则顺序访问列表，获取移动对象
            if(_adjE.getSp_id().equals(v.getId())){
                for(int j=0;j<objects.size();j++){
                    SnapObject _so = this.snapshotObjectSet_leps.get(objects.get(j));
                    if(_so.getPos()<=this.eps/2)
                        halfEps.put(_so.getId(),_so);
                    else
                        break;
                }
            }else {//若为终点，则逆序访问列表，获取移动对象
                for(int j=objects.size()-1;j>=0;j--){
                    SnapObject _so = this.snapshotObjectSet_leps.get(objects.get(j));
                    if(_adjE.getLength()-_so.getPos()<=this.eps/2)
                        halfEps.put(_so.getId(),_so);
                    else
                        break;
                }
            }
        }
        return halfEps;
    }

    private void convergingMonitor() throws IOException {
        //创建用于汇聚模式监测的层级图结构
        this.hierStructure = new HierarchicalStructure(this.initialClusterSet,startstamp);
        //开启持续性监测
        int totalTimeNum = TimeConversion.getTimestampAmount(startstamp,endstamp,timeInterval); //获取总的时间戳数量
        for(int i=1;i<totalTimeNum;i++){
            //获取当前时间戳
            String currTime = TimeConversion.getNextKTimeString(startstamp,i,timeInterval);
            this.curTimestampNum++;
            //读取当前的移动对象位置点集
            loadObjectData();
            //更新移动对象位置点集在不超过eps的路网中的位置
            updateLocations();
            //构建路网边-移动对象索引,String:路网边id;List<String>:边上的移动对象id列表
            edgeObjectIndexConstruct();



            int a=1;
        }
    }

    /**
     * 路网边和移动对象的索引，用于查询边上的移动对象列表，该列表从边起点侧开始，以pos值从小到大排列
     * @return 该索引
     */
    private void edgeObjectIndexConstruct(){
        this.edgeObjectIndex = new HashMap<>();
        //遍历移动对象，放到相应的边表中
        for(Map.Entry<String,SnapObject> entry:snapshotObjectSet_leps.entrySet()){
            SnapObject _so = entry.getValue();
            //如果对象所在的边表已存在，则加入到其中；否则，创建边表加入其中
            List<String> _edgeObjectList = new ArrayList<>();
            if(edgeObjectIndex.containsKey(_so.getEdgeid())){
                _edgeObjectList = edgeObjectIndex.get(_so.getEdgeid());
                for(int i=0;i<_edgeObjectList.size();i++){
                    if(_so.getPos()<=snapshotObjectSet_leps.get(_edgeObjectList.get(i)).getPos()){
                        _edgeObjectList.add(i,_so.getId());
                        break;
                    }
                }
            }else{
                _edgeObjectList.add(_so.getId());
            }
            edgeObjectIndex.put(_so.getEdgeid(),_edgeObjectList);
        }
    }


}
