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

        mapScale=new MBR(120, 30, 122.3, 32.3);    //��ʼ����ͼ��γ��,�������α�ʾ��С���ȣ���Сά�ȣ���󾭶ȣ����ά��

        Main test=new Main(mapScale,250,6,5,2);	   //�������󣬲���������mapscale��eps��minpts��minshare��mincon

        stime="2015-04-11 18:00:00.0";     //����Ŀ�ʼʱ��
        etime="2015-04-11 20:00:00.0";     //����Ľ���ʱ��

        samepath="/root/jjp/Converging_jjp/data/trajectories/";               //��ȡ�ļ���λ��
        outpath="/root/jjp/Converging_jjp/data/clusters_baseline/runtime_snn/";  //����ļ���λ��

        int all=getseconds(stime,etime);
        int count = 0;
        int clustersAmount = 0;
        System.out.println("���࿪ʼ��  "+new Date(System.currentTimeMillis()));
        long startRunTime = System.currentTimeMillis();
        for(int i=timetonum_s(stime);i<=timetonum_s(stime)+all;i+=10){
            time=alltotime_s(i);
            clustersAmount += printofile1(time);
            count++;
            if(count%200==0)
                System.out.println("���"+count+"��ʱ����µľ��࣡   "+ new Date(System.currentTimeMillis()));
        }
        long endRunTime = System.currentTimeMillis();
