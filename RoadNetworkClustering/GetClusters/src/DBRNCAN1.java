import traj.util.Cluster;
import traj.util.SpatialObject;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.*;

/*
 * 路网聚类
 * */

public class DBRNCAN1 {

    private MBR  mapScale;              //地图边界经纬度
    private double eps;                 //领域半径阈值
    private int minPts;                 //密度阈值
    private int minshare;               //最小共享点数
    private int mincon;                 //簇包含的最小数量
    private int numcluster;             //簇的个数

    private List<SnapPoint> procPoints; //所有移动点的集合
    private List<SnapPoint> corePoints; //核心点的集合
    private List<SnapPoint> nodepoints; //路网Nodes集合
    private GridIndex gridIndex;        //所有点网格索引
    private GridIndex gridIndexcore;    //核心点网格索引
    private List<Clusters> finalClusters;//所有簇集合
    HashMap<String, List<Onepair> > pairmap;
    HashMap<String,SnapPoint> nodesid_p;
    HashMap<String, ArrayList<SnapPoint>> edge_point;
    HashMap<Edge2Edge, Double> e2e_dist;
    HashMap<Edge2Edge,List<String>> e2e_path;
    HashMap<String,SnapPoint> pointsid_p;
    HashMap<String, Adj_Node> edges;


    Getdist new_g;                     //定义计算两点之间距离的类

    HashMap<String,List<SnapPoint>> nearbynode=new HashMap<String,List<SnapPoint>>();  //映射  点id-->这个点邻域内的id
    HashMap<String,Clusters> findaddCluster=new HashMap<String,Clusters>();            //映射  点id-->这个点所在的簇


    /*
     * 构造函数
     * points 所有点的集合
     * eps    距离阈值
     * minPts 密度阈值
     * minshare 共享可达近邻的数量阈值
     * mincon 单个簇的数量阈值
     * mapscele 地图的边界范围
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

        this.mapScale = mapscale;//两个类MBR和GridIndex
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

        //初始化计算距离的对象
        new_g=new Getdist(mapScale, eps, _gra, _edg, _map, _nodesid_p, _vex, _nodepoints, _gridIndexnode);

        //初始化普通点网格
        gridIndex = new GridIndex(mapScale,eps,procPoints);

    }


    /**第一个聚类算法
     * 返回finalClusters，包含所有簇
     */
    public List<Clusters> getClusters() throws IOException{
        //初始化簇集合
        finalClusters = new ArrayList<Clusters>();

        //给所有点打上标记,标记出哪些点是核心点
//        System.out.println("正在标记核心点...  "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
        markPoints1();
//        System.out.println("核心点标记完成！共"+corePoints.size()+"个核心点！  "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());

//        System.out.println("正在进行SNN比较...  "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
        baselinecluster();
//        System.out.println("SNN比较完成！       "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());


        return finalClusters;

    }

    /**
     * 标记出所有核心点
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
     *尝试标记单个点附近的区域
     * @throws IOException
     */
    private boolean markPoint1(SnapPoint p) throws IOException{
        List<SnapPoint> pointsNearCandi =  gridIndex.getPointsFromGrids(p,1);

        pointsNearCandi = getPointNearBy(p,pointsNearCandi);

        if(pointsNearCandi.size()>=minPts){
            nearbynode.put(p.id, pointsNearCandi);     //映射  点id-->这个点邻域内的id
            p.setMark(PointMark.CORE);
            return true;
        }

        //      edge_point.remove(edges.get(p.id));

        return false;
    }

    /**
     * 获得临近点(半径为领域密度)
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
     * 计算两个点之间的最短距离
     * @param pa
     * @param pb
     * @return
     * @throws IOException
     */
    protected double compMeterDistance(SnapPoint pa, SnapPoint pb) throws IOException {
        double EARTH_RADIUS = 6378137.0D;

        //移动对象所在边的id
        String edgeid1 = pa.getEdgeid();
        String edgeid2 = pb.getEdgeid();

        if (edgeid1.equals(edgeid2) && pa.getPos() <= pb.getPos())
        {
            return pb.getPos() - pa.getPos();
        }

        //所在边的长度
        double len1 = new_g.getlen(edgeid1);

        //pa到其所在边终点的距离
        //     double pa2ep=edges.get(edgeid1).length-pa.getPos();
        double pa2ep = pa.getPos();

        //pb所在边起点到pb的距离
        double sp2pb = pb.getPos();

        //pa所在边终点的id
        String epid = new_g.getepid(edgeid1);
        //pb所在边起点的id
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
     * 第一个聚类算法的SNN比较算法
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
                points = new ArrayList<SnapPoint>();   //储存簇中元素的列表
                queue.add(p);
                points.add(p);
                gridIndexcore.delete(p);
                while(!queue.isEmpty()){
                    head=queue.poll();   //取出队首元素
                    psc = gridIndexcore.getPointsFromGrids(head,1);  //得到核心点head的领域内的核心点
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
     * 计算两个list数组之间重合的点的个数
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
     * 计算a，b两个list数组之间重合的点的个数，并储存在c中
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
     * 由时间字符串转化为文件名，如000000.txt
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
     * 释放掉一部分的空间
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







