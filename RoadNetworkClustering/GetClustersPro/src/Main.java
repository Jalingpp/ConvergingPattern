import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;

public class Main {

    public static void main(String[] args) throws IOException {

        MBR mapScale=new MBR(120, 30, 122.3, 32.3);    //��ʼ����ͼ��γ��,�������α�ʾ��С���ȣ���Сά�ȣ���󾭶ȣ����ά��
        double eps = 250;                 //����뾶��ֵ
        int minPts = 6;                 //�ܶ���ֵ
        int minshare = 5;       //SNN��ֵ
        int mincontain = 2;    //���ں��ĵ���ֵ
        String objectsInPath = "/root/jjp/Converging_jjp/data/trajectories/";    //�ƶ���������·��
        String roadNetInPath = "/root/jjp/Converging_jjp/data/roadnetwork/";       //·������·��
        String eoIndexInPath = "/root/jjp/Converging_jjp/data/edgeobjectIndex/";  //�ߵ���������·��
        String clustersOutPath = "/root/jjp/Converging_jjp/data/clusters_pro/runtime_snn/";
        String startTime = "2015-04-11 18:00:00.0";  //����Ŀ�ʼʱ��
        String endTime = "2015-04-11 20:00:00.0";    //����Ľ���ʱ��

        HashMap<String,Edge> edgeList = new HashMap<>(); //·���߼�
        HashMap<String,Node> nodeList = new HashMap<>();  //·����㼯��
        HashMap<String,NeighborRange> nodesNR = new HashMap<>();  //·����������Χ
        /**
         * ����·������
         */
        readRoadNetFromFile(roadNetInPath,edgeList,nodeList);
        /**
         * ��������·���ڵ������Χ
         */
        calNodesNeighborRange(nodesNR,eps,nodeList,edgeList);

        HashMap<String,List<Onepair>> pairmap = new HashMap<>();   //����ȫ�ֵ�pq��Ա�,StringΪp���id
        HashMap<Edge2Edge,List<String>> e2e_path = new HashMap<>();   //����ȫ�ֵıߵ��ߵ�·����

        DBRNCANPro dbrncanPro;
        String time,inPath_o,inPath_eoI,outPath_c,csvpath,mbrcsvpath,jsonpath;
        int clustersAmount = 0;    //��¼ʱ����ڵ��ܴ���
        int count = 0;       //��¼ʱ����ھ������ʱ�����
        long runtime = 0;   //��¼�����ʱ����ms
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
             * �������ص�JSON�ļ���CSV�ļ�
             */ {
                //�����ļ������·������
                outPath_c=numsout(time.substring(11,19));
                //·���ɸĳ�samepath
                csvpath=clustersOutPath+"csv/Clusters_"+outPath_c+".csv";
                mbrcsvpath=clustersOutPath+"csv/ClustersMBR_"+outPath_c+".csv";
                jsonpath=clustersOutPath+"json/Clusters"+outPath_c+".json";
                //���������ݷֱ�����������ļ�
                CSVRW csvrw = new CSVRW();
                ClustersJSONRW cjsonrw = new ClustersJSONRW();
                csvrw.writeCSV(csvpath,finalClusters);
                csvrw.writeMBRtoCSV(mbrcsvpath, finalClusters);
                cjsonrw.writeJson(jsonpath, finalClusters);
            }
            count++;
            if(count%200==0)
                System.out.println("���"+count+"��ʱ����µľ��࣡   "+ new Date(System.currentTimeMillis()));

            dbrncanPro.clear();   //��������ǰʱ�̵ľ�����
        }
//        long endRunTime = System.currentTimeMillis();
//        System.out.println();
        System.out.println("������ɣ�ʱ��� "+startTime+" �� "+endTime+" �ڵľ�������ʱ��Ϊ "+runtime+" ms��ÿ��ʱ�����ƽ�� "+(clustersAmount/count)+"���أ�  "+ new Date(System.currentTimeMillis()));

//        System.out.println("Hello World!");
    }


    /**
     * ��·������
     * @param rnInpath  ·�������ļ���·��
     * @param edgeList  ·���߱�
     * @param nodeList  ·�������
     */
    public static void readRoadNetFromFile(String rnInpath,HashMap<String,Edge> edgeList,HashMap<String,Node> nodeList){
        System.out.println("���ڶ���·������...   "+ new Date(System.currentTimeMillis()));
        readEdgeFromFile(rnInpath+"edges.json",edgeList);
        readNodeFromFile(rnInpath+"nodes.json",nodeList);
        System.out.println("·�����ݶ�ȡ��ϣ���"+edgeList.size()+"���ߣ�"+nodeList.size()+"�����㣡    "+ new Date(System.currentTimeMillis()));
    }
    /**
     * ����·���ı�
     * @param edgesInpath
     */
    public static void readEdgeFromFile(String edgesInpath,HashMap<String,Edge> edgeList){
        JSONObject edgesObject = JSONRW.readJson(edgesInpath);
        JSONArray edgesArray = edgesObject.getJSONArray("edges");
        Edge edge;   //����ָ��һ����
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
     * ����·������
     * @param nodesInpath
     * @param nodeList
     */
    public static void readNodeFromFile(String nodesInpath,HashMap<String,Node> nodeList){
        JSONObject nodesObject = JSONRW.readJson(nodesInpath);
        JSONArray nodesArray = nodesObject.getJSONArray("nodes");
        Node node; //����ָ��һ������
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
     * ��������·���ڵ������Χ
     * @param nodesNR
     * @param eps
     * @param nodeList
     * @param edgeList
     */
    public static void calNodesNeighborRange(HashMap<String,NeighborRange> nodesNR,double eps,HashMap<String,Node> nodeList, HashMap<String,Edge> edgeList){
        System.out.println("���ڼ���·���ڵ������Χ...   "+ new Date(System.currentTimeMillis()));
        int count = 0;
        for(Map.Entry<String,Node> nodeEntry:nodeList.entrySet()){
//            System.out.println();
//            System.out.println("���ڼ���һ�����������Χ...   "+ new Date(System.currentTimeMillis()));
            nodesNR.put(nodeEntry.getKey(),calNodeNeighborRange(nodeEntry.getValue(),eps,nodeList,edgeList));
            count++;
//            if (count%200==0)
//                System.out.println("��"+count+"�����������Χ������ɣ�    "+ new Date(System.currentTimeMillis()));
        }
        System.out.println("·���ڵ������Χ������ɣ�    "+ new Date(System.currentTimeMillis()));
    }
    /**
     * ����һ��·���ڵ������Χ
     * @param n
     * @param eps
     * @param nodeList
     * @param edgeList
     * @return
     */
    public static NeighborRange calNodeNeighborRange(Node n,double eps,HashMap<String,Node> nodeList,HashMap<String,Edge> edgeList){
        NeighborRange nrforn = new NeighborRange();   //�ڵ�n������Χ
        //��ʼ������Χ���ĸ���
        for(int i=0;i<n.sEdges.size();i++){
            if(edgeList.get(n.sEdges.get(i)).length<=eps) {    //����߳�����ֵ�̣�˵����������ȫ��
                nrforn.safeEdges.add(n.sEdges.get(i));
                nrforn.safeLength.add(edgeList.get(n.sEdges.get(i)).length);    //����ڵ㵽�ñ��յ�ĳ���
                nrforn.allEdges.put(n.sEdges.get(i),edgeList.get(n.sEdges.get(i)).length);
            }else{                       //����߳�����ֵ����˵���Ƿǰ�ȫ��
                nrforn.unsafeEdges.add(n.sEdges.get(i));
                nrforn.unsafeLength.add(eps);         //�������������Χ�ڵĳ���
                nrforn.allEdges.put(n.sEdges.get(i),eps);
            }
        }
//        System.out.println("���ֽڵ�"+n.id+"�İ�ȫ����"+nrforn.safeEdges.size()+"����");

        //������Χ�еİ�ȫ�߱��ɶ��У�������չ�µıߵ����б��У��򵽷ǰ�ȫ�߱���
        Edge exploreEdge;   //ָ��ǰ��չ�ı�
        for(int i=0;i<nrforn.safeEdges.size();i++){
            for(int j=0;j<nodeList.get(edgeList.get(nrforn.safeEdges.get(i)).ep).sEdges.size();j++){
                exploreEdge = edgeList.get(nodeList.get(edgeList.get(nrforn.safeEdges.get(i)).ep).sEdges.get(j));
                if(nrforn.allEdges.get(exploreEdge.id)!=null)
                    continue;
                if(exploreEdge.length+nrforn.safeLength.get(i)<=eps){   //���n����չ���յ�ĳ���С����ֵ��˵����չ���ǰ�ȫ��
                    nrforn.safeEdges.add(exploreEdge.id);
                    nrforn.safeLength.add(exploreEdge.length+nrforn.safeLength.get(i));    //����ڵ�n����չ���յ�ľ���
                    nrforn.allEdges.put(exploreEdge.id,exploreEdge.length+nrforn.safeLength.get(i));
                }else if(eps-nrforn.safeLength.get(i)!=0){   //���n����չ���յ�ĳ��ȴ�����ֵ
                    nrforn.unsafeEdges.add(exploreEdge.id);
                    nrforn.unsafeLength.add(eps-nrforn.safeLength.get(i));  //������չ����������Χ�ĳ���
                    nrforn.allEdges.put(exploreEdge.id,eps);
                }
            }
//            System.out.println("���ֽڵ�"+n.id+"�İ�ȫ����"+nrforn.safeEdges.size()+"����");
        }
//        System.out.println("���ֽڵ�"+n.id+"�İ�ȫ���ܹ���"+nrforn.safeEdges.size()+"����");
//        System.out.println("���ֽڵ�"+n.id+"�ķǰ�ȫ���ܹ���"+nrforn.unsafeEdges.size()+"����");

        return nrforn;
    }

    /**
     * ����ʱ���ַ�������ȡ����ʱ�̼��ʱ��Ƭ��
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
     * ����ʱ���ַ�������ʱ������
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

    /**
     * ����ʱ���ַ���ת��Ϊ�������·��
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
