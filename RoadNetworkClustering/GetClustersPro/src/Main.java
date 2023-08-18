import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {

        MBR mapScale=new MBR(120, 30, 122.3, 32.3);    //初始化地图经纬度,参数依次表示最小经度，最小维度，最大经度，最大维度
        double eps = 250;                 //领域半径阈值
        int minPts = 6;                 //密度阈值
        int minshare = 5;       //SNN阈值
        int mincontain = 2;    //簇内核心点阈值
        String objectsInPath = "/root/jjp/Converging_jjp/data/trajectories/";    //移动对象输入路径
        String roadNetInPath = "/root/jjp/Converging_jjp/data/roadnetwork/";       //路网输入路径
        String eoIndexInPath = "/root/jjp/Converging_jjp/data/edgeobjectIndex/";  //边点索引输入路径
        String clustersOutPath = "/root/jjp/Converging_jjp/data/clusters_pro/runtime_snn/";
        String startTime = "2015-04-11 18:00:00.0";  //聚类的开始时间
        String endTime = "2015-04-11 20:00:00.0";    //聚类的结束时间

        HashMap<String,Edge> edgeList = new HashMap<>(); //路网边集
        HashMap<String,Node> nodeList = new HashMap<>();  //路网结点集合
        HashMap<String,NeighborRange> nodesNR = new HashMap<>();  //路网结点的邻域范围
        /**
         * 读入路网数据
         */
        readRoadNetFromFile(roadNetInPath,edgeList,nodeList);
        /**
         * 计算所有路网节点的邻域范围
         */
        calNodesNeighborRange(nodesNR,eps,nodeList,edgeList);

        HashMap<String,List<Onepair>> pairmap = new HashMap<>();   //定义全局的pq点对表,String为p点的id
        HashMap<Edge2Edge,List<String>> e2e_path = new HashMap<>();   //定义全局的边到边的路径表

        DBRNCANPro dbrncanPro;
        String time,inPath_o,inPath_eoI,outPath_c,csvpath,mbrcsvpath,jsonpath;
        int clustersAmount = 0;    //记录时间段内的总簇数
        int count = 0;       //记录时间段内聚类的总时间戳数
        long runtime = 0;   //记录聚类的时长，ms
        int all=getseconds(startTime,endTime);
        long startRunTime = System.currentTimeMillis();
        for(int i=timetonum_s(startTime);i<=timetonum_s(startTime)+all;i+=10){
            time=alltotime_s(i);
            inPath_o = objectsInPath+numsout(time.substring(11,19))+".txt";
            inPath_eoI = eoIndexInPath+"edgeObjectsIndex_"+numsout(time.substring(11,19))+".txt";
            dbrncanPro = new DBRNCANPro(mapScale,eps,minPts,minshare,mincontain,
                    inPath_o,edgeList,nodeList,nodesNR,inPath_eoI,pairmap,e2e_path);
            startRunTime = System.currentTimeMillis();
            List<Clusters> finalClusters = dbrncanPro.getClusters();
            runtime += System.currentTimeMillis()- startRunTime;
            clustersAmount += finalClusters.size();
            /**
             * 输出结果簇到JSON文件和CSV文件
             */ {
                //三个文件的输出路径设置
                outPath_c=numsout(time.substring(11,19));
                //路径可改成samepath
                csvpath=clustersOutPath+"csv/Clusters_"+outPath_c+".csv";
                mbrcsvpath=clustersOutPath+"csv/ClustersMBR_"+outPath_c+".csv";
                jsonpath=clustersOutPath+"json/Clusters"+outPath_c+".json";
                //将簇中内容分别输出到三个文件
                CSVRW csvrw = new CSVRW();
                ClustersJSONRW cjsonrw = new ClustersJSONRW();
                csvrw.writeCSV(csvpath,finalClusters);
                csvrw.writeMBRtoCSV(mbrcsvpath, finalClusters);
                cjsonrw.writeJson(jsonpath, finalClusters);
            }
            count++;
            if(count%200==0)
                System.out.println("完成"+count+"个时间戳下的聚类！   "+ new Date(System.currentTimeMillis()));

            dbrncanPro.clear();   //析构掉当前时刻的聚类类
        }
//        long endRunTime = System.currentTimeMillis();
//        System.out.println();
        System.out.println("聚类完成！时间段 "+startTime+" 至 "+endTime+" 内的聚类运行时间为 "+runtime+" ms！每个时间戳下平均 "+(clustersAmount/count)+"个簇！  "+ new Date(System.currentTimeMillis()));

//        System.out.println("Hello World!");
    }


    /**
     * 读路网数据
     * @param rnInpath  路网数据文件夹路径
     * @param edgeList  路网边表
     * @param nodeList  路网顶点表
     */
    public static void readRoadNetFromFile(String rnInpath,HashMap<String,Edge> edgeList,HashMap<String,Node> nodeList){
        System.out.println("正在读入路网数据...   "+ new Date(System.currentTimeMillis()));
        readEdgeFromFile(rnInpath+"edges.json",edgeList);
        readNodeFromFile(rnInpath+"nodes.json",nodeList);
        System.out.println("路网数据读取完毕！共"+edgeList.size()+"条边，"+nodeList.size()+"个顶点！    "+ new Date(System.currentTimeMillis()));
    }
    /**
     * 读入路网的边
     * @param edgesInpath
     */
    public static void readEdgeFromFile(String edgesInpath,HashMap<String,Edge> edgeList){
        JSONObject edgesObject = JSONRW.readJson(edgesInpath);
        JSONArray edgesArray = edgesObject.getJSONArray("edges");
        Edge edge;   //用于指向一条边
        for(int i=0;i<edgesArray.size();i++){
            edge = new Edge();
            edge.id = edgesArray.getJSONObject(i).getString("id");
            edge.sp = edgesArray.getJSONObject(i).getString("sp");
            edge.ep = edgesArray.getJSONObject(i).getString("ep");
            edge.length = edgesArray.getJSONObject(i).getDouble("length");
            edgeList.put(edge.id,edge);
        }
    }
    /**
     * 读入路网顶点
     * @param nodesInpath
     * @param nodeList
     */
    public static void readNodeFromFile(String nodesInpath,HashMap<String,Node> nodeList){
        JSONObject nodesObject = JSONRW.readJson(nodesInpath);
        JSONArray nodesArray = nodesObject.getJSONArray("nodes");
        Node node; //用于指向一个顶点
        for(int i=0;i<nodesArray.size();i++){
            node = new Node();
            node.id = nodesArray.getJSONObject(i).getString("id");
            node.lng = nodesArray.getJSONObject(i).getDouble("lng");
            node.lat = nodesArray.getJSONObject(i).getDouble("lat");
            JSONArray sedgesArray = nodesArray.getJSONObject(i).getJSONArray("sEdges");
            for(int j=0;j<sedgesArray.size();j++)
                node.sEdges.add(sedgesArray.getString(j));
            JSONArray eedgesArray = nodesArray.getJSONObject(i).getJSONArray("eEdges");
            for(int j=0;j<eedgesArray.size();j++)
                node.eEdges.add(eedgesArray.getString(j));
            nodeList.put(node.id,node);
        }
    }


    /**
     * 计算所有路网节点的邻域范围
     * @param nodesNR
     * @param eps
     * @param nodeList
     * @param edgeList
     */
    public static void calNodesNeighborRange(HashMap<String,NeighborRange> nodesNR,double eps,HashMap<String,Node> nodeList, HashMap<String,Edge> edgeList){
        System.out.println("正在计算路网节点的邻域范围...   "+ new Date(System.currentTimeMillis()));
        int count = 0;
        for(Map.Entry<String,Node> nodeEntry:nodeList.entrySet()){
//            System.out.println();
//            System.out.println("正在计算一个顶点的邻域范围...   "+ new Date(System.currentTimeMillis()));
            nodesNR.put(nodeEntry.getKey(),calNodeNeighborRange(nodeEntry.getValue(),eps,nodeList,edgeList));
            count++;
//            if (count%200==0)
//                System.out.println("第"+count+"个顶点的邻域范围计算完成！    "+ new Date(System.currentTimeMillis()));
        }
        System.out.println("路网节点的邻域范围计算完成！    "+ new Date(System.currentTimeMillis()));
    }
    /**
     * 计算一个路网节点的邻域范围
     * @param n
     * @param eps
     * @param nodeList
     * @param edgeList
     * @return
     */
    public static NeighborRange calNodeNeighborRange(Node n,double eps,HashMap<String,Node> nodeList,HashMap<String,Edge> edgeList){
        NeighborRange nrforn = new NeighborRange();   //节点n的邻域范围
        //初始化邻域范围的四个表
        for(int i=0;i<n.sEdges.size();i++){
            if(edgeList.get(n.sEdges.get(i)).length<=eps) {    //如果边长比阈值短，说明这是条安全边
                nrforn.safeEdges.add(n.sEdges.get(i));
                nrforn.safeLength.add(edgeList.get(n.sEdges.get(i)).length);    //保存节点到该边终点的长度
                nrforn.allEdges.put(n.sEdges.get(i),edgeList.get(n.sEdges.get(i)).length);
            }else{                       //如果边长比阈值长，说明是非安全边
                nrforn.unsafeEdges.add(n.sEdges.get(i));
                nrforn.unsafeLength.add(eps);         //保存边属于领域范围内的长度
                nrforn.allEdges.put(n.sEdges.get(i),eps);
            }
        }
//        System.out.println("发现节点"+n.id+"的安全边有"+nrforn.safeEdges.size()+"条！");

        //将邻域范围中的安全边表看成队列，不断扩展新的边到该列表中，或到非安全边表中
        Edge exploreEdge;   //指向当前扩展的边
        for(int i=0;i<nrforn.safeEdges.size();i++){
            for(int j=0;j<nodeList.get(edgeList.get(nrforn.safeEdges.get(i)).ep).sEdges.size();j++){
                exploreEdge = edgeList.get(nodeList.get(edgeList.get(nrforn.safeEdges.get(i)).ep).sEdges.get(j));
                if(nrforn.allEdges.get(exploreEdge.id)!=null)
                    continue;
                if(exploreEdge.length+nrforn.safeLength.get(i)<=eps){   //如果n到扩展边终点的长度小于阈值，说明扩展边是安全边
                    nrforn.safeEdges.add(exploreEdge.id);
                    nrforn.safeLength.add(exploreEdge.length+nrforn.safeLength.get(i));    //保存节点n到扩展边终点的距离
                    nrforn.allEdges.put(exploreEdge.id,exploreEdge.length+nrforn.safeLength.get(i));
                }else if(eps-nrforn.safeLength.get(i)!=0){   //如果n到扩展边终点的长度大于阈值
                    nrforn.unsafeEdges.add(exploreEdge.id);
                    nrforn.unsafeLength.add(eps-nrforn.safeLength.get(i));  //保存扩展边属于邻域范围的长度
                    nrforn.allEdges.put(exploreEdge.id,eps);
                }
            }
//            System.out.println("发现节点"+n.id+"的安全边有"+nrforn.safeEdges.size()+"条！");
        }
//        System.out.println("发现节点"+n.id+"的安全边总共有"+nrforn.safeEdges.size()+"条！");
//        System.out.println("发现节点"+n.id+"的非安全边总共有"+nrforn.unsafeEdges.size()+"条！");

        return nrforn;
    }

    /**
     * 根据时间字符串，获取两个时刻间的时间片数
     * @param start
     * @param end
     * @return
     */
    static int getseconds(String start,String end){
        Timestamp stimestamp=Timestamp.valueOf(start);
        long t1=stimestamp.getTime();
        Timestamp etimestamp=Timestamp.valueOf(end);
        long t2=etimestamp.getTime();
        return (int)((t2-t1)/1000);
    }
    /**
     * 根据时间字符串计算时间数字
     * @param stime
     * @return
     */
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
}
