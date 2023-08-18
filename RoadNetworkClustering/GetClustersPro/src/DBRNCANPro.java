import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Time;
import java.util.*;

public class DBRNCANPro {

    MBR  mapScale;              //地图边界经纬度
    double eps;                 //领域半径阈值
    int minPts;                 //密度阈值
    int minshare;               //最小共享点数
    int mincontain;                 //簇包含的最小数量

    HashMap<String,Edge> edgeList; //路网边集引用
    HashMap<String,Node> nodeList;  //路网结点集合引用
    HashMap<String,SnapObject> allObjects;  //输入的所有移动对象，String为移动对象ID
    HashMap<String,List<String>> coreObjectNeighbors;   //核心点列表，String为Object的ID，List是Object的邻域
    List<SnapObject> corePoints;   //核心点列表
    String edgeObjectIndexInPath;   //当前时刻边点表输入路径
    HashMap<String,List<String>> edgeObjectIndex;    //边点索引表，String为边的ID，list是边上移动对象的列表，偏移量的升序排列
    HashMap<String,Integer> nodesNeighborAmount;   //记录当前时刻下各节点邻域范围内的移动对象数
    HashMap<String,NeighborRange> nodesNR;  //指向每个顶点的邻域范围
    List<Clusters> finalClusters;//所有簇集合


    /**
     * 定义一些临时引用变量
     */
    String[] line;    //用于切分读入的每行数据
    SnapObject snapObject; //用于指向某个移动对象
    int amount;        //用于记录每条边上点的个数，便于读文件计数
    List<String> an_edgeObjects;    //用于指向一个边上的对象列表
    List<String> currObjectNeighbors;   //用于指向一个移动对象的邻域表
    GridIndex gridIndexcore;    //核心点网格索引

    HashMap<String, List<Onepair> > pairmap;   //存储所有的pq点对
    HashMap<Edge2Edge,List<String>> e2e_path;




    //构造函数
    public DBRNCANPro(
            //聚类参数
            MBR mapScale,double eps,int minPts,int minshare,int mincontain,
            //移动对象输入路径
            String objectsInpath,
            //路网边表、顶点表、顶点邻域范围表、当前时刻边点索引路径
            HashMap<String,Edge> edgeList,HashMap<String,Node> nodeList,HashMap<String,NeighborRange> nodesNR,String eoIndexInPath,
            //SNN比较阶段用到的表
            HashMap<String, List<Onepair>> pairmap,
            HashMap<Edge2Edge,List<String>> e2e_path
    ) throws IOException {
        allObjects = new HashMap<>();
        coreObjectNeighbors = new HashMap<>();
        corePoints = new ArrayList<>();
        edgeObjectIndex = new HashMap<>();
        nodesNeighborAmount = new HashMap<>();
        finalClusters = new ArrayList<>();

        this.mapScale = mapScale;
        this.eps = eps;
        this.minPts = minPts;
        this.minshare = minshare;
        this.mincontain = mincontain;
        readObjectsFromFile(objectsInpath);
        this.edgeList = edgeList;
        this.nodeList = nodeList;
        this.nodesNR = nodesNR;
        this.edgeObjectIndexInPath = eoIndexInPath;
        this.pairmap = pairmap;
        this.e2e_path = e2e_path;
    }

    /**
     * 主要功能函数，聚类，获得当前时刻下的所有簇
     * @return
     * @throws IOException
     */
    public List<Clusters> getClusters() throws IOException {
//        System.out.println("正在对移动对象进行聚类...     "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
        maskObjects();   //标记核心点
        clustering();   //对核心点进行聚类
//        System.out.println("聚类完成！时间戳"+corePoints.get(0).time+"下共有 "+finalClusters.size()+"个簇！   "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
        return finalClusters;
    }

