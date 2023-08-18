import traj.util.Cluster;
import traj.util.SpatialObject;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.*;

/*
 * ·������
 * */

public class DBRNCAN1 {

    private MBR  mapScale;              //��ͼ�߽羭γ��
    private double eps;                 //����뾶��ֵ
    private int minPts;                 //�ܶ���ֵ
    private int minshare;               //��С�������
    private int mincon;                 //�ذ�������С����
    private int numcluster;             //�صĸ���

    private List<SnapPoint> procPoints; //�����ƶ���ļ���
    private List<SnapPoint> corePoints; //���ĵ�ļ���
    private List<SnapPoint> nodepoints; //·��Nodes����
    private GridIndex gridIndex;        //���е���������
    private GridIndex gridIndexcore;    //���ĵ���������
    private List<Clusters> finalClusters;//���дؼ���
    HashMap<String, List<Onepair> > pairmap;
    HashMap<String,SnapPoint> nodesid_p;
    HashMap<String, ArrayList<SnapPoint>> edge_point;
    HashMap<Edge2Edge, Double> e2e_dist;
    HashMap<Edge2Edge,List<String>> e2e_path;
    HashMap<String,SnapPoint> pointsid_p;
    HashMap<String, Adj_Node> edges;


    Getdist new_g;                     //�����������֮��������

    HashMap<String,List<SnapPoint>> nearbynode=new HashMap<String,List<SnapPoint>>();  //ӳ��  ��id-->����������ڵ�id
    HashMap<String,Clusters> findaddCluster=new HashMap<String,Clusters>();            //ӳ��  ��id-->��������ڵĴ�


    /*
     * ���캯��
     * points ���е�ļ���
     * eps    ������ֵ
     * minPts �ܶ���ֵ
     * minshare ����ɴ���ڵ�������ֵ
     * mincon �����ص�������ֵ
     * mapscele ��ͼ�ı߽緶Χ
     */
    public DBRNCAN1(List<SnapPoint> points,
                     double eps,
                     int minPts,
                     int minshare,
                     int mincon,
                     MBR mapscale,
                     HashMap<Edge2Edge, Double> e2edist,
                     HashMap<Edge2Edge,List<String>> edge2edgepath,
                     HashMap<String, LinkedList<Adj_Node> > _gra,
                     HashMap<String, Adj_Node> _edg,
                     HashMap<String, Etoe> _map,
                     HashMap<String,SnapPoint> _nodesid_p,
                     HashMap<String,SnapPoint> pointsid_p,
                     int _vex,
                     List<SnapPoint> _nodepoints,
                     GridIndex _gridIndexnode,
                     HashMap<String, List<Onepair> > pairmap,
                     HashMap<String, ArrayList<SnapPoint>> edge_point
    ) throws IOException{

        this.mapScale = mapscale;//������MBR��GridIndex
        this.eps = eps;
        this.minPts = minPts;
        this.minshare=minshare;
        this.mincon=mincon;
        this.pairmap=pairmap;
        this.edge_point=edge_point;
        this.e2e_dist = e2edist;
        this.e2e_path=edge2edgepath;
        this.pointsid_p=pointsid_p;
        this.procPoints =points;
        this.edges=_edg;

        //��ʼ���������Ķ���
        new_g=new Getdist(mapScale, eps, _gra, _edg, _map, _nodesid_p, _vex, _nodepoints, _gridIndexnode);

        //��ʼ����ͨ������
        gridIndex = new GridIndex(mapScale,eps,procPoints);

    }


    /**��һ�������㷨
     * ����finalClusters���������д�
     */
    public List<Clusters> getClusters() throws IOException{
        //��ʼ���ؼ���
        finalClusters = new ArrayList<Clusters>();

        //�����е���ϱ��,��ǳ���Щ���Ǻ��ĵ�
//        System.out.println("���ڱ�Ǻ��ĵ�...  "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
        markPoints1();
//        System.out.println("���ĵ�����ɣ���"+corePoints.size()+"�����ĵ㣡  "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());

//        System.out.println("���ڽ���SNN�Ƚ�...  "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
        baselinecluster();
//        System.out.println("SNN�Ƚ���ɣ�       "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());


        return finalClusters;

    }

    /**
     * ��ǳ����к��ĵ�
     * @throws IOException
     */
    private void markPoints1() throws IOException{
        corePoints = new ArrayList<SnapPoint>();
        for(int i=0;i<procPoints.size();i++){
            if(markPoint1(procPoints.get(i))){
                corePoints.add(procPoints.get(i));
            }
        }
    }

    /**
     *���Ա�ǵ����㸽��������
     * @throws IOException
     */
    private boolean markPoint1(SnapPoint p) throws IOException{
        List<SnapPoint> pointsNearCandi =  gridIndex.getPointsFromGrids(p,1);

        pointsNearCandi = getPointNearBy(p,pointsNearCandi);

        if(pointsNearCandi.size()>=minPts){
            nearbynode.put(p.id, pointsNearCandi);     //ӳ��  ��id-->����������ڵ�id
            p.setMark(PointMark.CORE);
            return true;
        }

        //      edge_point.remove(edges.get(p.id));

        return false;
    }

