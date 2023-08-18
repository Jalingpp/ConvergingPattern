import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.csvreader.CsvWriter;
import com.sun.javafx.geom.Edge;

import javax.xml.crypto.Data;
import java.io.*;
import java.nio.charset.Charset;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

public class Main {

    static HashMap<String,Integer> clustersh_E = new HashMap<>(),clustersh_EPM = new HashMap<>(),clustersh_EPUm = new HashMap<>(),clustersh_EUpM = new HashMap<>(),
            clustersh_EUpUm = new HashMap<>(),clustersh_ERM = new HashMap<>(),clustersh_ERUm = new HashMap<>();

    public static void main(String[] args) throws IOException {

        String objectsinpath = "/root/Converging_jjp/data/trajectories/";
        String objectsTrajsInpath = "/root/Converging_jjp/data/edgeobjectIndex/";
        String roadPath = "/root/Converging_jjp/data/roadnetwork/";
        String clustersinpath = "/root/Converging_jjp/data/clusters/clusters_rdnw/";
        String edgeturninpath = "/root/Converging_jjp/data/cluster_baseline_files/edgeturn.txt";
        String convergingoutpath = "/root/Converging_jjp/data/events_pro/";
        String startTime = "2015-04-11 18:00:00.0";
        String endTime = "2015-04-11 20:00:00.0";
        int timeInterval = 10;

        int timeWindowSize = 60;   //时间窗口大小

        int minScale = 10;   //规模阈值，单位个
        int minDuration = 60;  //持续时长阈值，单位10s


        /**
         * 构建路网 RoadNetWork road
         */
        RoadNetWork road = new RoadNetWork(roadPath);

        /**
         * 读边-移动对象索引文件，生成移动对象-时间-边索引
         */
        HashMap<String,HashMap<String,String>> objectsTEIndex = generateObjectEdgeIndex(objectsTrajsInpath,startTime,endTime,timeInterval);
        /**
         * 读入转向概率矩阵 HashMap<seid,HashMap<feid,pro>>
         */
        HashMap<String, HashMap<String, Double>> edgeturnlist = readEdgeturnFromFile(edgeturninpath);



        String sTW = startTime;
        String eTW = alltotime_s(timetonum_s(startTime)+timeWindowSize*timeInterval);

        /**
         * 发现所有的汇聚树
         */
        ArrayList<List<TimeClusters>> allTWClusters = new ArrayList<>();
        ArrayList<TimeClusters> proTc = null;
        ArrayList<TimeClusters> Tc;
        HashMap<String, Event> allEvent = new HashMap<>();

        long startRuntime;
        long runtime = 0;

        while(timetonum_s(eTW)<=timetonum_s(endTime)){

            startRuntime = System.currentTimeMillis();

            /**
             * 读时间窗口[sTW,eTW]内的簇
             */
            Tc = readClustersFromFile(clustersinpath,sTW,eTW,timeInterval);
            allTWClusters.add(Tc);
            if(proTc!=null){
                Tc.add(0,proTc.get(proTc.size()-1));
            }
            proTc = Tc;
            sTW = alltotime_s(timetonum_s(eTW)+timeInterval);
            eTW = alltotime_s(timetonum_s(sTW)+timeWindowSize*timeInterval);

            runtime += System.currentTimeMillis()-startRuntime;

            /**
             * 构建边和簇的映射  HashMap<String,HashMap<String,ArrayList<String>>> edges_clusters
             */
            HashMap<String,HashMap<String,ArrayList<String>>> edges_clusters = setupEdgeClus(Tc);

            startRuntime = System.currentTimeMillis();

            /**
             * 相邻时刻进行簇包含匹配，发现汇聚事件
             */
            getAllEvents(allEvent,Tc,edges_clusters,objectsTEIndex,road,minScale,minDuration,timeInterval,edgeturnlist);

            runtime += System.currentTimeMillis()-startRuntime;

            System.out.println("当前时间窗口为：[ "+sTW+" , "+eTW+" ]");
            System.out.println("目前为止共检测到 "+allEvent.size()+" 个汇聚事件！");

        }

        /**
         * 检测发现的汇聚树，找出汇聚模式
         */
        HashMap<String,Event> allRefinedEvent = refineConverging(allEvent,minScale,minDuration,timeInterval);


        System.out.println("minScale="+minScale+"  minDuration="+minDuration+"s");
        System.out.println("汇聚事件检测完成，共发现"+allRefinedEvent.size()+"个汇聚事件！");
//        System.out.println(new Date(System.currentTimeMillis()));

        long endRuntime = System.currentTimeMillis();
//        long runtime = endRuntime-startRuntime;
        System.out.println("汇聚事件检测共用时 "+runtime+" ms!");

        /**
         * 统计各个类别的簇的个数
         */
        int clusters_ERM = 0;
        for(Map.Entry<String,Integer> chERM:clustersh_ERM.entrySet()){
            if(chERM.getValue()==1){
                clusters_ERM++;
            }
        }
        int clusters_ERUm = 0;
        for(Map.Entry<String,Integer> chERUm:clustersh_ERUm.entrySet()){
            if(chERUm.getValue()==1){
                clusters_ERUm++;
            }
        }

        System.out.println("clusters_E = "+clustersh_E.size()+"  clusters_EPM = "+clustersh_EPM.size() +"  clusters_EPUm = "+clustersh_EPUm.size() +"  clusters_EUpM = "
                +clustersh_EUpM.size()+"  clusters_EUpUm = "+clustersh_EUpUm.size()+"  clusters_ERM = "+clusters_ERM+"  clusters_ERUm = "+clusters_ERUm);

        /**
         * 输出所有的汇聚事件为json格式文件
         */
        String eventOutpath = convergingoutpath+"/json/event"+startTime.substring(11,13)+"to"+endTime.substring(11,13)+".json";
//          String eventOutpath = convergingoutpath+"\\json\\event_"+stime.substring(11,13)+"to"+etime.substring(11,13)+".json";
        JSONObject eventSetJson = writeEventsToJson(eventOutpath,allRefinedEvent,objectsinpath);

        /**
         * 输出所有的汇聚事件为CSV格式文件，两个文件：边的文件和移动对象的文件
         */
        //String eventEdgesOutpath = convergingoutpath+"/csv/eventEdges/eventEdges"+stime.substring(11,13)+"to"+etime.substring(11,13)+".csv";
        String eventObjectsOutpath = convergingoutpath+"/csv/eventObjects"+startTime.substring(11,13)+"to"+endTime.substring(11,13)+".csv";
        writeEventsToCsv(eventSetJson,eventObjectsOutpath,allRefinedEvent);


//        System.out.println("共发现 "+allEvent.size()+"个汇聚事件！");

//        System.out.println("Hello World!");

    }

