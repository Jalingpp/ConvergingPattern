import traj.util.Cluster;
import traj.util.SpatialObject;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.*;

/*
 * 路网聚类
 * */

public class NewDBSCAN {

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


    public NewDBSCAN(List<SnapPoint> points,
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


    /*
     * 聚类函数
     * 返回finalClusters，包含所有簇
     */


    public List<Clusters> getClusters() throws IOException{
        //初始化簇集合
        finalClusters = new ArrayList<Clusters>();

        //给所有点打上标记,标记出哪些点是核心点
        System.out.println("正在标记核心点...  "+new Date(System.currentTimeMillis()));
        markPoints();
        System.out.println("核心点标记完成！共"+corePoints.size()+"个核心点！  "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());

        baselinecluster();
//        onlinecluster();


        return finalClusters;

    }

    /**
     * 标记出所有核心点
     * @throws IOException
     */
    private void markPoints() throws IOException{
        corePoints = new ArrayList<SnapPoint>();
        for(int i=0;i<procPoints.size();i++){
            if(markPoint(procPoints.get(i))){
                corePoints.add(procPoints.get(i));
            }
        }
    }

    /**
     *尝试标记单个点附近的区域
     * @throws IOException
     */
    private boolean markPoint(SnapPoint p) throws IOException{
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



    public void onlinecluster() throws IOException{
        System.out.println("开始构建核心点网格索引...  "+new Date(System.currentTimeMillis()));
        gridIndexcore = new GridIndex(mapScale,2*eps,corePoints); //构建核心点网格索引
        System.out.println("核心点网格索引构建完成！  "+new Date(System.currentTimeMillis()));
        Set<String> ids=gridIndexcore.pointsHash.keySet();   //获取网格中核心点的ID
        Queue<SnapPoint> queue=new LinkedList<SnapPoint>();   //核心点队列
        List<Onepair> pairlist;//=new ArrayList<Onepair>();   //核心点对列表

        /**
         *定义程序中可重复使用的变量
         */
        List<SnapPoint> psc;      //存储核心点附近的核心点
        List<SnapPoint> points;
        SnapPoint p,head;
        Onepair onepair;      //一对核心点对
        Edge2Edge e2e;
        List<String> pathlist;
        List<SnapPoint> pathpointslist;
        String Sedge;    //移动对象起点所在的边的id
        String Eedge;    //移动对象终点所在的边的id

        int allNeighborCoreAmount = 0;    //所有核心点的所有邻近核心点总数
        int omitCalNCAmount = 0;          //性质一省略计算的核心点数

        int num=0;
        while(!ids.isEmpty()){
            for(String id:ids){
                p = gridIndexcore.pointsHash.get(id);    //获取一个核心点p
                points = new ArrayList<SnapPoint>();
                queue.add(p);      //将p加入到队列中
                points.add(p);     //
                gridIndexcore.delete(p);
                while(!queue.isEmpty()){
                    head=queue.poll();   //取出队首元素
                    psc = gridIndexcore.getPointsFromGrids(head,1);  //得到核心点head的领域内的核心点
                    allNeighborCoreAmount += psc.size();
                    HashMap<String,Boolean> flag=new HashMap<String,Boolean>();   //记录
                    for(int i=0;i<psc.size();i++){
                        flag.put(psc.get(i).id, false);
                    }
                    //采用性质一进行划分簇
//                    System.out.println("正在使用性质一判断邻近核心点是否与点"+head.id+"同簇...  "+ new Date(System.currentTimeMillis()));
                    pairlist=pairmap.get(head.id);       //获取以head为p的pq对
                    for(int i=0;i<pairlist.size();i++){   //遍历每一对
                        onepair=pairlist.get(i);
                        //首先判断是否在head的核心点邻域内
                        if(flag.containsKey(onepair.qid)){  //说明qid在psc里面
                            if(numofoverlp(nearbynode.get(head.id),nearbynode.get(onepair.qid)) >=minshare){  //判断核心点对的SNN是否满足阈值条件
                                SnapPoint tempp=pointsid_p.get(onepair.qid);
                                flag.put(tempp.id, true);
                                queue.add(tempp);
                                points.add(tempp);
                                gridIndexcore.delete(tempp);
//                                if(compMeterDistance(head,tempp)<=eps){  //判断tempp是否在head的ε邻域中，在则满足性质一
                                if(nearbynode.get(head.id).contains(tempp)){
                                    e2e=new Edge2Edge(head.getEdgeid(),tempp.getEdgeid());
                                    if(e2e_path.containsKey(e2e)){
                                        pathlist=e2e_path.get(e2e);
                                        Sedge=head.getEdgeid();
                                        Eedge=tempp.getEdgeid();
                                        //得到这些路径上在两点之间的点，加入到簇中
                                        pathpointslist=edge_point.get(Sedge);
                                        for(int m=0;m<pathpointslist.size();m++){
                                            if(pathpointslist.get(m).getPos()>head.getPos()&&flag.containsKey(pathpointslist.get(m).id)&&points.contains(pathpointslist.get(m))==false){
                                                flag.put(pathpointslist.get(m).id, true);
                                                queue.add(pathpointslist.get(m));
                                                points.add(pathpointslist.get(m));
                                                gridIndexcore.delete(pathpointslist.get(m));
                                            }
                                        }
                                        pathpointslist=edge_point.get(Eedge);
                                        for(int m=0;m<pathpointslist.size();m++){
                                            if(pathpointslist.get(m).getPos()<tempp.getPos()&&flag.containsKey(pathpointslist.get(m).id)&&!points.contains(pathpointslist.get(m))){
                                                flag.put(pathpointslist.get(m).id, true);
                                                points.add(pathpointslist.get(m));
                                                queue.add(pathpointslist.get(m));
                                                gridIndexcore.delete(pathpointslist.get(m));
                                            }
                                        }

                                        for(int h=0;h<pathlist.size();h++){
                                            if(edge_point.containsKey(pathlist.get(h))){
                                                pathpointslist=edge_point.get(pathlist.get(h));
                                                for(int m=0;m<pathpointslist.size();m++){
                                                    if(!points.contains(pathpointslist.get(m))&&flag.containsKey(pathpointslist.get(m).id)){
                                                        omitCalNCAmount++;
                                                        flag.put(pathpointslist.get(m).id, true);
                                                        points.add(pathpointslist.get(m));
                                                        queue.add(pathpointslist.get(m));
                                                        gridIndexcore.delete(pathpointslist.get(m));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }else{
                                    pairlist.remove(i);
                                }
                            }else{
                                pairlist.remove(i);
                            }
                        }else{
                            pairlist.remove(i);
                        }
                    }
//                    System.out.println("判断完毕！  "+ new Date(System.currentTimeMillis()));

//                    System.out.println("正在判断剩余的邻近核心点是否与点"+head.id+"同簇...  "+ new Date(System.currentTimeMillis()));
                    for(int i=0;i<psc.size();i++){
                        if(flag.get(psc.get(i).id)==false){
                            List<SnapPoint> templist=new ArrayList<SnapPoint>();
                            if(numofoverlp(nearbynode.get(head.id),nearbynode.get(psc.get(i).id),templist) >=minshare)
                            {
                                queue.add(psc.get(i));
                                points.add(psc.get(i));
                                gridIndexcore.delete(psc.get(i));

//                                if(compMeterDistance(head,psc.get(i))<=eps){
                                if(nearbynode.get(head.id).contains(psc.get(i))){
                                    List<Onepair> temppairlist=new ArrayList<Onepair>();
                                    for(int j=0;j<pairlist.size();j++){
                                        onepair=pairlist.get(j);
                                        if(onepair.sharelist.containsAll(templist)){  //如果head和psc[i]的普通点交集是sharelist的子集
                                            temppairlist.remove(onepair);
                                            onepair=new Onepair(psc.get(i).id,templist);
                                            if(!temppairlist.contains(onepair)){
                                                temppairlist.add(onepair);
                                            }

                                        }else if(templist.containsAll(onepair.sharelist)){
                                            if(!temppairlist.contains(onepair)){
                                                temppairlist.add(onepair);
                                            }
                                            //不做任何操作
                                        }else{
                                            onepair=new Onepair(psc.get(i).id,templist);
                                            if(!temppairlist.contains(onepair)){
                                                temppairlist.add(onepair);
                                            }
                                        }
                                    }

                                    if(pairlist.size()==0){
                                        onepair=new Onepair(psc.get(i).id,templist);
                                        pairlist.add(onepair);
                                    }
                                    else{
                                        pairmap.remove(head.id);
                                        pairmap.put(head.id, temppairlist);
                                    }
                                }
                            }
                        }
                    }
//                    System.out.println("判断完毕！  "+ new Date(System.currentTimeMillis()));
                }
                if(points.size()>=mincon){
                    this.finalClusters.add(new Clusters(num+"", points.get(0).getTime(), points));
                    num++;
                }
                ids=gridIndexcore.pointsHash.keySet();
                break;
            }
        }

        System.out.println("总的需要进行SNN判断的核心点对有"+allNeighborCoreAmount+"对，采用性质一省略"+omitCalNCAmount+"对的计算！"+ new Date(System.currentTimeMillis()));
    }






    /*
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


    /*
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
    /*
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


//class Edge2Edge {
//    String edge1;
//    String edge2;
//
//    Edge2Edge(String e1, String e2)
//    {
//        edge1 = e1;
//        edge2 = e2;
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        // TODO Auto-generated method stub
//        return obj instanceof Edge2Edge &&
//                this.edge1.equals(((Edge2Edge)obj).edge1) &&
//                this.edge2.equals(((Edge2Edge)obj).edge2);
//    }
//
//    @Override
//    public int hashCode() {
//        // TODO Auto-generated method stub
//        int result = 17;
//        result = 37*result+edge1.hashCode();
//        result = 37*result+edge2.hashCode();
//        return result;
//    }
//}