    /**
     * ����ٽ���(�뾶Ϊ�����ܶ�)
     * @throws IOException
     */
    private List<SnapPoint> getPointNearBy(SnapPoint core, List<SnapPoint> points) throws IOException {
        if(points.size()<=0)
            return points;
        for(int i = points.size()-1;i>=0;i--){
            double roaddist = compMeterDistance(core, points.get(i));
            if(points.get(i).id.equals(core.id)){
                points.remove(i);
            }
            else if(roaddist > eps)
            {
                points.remove(i);
            }
        }
        return points;
    }


    /**
     * ����������֮�����̾���
     * @param pa
     * @param pb
     * @return
     * @throws IOException
     */
    protected double compMeterDistance(SnapPoint pa, SnapPoint pb) throws IOException {
        double EARTH_RADIUS = 6378137.0D;

        //�ƶ��������ڱߵ�id
        String edgeid1 = pa.getEdgeid();
        String edgeid2 = pb.getEdgeid();

        if (edgeid1.equals(edgeid2) && pa.getPos() <= pb.getPos())
        {
            return pb.getPos() - pa.getPos();
        }

        //���ڱߵĳ���
        double len1 = new_g.getlen(edgeid1);

        //pa�������ڱ��յ�ľ���
        //     double pa2ep=edges.get(edgeid1).length-pa.getPos();
        double pa2ep = pa.getPos();

        //pb���ڱ���㵽pb�ľ���
        double sp2pb = pb.getPos();

        //pa���ڱ��յ��id
        String epid = new_g.getepid(edgeid1);
        //pb���ڱ�����id
        String spid = new_g.getspid(edgeid2);

        Edge2Edge e2e = new Edge2Edge(edgeid1.toString(), edgeid2.toString());

        ArrayList<String> e2elist=new ArrayList<String>();

        if (e2e_dist.containsKey(e2e))
        {
            return e2e_dist.get(e2e) + pa2ep + sp2pb;
        }
        double ep2sp = new_g.new_Dijkstra(epid, spid, edgeid1, edgeid2, e2elist);

        if (ep2sp == Double.MAX_VALUE)
        {
            return Double.MAX_VALUE;
        }

        e2e_dist.put(e2e, ep2sp);
        e2e_path.put(e2e, e2elist);

        return ep2sp + pa2ep + sp2pb;
    }


    /**
     * ��һ�������㷨��SNN�Ƚ��㷨
     */
    public void baselinecluster(){
        gridIndexcore = new GridIndex(mapScale,2*eps,corePoints);
        Set<String> ids=gridIndexcore.pointsHash.keySet();
        Queue<SnapPoint> queue=new LinkedList<SnapPoint>();
        List<SnapPoint> psc;
        List<SnapPoint> points;
        SnapPoint p,head;
        int num=0;
        while(!ids.isEmpty()){
            for(String id:ids){
                p = gridIndexcore.pointsHash.get(id);
                points = new ArrayList<SnapPoint>();   //�������Ԫ�ص��б�
                queue.add(p);
                points.add(p);
                gridIndexcore.delete(p);
                while(!queue.isEmpty()){
                    head=queue.poll();   //ȡ������Ԫ��
                    psc = gridIndexcore.getPointsFromGrids(head,1);  //�õ����ĵ�head�������ڵĺ��ĵ�
                    for(int i=0;i<psc.size();i++){
                        if(numofoverlp(nearbynode.get(head.id),nearbynode.get(psc.get(i).id)) >=minshare){
                            points.add(psc.get(i));
                            queue.add(psc.get(i));
                            gridIndexcore.delete(psc.get(i));
                        }
                    }
                }
                if(points.size()>=mincon){
                    this.finalClusters.add(new Clusters(numsout(""+points.get(0).getTime()).substring(8,14)+"_"+num+"", points.get(0).getTime(), points));
                    num++;
                }
                ids=gridIndexcore.pointsHash.keySet();
                break;
            }
        }
    }

    /**
     * ��������list����֮���غϵĵ�ĸ���
     */
    private int numofoverlp(List<SnapPoint> a,List<SnapPoint> b){
        int num=0;
        for(SnapPoint i:a){
            if(b.contains(i)){
                num++;
            }
        }
        return num;
    }


    /**
     * ����a��b����list����֮���غϵĵ�ĸ�������������c��
     */
    private int numofoverlp(List<SnapPoint> a,List<SnapPoint> b,List<SnapPoint> c){
        int num=0;
        for(SnapPoint i:a){
            if(b.contains(i)){
                num++;
                c.add(i);
            }
        }
        return num;
    }

    /**
     * ��ʱ���ַ���ת��Ϊ�ļ�������000000.txt
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
     * �ͷŵ�һ���ֵĿռ�
     */
    public void clear(){
        procPoints.clear();
        corePoints.clear();
        finalClusters.clear();
        nearbynode.clear();
        findaddCluster.clear();
        gridIndex.clear();
        gridIndexcore.clear();
    }

}


class Edge2Edge {
    String edge1;
    String edge2;

    Edge2Edge(String e1, String e2)
    {
        edge1 = e1;
        edge2 = e2;
    }

    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        return obj instanceof Edge2Edge &&
                this.edge1.equals(((Edge2Edge)obj).edge1) &&
                this.edge2.equals(((Edge2Edge)obj).edge2);
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        int result = 17;
        result = 37*result+edge1.hashCode();
        result = 37*result+edge2.hashCode();
        return result;
    }
}