    public static int rnConnectVerify(RoadNetWork road){
        HashMap<String,Integer> connectGraph = new HashMap<>();   //记录每个顶点属于哪个连通分量
        int num = 0;   //记录连通分量总数

        List<Nodes> nodesQueue = new ArrayList<>();  //用于宽度遍历

        for(Map.Entry<String,Nodes> nodesEntry:road.getNodes().entrySet()){
            if(!connectGraph.containsKey(nodesEntry.getKey())){
                nodesQueue.add(nodesEntry.getValue());
            }
            int extendNodesAmount = 0;
            while(nodesQueue.size()!=0){
                Nodes topNode = nodesQueue.get(0);   //取队列首元素
                if(!connectGraph.containsKey(topNode.getId())){
                    connectGraph.put(topNode.getId(),num);  //将队列首元素的连通图号置为当前连通图号
                    extendNodesAmount++;
                    if(extendNodesAmount%500==0){
                        System.out.println(extendNodesAmount+"个路网节点扩展完毕！");
                    }
                }
                //遍历以该节点为终点的边，获取它们的起点放入队列中
                System.out.println("topNode.geteEdges().size() = "+topNode.geteEdges().size());
                for(int i=0;i<topNode.geteEdges().size();i++){
                    Nodes _sn = road.getNodes().get(road.getEdges().get(topNode.geteEdges().get(i)).getSp());
                    System.out.println("topNode.getId() = "+topNode.getId()+"  _sn.getId() = "+_sn.getId());
                    if(!connectGraph.containsKey(_sn.getId())){
                        nodesQueue.add(_sn);
                    }
                }
                //遍历该节点为起点的边，获取它们的终点放入队列中
                System.out.println("topNode.getsEdges().size() = "+topNode.getsEdges().size());
                for(int i=0;i<topNode.getsEdges().size();i++){
                    Nodes _en = road.getNodes().get(road.getEdges().get(topNode.getsEdges().get(i)).getEp());
                    System.out.println("topNode.getId() = "+topNode.getId()+"  _en.getId() = "+_en.getId());
                    if(!connectGraph.containsKey(_en.getId())){
                        nodesQueue.add(_en);
                    }
                }
                nodesQueue.remove(0);
            }
            num++;
            System.out.println("已找到"+num+"个连通子图！第"+num+"个连通子图扩展了"+extendNodesAmount+"条边！");
        }

        System.out.println("共有"+num+"个连通图！");
        return num;
    }


    /**
     * 读移动对象轨迹文件trajA，生成移动对象-时间-边索引
     * @param inPath   trajA路径
     * @param st
     * @param et
     * @return
     * @throws IOException
     */
    public static HashMap<String,HashMap<String,String>> generateObjectEdgeIndex(String inPath,String st,String et) throws IOException {
        /* HashMap<oid,HashMap<time,edge>> */
        HashMap<String,HashMap<String,String>> objectEdgeIndex = new HashMap<>();

        long startT = Timestamp.valueOf(st).getTime();
        long endT = Timestamp.valueOf(et).getTime();

        BufferedReader reader = null;
        File file = new File(inPath);
        reader = new BufferedReader(new FileReader(file));
        String line;
        long curtime;
        int objectsCount = 0;
        System.out.println("正在生成移动对象-时间-边索引...  "+new Date(System.currentTimeMillis()));
        while((line = reader.readLine()) != null) {
            String[] _object = line.split(",");   //_object[0]为移动对象id
            curtime = Timestamp.valueOf(_object[5]).getTime();
            if(curtime>startT&&curtime<endT){   //如果移动对象路网位置时间在所给时间段内，读入
                if(!objectEdgeIndex.containsKey(_object[0])) {
                    objectsCount++;
                    if(objectsCount%200==0)
                        System.out.println("完成"+objectsCount+"个移动对象时间-边索引的读取！  "+new Date(System.currentTimeMillis()));
                    objectEdgeIndex.put(_object[0], new HashMap<String, String>());
                }
                objectEdgeIndex.get(_object[0]).put(_object[5],_object[3]);
            }
        }
        System.out.println("移动对象-时间-边索引构建完成！  "+new Date(System.currentTimeMillis()));
        return objectEdgeIndex;
    }

    /**
     * 读边-移动对象索引文件，生成移动对象-时间-边索引
     * @param inFilePath
     * @param st
     * @param et
     * @param interval
     * @return
     * @throws IOException
     */
    public static HashMap<String,HashMap<String,String>> generateObjectEdgeIndex(String inFilePath,String st,String et,int interval) throws IOException {
        /* HashMap<oid,HashMap<time,edge>> */
        HashMap<String,HashMap<String,String>> objectEdgeIndex = new HashMap<>();

        HashMap<String,String> timeEdge;
        String inPath;
        BufferedReader reader;
        File file;
        String line;
        String[] linstr;
        String curtime;
        int all=getseconds(st,et),timecount=0;
        System.out.println("正在进行移动对象-时间-边索引的构建...  "+new Date(System.currentTimeMillis()));
        for(int i=timetonum_s(st);i<=timetonum_s(st)+all;i+=interval){
            curtime = alltotime_s(i);
            inPath = inFilePath+"edgeObjectsIndex_"+numsout(curtime.substring(11,19))+".txt";
            file = new File(inPath);
            reader = new BufferedReader(new FileReader(file));
            while((line = reader.readLine()) != null){
                linstr = line.split(",");
                for(int j=2;j<linstr.length;j+=2){
                    if(!objectEdgeIndex.containsKey(linstr[j])){
                        objectEdgeIndex.put(linstr[j],new HashMap<String,String>());
                    }
                    objectEdgeIndex.get(linstr[j]).put(curtime,linstr[0]);
                }
            }
            timecount++;
//            if(timecount%200==0)
//                System.out.println("完成"+i+"个时间戳下的边-移动对象读取！  "+new Date(System.currentTimeMillis()));
            reader.close();
        }
        System.out.println("移动对象-时间-边索引构建完成！   "+new Date(System.currentTimeMillis()));
        return objectEdgeIndex;
    }

