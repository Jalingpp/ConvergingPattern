package Controls;

import CCJ.NLCCJ;
import Classes.*;
import Clustering.DBSCAN_RoadNetwork;
import Clustering.FVARangeQuery.RNRangeQuery;
import Clustering.MBCClusterUpdating;
import Clustering.MBCCUStructure.ObjectsInforList;
import Clustering.MBKCUStructure.ObjectsInforListKP;
import Clustering.MBKClusterUpdating;
import Clustering.SCVClusterUpdating;
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
    HashMap<String, SnapObject> incrementalObjectSet; //每次更新的移动对象集合
    HashMap<String, SnapObject> objectsToDelete; //每次待删除的移动对象位置点集（被更新的位置覆盖）
    HashMap<String, Cluster> preClusterSet; //初始时刻的聚类结果（也用于存放前一时刻的聚类结果）
    HashMap<String, Cluster> currentClusterSet; //当前时刻的聚类结果
    HashMap<String, String> clusterContainment; //两个簇集合之间的簇包含关系

    //工具类
    RNRangeQuery rnRQ,rnRQ_leps;  //路网范围查询索引
    HierarchicalStructure hierStructure; //汇聚模式监测层级图结构

    //数据路径
    String rnFilePath,edgeFN,vertexFN,lepsedgeFN,lepsvertexFN;//路网文件路径，边表文件名，顶点表文件名
    String trajFilePath,incTrajFilePath; //轨迹数据文件路径

    //时间参数
    String startstamp,endstamp; //轨迹数据起止时间
    int timeInterval; //采样频率
    int curTimestampNum; //当前已经完成计算的时间片数，初始为0

    //阈值参数
    int eps; //聚类半径
    int minpts; //聚类密度
    int mc; //成员数量阈值
    int patternScale; //模式规模阈值
    int patternDuration; //模式时长阈值

    //************************************************************************

    /**
     * 核心计算函数：挖掘汇聚模式
     */
    public void miningConverging() throws IOException {
        //读取初始时刻移动对象数据
//        loadObjectData();   //快照模式
        loadIncObjectData();  //增量窗口模式
        //初始时刻移动对象聚类
        DCRNClustering();   //需要手动切换rnRQ与rnRQ_leps
        //在线监测汇聚模式挖掘更新
        //基于DCRN的汇聚模式在线挖掘算法
        long ts_DCRN = System.currentTimeMillis();
        convergingMonitor_DCRN();
        long te_DCRN = System.currentTimeMillis();
        System.out.println("DCRN for ConvergingMonitoring: "+(te_DCRN-ts_DCRN)+" ms!");
        //基于同簇关系验证簇更新算法的汇聚模式在线挖掘算法
//        convergingMonitor_SCVCU();
        //基于最小外接核心点集簇更新算法的汇聚模式在线挖掘算法
//         convergingMonitor_MBCCU();
        //基于最小外接关键点集簇更新算法的汇聚模式在线挖掘算法
//        convergingMonitor_MBKCU();


        int a=1;
    }

    /**
     * 基于DCRN的汇聚模式在线挖掘算法
     * @throws IOException
     */
    private void convergingMonitor_DCRN() throws IOException {
        System.out.println("eps="+eps+", minpts="+minpts);
        //创建用于汇聚模式监测的层级图结构
        this.hierStructure = new HierarchicalStructure(this.preClusterSet,startstamp);
        //开启持续性监测
        int totalTimeNum = TimeConversion.getTimestampAmount(startstamp,endstamp,timeInterval); //获取总的时间戳数量
        for(int i=1;i<=totalTimeNum;i++){
            //获取当前时间戳
            String currTime = TimeConversion.getNextKTimeString(startstamp,i,timeInterval);
            this.curTimestampNum++;
            //读取当前的移动对象位置点集
            loadObjectData();
//            //更新移动对象位置点集在不超过eps的路网中的位置
//            updateLocations();
//            //构建路网边-移动对象索引,String:路网边id;List<String>:边上的移动对象id列表
//            edgeObjectIndexConstruct();
            //对当前移动对象位置点集聚类
            System.out.print(currTime+": Clustering..."+"  ");
            DCRNClustering();
            //进行簇包含连接
            System.out.print("CCJ...");
            ClusterContainmentJoin();
            //更新层级图结构
            System.out.print("updating...");
            updateHierarchialStructure(currTime);
            System.out.println("finish!");
            //清空容器：位置点集、当前簇集、前一时刻簇集
            clearContainers();
        }
    }
    /**
     * DCRN聚类算法
     */
    private void DCRNClustering(){
        if(curTimestampNum==0){
//            this.preClusterSet = DBSCAN_RoadNetwork.getClusters(snapshotObjectSet,rnRQ,minpts,startstamp,mc);  //用于快照数据的首片数据读入
            this.preClusterSet = DBSCAN_RoadNetwork.getClusters(snapshotObjectSet,rnRQ_leps,minpts,startstamp,mc);  //用于增量更新数据的首片数据读入
        }else{
            //采用FRV快照式算法聚类
            String timestamp = TimeConversion.getNextKTimeString(startstamp,curTimestampNum,timeInterval);
            this.currentClusterSet = DBSCAN_RoadNetwork.getClusters(snapshotObjectSet,rnRQ,minpts,timestamp,mc);
        }
    }

    /**
     * 基于SCVCU的汇聚模式在线挖掘算法
     * @throws IOException
     */
    private void convergingMonitor_SCVCU() throws IOException {
        System.out.println("eps="+eps+", minpts="+minpts+", timeInterval="+timeInterval);
        //创建用于汇聚模式监测的层级图结构
        this.hierStructure = new HierarchicalStructure(this.preClusterSet,startstamp);
        //开启持续性监测
        int totalTimeNum = TimeConversion.getTimestampAmount(startstamp,endstamp,timeInterval); //获取总的时间戳数量
        long runtime = 0;
        for(int i=1;i<=totalTimeNum;i++) {
            //获取当前时间戳
            String currTime = TimeConversion.getNextKTimeString(startstamp, i, timeInterval);
            this.curTimestampNum++;
            //读取当前时间戳下的增量位置数据
            loadIncObjectData();
            //更新位置数据
            updateObjectSet();
            //基于同簇验证的簇更新算法，获得簇及簇包含关系
            long ts_SCVCU = System.currentTimeMillis();
            SCVCUClustering(currTime);
            long te_SCVCU = System.currentTimeMillis();
            runtime = runtime+(te_SCVCU-ts_SCVCU);
            //更新层级图结构
            updateHierarchialStructure(currTime);
            //清空容器：位置点集、当前簇集、前一时刻簇集
            clearIncContainers();
        }
        System.out.println("SCVCU for ConvergingMonitoring: "+runtime+" ms!");
    }
    /**
     * 基于同簇验证的簇更新算法
     * @param currTime  当前时间戳，为了标注簇ID
     */
    private void SCVCUClustering(String currTime){
        SCVClusterUpdating scvcu = new SCVClusterUpdating(roadNetwork_leps,preClusterSet,snapshotObjectSet,rnRQ_leps,eps,minpts,mc,currTime);
        scvcu.SCVClusterUpdating(currentClusterSet,clusterContainment);
    }

    /**
     * 基于MBCCU的汇聚模式在线挖掘算法
     * @throws IOException
     */
    private void convergingMonitor_MBCCU() throws IOException {
        System.out.println("eps="+eps+", minpts="+minpts+", timeInterval="+timeInterval);
        //创建用于汇聚模式监测的层级图结构
        this.hierStructure = new HierarchicalStructure(this.preClusterSet,startstamp);
        //创建移动对象位置信息表
        ObjectsInforList objectsInforList = new ObjectsInforList(startstamp,snapshotObjectSet,preClusterSet,rnRQ_leps,minpts);
        //开启持续性监测
        int totalTimeNum = TimeConversion.getTimestampAmount(startstamp,endstamp,timeInterval); //获取总的时间戳数量
        System.out.println("totalTimeNum="+totalTimeNum);
        long runtime=0;
        for(int i=1;i<=totalTimeNum;i++) {
            //获取当前时间戳
            String currTime = TimeConversion.getNextKTimeString(startstamp, i, timeInterval);
            this.curTimestampNum++;
            //读取当前时间戳下的增量位置数据
            loadIncObjectData();
            //更新位置数据
            updateObjectSet();
            //基于最小外接核心点集的簇更新算法
            System.out.print(currTime+": Clustering..."+"  ");
            long ts_MBCCU = System.currentTimeMillis();
            MBCCUClustering(currTime,objectsInforList);
            long te_MBCCU = System.currentTimeMillis();
            runtime = runtime +(te_MBCCU-ts_MBCCU);
            //簇包含连接
            System.out.print("CCJ...");
            ClusterContainmentJoin();
            //更新层级图结构
            System.out.print("updating...");
            updateHierarchialStructure(currTime);
            System.out.println("finish!");
            //清空容器：位置点集、当前簇集、前一时刻簇集
            clearIncContainers();
        }
        System.out.println("MBCCU for ConvergingMonitoring: "+runtime+" ms!");
    }
    /**
     * 基于MBC的簇更新算法
     * @param currTime
     * @param objectsInforList
     */
    private void MBCCUClustering(String currTime,ObjectsInforList objectsInforList){
        MBCClusterUpdating mbccu = new MBCClusterUpdating(roadNetwork_leps,rnRQ_leps,preClusterSet,snapshotObjectSet,incrementalObjectSet,objectsToDelete,objectsInforList,eps,minpts,mc,currTime);
        mbccu.MBCClusterUpdating(currentClusterSet);
    }

    /**
     * 基于MBKCU的汇聚模式在线挖掘算法
     * @throws IOException
     */
    private void convergingMonitor_MBKCU() throws IOException {
        System.out.println("eps="+eps+", minpts="+minpts+", timeInterval="+timeInterval);
        //创建用于汇聚模式监测的层级图结构
        this.hierStructure = new HierarchicalStructure(this.preClusterSet,startstamp);
        //创建移动对象位置信息表
        ObjectsInforListKP objectsInforListKP = new ObjectsInforListKP(startstamp,snapshotObjectSet,preClusterSet,rnRQ_leps,minpts);
        //开启持续性监测
        int totalTimeNum = TimeConversion.getTimestampAmount(startstamp,endstamp,timeInterval); //获取总的时间戳数量
        long runtime = 0;
        for(int i=1;i<=totalTimeNum;i++) {
            //获取当前时间戳
            String currTime = TimeConversion.getNextKTimeString(startstamp, i, timeInterval);
            this.curTimestampNum++;
            //读取当前时间戳下的增量位置数据
            loadIncObjectData();
            //更新位置数据
            updateObjectSet();
            //基于最小外接核心点集的簇更新算法
            System.out.print(currTime+": Clustering..."+"  ");
            long ts_MBKCU = System.currentTimeMillis();
            MBKCUClustering(currTime,objectsInforListKP);
            long te_MBKCU = System.currentTimeMillis();
            runtime = runtime + (te_MBKCU-ts_MBKCU);
            //簇包含连接
            System.out.print("CCJ...");
            ClusterContainmentJoin();
            //更新层级图结构
            System.out.print("updating...");
            updateHierarchialStructure(currTime);
            System.out.println("finish!");
            //清空容器：位置点集、当前簇集、前一时刻簇集
            clearIncContainers();
        }
        System.out.println("MBKCU for ConvergingMonitoring: "+(runtime)+" ms!");
    }
    /**
     * 基于最小外接关键点集的簇更新算法
     * @param currTime
     * @param objectsInforListKP
     */
    private void MBKCUClustering(String currTime,ObjectsInforListKP objectsInforListKP){
        MBKClusterUpdating mbkcu = new MBKClusterUpdating(roadNetwork_leps,rnRQ_leps,preClusterSet,snapshotObjectSet,incrementalObjectSet,objectsToDelete,objectsInforListKP,eps,minpts,mc,currTime);
        mbkcu.MBCClusterUpdating(currentClusterSet);
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
            //构建范围近邻查询索引
            this.rnRQ = new RNRangeQuery(roadNetwork,eps);
            this.rnRQ_leps = new RNRangeQuery(roadNetwork_leps,eps);
            //创建汇聚模式监测层级图结构
            this.hierStructure = new HierarchicalStructure();
            //初始化数据类
            this.snapshotObjectSet = new HashMap<>();
            this.incrementalObjectSet = new HashMap<>();
            this.objectsToDelete = new HashMap<>();
            this.preClusterSet = new HashMap<>();
            this.currentClusterSet = new HashMap<>();
            this.clusterContainment = new HashMap<>();
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
        this.lepsedgeFN = properties.getProperty("lepsedgeFN");
        this.lepsvertexFN = properties.getProperty("lepsvertexFN");
        this.trajFilePath = properties.getProperty("trajFilePath");
        this.incTrajFilePath = properties.getProperty("incTrajFilePath");
        this.startstamp = properties.getProperty("startstamp");
        this.endstamp = properties.getProperty("endstamp");
        this.timeInterval = Integer.parseInt(properties.getProperty("timeInterval"));
        this.curTimestampNum = 0;
        this.eps = Integer.parseInt(properties.getProperty("eps"));
        this.minpts = Integer.parseInt(properties.getProperty("minpts"));
        this.mc=Integer.parseInt(properties.getProperty("mc"));
        this.patternScale=Integer.parseInt(properties.getProperty("patternScale"));
        this.patternDuration=Integer.parseInt(properties.getProperty("patternDuration"));
    }
    /**
     * 加载路网数据
     */
    private void loadRNData(){
        this.roadNetwork = DataRw.readRNFromFile(this.rnFilePath,this.edgeFN,this.vertexFN);
        this.roadNetwork_leps = DataRw.readRNFromFile(this.rnFilePath,this.lepsedgeFN,this.lepsvertexFN);
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
     * 读取一个时刻下的增量移动对象位置点集合
     * @throws IOException
     */
    private void loadIncObjectData() throws IOException {
        if(curTimestampNum==0){
            String timeFileName = TimeConversion.timeToOutpath(TimeConversion.getNextKTimeString(startstamp,curTimestampNum,timeInterval));
            DataRw.readObjectsFromFile(this.incTrajFilePath+timeFileName+".txt",this.snapshotObjectSet);
        }else{
            String timeFileName = TimeConversion.timeToOutpath(TimeConversion.getNextKTimeString(startstamp,curTimestampNum,timeInterval));
            DataRw.readObjectsFromFile(this.incTrajFilePath+timeFileName+".txt",this.incrementalObjectSet);
        }
    }
    /**
     * 用增量的对象集更新移动对象集合：遍历增量对象，替换对象集中的位置，把原位置放入待删集中
     */
    private void updateObjectSet(){
        for(Map.Entry<String,SnapObject> entry:this.incrementalObjectSet.entrySet()){
            SnapObject incSo = entry.getValue();
            SnapObject toDelSo = this.snapshotObjectSet.get(incSo.getId());
            if(toDelSo!=null)
                this.objectsToDelete.put(toDelSo.getId(),toDelSo);
            this.snapshotObjectSet.put(incSo.getId(),incSo);
        }
    }

    /**
     * 清空各个容器：位置点集，当前簇集，前一簇集
     */
    private void clearContainers(){
        this.snapshotObjectSet.clear();
        this.preClusterSet.clear();
        for(Map.Entry<String,Cluster> entry:currentClusterSet.entrySet()){
            preClusterSet.put(entry.getKey(),entry.getValue());
        }
        this.currentClusterSet.clear();
        this.clusterContainment.clear();
    }
    /**
     * 清空用于增量更新的容器：增量位置点集，待删点集，前一窗口簇集，当前窗口簇集
     */
    private void clearIncContainers(){
        this.incrementalObjectSet.clear();
        this.objectsToDelete.clear();
        this.preClusterSet.clear();
        for(Map.Entry<String,Cluster> entry:currentClusterSet.entrySet()){
            preClusterSet.put(entry.getKey(),entry.getValue());
        }
        this.currentClusterSet.clear();
        this.clusterContainment.clear();
    }

    /**
     * 更新层级图结构
     * @param curTime  当前时间戳
     */
    private void updateHierarchialStructure(String curTime){
        this.hierStructure.update(currentClusterSet,curTime,timeInterval,clusterContainment,patternScale,patternDuration);
    }
    /**
     * 簇包含连接
     */
    private void ClusterContainmentJoin(){
        this.clusterContainment = NLCCJ.getConverging(preClusterSet,currentClusterSet);
    }

}