//        System.out.println();
        System.out.println("������ɣ�ʱ��� "+stime+" �� "+etime+" �ڵľ�������ʱ��Ϊ "+(endRunTime-startRunTime)+" ms��ÿ��ʱ�����ƽ�� "+(clustersAmount/count)+"���أ�  "+ new Date(System.currentTimeMillis()));

    }

    public static int printofile1(String time) throws IOException{

        //��filepath·�����ļ��ж�ȡ���õ������ƶ���ļ���points
        filepath=samepath+numsout(time.substring(11,19))+".txt";
        edge_point = new HashMap<String, ArrayList<SnapPoint>>();
        points=readpoints.read(time, filepath, edge_point,pointsidtop);
//        System.out.println();
//        System.out.println("���ڶ�ʱ���"+time+"�µĹ켣�������ļ�...  "+new Date(System.currentTimeMillis()));
        if(time.equals(stime)){
            for(int i=0;i<points.size();i++){
                List<Onepair> list=new ArrayList<Onepair>();
                pairmap.put(points.get(i).getId(), list);
            }
        }
//        System.out.println("ʱ���"+time+"�µĹ켣�������ļ���ȡ��ɣ�  "+new Date(System.currentTimeMillis()));


        /**
         *ʹ���㷨1���࣬����DBRNCAN1�������о���
         */
        DBRNCAN1 subDbscan = new DBRNCAN1
                (       points,                  //��ͨ��ļ���
                        eps,                     //������ֵ
                        minPts,                  //�ܶ���ֵ
                        minshare,                //��С�������������ֵ
                        mincon,                  //����������ֵ
                        mapScale,                //��ͼ�߽�
                        edge2edgedist,           //��a���ϵĵ㵽b���ϵĵ�ľ���
                        edge2edgepath,
                        graph,                   //�ɴ����
                        edges,                   //��
                        map,                     //ת�����
                        nodesidtop,              //��·��id��·��������Ϣ��ӳ��
                        pointsidtop,			   //�Ӷ���id������������Ϣ��ӳ��
                        nodepoints.size()+1,     //nodes�ĸ���
                        nodepoints,              //nodes�ļ���
                        gridIndexnode,
                        pairmap,
                        edge_point
                );          //nodes������


        /**
         * ���࣬���ؾ���õ��Ĵ�
         */ {
//            System.out.println(time+"���࿪ʼ...     "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
            clusters=subDbscan.getClusters();
//            System.out.println(time+"�������!������"+clusters.size()+"���أ�   "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
//            System.out.println();
        }

        /**
         * �������ص�JSON�ļ���CSV�ļ�
         */ {
            //�����ļ������·������
            path=numsout(time);
            //·���ɸĳ�samepath
            csvpath=outpath+"csv/Clusters_"+path+".csv";
            mbrcsvpath=outpath+"csv/ClustersMBR_"+path+".csv";
            jsonpath=outpath+"json/Clusters"+path+".json";
            //���������ݷֱ�����������ļ�
            csvrw.writeCSV(csvpath,clusters);
            csvrw.writeMBRtoCSV(mbrcsvpath, clusters);
            jsonrw.writeJson(jsonpath, clusters);
        }
        int clustersAmount = clusters.size();

        /**
         * �������ͷŲ��ֿռ�
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
     * �����ת��Ϊʱ���ַ�������Ҫ�ֶ�����������
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
     * �����ת��Ϊʱ���ַ�������Ҫ�ֶ�����������
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
     * ���������Ժͱ�������
     */
    private static MBR  mapScale;//��ͼ�߽羭γ��
    private static double eps;//����뾶��ֵ
    private static int minPts;//�ܶ���ֵ
    private static int minshare; //��С�������
    private static int mincon;//�ذ�������С����
    static String stime;     //��ʼʱ��
    static String etime;     //����ʱ��
    static String time;
    static List<SnapPoint> points=new ArrayList<SnapPoint>();                //���ļ��ж����������ƶ��㼯��
    static List<Clusters> clusters=new ArrayList<Clusters>();                //����õ��Ĵؼ���
    static List<SnapPoint> menberPoints=new ArrayList<SnapPoint>();
    static HashMap<String, List<Onepair> > pairmap=new HashMap<String,List<Onepair> >();
    static HashMap<Edge2Edge, Double> edge2edgedist = new HashMap<Edge2Edge, Double>();
    static HashMap<Edge2Edge,List<String>> edge2edgepath=new HashMap<Edge2Edge,List<String>>();  //��a�㵽b�㣬���о����ı�
    static HashMap<String, LinkedList<Adj_Node> > graph=new HashMap<String, LinkedList<Adj_Node> >(); //�ɴ����
    static HashMap<String, Adj_Node> edges=new HashMap<String, Adj_Node>();  //�ߵ������ӳ��
    static HashMap<String, Etoe> map=new HashMap<String, Etoe>();  //ת�����
    static HashMap<String,SnapPoint> nodesidtop = new HashMap<String,SnapPoint>();  //ӳ��  ·�ڵ�id-->·�ڵ�������Ϣ
    static HashMap<String,SnapPoint> pointsidtop = new HashMap<String,SnapPoint>();  //ӳ��  ·�ڵ�id-->·�ڵ�������Ϣ
    static List<SnapPoint> nodepoints=new ArrayList<SnapPoint>();  //���еĶ���ļ���
    static GridIndex gridIndexnode; //��������
    static HashMap<String, ArrayList<SnapPoint>> edge_point=new HashMap<String, ArrayList<SnapPoint>>();  //��timeʱ�̣�ÿ�����ϰ����ĵ�

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
     * �������Ĺ��캯��(�ɵ�����)
     * mapScale  ��ͼ��Χ
     * eps       ·��������ֵ
     * minPts    �ܶ���ֵ
     * minshare  ����ɴ����������ֵ
     * mincon    �ص�������ֵ
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

        System.out.println("���ڶ���·������...   "+ new Date(System.currentTimeMillis()));

        //��ת�����
        Readedgeturn edgeturn = new Readedgeturn();
        map = edgeturn.read(edgeturn_name);

        //���ɴ����
        Readvexarrive readvexarrive=new Readvexarrive();
        graph=readvexarrive.read(vexarrive_name);

        //��edges
        Readedge readedge=new Readedge();
        edges=readedge.read(edge_name);

        //��nodes
        ReadNodes readnodes=new ReadNodes();
        nodepoints=readnodes.read(node_name,nodesidtop);

        gridIndexnode=new GridIndex(mapScale, eps, nodepoints);

        System.out.println("·�����ݶ�ȡ��ϣ���"+edges.size()+"���ߣ�"+nodepoints.size()+"�����㣡    "+ new Date(System.currentTimeMillis()));

        csvrw=new CSVRW();                         //���ڶ�дcsv�ļ�
        jsonrw=new JSONRW();                       //���ڶ�дjson�ļ�
        readpoints=new ReadPoints();               //���ڶ���

    }

}