    static int getseconds(String start,String end){
        Timestamp stimestamp=Timestamp.valueOf(start);
        long t1=stimestamp.getTime();
        Timestamp etimestamp=Timestamp.valueOf(end);
        long t2=etimestamp.getTime();
        return (int)((t2-t1)/1000);
    }
    static int timetonum_s(String stime){
        int res;
        String a=stime.substring(11,13);
        int hours=Integer.parseInt(stime.substring(11,13));
        int minutes=Integer.parseInt(stime.substring(14,16));
        int second=Integer.parseInt(stime.substring(17,19));
        res=hours*60*60+minutes*60+second;
        return res;
    }
    /**
     * 将序号转化为时间字符串，需要手动设置年月日
     * @param all
     * @return
     */
    static String alltotime_s(int all){
        String res="2015-04-11 ";
        int hours=all/(60*60);
        if(hours<10){
            res=res+"0"+hours+":";
        }else{
            res=res+hours+":";
        }
        int minutes=(all%(60*60))/60;
        if(minutes<10){
            res=res+"0"+minutes+":";
        }
        else{
            res=res+minutes+":";
        }
        int seconds=(all%(60*60))%60;
        if(seconds<10){
            res=res+"0"+seconds+".0";
        }else{
            res=res+seconds+".0";
        }

        return res;
    }
    /**
     * 根据时间字符串转化为输入输出路径
     * @param str
     * @return
     */
    public static String numsout(String str){
        String res="";
        str=str.trim();
        if(str!=null&&!"".equals(str)){
            for(int i=0;i<str.length();i++){
                if(str.charAt(i)>=48&&str.charAt(i)<=57){
                    res+=str.charAt(i);
                }
            }
        }
        return res;
    }

    /**
     * 从json文件读入时间段内的簇
     * @param clustersInpath
     * @param st
     * @param et
     * @param intrval
     * @return
     * @throws IOException
     */
    static ArrayList<TimeClusters> readClustersFromFile(String clustersInpath,String st,String et,int intrval) throws IOException {
        ArrayList<TimeClusters> Tc = new ArrayList<TimeClusters>();   //所有时间戳下的簇的列表

        int all = getseconds(st, et);   //时间片数
//        System.out.println("正在读取簇...  "+new Date(System.currentTimeMillis()));
        int readCluCount = 0;
        String curtime;
        for (int i = timetonum_s(st); i <= timetonum_s(st) + all; i+=intrval) {           //将各个时间段的簇读入
            curtime = alltotime_s(i);
            String path = clustersInpath +"Clusters"+ numsout(curtime.substring(11,19))+".json";
            TimeClusters tc = new TimeClusters(path, i);
            Tc.add(tc);
            readCluCount++;
//            if(readCluCount%200==0)
//                System.out.println(curtime+"下的簇读取完毕！  "+new Date(System.currentTimeMillis()));
        }
//        System.out.println("所有时刻下的簇读取完毕！   "+new Date(System.currentTimeMillis()));
        return Tc;
    }

