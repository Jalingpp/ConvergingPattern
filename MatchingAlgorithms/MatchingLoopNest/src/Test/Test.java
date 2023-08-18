package Test;

import Classes.Event;
import Clusters.Cluster;
import Clusters.Objects;
import Clusters.TimeClusters;
import RoadNetWork.RoadNetWork;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.csvreader.CsvWriter;

import java.io.*;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class Test {
     public static void main(String[] args) throws IOException {

          String stime = "2015-04-11 18:00:00.0";     //开始时间
          String etime = "2015-04-11 20:00:00.0";     //结束时间
          String objectsinpath = "/root/Converging_jjp/data/trajectories/";
          String clustersinpath = "/root/Converging_jjp/data/clusters_pro/runtime_eps/cluster107_100_3_2/clusters_baseline/json/";
          String convergingoutpath = "/root/Converging_jjp/data/events_LoopNest/runtime_eps/cluster107_100_3_2/";
          String roadPath = "/root/Converging_jjp/data/roadnetwork/";

//          String objectsinpath = "C:\\data\\Converging\\trajectories\\";
//          String clustersinpath = "C:\\data\\Converging\\clusters\\json\\";
//          String convergingoutpath = "C:\\data\\Converging\\events\\";
//          String roadPath = "C:\\data\\Converging\\roadnetwork\\";

          int minScale = 5;   //规模阈值，单位个
          int minDuration = 30;  //持续时长阈值，单位s
          int timeinterval = 10;

          /**
           * 构建路网 RoadNetWork road
           */
          RoadNetWork road = new RoadNetWork(roadPath);

          long startRuntime = System.currentTimeMillis();

          /**
           * 读入所有时间戳下的簇到 ArrayList<TimeClusters> Tc
           */
          ArrayList<TimeClusters> Tc = new ArrayList<TimeClusters>();   //所有时间戳下的簇的列表
          int all = getseconds(stime, etime);   //时间片数
          System.out.println("正在读取簇...  "+new Date(System.currentTimeMillis()));
          int readCluCount = 0;
          for (int i = timetonum_s(stime); i <= timetonum_s(stime) + all; i+=timeinterval) {           //将各个时间段的簇读入
               String time = alltotime_s(i);
               String path = clustersinpath +"Clusters"+ time.substring(8,14)+".json";
               TimeClusters tc = new TimeClusters(path,i);
               Tc.add(tc);
               readCluCount++;
               if(readCluCount%200==0)
                    System.out.println(time+"下的簇读取完毕！  "+new Date(System.currentTimeMillis()));
          }
          System.out.println("输入时间段内的簇全部加载完毕！  "+new Date(System.currentTimeMillis()));

          /**
           * 构建边和簇的映射  HashMap<String,HashMap<String,ArrayList<String>>> edges_clusters
           */
          HashMap<String,HashMap<String,ArrayList<String>>> edges_clusters = setupEdgeClus(Tc);

          /**
           * 相邻时刻进行簇包含匹配，发现汇聚事件
           */
          HashMap<String, Event> allEvent = getAllEvents(Tc,edges_clusters,road,minScale,minDuration,timeinterval);

          long endRuntime = System.currentTimeMillis();
          long runtime = endRuntime-startRuntime;
          System.out.println("汇聚事件检测共用时 "+runtime+" ms!");

          /**
           * 输出所有的汇聚事件为json格式文件
           */
          String eventOutpath = convergingoutpath+"/json/event"+stime.substring(11,13)+"to"+etime.substring(11,13)+".json";
//          String eventOutpath = convergingoutpath+"\\json\\event_"+stime.substring(11,13)+"to"+etime.substring(11,13)+".json";
          JSONObject eventSetJson = writeEventsToJson(eventOutpath,allEvent,objectsinpath);

          /**
           * 输出所有的汇聚事件为CSV格式文件，两个文件：边的文件和移动对象的文件
           */
          //String eventEdgesOutpath = convergingoutpath+"/csv/eventEdges/eventEdges"+stime.substring(11,13)+"to"+etime.substring(11,13)+".csv";
          String eventObjectsOutpath = convergingoutpath+"/csv/eventObjects"+stime.substring(11,13)+"to"+etime.substring(11,13)+".csv";
          writeEventsToCsv(eventSetJson,eventObjectsOutpath,allEvent);
     }

     /**
      * 获取所给时间段的秒数
      * @param start  起始时间
      * @param end    终止时间
      * @return
      */
     static int getseconds(String start, String end) {
          Timestamp stimestamp = Timestamp.valueOf(start);
          long t1 = stimestamp.getTime();
          Timestamp etimestamp = Timestamp.valueOf(end);
          long t2 = etimestamp.getTime();
          return (int) ((t2 - t1) / 1000);
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
      * 将时间(精确到秒)转化为数字序号
      * @param stime
      * @return
      */
     static int timetonum_s(String stime) {
          int res;
          int hours = Integer.parseInt(stime.substring(11, 13));
          int minutes = Integer.parseInt(stime.substring(14, 16));
          int second = Integer.parseInt(stime.substring(17, 19));
          res = hours * 60 * 60 + minutes * 60 + second;
          return res;
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
      * 将序号转化为时间字符串，需手动设置年月日！！
      * @param all
      * @return
      */
     static String alltotime_s(int all){
          String res="20150411";
          int hours=all/(60*60);
          if(hours<10){
               res=res+"0"+hours;
          }else{
               res=res+hours;
          }
          int minutes=(all%(60*60))/60;
          if(minutes<10){
               res=res+"0"+minutes;
          }
          else{
               res=res+minutes;
          }
          int seconds=(all%(60*60))%60;
          if(seconds<10){
               res=res+"0"+seconds+"0";
          }else{
               res=res+seconds+"0";
          }

          return res;
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
          System.out.println("正在生成边-簇映射表...   "+new Date(System.currentTimeMillis()));
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
          System.out.println("边-簇表生成完毕！  "+new Date(System.currentTimeMillis()));
          return edg_clu;
     }

     /**
      * 发现当前时间段的所有满足阈值要求的汇聚事件
      * @param Tc    当前时间段内所有的簇集
      * @param edges_clusters    边-簇映射表
      * @param road       路网数据
      * @return
      */
     public static HashMap<String, Event>  getAllEvents(ArrayList<TimeClusters> Tc,HashMap<String,HashMap<String,ArrayList<String>>> edges_clusters,RoadNetWork road,int minScale,int minDuration,int timeinterval){

          int contained=0,partialContained=0,noneContained=0,all=0;
          boolean isC=false,isPC=false;

          int eventAmount = 0;  //统计发现的事件总数
          HashMap<String,Event> allEvent = new HashMap<>();  //定义发现的所有事件列表

          List<String>  rootClustersList = new ArrayList<>();   //维护一个事件的根节点表,String为根节点时间序号i+id
          HashMap<String,Integer> rootClustersMap = new HashMap<>();   //判断根节点是否还在list中，key为根节点时间序号i+id，value为1或0,1表示在，0表示不在

          System.out.println("正在发现汇聚事件...  "+new Date(System.currentTimeMillis()));
          //簇包含连接，逐层进行匹配，确定簇包含关系
          for(int i=0;i<Tc.size()-1;i++) {
               for(Map.Entry<String,Cluster> entry1: Tc.get(i).getClusters().entrySet()) {
                    all++;
                    for(Map.Entry<String,Cluster> entry2: Tc.get(i+1).getClusters().entrySet()){
                         int fartherRelation = isFarther(entry1.getValue(),entry2.getValue());
                         if(fartherRelation==1){
                              isC = true;
                              String _rootkey = (i+1)+"-"+entry2.getKey();
                              rootClustersList.add(_rootkey);
                              rootClustersMap.put(_rootkey,1);
                              //设置当前簇的父簇，并将其加入到父簇的子簇表中
                              entry1.getValue().setFatherId(_rootkey);
                              String _childkey = i+"-"+entry1.getKey();
                              entry2.getValue().getChildClusters().add(_childkey);
                              //修改父簇的叶节点时间（事件最早起始时间）
                              entry2.getValue().setEventStartTime(entry1.getValue().getEventStartTime());
                              //判断当前簇曾经是不是，若是，则在根节点表中删除
                              String _queryRootkey = i+"-"+entry1.getKey();
                              if(rootClustersMap.get(_queryRootkey)!=null)
                                   rootClustersMap.put(_queryRootkey,0);
                              break;
                         }else if(fartherRelation==2){
                              isPC=true;
                              break;
                         }
                    }
                    if(isC){
                         contained++;
                    }else if(isPC){
                         partialContained++;
                    }else{
                         noneContained++;
                    }
                    isC = false;
                    isPC = false;
               }
               if(i%200==0) {
                    System.out.println("完成时刻" + Tc.get(i).getTime() + "与时刻" + Tc.get(i+1).getTime() + "的匹配...  " + new Date(System.currentTimeMillis()));
               }
          }

          //遍历根节点表生成事件列表
          for(int i=0;i<rootClustersList.size();i++) {
               if (rootClustersMap.get(rootClustersList.get(i)) == 1) {
                    String[] _rootInf = rootClustersList.get(i).split("-");
                    int timeI = Integer.parseInt(_rootInf[0]);
                    String rootID = _rootInf[1];
                    Cluster rootCluster = Tc.get(timeI).getClusters().get(rootID);
                    rootCluster.setCid(rootClustersList.get(i));
                    Event _event;
                    if (rootCluster.getFatherId() == null) {     //如果父簇为空，说明该簇为根簇，新建事件，并添加到事件列表中
                         _event = new Event(Integer.toString(eventAmount), rootCluster.getTime(),rootCluster);   //设置事件的结束时间
                         allEvent.put(Integer.toString(eventAmount), _event);
                         rootCluster.setEventId(Integer.toString(eventAmount));
                         eventAmount++;
                    }
               }
          }

          //检测生成的汇聚事件是否满足持续时长和规模的要求
          int averageScale = 0;
          int averageDuration = 0;
          for(Iterator<Map.Entry<String,Event>> entryIt = allEvent.entrySet().iterator();entryIt.hasNext();) {
               Map.Entry<String,Event> entry = entryIt.next();
               //计算事件的属性
               entry.getValue().calProperties();
               averageScale+=entry.getValue().getScale();
               int _duration = getseconds(entry.getValue().getStartTime(),entry.getValue().getEndTime())/timeinterval;
               averageDuration+=_duration;
               //判断事件是否满足规模阈值和持续时长的阈值，不满足则删除，满足则计算每个时间戳下的边集并输出成json和CSV
               if(entry.getValue().getScale()<minScale||_duration<minDuration)
               {
                    entryIt.remove();
               }
          }

          System.out.println("minScale="+minScale+"  minDuration="+minDuration+"s");
          System.out.println("汇聚事件检测完成，共发现"+eventAmount+"个汇聚树！");
          System.out.println("其中满足阈值条件的汇聚事件有"+allEvent.size()+"个!");
          System.out.println("平均规模为："+(averageScale/eventAmount)+"  平均持续时长为："+(averageDuration/eventAmount));
          System.out.println(new Date(System.currentTimeMillis()));

          System.out.println("总簇数="+all+"  完全被包含数="+contained+"  部分被包含数="+partialContained+"  未被包含数="+noneContained);

          return allEvent;
     }

     /**
      * 判断一个字符串表中是否存在某个字符串,存在返回Index，否则返回-1
      * @param slist
      * @param s
      * @return
      */
     public static int isContains(List<String> slist,String s){
          for(int i=0;i<slist.size();i++)
               if (slist.get(i).equals(s)) { return i; }
          return -1;
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
                    if(isFarther(c,nextTC.getClusters().get(candiClu.get(i)))==1)
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
     public static int isFarther(Cluster c1,Cluster c2) {
          int count = 0;
          for(int i=0;i<c1.getScale();i++) {
//               boolean ischange = false;
               for (int j = 0; j < c2.getScale(); j++) {
                    if (c1.getObjects().get(i).getOId().equals(c2.getObjects().get(j).getOId())) {
                         count++;
//                         ischange = true;
                         break;
                    }
               }
//               if(!ischange)
//                    return false;
          }
//          if(count==c1.getScale())
//               return true;  //完全包含
//          else
//               return false;  //部分包含
          if(count==c1.getScale()){
               return 1;  //完全包含
          }else if(count==0){
               return 3;   //不包含
          }else{
               return 2;   //部分包含
          }
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

}