    /**
     * 遍历所有移动对象，计算移动对象的邻域，生成核心点集HashMap<String,List<String>> coreObjectNeighbors
     * @throws IOException
     */
    public void maskObjects() throws IOException {
//        System.out.println("正在发现核心点...    "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
        readEOIndexFromFile(edgeObjectIndexInPath);   //读入该时刻下的边点索引

        Edge currEdge;
        int neiAmount_ep;
        double distOtoEp;
        for(Map.Entry<String,SnapObject> objectEntry:allObjects.entrySet()){   //遍历移动对象点，验证是否为核心点
            currEdge = edgeList.get(objectEntry.getValue().edgeid);   //获取移动对象所在边
            if(nodesNeighborAmount.get(currEdge.ep)==null){
                neiAmount_ep = calNodeNeiAmount(nodeList.get(currEdge.ep));  //计算移动对象所在边终点的邻域对象数
                nodesNeighborAmount.put(currEdge.ep,neiAmount_ep);
            }
//            System.out.println("当前移动对象的id为："+objectEntry.getValue().id+",当前边的id为："+currEdge.id);
            if(nodesNeighborAmount.get(currEdge.ep)+edgeObjectIndex.get(currEdge.id).size()>=minPts){   //仅考虑上界超过阈值的点
                distOtoEp = currEdge.length-objectEntry.getValue().pos;   //计算当前移动对象点到所在边终点的距离
                if(distOtoEp>=0) {
                    currObjectNeighbors = calObjectsNei(objectEntry.getValue(), nodesNR.get(currEdge.ep), distOtoEp);
                    if(currObjectNeighbors.size()>=minPts)   //满足核心点要求
                        coreObjectNeighbors.put(objectEntry.getKey(),currObjectNeighbors);  //放入核心点列表中
                }
            }
        }
//        System.out.println("核心点发现完成！当前时间戳下共有"+coreObjectNeighbors.size()+"个核心点！  "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
    }

    /**
     * 计算一个节点的邻域范围内的移动对象数
     * @param n
     * @return
     */
    public int calNodeNeiAmount(Node n){
        int amount = 0;
        NeighborRange nbr = nodesNR.get(n.id);
        for(int i=0;i<nbr.safeEdges.size();i++)
            if(edgeObjectIndex.get(nbr.safeEdges.get(i))!=null)
                amount += edgeObjectIndex.get(nbr.safeEdges.get(i)).size();
        List<String> currEdgeObjects;
        SnapObject currObject;
        for(int i=0;i<nbr.unsafeEdges.size();i++){
            currEdgeObjects = edgeObjectIndex.get(nbr.unsafeEdges.get(i));
            if(currEdgeObjects!=null)
                for(int j=0;j<currEdgeObjects.size();j++){
                    currObject = allObjects.get(currEdgeObjects.get(j));
                    if(currObject.pos<=nbr.unsafeLength.get(i))
                        amount++;
                    else
                        break;
                }
        }
        return amount;
    }

    /**
     * 根据边终点的邻域范围计算一个移动对象的邻域
     * @param o
     * @param epNR
     * @param distOtoEp
     * @return
     */
    public List<String> calObjectsNei(SnapObject o,NeighborRange epNR,double distOtoEp){
        List<String> neighbors = new ArrayList<>();

        /**
         *通过spNR计算当前移动对象的领域范围
         */
        NeighborRange oNR = new NeighborRange();
        for(int i=0;i<epNR.unsafeEdges.size();i++){    //计算oNR的非安全边
            if(epNR.unsafeLength.get(i)>distOtoEp){    //若缩进的区域比非安全区短，则非安全边可保留，否则不保留
                oNR.unsafeEdges.add(epNR.unsafeEdges.get(i));
                oNR.unsafeLength.add(epNR.unsafeLength.get(i)-distOtoEp);
            }
        }
        for(int i=0;i<epNR.safeEdges.size();i++){    //计算oNR的安全边
            if(epNR.safeLength.get(i)+distOtoEp<=eps){   //若到安全边的长度更新后仍在eps范围内，则保留
                oNR.safeEdges.add(epNR.safeEdges.get(i));
                oNR.safeLength.add(epNR.safeLength.get(i));
            }else if(epNR.safeLength.get(i)+distOtoEp-eps<edgeList.get(o.edgeid).length){  //多出来的部分短于安全边的长度，将安全边转化为非安全边，否则不保留
                oNR.unsafeEdges.add(epNR.safeEdges.get(i));
                oNR.unsafeLength.add(edgeList.get(o.edgeid).length-(epNR.safeLength.get(i)+distOtoEp-eps));
            }
        }

        /**
         * 遍历oNR中的边，获取上面的移动对象加入到返回列表中
         */
        for(int i=0;i<oNR.safeEdges.size();i++){   //安全边上的移动对象，无条件全部加入到邻域中
            an_edgeObjects = edgeObjectIndex.get(oNR.safeEdges.get(i));
            if(an_edgeObjects!=null)
                for(int j=0;j<an_edgeObjects.size();j++)
                    neighbors.add(an_edgeObjects.get(j));
        }
        for(int i=0;i<oNR.unsafeEdges.size();i++){   //非安全边上的移动对象，把在记录长度内的移动对象加入到邻域中
            an_edgeObjects = edgeObjectIndex.get(oNR.unsafeEdges.get(i));
            if(an_edgeObjects!=null)
                for(int j=0;j<an_edgeObjects.size();j++){
                    if(allObjects.get(an_edgeObjects.get(j)).pos<=oNR.unsafeLength.get(i))
                        neighbors.add(an_edgeObjects.get(j));
                    else
                        break;
                }
        }
        for(int i=0;i<edgeObjectIndex.get(o.edgeid).size();i++) {   //同一条边上的移动对象，把比点o偏移量更大的点加入到邻域中
            if(allObjects.get(edgeObjectIndex.get(o.edgeid).get(i)).pos>o.pos)
                neighbors.add(edgeObjectIndex.get(o.edgeid).get(i));
        }

        return neighbors;
    }