    /**
     * 从文件中读入转向概率矩阵 HashMap<String,HashMap<String,Double>>
     * @param filename
     * @return
     */
    public static HashMap<String,HashMap<String,Double>> readEdgeturnFromFile(String filename) {
        /* edgeturn: seid,feid1,pro1,feid2,pro2,…… */
        /* HashMap<seid,HashMap<feid,pro>> */
        HashMap<String,HashMap<String,Double>> ret = new HashMap<String, HashMap<String, Double>>();
        BufferedReader reader = null;
        File file = new File(filename);
        //读文件
        System.out.println("正在读入转向概率矩阵...   "+new Date(System.currentTimeMillis()));
        try {
            reader = new BufferedReader(new FileReader(file));
            String str = null;
            while((str = reader.readLine()) != null) {
                HashMap<String,Double> temp = new HashMap<String,Double>();
                if(str == null)
                    break;
                String[] array = str.split(",");
                for(int i = 1; i < array.length; i = i+2) {
                    temp.put(array[i],Double.parseDouble(array[i+1]));
                }
                ret.put(array[0],temp);
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("转向概率矩阵读取完毕！    "+new Date(System.currentTimeMillis()));
        return ret;
    }

    /**
     *生成所有时刻下边和簇的映射，并输出成一个txt文件，每一行的格式为time eid length size cid1 cid2……
     * @param Tc  所有时刻下的大的簇集
     * @return
     */
    public static HashMap<String,HashMap<String,ArrayList<String>>> setupEdgeClus(ArrayList<TimeClusters> Tc) {
        //所有时刻下的边-簇关系映射
        //第一层的map为：key：时间戳，value:边簇映射表
        //第二层的map为：key: 边id, value:簇id
        HashMap<String,HashMap<String,ArrayList<String>>> edg_clu = new HashMap<String,HashMap<String,ArrayList<String>>>();
//        System.out.println("正在生成边-簇映射表...   "+new Date(System.currentTimeMillis()));
        for(int i=0;i<Tc.size();i++)
        {
            String _timestamp = Tc.get(i).getTime();
            HashMap<String,ArrayList<String>> _edgeClu = new HashMap<>();

            for(Map.Entry<String, Cluster> entry: Tc.get(i).getClusters().entrySet())
            {
                for(int k=0;k<entry.getValue().getEdge().size();k++)
                {
                    if(_edgeClu.get(entry.getValue().getEdge().get(k))==null)
                    {
                        ArrayList<String> _clusters = new ArrayList<String>();
                        _clusters.add(entry.getValue().getCid());
                        _edgeClu.put(entry.getValue().getEdge().get(k),_clusters);
                    }else
                    {
                        _edgeClu.get(entry.getValue().getEdge().get(k)).add(entry.getValue().getCid());
                    }
                }
            }

            edg_clu.put(_timestamp,_edgeClu);
        }
//          String filepath = outpath+"Edges_Clusters_List/all.txt";
//          try {
//               EdgesClustersList.writeECList(filepath,edg_clu,road.getEdges());
//          } catch (IOException e) {
//               e.printStackTrace();
//          }
//        System.out.println("边-簇表生成完毕！  "+new Date(System.currentTimeMillis()));
        return edg_clu;
    }

    /**
     * 获取所有满足阈值要求的汇聚事件
     * @param Tc
     * @param edges_clusters
     * @param objectsTEIndex
     * @param road
     * @param minScale
     * @param minDuration
     * @param timeInterval
     * @param edgeturnlist
     * @return
     */
    public static void getAllEvents(HashMap<String,Event> allEvent,ArrayList<TimeClusters> Tc,HashMap<String,HashMap<String,ArrayList<String>>> edges_clusters,HashMap<String,HashMap<String,String>> objectsTEIndex,RoadNetWork road,int minScale,int minDuration,int timeInterval,HashMap<String, HashMap<String, Double>> edgeturnlist){

        TimeClusters curTc;
        Cluster curCluster;
        Cluster curFatherRoot;
        Event curEvent;
        HashMap<String,HashMap<String,List<String>>> curEventClusters;  //时间-边-簇id列表
        System.out.println("正在发现汇聚事件...  "+new Date(System.currentTimeMillis()));

        for(int i=0;i<Tc.size()-1;i++) {  //遍历每个时刻下的簇
            curTc = Tc.get(i);
            for(Map.Entry<String,Cluster> entryCluster:curTc.Clusters.entrySet()){
                curCluster = entryCluster.getValue();
//                System.out.println("当前簇为"+curCluster.Time+"-"+curCluster.Cid);
                if(curCluster.FatherId==null){
                    curFatherRoot = getFatherRoot(curCluster,i,Tc,edges_clusters,objectsTEIndex);   //获取当前簇所在汇聚树的根节点
//                    System.out.println("其所在汇聚树的根节点簇为"+curFatherRoot.Time+curFatherRoot.Cid);
                    if(curCluster.eventId!=null&&allEvent.containsKey(curCluster.eventId)){
                        allEvent.get(curCluster.eventId).endTime = curFatherRoot.getTime();
                        allEvent.get(curCluster.eventId).rootCluster = curFatherRoot;
                        curEvent = allEvent.get(curCluster.eventId);
                        curEvent.calProperties();
                    }else{
                        curEvent = new Event(allEvent.size()+"",curCluster.Time,curFatherRoot.Time,curFatherRoot);  //初始化汇聚树
                    }
                    HashMap<String, Integer> eventEdges_CA = new HashMap<>();   //当前树中所有移动对象覆盖的边集及其上簇的数量
                    curEventClusters = getEventClusters(curEvent,objectsTEIndex,edges_clusters,Tc,timeInterval,eventEdges_CA);   //获取当前汇聚树所包含的簇
//                    System.out.println("curEventClusters.size() = "+curEventClusters.size());
                    //判断当前树是否满足汇聚模式的要求，满足的话，对当前树中的簇进行匹配后加入到事件列表中,否则置当前树所有的簇的父簇为none
                    if(curEvent.endTime.equals(Tc.get(Tc.size()-1).Time)||(getseconds(curEvent.startTime,curEvent.endTime)/timeInterval>=minDuration&&curEvent.rootCluster.Objects.size()>=minScale)){
                        matchingPro(curEvent,curEventClusters,eventEdges_CA,timeInterval,Tc,road,edgeturnlist);
                        //计算事件的属性值后加入到事件列表中
                        curEvent.calProperties();
                        allEvent.put(allEvent.size()+"",curEvent);
                    }else{  //不满足，只需要对该树中每个簇的父簇置none
                        List<String> curTEC;     //用于指向当前事件中某个时间片下某条边上的簇集
                        String time;
                        int timeOrder;
                        for(Map.Entry<String,HashMap<String,List<String>>> tECEntry:curEventClusters.entrySet()){   //遍历每个时间戳
                            time = tECEntry.getKey();   //获取当前时间戳
                            timeOrder = indexOfClusters(time,Tc);   //获取当前时间戳在Tc中的索引号
                            for(Map.Entry<String,List<String>> eCEntry:tECEntry.getValue().entrySet()){   //遍历当前时间戳下的每条边
                                curTEC = eCEntry.getValue();    //获取当前事件中当时间片下当前边上的簇集
                                for(int j=0;j<curTEC.size();j++){   //遍历簇集，置其父簇为none
                                    if(Tc.get(timeOrder).getClusters().get(curTEC.get(j)).FatherId==null){

                                    }else if(Tc.get(timeOrder).getClusters().get(curTEC.get(j)).FatherId.equals("none")){
                                        Main.clustersh_ERUm.put(Tc.get(timeOrder).getClusters().get(curTEC.get(j)).Time+curTEC.get(j),0);
                                    }else{
                                        Main.clustersh_ERM.put(Tc.get(timeOrder).getClusters().get(curTEC.get(j)).Time+curTEC.get(j),0);
                                    }
                                    Tc.get(timeOrder).getClusters().get(curTEC.get(j)).FatherId = "none";
                                }
                            }
                        }
                    }
                    curEventClusters.clear();
                    eventEdges_CA.clear();
                }
            }
//            System.out.println("目前发现了"+allEvent.size()+"个汇聚事件！  "+new Date(System.currentTimeMillis()));
//            for(Map.Entry<String,Event> eventEntry:allEvent.entrySet()){
//                System.out.println("event"+eventEntry.getKey()+"的持续时间为："+eventEntry.getValue().getDuration());
//            }
//            System.out.println();

//            if(i%200==0){
//                System.out.println("完成第"+i+"个时刻下的簇所在汇聚树的查询！  "+new Date(System.currentTimeMillis()));
//            }

        }
//        System.out.println("minScale="+minScale+"  minDuration="+minDuration+"s");
//        System.out.println("汇聚事件检测完成，共发现"+allEvent.size()+"个汇聚事件！");
//        System.out.println(new Date(System.currentTimeMillis()));
    }

    /**
     * 获取当前簇所在汇聚树的根节点簇
     * @param c
     * @param cOrder
     * @param Tc
     * @param edges_clusters
     * @return
     */
    public static Cluster getFatherRoot(Cluster c,int cOrder,ArrayList<TimeClusters> Tc,HashMap<String,HashMap<String,ArrayList<String>>> edges_clusters,HashMap<String,HashMap<String,String>> objectsTEIndex){
        Cluster curC = c;
        int curOrder = cOrder;

        List<String> curCandidates;
        String nextTimestamp;
        String curFather;

        boolean isRoot = false;
        while(!isRoot){
            if(curOrder!=Tc.size()-1) {
                nextTimestamp = Tc.get(curOrder+1).getTime();
                curCandidates = getCandidates(nextTimestamp,curC,edges_clusters,objectsTEIndex);
                curFather = getFatherCluster(curC,curCandidates,Tc.get(curOrder+1));
                if(curFather!=null) {
                    curC.FatherId = (curOrder + 1) + "-" + curFather;  //设置当前簇的父簇
                    Main.clustersh_ERM.put(curC.Time + curC.Cid, 1);
                    Tc.get(curOrder + 1).Clusters.get(curFather).childClusters.add(curOrder + "-" + curFather);  //将当前簇加入到父簇的子簇列表中
                    curC = Tc.get(curOrder + 1).Clusters.get(curFather);
                    curOrder = curOrder + 1;
                    curCandidates.clear();
                }else{
                    curC.FatherId = "none";
                    Main.clustersh_ERUm.put(curC.Time+curC.Cid,1);
                    isRoot=true;
                }
            }else{
                isRoot = true;
            }
        }
        return curC;
    }

    /**
     * 求某个簇的父簇
     * @param c   该簇
     * @param candiClu   该簇的待匹配候选簇
     * @param nextTC     下一时刻的簇集
     * @return
     */
    public static String getFatherCluster(Cluster c,List<String> candiClu,TimeClusters nextTC){
        String father = null;
        for(int i=0;i<candiClu.size();i++)
        {
            if(nextTC.getClusters().get(candiClu.get(i)).getScale()<c.getScale())
                continue;
            else
            if(isFarther(c,nextTC.getClusters().get(candiClu.get(i))))
            {
                father = candiClu.get(i);
                break;
            }
        }
        return father;
    }

    /**
     * 判断簇2是否是簇1的父节点，即判断簇2是否包含簇1
     * @param c1
     * @param c2
     * @return
     */
    public static boolean isFarther(Cluster c1,Cluster c2) {
        int count = 0;
//        System.out.println(c1.Cid +".getScale() = "+c1.getScale());
//        System.out.println(c2.Cid +".getScale() = "+c2.getScale());
        for(int i=0;i<c1.getScale();i++) {
            boolean ischange = false;
            for (int j = 0; j < c2.getScale(); j++) {
                if (c1.getObjects().get(i).getOId().equals(c2.getObjects().get(j).getOId())) {
                    count++;
                    ischange = true;
                    break;
                }
            }
            if(!ischange)
                return false;
        }
        if(count==c1.getScale())
            return true;
        else
            return false;
    }

    /**
     * 获取汇聚树e中包含的簇HashMapHashMap<String,HashMap<String,List<String>>> edgeList:时间-边-簇id列表
     * @param e
     * @param objectsTEIndex   对象id-时间-边id
     * @param edges_clusters
     * @param Tc
     * @param timeInterval
     * @return
     */
    public static HashMap<String,HashMap<String,List<String>>> getEventClusters(Event e,HashMap<String,HashMap<String,String>> objectsTEIndex,HashMap<String,HashMap<String,ArrayList<String>>> edges_clusters,ArrayList<TimeClusters> Tc,int timeInterval,HashMap<String, Integer> eventEdges_CA){
        HashMap<String,HashMap<String,List<String>>> eventClusters = new HashMap<>();

        Objects curObject;
        HashMap<String,String> curObjectEdges;
        String curTime;
        HashMap<String,HashMap<String,String>> eventEdges = new HashMap<>();  //时间-边-边
        String eventST = timetonum_s(e.startTime)<timetonum_s(Tc.get(0).Time)?Tc.get(0).Time:e.startTime;  //设定当前事件在当前窗口内的开始时间

        int duration = getseconds(eventST,e.endTime);        //获取事件包含的时间秒数
        /**
         * 获取根节点中每个移动对象在事件时间范围内所覆盖的所有边
         */
        for(int i=0;i<e.rootCluster.Objects.size();i++){          //遍历根节点的每个对象
            curObject = e.rootCluster.Objects.get(i);   //获取一个移动对象
            curObjectEdges = objectsTEIndex.get(curObject.getOId());   //获取移动对象所在的边集
            for(int j=timetonum_s(eventST);j<=timetonum_s(eventST)+duration;j+=timeInterval){
                curTime = alltotime_s(j);
                if(!eventEdges.containsKey(curTime)){
                    eventEdges.put(curTime,new HashMap<>());
                }
                if(!eventEdges.get(curTime).containsKey(curObjectEdges.get(curTime))) {
                    eventEdges.get(curTime).put(curObjectEdges.get(curTime), curObjectEdges.get(curTime));
                }
            }
        }

        /**
         * 获取事件包含的簇
         */
        for(Map.Entry<String,HashMap<String,String>> timeEdges:eventEdges.entrySet()){   //遍历每个时间下的边集
            HashMap<String,String> edgeList = timeEdges.getValue();      //获取一个时间戳下的边集
            String timeStamp =timeEdges.getKey();         //获取当前时间戳
            for(Map.Entry<String,String> edge:edgeList.entrySet()){      //遍历当前时间戳下的边
                List<String> edgeClusters = edges_clusters.get(timeStamp).get(edge.getValue());   //获取当前时间戳下当前边上所有簇
                //生成路径计算用的汇聚树的边集
                if(edgeClusters!=null){
                    if(!eventEdges_CA.containsKey(edge.getValue())){
                        eventEdges_CA.put(edge.getValue(),edgeClusters.size());
                    }else{
                        int curCA = eventEdges_CA.get(edge.getValue());
                        eventEdges_CA.put(edge.getValue(),curCA+edgeClusters.size());
                    }
                    for(int i=0;i<edgeClusters.size();i++){
                        Cluster edgeCluster = Tc.get(indexOfClusters(timeStamp,Tc)).Clusters.get(edgeClusters.get(i));  //获取当前边-簇表上某个簇的实体
                        if(isFarther(edgeCluster,e.rootCluster)){     //判断边上的簇与根节点簇的包含关系
                            if(!eventClusters.containsKey(timeStamp)){
                                eventClusters.put(timeStamp,new HashMap<>());
                            }
                            if(!eventClusters.get(timeStamp).containsKey(edge.getKey())) {
                                eventClusters.get(timeStamp).put(edge.getKey(),new ArrayList<>());
                            }
                            edgeCluster.eventId = e.id;
                            eventClusters.get(timeStamp).get(edge.getKey()).add(edgeCluster.Cid);  //将簇加入到该时间该边下的簇表中
                        }
                    }
                }else{
                    if(!eventEdges_CA.containsKey(edge.getValue())){
                        eventEdges_CA.put(edge.getValue(),0);
                    }
                }
            }
        }

        return eventClusters;
    }

    /**
     * 根据时间字符串获取在簇列表ArrayList<TimeClusters> Tc中的时间索引
     * @param time
     * @param Tc
     * @return
     */
    public static int indexOfClusters(String time,ArrayList<TimeClusters> Tc){
        for(int i=0;i<Tc.size();i++){
            if(Tc.get(i).Time.equals(time)){
                return i;
            }
        }
        return -1;
    }

    /**
     * 对一个事件中找到的所有簇进行匹配
     * @param event
     * @param eventClusters   时间-边-簇id列表
     * @param eventEdges_CA
     * @param timeInterval
     * @param Tc
     * @param road
     * @param edgeturnlist
     */
    public static void matchingPro(Event event,HashMap<String,HashMap<String,List<String>>> eventClusters,HashMap<String, Integer> eventEdges_CA,int timeInterval,ArrayList<TimeClusters> Tc,RoadNetWork road,HashMap<String, HashMap<String, Double>> edgeturnlist){
        HashMap<String,List<String>> curEventTimeClusters;
        List<String> curEventEdgeClusters;
        int curOrderUC;
        Cluster curCluster;
        String eventST = timetonum_s(event.startTime)<timetonum_s(Tc.get(0).Time)?Tc.get(0).Time:event.startTime;  //设定当前事件在当前窗口内的开始时间
        int duration = getseconds(eventST,event.endTime);
        for(int i=timetonum_s(eventST);i<timetonum_s(eventST)+duration-timeInterval;i+=timeInterval){
            String timeUC = alltotime_s(i);   //获取当前时间
            String time = timeUC;
            String nextTimeUC = alltotime_s(i+timeInterval);  //获取下一时间戳的时间
            String nextTime = nextTimeUC;
            curEventTimeClusters = eventClusters.get(time);   //获取事件中当前时刻下所有的边-簇表
            curOrderUC = indexOfClusters(time,Tc);    //获取当前时刻在Tc中的索引号
            int curOrder = curOrderUC;
            for(Map.Entry<String,List<String>> curEtc:curEventTimeClusters.entrySet()){     //遍历每条边
                time = timeUC;   //获取当前时间
                curOrder = curOrderUC;    //获取当前时刻在Tc中的索引号
                nextTime = nextTimeUC;
                curEventEdgeClusters = curEtc.getValue();           //获得当前事件当前时间当前边上的簇集
                for(int j=0;j<curEventEdgeClusters.size();j++){   //遍历当前事件当前时间当前边上的簇集
                    time = timeUC;   //获取当前时间
                    curOrder = curOrderUC;    //获取当前时刻在Tc中的索引号
                    nextTime = nextTimeUC;
                    curCluster = Tc.get(curOrder).Clusters.get(curEventEdgeClusters.get(j));    //获取其中的一个簇
                    Main.clustersh_E.put(curCluster.Time+curCluster.Cid,1);
                    if(curCluster.FatherId==null){
                        //获取当前簇到根节点簇的所有路径
                        ArrayList<Path> curPaths = curCluster.getPathOrder(road,eventEdges_CA,event.getRootCluster(),edgeturnlist);
                        //沿路径进行匹配
                        int findPath = -1;
                        int findEdge = -1;
                        List<String> clustersIncurEdge;
                        for(int k=0;k<curPaths.size();k++){   //遍历每条路径
                            if(findPath<0){
                                for(int m=0;m<curPaths.get(k).path.size();m++){   //遍历一条路径上的边
                                    //获取下一时间片当前路径当前边上的位于当前事件中的簇集
                                    clustersIncurEdge = eventClusters.get(nextTime).get(curPaths.get(k).path.get(m));
                                    //遍历该簇集，获取父节点，找到了置findPath为true，否则继续找下一条边
                                    if(clustersIncurEdge!=null){
                                        for(int n=0;n<clustersIncurEdge.size();n++){
                                            Cluster nextCluster = Tc.get(curOrder+1).Clusters.get(clustersIncurEdge.get(n));
                                            if(isFarther(curCluster,nextCluster)){
                                                curCluster.FatherId = (curOrder+1)+"-"+nextCluster.Cid;
                                                nextCluster.childClusters.add(curOrder+"-"+curCluster.Cid);
                                                Main.clustersh_EPM.put(curCluster.Time+curCluster.Cid,1);
                                                findPath = k;   //选定第k条路径
                                                findEdge = m;   //选定第k条边上第m条路径
                                                break;
                                            }
                                        }
                                    }
                                    if(findPath>=0){
                                        break;
                                    }
                                }
                            }else{
                                break;
                            }
                        }
                        //如果在某条路径上匹配到了，继续沿着该路径搜索
                        Cluster curFather;
                        boolean findFather = false;
                        while(curCluster.FatherId!=null&&!curCluster.FatherId.equals("none")){
                            curFather = Tc.get(curOrder+1).Clusters.get(curCluster.getFatherId().split("-")[1]);
                            curCluster = curFather;   //获取当前簇的父簇作为当前簇
                            if(curCluster.FatherId!=null&&curCluster.FatherId.equals("none")){   //表示当前节点簇为根节点簇
                                break;
                            }
                            time = nextTime;    //获取新的当前时间
                            curOrder++;
                            int nextTime_s = timetonum_s(time)+timeInterval;
                            if(nextTime_s>timetonum_s(event.endTime)){
                                break;
                            }  //如果下一时刻簇超出了事件范围，则退出搜索
                            nextTime = alltotime_s(timetonum_s(time)+timeInterval);   //获取新的下一时间
                            for(int m=findEdge;m<curPaths.get(findPath).path.size();m++){   //从上次匹配成功的路径上的边开始
                                //获取下一时间片当前路径当前边上的位于当前事件中的簇集
                                clustersIncurEdge = eventClusters.get(nextTime).get(curPaths.get(findPath).path.get(m));
                                //遍历该簇集，获取父节点，找到了修改findEdge，否则继续找下一条边
                                if(clustersIncurEdge!=null){
                                    for(int n=0;n<clustersIncurEdge.size();n++){
                                        Cluster nextCluster = Tc.get(curOrder+1).Clusters.get(clustersIncurEdge.get(n));
                                        if(isFarther(curCluster,nextCluster)){
                                            curCluster.FatherId = (curOrder+1)+"-"+nextCluster.Cid;
                                            nextCluster.childClusters.add(curOrder+"-"+curCluster.Cid);
                                            findEdge = m;   //选定第k条边上第m条路径
                                            findFather = true;
                                            break;
                                        }
                                    }
                                }
                                if(findFather){
                                    break;
                                }
                            }
                            findFather = false;
                        }
                        //如果在所有路径上没有匹配到,说明当前节点不存在父节点，置为none
                        if(curPaths.size()!=0&&findPath==-1){
                            Main.clustersh_EPUm.put(curCluster.Time+curCluster.Cid,1);
                            curCluster.FatherId = "none";
                        }else if(curPaths.size()==0){
                            HashMap<String,List<String>> nextTEClusters = eventClusters.get(nextTime);//获取下一时刻的边-簇集
                            boolean findFather2 = false;
                            for(Map.Entry<String,List<String>> nTECSEntry:nextTEClusters.entrySet()){
                                List<String> NTECS = nTECSEntry.getValue();
                                for(int k=0;k<NTECS.size();k++){
                                    if(isFarther(curCluster,Tc.get(curOrder+1).getClusters().get(NTECS.get(k)))){
                                        curCluster.FatherId = (curOrder+1)+"-" + NTECS.get(k);
                                        Tc.get(curOrder+1).getClusters().get(NTECS.get(k)).childClusters.add(curOrder+"-"+curCluster.Cid);
                                        Main.clustersh_EUpM.put(curCluster.Time+curCluster.Cid,1);
                                        findFather2 = true;
                                        break;
                                    }
                                }
                                if (findFather2){
                                    break;
                                }
                            }
                            if(!findFather2){
                                curCluster.FatherId = "none";
                                Main.clustersh_EUpUm.put(curCluster.Time+curCluster.Cid,1);
                            }
                        }
                    }
//                    else if(!curCluster.FatherId.equals("none")){
////                        Main.clusters_EPM++;
//                    }
                }
            }
        }
    }

    /**
     * 求某个簇的待匹配候选集
     * @param nextTimestamp  下一时间戳字符串
     * @param edgelist     当前簇所在边集
     * @param edges_clusters  边-簇映射表
     * @param road     路网数据，为了获取邻边信息
     * @return
     */
    public static List<String> getMatchCandidates(String nextTimestamp,List<String> edgelist,HashMap<String,HashMap<String,ArrayList<String>>> edges_clusters,RoadNetWork road, List<String> _adjaEdges) {
        List<String> cadidateClusters = new ArrayList<>();

//          System.out.println("当前簇所在边数为:"+edgelist.size());
        for(int i=0;i<edgelist.size();i++)
        {
            //查边-簇映射表，把当前边上的簇加入候选簇集中
            List<String> _cadiClu = edges_clusters.get(nextTimestamp).get(edgelist.get(i));
            if(_cadiClu!=null){
//                    System.out.println("边"+edgelist.get(i)+"上的簇数为:"+_cadiClu.size());
                for(int j=0;j<_cadiClu.size();j++)
                    cadidateClusters.add(_cadiClu.get(j));
            }

            //查路网，获取当前边的邻边
            String _epid = road.getEdges().get(edgelist.get(i)).getEp();
            for(int j=0;j<road.getNodes().get(_epid).getsEdges().size();j++)
                _adjaEdges.add(road.getNodes().get(_epid).getsEdges().get(j));

//               //查路网，获取当前边邻边的邻边
//               int firstAEAmount = _adjaEdges.size();
//               for(int j=0;j<firstAEAmount;j++){
//                    _epid = road.getEdges().get(_adjaEdges.get(j)).getEp();
//                    for(int k=0;k<road.getNodes().get(_epid).getsEdges().size();k++)
//                         _adjaEdges.add(road.getNodes().get(_epid).getsEdges().get(k));
//               }
//               System.out.println("当前边的邻边数为:"+_adjaEdges.size());

            //查边-簇映射表，把邻边上的簇加入到候选簇集中
            for(int j=0;j<_adjaEdges.size();j++) {
                if(edges_clusters.get(nextTimestamp).get(_adjaEdges.get(j))!=null) {
//                         System.out.println("当前邻边上的簇数:" + edges_clusters.get(nextTimestamp).get(_adjaEdges.get(j)).size());
                    for (int k = 0; k < edges_clusters.get(nextTimestamp).get(_adjaEdges.get(j)).size(); k++) {
                        cadidateClusters.add(edges_clusters.get(nextTimestamp).get(_adjaEdges.get(j)).get(k));
                    }
                }
            }
        }
        return cadidateClusters;
    }

    /**
     * 求某个簇的待匹配候选集
     * @param nextTimestamp
     * @param c
     * @param edges_clusters
     * @param objectsTEIndex  移动对象-时间-边
     * @return
     */
    public static List<String> getCandidates(String nextTimestamp,Cluster c,HashMap<String,HashMap<String,ArrayList<String>>> edges_clusters,HashMap<String,HashMap<String,String>> objectsTEIndex){
        List<String> cadidates = new ArrayList<>();
        String nextEdge;
        ArrayList<String> edgeClusters;
        for(int i=0;i<c.Objects.size();i++){   //遍历移动对象点，获取每个移动对象下一时刻所在的边放入候选边集中
            nextEdge = objectsTEIndex.get(c.Objects.get(i).OId).get(nextTimestamp);
            edgeClusters = edges_clusters.get(nextTimestamp).get(nextEdge);
            if(edgeClusters!=null) {
                for (int j = 0; j < edgeClusters.size(); j++) {
                    cadidates.add(edgeClusters.get(j));
                }
            }
        }
        return cadidates;
    }

    /**
     * 将汇聚事件表输出成json格式文件
     * @param outpath   输出路径
     * @param allEvent  汇聚事件表
     * @throws IOException
     */
    public static JSONObject writeEventsToJson(String outpath,HashMap<String, Event> allEvent,String objectsinpath) throws IOException {
        BufferedWriter writer = null;
        File file = new File(outpath);
        if(!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        writer = new BufferedWriter(new FileWriter(file));
        System.out.println("正在输出事件到json文件...  "+new Date(System.currentTimeMillis()));
        JSONObject eventSet=new JSONObject();
        if(!allEvent.isEmpty()){
            eventSet.put("convergingEventAmount",allEvent.size());  //写汇聚事件总个数
            JSONArray eventlist = new JSONArray();    //一个汇聚事件列表
            JSONObject _event;
            int convergingAmount = 0;
            for(Map.Entry<String,Event> eventEntry:allEvent.entrySet()){  //写一个汇聚事件
                _event = new JSONObject();
                _event.put("eventID",eventEntry.getValue().getId());  //写汇聚事件的ID
                _event.put("scale",eventEntry.getValue().getScale());   //写汇聚事件的规模
                _event.put("centerLng",eventEntry.getValue().getCenterlng());  //写汇聚事件的中心经度
                _event.put("centerLat",eventEntry.getValue().getCenterlat());  //写汇聚事件的中心纬度
                _event.put("startTime",eventEntry.getValue().getStartTime());  //写汇聚事件的起始时间
                _event.put("endTime",eventEntry.getValue().getEndTime());   //写汇聚事件的终止时间
                _event.put("duration",eventEntry.getValue().getDuration());   //写汇聚事件的持续时间
                //读取当前事件对应时间段的轨迹段，放在HashMap
                HashMap<String,HashMap<String, Objects>> eventTrajSegs = getEventTrajSegs(objectsinpath,eventEntry.getValue());
                //写汇聚事件所包含的移动对象轨迹段
                JSONObject objectsTrajSegs = new JSONObject();     //所有移动对象的轨迹段
                for(int i=0;i<eventEntry.getValue().getRootCluster().getScale();i++){
                    JSONArray _objTraj = new JSONArray();   //一条移动对象的轨迹段
                    int _trajLength = getseconds(eventEntry.getValue().getStartTime(),eventEntry.getValue().getEndTime());
                    String currentTime;
                    String currentOID = eventEntry.getValue().getRootCluster().getObjects().get(i).getOId();
                    for(int j=0;j<_trajLength;j++){
                        JSONObject _otp = new JSONObject();
                        currentTime = getCurrentTime(eventEntry.getValue().getStartTime(),j);
                        _otp.put("timestamp",currentTime);               //写一个移动对象轨迹点的时间戳
                        Objects _o = eventTrajSegs.get(currentTime).get(currentOID);
                        _otp.put("lng",_o.getLng());               //写一个移动对象轨迹点的经度
                        _otp.put("lat",_o.getLat());           //写一个移动对象轨迹点的纬度
                        _objTraj.add(_otp);
                    }
                    objectsTrajSegs.put(currentOID,_objTraj);   //写一条移动对象轨迹段
                }
                _event.put("objectsTrajSeg",objectsTrajSegs);   //写所有移动对象的轨迹段

                eventlist.add(_event);    //写一个汇聚事件
                System.out.println("完成第"+convergingAmount+"个汇聚事件的输出！  "+new Date(System.currentTimeMillis()));
                convergingAmount++;
                eventTrajSegs.clear();
            }
            eventSet.put("convergingEvents",eventlist);
            writer.write(eventSet.toString());
            writer.flush();
            writer.flush();
            writer.flush();
            writer.flush();
            writer.close();
            System.out.println("汇聚事件到json文件输出完成！  "+new Date(System.currentTimeMillis()));
        }
        return eventSet;
    }

    /**
     * 将汇聚事件表输出成CSV格式文件
     * @param eventSetJson    汇聚事件集合的Json对象
     * @param eventObjectsOutpath  汇聚事件移动对象的输出路径
     * @param allEvent    汇聚事件哈希表
     * @throws IOException
     */
    public static void writeEventsToCsv(JSONObject eventSetJson,String eventObjectsOutpath,HashMap<String, Event> allEvent) throws IOException {
        System.out.println("正在输出汇聚事件到CSV文件...  "+new Date(System.currentTimeMillis()));
        //输出汇聚事件的移动对象点
        System.out.println("正在输出汇聚事件的移动对象到CSV文件...  "+new Date(System.currentTimeMillis()));
        String csvpath = eventObjectsOutpath;
        CsvWriter objectsCsvWriter = new CsvWriter(csvpath, ',', Charset.forName("UTF-8"));
        String[] objectsHeaders = {"eventID","timeStamp","objectID","objectLng","objectLat"};
        objectsCsvWriter.writeRecord(objectsHeaders);   //写对象文件的表头
        objectsCsvWriter.flush();
        String[] writeline = new String[5];   //定义一个数组，用来存一行的数据
        for(int i=0;i<eventSetJson.getJSONArray("convergingEvents").size();i++){
            JSONObject _event = eventSetJson.getJSONArray("convergingEvents").getJSONObject(i);  //获取一个汇聚事件
            writeline[0] = _event.getString("eventID");    //写移动对象所属的汇聚事件ID
            JSONObject _trajs = _event.getJSONObject("objectsTrajSeg");   //获取汇聚事件的轨迹段表
            for(int j=0;j<allEvent.get(writeline[0]).getRootCluster().getScale();j++){
                writeline[2] = allEvent.get(writeline[0]).getRootCluster().getObjects().get(j).getOId();   //写移动对象ID
                for(int k=0;k<_trajs.getJSONArray(writeline[2]).size();k++){
                    writeline[1] = _trajs.getJSONArray(writeline[2]).getJSONObject(k).getString("timestamp");    //写移动对象点的时间戳
                    writeline[3] = Double.toString(_trajs.getJSONArray(writeline[2]).getJSONObject(k).getDouble("lng"));  //写移动对象点的经度
                    writeline[4] = Double.toString(_trajs.getJSONArray(writeline[2]).getJSONObject(k).getDouble("lat"));  //写移动对象点的纬度
                    objectsCsvWriter.writeRecord(writeline);    //写一行移动对象的数据
                    objectsCsvWriter.flush();
                }
            }
            System.out.println("完成第"+i+"个汇聚事件的输出！  "+new Date(System.currentTimeMillis()));
        }
        objectsCsvWriter.close();
        System.out.println("汇聚事件的边到CSV文件输出完毕！  "+new Date(System.currentTimeMillis()));
        System.out.println("汇聚事件到CSV文件输出完毕！  "+new Date(System.currentTimeMillis()));
    }

    /**
     * 从轨迹的文本文件中读取移动对象点的ID和经纬度，生成每个时刻下的对象列表HashMap<String,HashMap<String,Objects>>，第一个String为时间戳，第二个为对象ID
     * @param objectsinpath
     * @param event
     * @return
     * @throws IOException
     */
    public static HashMap<String,HashMap<String,Objects>> getEventTrajSegs(String objectsinpath,Event event) throws IOException {
        HashMap<String,HashMap<String,Objects>> eventTrajSegs = new HashMap<>();
        int timeAmounts = getseconds(event.getStartTime(),event.getEndTime());
        for(int i=0;i<timeAmounts;i++){
            String currentTime = getCurrentTime(event.getStartTime(),i);
            String _objInpath = objectsinpath+timetoNumString_s(currentTime)+".txt";
            HashMap<String,Objects> objectsList = readObjects(_objInpath);
            eventTrajSegs.put(currentTime,objectsList);
        }
        return  eventTrajSegs;
    }

    /**
     * 根据起始时间和时间间隔（秒数）计算当前时间
     * @param start
     * @param s
     * @return
     */
    static String getCurrentTime(String start, int s){
        Timestamp currenttime = Timestamp.valueOf(start);
        long t1 = currenttime.getTime();
        t1 = t1+s*1000;
        currenttime.setTime(t1);
        return currenttime.toString();
    }

    /**
     * 将时间转化为时间字符串，即2015-04-11 12:34:56.0转化为123456
     * @param stime
     * @return
     */
    static String timetoNumString_s(String stime){
        String res;
        String hours = stime.substring(11, 13);
        String minutes = stime.substring(14, 16);
        String second = stime.substring(17, 19);
        res = hours + minutes + second;
        return res;
    }

    /**
     * 从轨迹的文本文件读取点的ID和经纬度，生成一个时间戳下的移动对象列表HashMap<String,Objects>，String为对象ID
     * @param objectsInpath
     * @return
     * @throws IOException
     */
    public static HashMap<String,Objects> readObjects(String objectsInpath) throws IOException {
        HashMap<String,Objects> objectsList = new HashMap<>();
        BufferedReader br=new BufferedReader(new FileReader(objectsInpath));
        String readline = br.readLine();
        while(readline!=null){
            Objects _object = new Objects();
            String[] _rl = readline.split(",");
            _object.setOId(_rl[0]);
            _object.setLng(Double.parseDouble(_rl[1]));
            _object.setLat(Double.parseDouble(_rl[2]));
            objectsList.put(_rl[0],_object);
            readline = br.readLine();
        }
        return objectsList;
    }

    /**
     * 对汇聚树进行参数验证，返回满足阈值要求的汇聚模式
     * @param allEvent
     * @param minScale
     * @param minDuration
     * @param timeInterval
     * @return
     */
    public static HashMap<String, Event> refineConverging(HashMap<String, Event> allEvent,int minScale,int minDuration,int timeInterval){
        HashMap<String, Event> allRefinedEvent = new HashMap<>();
        for(Map.Entry<String, Event> eventEntry:allEvent.entrySet()){
            if(eventEntry.getValue().getScale()>=minScale&&getseconds(eventEntry.getValue().startTime,eventEntry.getValue().endTime)/timeInterval>=minDuration){
                allRefinedEvent.put(eventEntry.getKey(),eventEntry.getValue());
            }
        }
        return allRefinedEvent;
    }
}
