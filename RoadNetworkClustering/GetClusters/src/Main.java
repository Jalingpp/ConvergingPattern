import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException{

        mapScale=new MBR(120, 30, 122.3, 32.3);    //初始化地图经纬度,参数依次表示最小经度，最小维度，最大经度，最大维度

        Main test=new Main(mapScale,250,6,5,2);	   //创建对象，参数依次是mapscale，eps，minpts，minshare，mincon

        stime="2015-04-11 18:00:00.0";     //聚类的开始时间
        etime="2015-04-11 20:00:00.0";     //聚类的结束时间

        samepath="/root/jjp/Converging_jjp/data/trajectories/";               //读取文件的位置
        outpath="/root/jjp/Converging_jjp/data/clusters_baseline/runtime_snn/";  //输出文件的位置

        int all=getseconds(stime,etime);
        int count = 0;
        int clustersAmount = 0;
        System.out.println("聚类开始！  "+new Date(System.currentTimeMillis()));
        long startRunTime = System.currentTimeMillis();
        for(int i=timetonum_s(stime);i<=timetonum_s(stime)+all;i+=10){
            time=alltotime_s(i);
            clustersAmount += printofile1(time);
            count++;
            if(count%200==0)
                System.out.println("完成"+count+"个时间戳下的聚类！   "+ new Date(System.currentTimeMillis()));
        }
        long endRunTime = System.currentTimeMillis();
//        System.out.println();
        System.out.println("聚类完成！时间段 "+stime+" 至 "+etime+" 内的聚类运行时间为 "+(endRunTime-startRunTime)+" ms！每个时间戳下平均 "+(clustersAmount/count)+"个簇！  "+ new Date(System.currentTimeMillis()));

    }

    public static int printofile1(String time) throws IOException{

        //从filepath路径的文件中读取并得到所有移动点的集合points
        filepath=samepath+numsout(time.substring(11,19))+".txt";
        edge_point = new HashMap<String, ArrayList<SnapPoint>>();
        points=readpoints.read(time, filepath, edge_point,pointsidtop);
//        System.out.println();
//        System.out.println("正在读时间戳"+time+"下的轨迹点数据文件...  "+new Date(System.currentTimeMillis()));
        if(time.equals(stime)){
            for(int i=0;i<points.size();i++){
                List<Onepair> list=new ArrayList<Onepair>();
                pairmap.put(points.get(i).getId(), list);
            }
        }
//        System.out.println("时间戳"+time+"下的轨迹点数据文件读取完成！  "+new Date(System.currentTimeMillis()));


        /**
         *使用算法1聚类，调用DBRNCAN1函数进行聚类
         */
        DBRNCAN1 subDbscan = new DBRNCAN1
                (       points,                  //普通点的集合
                        eps,                     //距离阈值
                        minPts,                  //密度阈值
                        minshare,                //最小共享近邻数量阈值
                        mincon,                  //簇数量的阈值
                        mapScale,                //地图边界
                        edge2edgedist,           //从a边上的点到b边上的点的距离
                        edge2edgepath,
                        graph,                   //可达矩阵
                        edges,                   //边
                        map,                     //转向矩阵
                        nodesidtop,              //从路口id到路口所有信息的映射
                        pointsidtop,			   //从对象id到对象所有信息的映射
                        nodepoints.size()+1,     //nodes的个数
                        nodepoints,              //nodes的集合
                        gridIndexnode,
                        pairmap,
                        edge_point
                );          //nodes的网格


        /**
         * 聚类，返回聚类得到的簇
         */ {
//            System.out.println(time+"聚类开始...     "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
            clusters=subDbscan.getClusters();
//            System.out.println(time+"聚类完成!共发现"+clusters.size()+"个簇！   "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
//            System.out.println();
        }

        /**
         * 输出结果簇到JSON文件和CSV文件
         */ {
            //三个文件的输出路径设置
            path=numsout(time);
            //路径可改成samepath
            csvpath=outpath+"csv/Clusters_"+path+".csv";
            mbrcsvpath=outpath+"csv/ClustersMBR_"+path+".csv";
            jsonpath=outpath+"json/Clusters"+path+".json";
            //将簇中内容分别输出到三个文件
            csvrw.writeCSV(csvpath,clusters);
            csvrw.writeMBRtoCSV(mbrcsvpath, clusters);
            jsonrw.writeJson(jsonpath, clusters);
        }
        int clustersAmount = clusters.size();

        /**
         * 析构，释放部分空间
         */{
            subDbscan.clear();
            clusters.clear();
            points.clear();
            menberPoints.clear();
            edge_point.clear();
        }
        return clustersAmount;
    }


    static int timetonum_m(String stime){
        int res;
        String a=stime.substring(11,13);
        int hours=Integer.parseInt(stime.substring(11,13));
        int minutes=Integer.parseInt(stime.substring(14,16));
        res=hours*60+minutes;
        return res;
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
    static String alltotime_m(int all){
        String res="2015-04-11";
        int hours=all/60;
        if(hours<10){
            res=res+"0"+hours+":";
        }
        else{
            res=res+hours+":";
        }
        int minutes=all%60;
        if(minutes<10){
            res=res+"0"+minutes+":00.0";
        }
        else{
            res=res+minutes+":00.0";
        }

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

    static int getminutes(String start,String end){
        Timestamp stimestamp=Timestamp.valueOf(start);
        long t1=stimestamp.getTime();
        Timestamp etimestamp=Timestamp.valueOf(end);
        long t2=etimestamp.getTime();
        return (int)((t2-t1)/1000)/60;
    }

    static int getseconds(String start,String end){
        Timestamp stimestamp=Timestamp.valueOf(start);
        long t1=stimestamp.getTime();
        Timestamp etimestamp=Timestamp.valueOf(end);
        long t2=etimestamp.getTime();
        return (int)((t2-t1)/1000);
    }


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
     * 主函数属性和变量定义
     */
    private static MBR  mapScale;//地图边界经纬度
    private static double eps;//领域半径阈值
    private static int minPts;//密度阈值
    private static int minshare; //最小共享点数
    private static int mincon;//簇包含的最小数量
    static String stime;     //开始时间
    static String etime;     //结束时间
    static String time;
    static List<SnapPoint> points=new ArrayList<SnapPoint>();                //从文件中读到的所有移动点集合
    static List<Clusters> clusters=new ArrayList<Clusters>();                //聚类得到的簇集合
    static List<SnapPoint> menberPoints=new ArrayList<SnapPoint>();
    static HashMap<String, List<Onepair> > pairmap=new HashMap<String,List<Onepair> >();
    static HashMap<Edge2Edge, Double> edge2edgedist = new HashMap<Edge2Edge, Double>();
    static HashMap<Edge2Edge,List<String>> edge2edgepath=new HashMap<Edge2Edge,List<String>>();  //从a点到b点，其中经过的边
    static HashMap<String, LinkedList<Adj_Node> > graph=new HashMap<String, LinkedList<Adj_Node> >(); //可达矩阵
    static HashMap<String, Adj_Node> edges=new HashMap<String, Adj_Node>();  //边到顶点的映射
    static HashMap<String, Etoe> map=new HashMap<String, Etoe>();  //转向矩阵
    static HashMap<String,SnapPoint> nodesidtop = new HashMap<String,SnapPoint>();  //映射  路口的id-->路口的所有信息
    static HashMap<String,SnapPoint> pointsidtop = new HashMap<String,SnapPoint>();  //映射  路口的id-->路口的所有信息
    static List<SnapPoint> nodepoints=new ArrayList<SnapPoint>();  //所有的顶点的集合
    static GridIndex gridIndexnode; //顶点索引
    static HashMap<String, ArrayList<SnapPoint>> edge_point=new HashMap<String, ArrayList<SnapPoint>>();  //在time时刻，每条边上包含的点

    static CSVRW csvrw;
    static JSONRW jsonrw;
    static ReadPoints readpoints;
    static String filepath;
    static String csvpath;
    static String path;
    static String mbrcsvpath;
    static String jsonpath;
    static String samepath;
    static String outpath;




    /**
     * 主函数的构造函数(可调参数)
     * mapScale  地图范围
     * eps       路网距离阈值
     * minPts    密度阈值
     * minshare  共享可达近邻数量阈值
     * mincon    簇的数量阈值
     *
     * */
    Main(MBR mapScale,double eps,int minPts,int minshare,int mincon) throws IOException{
        this.mapScale=mapScale;
        this.eps=eps;
        this.minPts=minPts;
        this.minshare=minshare;
        this.mincon=mincon;

        String edgeturn_name = "/root/jjp/Converging_jjp/data/cluster_baseline_files/edgeturn.txt";
        String vexarrive_name = "/root/jjp/Converging_jjp/data/cluster_baseline_files/vexarrive.txt";
        String edge_name = "/root/jjp/Converging_jjp/data/cluster_baseline_files/Edges.txt";
        String node_name="/root/jjp/Converging_jjp/data/cluster_baseline_files/Nodes.txt";

        System.out.println("正在读入路网数据...   "+ new Date(System.currentTimeMillis()));

        //读转向矩阵
        Readedgeturn edgeturn = new Readedgeturn();
        map = edgeturn.read(edgeturn_name);

        //读可达矩阵
        Readvexarrive readvexarrive=new Readvexarrive();
        graph=readvexarrive.read(vexarrive_name);

        //读edges
        Readedge readedge=new Readedge();
        edges=readedge.read(edge_name);

        //读nodes
        ReadNodes readnodes=new ReadNodes();
        nodepoints=readnodes.read(node_name,nodesidtop);

        gridIndexnode=new GridIndex(mapScale, eps, nodepoints);

        System.out.println("路网数据读取完毕！共"+edges.size()+"条边，"+nodepoints.size()+"个顶点！    "+ new Date(System.currentTimeMillis()));

        csvrw=new CSVRW();                         //用于读写csv文件
        jsonrw=new JSONRW();                       //用于读写json文件
        readpoints=new ReadPoints();               //用于读点

    }

}