    /**
     * 根据文件路径读取一个时间戳下的移动对象到HashMap<String,SnapObject> allObjects中
     * @param inPath
     * @throws IOException
     */
    public void readObjectsFromFile(String inPath) throws IOException {
        BufferedReader br=new BufferedReader(new FileReader(inPath));
//        System.out.println();
//        System.out.println("正在读轨迹点数据文件...  "+new Date(System.currentTimeMillis()));
        String readLine=br.readLine();
        while(readLine!=null) {
            line = readLine.split(",");
            snapObject = new SnapObject();
            snapObject.id = line[0];
            snapObject.lng = Double.parseDouble(line[1]);
            snapObject.lat = Double.parseDouble(line[2]);
            snapObject.edgeid = line[3];
            snapObject.pos = Double.parseDouble(line[4]);
            snapObject.time = line[5];
            allObjects.put(line[0],snapObject);    //读入一个移动对象点，放入列表中
            readLine = br.readLine();
        }
//        System.out.println("轨迹点数据文件读取完成！  "+new Date(System.currentTimeMillis()));
    }

    /**
     * 根据文件路径读入一个时间戳下的边点索引HashMap<String,List<String>> edgeObjectIndex = new HashMap<>();
     * @param eoInpath
     * @throws IOException
     */
    public void readEOIndexFromFile(String eoInpath) throws IOException {
        BufferedReader br=new BufferedReader(new FileReader(eoInpath));
        String readLine=br.readLine();
        while(readLine!=null) {
            line = readLine.split(",");
            amount = Integer.parseInt(line[1]);
            an_edgeObjects = new ArrayList<>();
            for(int i=0;i<amount;i++)
                an_edgeObjects.add(line[2*(i+1)]);
            edgeObjectIndex.put(line[0],an_edgeObjects);
            readLine = br.readLine();
        }
    }

    /**
     * 由HashMap<String,List<String>> coreObjectNeighbors转化为List<SnapObject> corePoints
     */
    public void getCorePoint(){
        SnapObject coreObject;
        for(Map.Entry<String,List<String>> coreEntry:coreObjectNeighbors.entrySet()){
            coreObject = allObjects.get(coreEntry.getKey());
            corePoints.add(coreObject);
        }
    }

