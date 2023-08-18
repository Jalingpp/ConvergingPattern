import traj.util.Cluster;
import traj.util.SpatialObject;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.*;

/*
 * ·������
 * */

public class NewDBSCAN {

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


    /*
     * ���ຯ��
     * ����finalClusters���������д�
     */


    public List<Clusters> getClusters() throws IOException{
        //��ʼ���ؼ���
        finalClusters = new ArrayList<Clusters>();

        //�����е���ϱ��,��ǳ���Щ���Ǻ��ĵ�
        System.out.println("���ڱ�Ǻ��ĵ�...  "+new Date(System.currentTimeMillis()));
        markPoints();
        System.out.println("���ĵ�����ɣ���"+corePoints.size()+"�����ĵ㣡  "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());

        baselinecluster();
//        onlinecluster();


        return finalClusters;

    }

    /**
     * ��ǳ����к��ĵ�
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
     *���Ա�ǵ����㸽��������
     * @throws IOException
     */
    private boolean markPoint(SnapPoint p) throws IOException{
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



    public void onlinecluster() throws IOException{
        System.out.println("��ʼ�������ĵ���������...  "+new Date(System.currentTimeMillis()));
        gridIndexcore = new GridIndex(mapScale,2*eps,corePoints); //�������ĵ���������
        System.out.println("���ĵ���������������ɣ�  "+new Date(System.currentTimeMillis()));
        Set<String> ids=gridIndexcore.pointsHash.keySet();   //��ȡ�����к��ĵ��ID
        Queue<SnapPoint> queue=new LinkedList<SnapPoint>();   //���ĵ����
        List<Onepair> pairlist;//=new ArrayList<Onepair>();   //���ĵ���б�

        /**
         *��������п��ظ�ʹ�õı���
         */
        List<SnapPoint> psc;      //�洢���ĵ㸽���ĺ��ĵ�
        List<SnapPoint> points;
        SnapPoint p,head;
        Onepair onepair;      //һ�Ժ��ĵ��
        Edge2Edge e2e;
        List<String> pathlist;
        List<SnapPoint> pathpointslist;
        String Sedge;    //�ƶ�����������ڵıߵ�id
        String Eedge;    //�ƶ������յ����ڵıߵ�id

        int allNeighborCoreAmount = 0;    //���к��ĵ�������ڽ����ĵ�����
        int omitCalNCAmount = 0;          //����һʡ�Լ���ĺ��ĵ���

        int num=0;
        while(!ids.isEmpty()){
            for(String id:ids){
                p = gridIndexcore.pointsHash.get(id);    //��ȡһ�����ĵ�p
                points = new ArrayList<SnapPoint>();
                queue.add(p);      //��p���뵽������
                points.add(p);     //
                gridIndexcore.delete(p);
                while(!queue.isEmpty()){
                    head=queue.poll();   //ȡ������Ԫ��
                    psc = gridIndexcore.getPointsFromGrids(head,1);  //�õ����ĵ�head�������ڵĺ��ĵ�
                    allNeighborCoreAmount += psc.size();
                    HashMap<String,Boolean> flag=new HashMap<String,Boolean>();   //��¼
                    for(int i=0;i<psc.size();i++){
                        flag.put(psc.get(i).id, false);
                    }
                    //��������һ���л��ִ�
//                    System.out.println("����ʹ������һ�ж��ڽ����ĵ��Ƿ����"+head.id+"ͬ��...  "+ new Date(System.currentTimeMillis()));
                    pairlist=pairmap.get(head.id);       //��ȡ��headΪp��pq��
                    for(int i=0;i<pairlist.size();i++){   //����ÿһ��
                        onepair=pairlist.get(i);
                        //�����ж��Ƿ���head�ĺ��ĵ�������
                        if(flag.containsKey(onepair.qid)){  //˵��qid��psc����
                            if(numofoverlp(nearbynode.get(head.id),nearbynode.get(onepair.qid)) >=minshare){  //�жϺ��ĵ�Ե�SNN�Ƿ�������ֵ����
                                SnapPoint tempp=pointsid_p.get(onepair.qid);
                                flag.put(tempp.id, true);
                                queue.add(tempp);
                                points.add(tempp);
                                gridIndexcore.delete(tempp);
//                                if(compMeterDistance(head,tempp)<=eps){  //�ж�tempp�Ƿ���head�Ħ������У�������������һ
                                if(nearbynode.get(head.id).contains(tempp)){
                                    e2e=new Edge2Edge(head.getEdgeid(),tempp.getEdgeid());
                                    if(e2e_path.containsKey(e2e)){
                                        pathlist=e2e_path.get(e2e);
                                        Sedge=head.getEdgeid();
                                        Eedge=tempp.getEdgeid();
                                        //�õ���Щ·����������֮��ĵ㣬���뵽����
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
//                    System.out.println("�ж���ϣ�  "+ new Date(System.currentTimeMillis()));

//                    System.out.println("�����ж�ʣ����ڽ����ĵ��Ƿ����"+head.id+"ͬ��...  "+ new Date(System.currentTimeMillis()));
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
                                        if(onepair.sharelist.containsAll(templist)){  //���head��psc[i]����ͨ�㽻����sharelist���Ӽ�
                                            temppairlist.remove(onepair);
                                            onepair=new Onepair(psc.get(i).id,templist);
                                            if(!temppairlist.contains(onepair)){
                                                temppairlist.add(onepair);
                                            }

                                        }else if(templist.containsAll(onepair.sharelist)){
                                            if(!temppairlist.contains(onepair)){
                                                temppairlist.add(onepair);
                                            }
                                            //�����κβ���
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
//                    System.out.println("�ж���ϣ�  "+ new Date(System.currentTimeMillis()));
                }
                if(points.size()>=mincon){
                    this.finalClusters.add(new Clusters(num+"", points.get(0).getTime(), points));
                    num++;
                }
                ids=gridIndexcore.pointsHash.keySet();
                break;
            }
        }

        System.out.println("�ܵ���Ҫ����SNN�жϵĺ��ĵ����"+allNeighborCoreAmount+"�ԣ���������һʡ��"+omitCalNCAmount+"�Եļ��㣡"+ new Date(System.currentTimeMillis()));
    }






    /*
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


    /*
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