    /**
     * 对核心点进行聚类，得到簇集List<Clusters> finalClusters
     * @throws IOException
     */
    public void clustering() throws IOException{
//        System.out.println("开始构建核心点网格索引...  "+new Date(System.currentTimeMillis()));
        getCorePoint();
        gridIndexcore = new GridIndex(mapScale,2*eps,corePoints); //构建核心点网格索引
//        System.out.println("核心点网格索引构建完成！  "+new Date(System.currentTimeMillis()));
        Set<String> ids=gridIndexcore.pointsHash.keySet();   //获取网格中核心点的ID
        Queue<SnapObject> queue=new LinkedList<SnapObject>();   //核心点队列
        List<Onepair> pairlist;//=new ArrayList<Onepair>();   //核心点对列表

        /**
         *定义程序中可重复使用的变量
         */
        List<SnapObject> psc;      //存储核心点附近的核心点
        List<SnapObject> points;
        SnapObject p,head;
        Onepair onepair;      //一对核心点对
        Edge2Edge e2e;
        List<String> pathlist;
        List<String> pathpointslist;
        String Sedge;    //移动对象起点所在的边的id
        String Eedge;    //移动对象终点所在的边的id

        int allNeighborCoreAmount = 0;    //所有核心点的所有邻近核心点总数
        int omitCalNCAmount = 0;          //性质一省略计算的核心点数

//        System.out.println("正在进行SNN比较...   "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
        int num=0;
        while(!ids.isEmpty()){
            for(String id:ids){
                p = gridIndexcore.pointsHash.get(id);    //获取一个核心点p
                points = new ArrayList<SnapObject>();
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
                    if(pairlist==null){     //如果pairmap中没有p的pq对表，则初始化一个空的链表
                        pairlist = new ArrayList<>();
                        pairmap.put(head.id,pairlist);
                    }
                    pairlist=pairmap.get(head.id);
                    for(int i=0;i<pairlist.size();i++){   //遍历每一对
                        onepair=pairlist.get(i);
                        //首先判断是否在head的核心点邻域内
                        if(flag.containsKey(onepair.qid)){  //说明qid在psc里面
                            if(numofoverlp(coreObjectNeighbors.get(head.id),coreObjectNeighbors.get(onepair.qid)) >=minshare){  //判断核心点对的SNN是否满足阈值条件
                                SnapObject tempp=allObjects.get(onepair.qid);
                                flag.put(tempp.id, true);
                                queue.add(tempp);
                                points.add(tempp);
                                gridIndexcore.delete(tempp);
//                                if(compMeterDistance(head,tempp)<=eps){  //判断tempp是否在head的ε邻域中，在则满足性质一
                                if(coreObjectNeighbors.get(head.id).contains(tempp)){
                                    e2e=new Edge2Edge(head.edgeid,tempp.edgeid);
                                    if(!e2e_path.containsKey(e2e)) {
                                        calE2EPath(head,tempp);
                                    }
                                    pathlist=e2e_path.get(e2e);
                                    Sedge=head.edgeid;
                                    Eedge=tempp.edgeid;
                                    //得到这些路径上在两点之间的点，加入到簇中
                                    pathpointslist=edgeObjectIndex.get(Sedge);
                                    for(int m=0;m<pathpointslist.size();m++){
                                        if(allObjects.get(pathpointslist.get(m)).pos>head.pos&&flag.containsKey(pathpointslist.get(m))&&points.contains(pathpointslist.get(m))==false){
                                            omitCalNCAmount++;
                                            flag.put(pathpointslist.get(m), true);
                                            queue.add(allObjects.get(pathpointslist.get(m)));
                                            points.add(allObjects.get(pathpointslist.get(m)));
                                            gridIndexcore.delete(allObjects.get(pathpointslist.get(m)));
                                        }
                                    }
                                    pathpointslist=edgeObjectIndex.get(Eedge);
                                    for(int m=0;m<pathpointslist.size();m++){
                                        if(allObjects.get(pathpointslist.get(m)).pos<tempp.pos&&flag.containsKey(pathpointslist.get(m))&&!points.contains(pathpointslist.get(m))){
                                            omitCalNCAmount++;
                                            flag.put(pathpointslist.get(m), true);
                                            points.add(allObjects.get(pathpointslist.get(m)));
                                            queue.add(allObjects.get(pathpointslist.get(m)));
                                            gridIndexcore.delete(allObjects.get(pathpointslist.get(m)));
                                        }
                                    }

                                    for(int h=0;h<pathlist.size();h++){
                                        if(edgeObjectIndex.containsKey(pathlist.get(h))){
                                            pathpointslist=edgeObjectIndex.get(pathlist.get(h));
                                            for(int m=0;m<pathpointslist.size();m++){
                                                if(!points.contains(allObjects.get(pathpointslist.get(m)))&&flag.containsKey(pathpointslist.get(m))){
                                                    omitCalNCAmount++;
                                                    flag.put(pathpointslist.get(m), true);
                                                    points.add(allObjects.get(pathpointslist.get(m)));
                                                    queue.add(allObjects.get(pathpointslist.get(m)));
                                                    gridIndexcore.delete(allObjects.get(pathpointslist.get(m)));
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
                            List<String> templist=new ArrayList<>();
                            if(numofoverlp(coreObjectNeighbors.get(head.id),coreObjectNeighbors.get(psc.get(i).id),templist) >=minshare) {
                                queue.add(psc.get(i));
                                points.add(psc.get(i));
                                gridIndexcore.delete(psc.get(i));

//                                if(compMeterDistance(head,psc.get(i))<=eps){
                                if(coreObjectNeighbors.get(head.id).contains(psc.get(i).id)){
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
//                                        System.out.println("当前核心对象的pq对有"+templist.size()+"对！");
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
                if(points.size()>=mincontain){
                    this.finalClusters.add(new Clusters(num+"", points.get(0).time, points));
                    num++;
                }
                ids=gridIndexcore.pointsHash.keySet();
                break;
            }
        }
//        System.out.println("SNN比较完成！总的需要进行SNN判断的核心点对有"+allNeighborCoreAmount+"对，采用性质一省略"+omitCalNCAmount+"对的计算！"+ new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
//        System.out.println("SNN比较完成！   "+ new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
    }

    /**
     * 计算两个list数组之间重合的点的个数
     */
    private int numofoverlp(List<String> a,List<String> b){
        int num=0;
        for(String i:a){
            if(b.contains(i)){
                num++;
            }
        }
        return num;
    }

    /**
     * 计算a，b两个list数组之间重合的点的个数，并储存在c中
     */
    private int numofoverlp(List<String> a,List<String> b,List<String> c){
        int num=0;
        for(String i:a){
            if(b.contains(i)){
                num++;
                c.add(i);
            }
        }
        return num;
    }

    /**
     * 计算两条边间的最短路径加入到HashMap<Edge2Edge,List<String>> e2e_path中
     */
    private void calE2EPath(SnapObject so1,SnapObject so2){
        List<String> _path = new ArrayList<>();
        String ne1 = edgeList.get(so1.edgeid).ep;
        List<String> p_eEdges;
        String p;
        if(so1.edgeid.equals(so2.edgeid)){
            _path.add(so1.edgeid);
            e2e_path.put(new Edge2Edge(so1.edgeid,so2.edgeid),_path);
        }else{
            for(int i=0;i<_path.size();i++){
                p = edgeList.get(_path.get(i)).sp;
                p_eEdges = nodeList.get(p).eEdges;
                for(int j=0;j<p_eEdges.size();j++){
                    if(nodesNR.get(ne1).safeEdges.contains(p_eEdges.get(j)))
                        _path.add(p_eEdges.get(j));
                }
            }
            _path.remove(0);
            e2e_path.put(new Edge2Edge(so1.edgeid,so2.edgeid),_path);
        }
    }

    /**
     * 析构函数
     */
    public void clear(){
        allObjects.clear();
        coreObjectNeighbors.clear();
        corePoints.clear();
        edgeObjectIndex.clear();
        nodesNeighborAmount.clear();
        finalClusters.clear();
    }

}

//表示p点对应的一个(p,q)对
class Onepair{
    String qid;
    List<String> sharelist;//=new ArrayList<SnapObject>();
    Onepair(){
        sharelist=new ArrayList<String>();
    }
    Onepair(String id,List<String> list){
        sharelist=new ArrayList<String>();
        this.qid=id;
        this.sharelist=list;
    }

    void setqid(String id){
        this.qid=id;
    }

    void setlist(List<String> a){
        sharelist.clear();
        for(int i=0;i<a.size();i++){
            sharelist.add(a.get(i));
        }
    }


    @Override
    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        return obj instanceof Onepair &&
                this.qid.equals(((Onepair)obj).qid);
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        int result = 17;
        result = 37*result+qid.hashCode();
        return result;
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
