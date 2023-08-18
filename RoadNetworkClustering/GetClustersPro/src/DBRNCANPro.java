import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Time;
import java.util.*;

public class DBRNCANPro {

    MBR  mapScale;              //��ͼ�߽羭γ��
    double eps;                 //����뾶��ֵ
    int minPts;                 //�ܶ���ֵ
    int minshare;               //��С�������
    int mincontain;                 //�ذ�������С����

    HashMap<String,Edge> edgeList; //·���߼�����
    HashMap<String,Node> nodeList;  //·����㼯������
    HashMap<String,SnapObject> allObjects;  //����������ƶ�����StringΪ�ƶ�����ID
    HashMap<String,List<String>> coreObjectNeighbors;   //���ĵ��б�StringΪObject��ID��List��Object������
    List<SnapObject> corePoints;   //���ĵ��б�
    String edgeObjectIndexInPath;   //��ǰʱ�̱ߵ������·��
    HashMap<String,List<String>> edgeObjectIndex;    //�ߵ�������StringΪ�ߵ�ID��list�Ǳ����ƶ�������б�ƫ��������������
    HashMap<String,Integer> nodesNeighborAmount;   //��¼��ǰʱ���¸��ڵ�����Χ�ڵ��ƶ�������
    HashMap<String,NeighborRange> nodesNR;  //ָ��ÿ�����������Χ
    List<Clusters> finalClusters;//���дؼ���


    /**
     * ����һЩ��ʱ���ñ���
     */
    String[] line;    //�����зֶ����ÿ������
    SnapObject snapObject; //����ָ��ĳ���ƶ�����
    int amount;        //���ڼ�¼ÿ�����ϵ�ĸ��������ڶ��ļ�����
    List<String> an_edgeObjects;    //����ָ��һ�����ϵĶ����б�
    List<String> currObjectNeighbors;   //����ָ��һ���ƶ�����������
    GridIndex gridIndexcore;    //���ĵ���������

    HashMap<String, List<Onepair> > pairmap;   //�洢���е�pq���
    HashMap<Edge2Edge,List<String>> e2e_path;




    //���캯��
    public DBRNCANPro(
            //�������
            MBR mapScale,double eps,int minPts,int minshare,int mincontain,
            //�ƶ���������·��
            String objectsInpath,
            //·���߱��������������Χ����ǰʱ�̱ߵ�����·��
            HashMap<String,Edge> edgeList,HashMap<String,Node> nodeList,HashMap<String,NeighborRange> nodesNR,String eoIndexInPath,
            //SNN�ȽϽ׶��õ��ı�
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
     * ��Ҫ���ܺ��������࣬��õ�ǰʱ���µ����д�
     * @return
     * @throws IOException
     */
    public List<Clusters> getClusters() throws IOException {
//        System.out.println("���ڶ��ƶ�������о���...     "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
        maskObjects();   //��Ǻ��ĵ�
        clustering();   //�Ժ��ĵ���о���
//        System.out.println("������ɣ�ʱ���"+corePoints.get(0).time+"�¹��� "+finalClusters.size()+"���أ�   "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
        return finalClusters;
    }

    /**
     * ���������ƶ����󣬼����ƶ�������������ɺ��ĵ㼯HashMap<String,List<String>> coreObjectNeighbors
     * @throws IOException
     */
    public void maskObjects() throws IOException {
//        System.out.println("���ڷ��ֺ��ĵ�...    "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
        readEOIndexFromFile(edgeObjectIndexInPath);   //�����ʱ���µıߵ�����

        Edge currEdge;
        int neiAmount_ep;
        double distOtoEp;
        for(Map.Entry<String,SnapObject> objectEntry:allObjects.entrySet()){   //�����ƶ�����㣬��֤�Ƿ�Ϊ���ĵ�
            currEdge = edgeList.get(objectEntry.getValue().edgeid);   //��ȡ�ƶ��������ڱ�
            if(nodesNeighborAmount.get(currEdge.ep)==null){
                neiAmount_ep = calNodeNeiAmount(nodeList.get(currEdge.ep));  //�����ƶ��������ڱ��յ�����������
                nodesNeighborAmount.put(currEdge.ep,neiAmount_ep);
            }
//            System.out.println("��ǰ�ƶ������idΪ��"+objectEntry.getValue().id+",��ǰ�ߵ�idΪ��"+currEdge.id);
            if(nodesNeighborAmount.get(currEdge.ep)+edgeObjectIndex.get(currEdge.id).size()>=minPts){   //�������Ͻ糬����ֵ�ĵ�
                distOtoEp = currEdge.length-objectEntry.getValue().pos;   //���㵱ǰ�ƶ�����㵽���ڱ��յ�ľ���
                if(distOtoEp>=0) {
                    currObjectNeighbors = calObjectsNei(objectEntry.getValue(), nodesNR.get(currEdge.ep), distOtoEp);
                    if(currObjectNeighbors.size()>=minPts)   //������ĵ�Ҫ��
                        coreObjectNeighbors.put(objectEntry.getKey(),currObjectNeighbors);  //������ĵ��б���
                }
            }
        }
//        System.out.println("���ĵ㷢����ɣ���ǰʱ����¹���"+coreObjectNeighbors.size()+"�����ĵ㣡  "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
    }

    /**
     * ����һ���ڵ������Χ�ڵ��ƶ�������
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
     * ���ݱ��յ������Χ����һ���ƶ����������
     * @param o
     * @param epNR
     * @param distOtoEp
     * @return
     */
    public List<String> calObjectsNei(SnapObject o,NeighborRange epNR,double distOtoEp){
        List<String> neighbors = new ArrayList<>();

        /**
         *ͨ��spNR���㵱ǰ�ƶ����������Χ
         */
        NeighborRange oNR = new NeighborRange();
        for(int i=0;i<epNR.unsafeEdges.size();i++){    //����oNR�ķǰ�ȫ��
            if(epNR.unsafeLength.get(i)>distOtoEp){    //������������ȷǰ�ȫ���̣���ǰ�ȫ�߿ɱ��������򲻱���
                oNR.unsafeEdges.add(epNR.unsafeEdges.get(i));
                oNR.unsafeLength.add(epNR.unsafeLength.get(i)-distOtoEp);
            }
        }
        for(int i=0;i<epNR.safeEdges.size();i++){    //����oNR�İ�ȫ��
            if(epNR.safeLength.get(i)+distOtoEp<=eps){   //������ȫ�ߵĳ��ȸ��º�����eps��Χ�ڣ�����
                oNR.safeEdges.add(epNR.safeEdges.get(i));
                oNR.safeLength.add(epNR.safeLength.get(i));
            }else if(epNR.safeLength.get(i)+distOtoEp-eps<edgeList.get(o.edgeid).length){  //������Ĳ��ֶ��ڰ�ȫ�ߵĳ��ȣ�����ȫ��ת��Ϊ�ǰ�ȫ�ߣ����򲻱���
                oNR.unsafeEdges.add(epNR.safeEdges.get(i));
                oNR.unsafeLength.add(edgeList.get(o.edgeid).length-(epNR.safeLength.get(i)+distOtoEp-eps));
            }
        }

        /**
         * ����oNR�еıߣ���ȡ������ƶ�������뵽�����б���
         */
        for(int i=0;i<oNR.safeEdges.size();i++){   //��ȫ���ϵ��ƶ�����������ȫ�����뵽������
            an_edgeObjects = edgeObjectIndex.get(oNR.safeEdges.get(i));
            if(an_edgeObjects!=null)
                for(int j=0;j<an_edgeObjects.size();j++)
                    neighbors.add(an_edgeObjects.get(j));
        }
        for(int i=0;i<oNR.unsafeEdges.size();i++){   //�ǰ�ȫ���ϵ��ƶ����󣬰��ڼ�¼�����ڵ��ƶ�������뵽������
            an_edgeObjects = edgeObjectIndex.get(oNR.unsafeEdges.get(i));
            if(an_edgeObjects!=null)
                for(int j=0;j<an_edgeObjects.size();j++){
                    if(allObjects.get(an_edgeObjects.get(j)).pos<=oNR.unsafeLength.get(i))
                        neighbors.add(an_edgeObjects.get(j));
                    else
                        break;
                }
        }
        for(int i=0;i<edgeObjectIndex.get(o.edgeid).size();i++) {   //ͬһ�����ϵ��ƶ����󣬰ѱȵ�oƫ��������ĵ���뵽������
            if(allObjects.get(edgeObjectIndex.get(o.edgeid).get(i)).pos>o.pos)
                neighbors.add(edgeObjectIndex.get(o.edgeid).get(i));
        }

        return neighbors;
    }

    /**
     * �����ļ�·����ȡһ��ʱ����µ��ƶ�����HashMap<String,SnapObject> allObjects��
     * @param inPath
     * @throws IOException
     */
    public void readObjectsFromFile(String inPath) throws IOException {
        BufferedReader br=new BufferedReader(new FileReader(inPath));
//        System.out.println();
//        System.out.println("���ڶ��켣�������ļ�...  "+new Date(System.currentTimeMillis()));
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
            allObjects.put(line[0],snapObject);    //����һ���ƶ�����㣬�����б���
            readLine = br.readLine();
        }
//        System.out.println("�켣�������ļ���ȡ��ɣ�  "+new Date(System.currentTimeMillis()));
    }

    /**
     * �����ļ�·������һ��ʱ����µıߵ�����HashMap<String,List<String>> edgeObjectIndex = new HashMap<>();
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
     * ��HashMap<String,List<String>> coreObjectNeighborsת��ΪList<SnapObject> corePoints
     */
    public void getCorePoint(){
        SnapObject coreObject;
        for(Map.Entry<String,List<String>> coreEntry:coreObjectNeighbors.entrySet()){
            coreObject = allObjects.get(coreEntry.getKey());
            corePoints.add(coreObject);
        }
    }

    /**
     * �Ժ��ĵ���о��࣬�õ��ؼ�List<Clusters> finalClusters
     * @throws IOException
     */
    public void clustering() throws IOException{
//        System.out.println("��ʼ�������ĵ���������...  "+new Date(System.currentTimeMillis()));
        getCorePoint();
        gridIndexcore = new GridIndex(mapScale,2*eps,corePoints); //�������ĵ���������
//        System.out.println("���ĵ���������������ɣ�  "+new Date(System.currentTimeMillis()));
        Set<String> ids=gridIndexcore.pointsHash.keySet();   //��ȡ�����к��ĵ��ID
        Queue<SnapObject> queue=new LinkedList<SnapObject>();   //���ĵ����
        List<Onepair> pairlist;//=new ArrayList<Onepair>();   //���ĵ���б�

        /**
         *��������п��ظ�ʹ�õı���
         */
        List<SnapObject> psc;      //�洢���ĵ㸽���ĺ��ĵ�
        List<SnapObject> points;
        SnapObject p,head;
        Onepair onepair;      //һ�Ժ��ĵ��
        Edge2Edge e2e;
        List<String> pathlist;
        List<String> pathpointslist;
        String Sedge;    //�ƶ�����������ڵıߵ�id
        String Eedge;    //�ƶ������յ����ڵıߵ�id

        int allNeighborCoreAmount = 0;    //���к��ĵ�������ڽ����ĵ�����
        int omitCalNCAmount = 0;          //����һʡ�Լ���ĺ��ĵ���

//        System.out.println("���ڽ���SNN�Ƚ�...   "+new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
        int num=0;
        while(!ids.isEmpty()){
            for(String id:ids){
                p = gridIndexcore.pointsHash.get(id);    //��ȡһ�����ĵ�p
                points = new ArrayList<SnapObject>();
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
                    if(pairlist==null){     //���pairmap��û��p��pq�Ա����ʼ��һ���յ�����
                        pairlist = new ArrayList<>();
                        pairmap.put(head.id,pairlist);
                    }
                    pairlist=pairmap.get(head.id);
                    for(int i=0;i<pairlist.size();i++){   //����ÿһ��
                        onepair=pairlist.get(i);
                        //�����ж��Ƿ���head�ĺ��ĵ�������
                        if(flag.containsKey(onepair.qid)){  //˵��qid��psc����
                            if(numofoverlp(coreObjectNeighbors.get(head.id),coreObjectNeighbors.get(onepair.qid)) >=minshare){  //�жϺ��ĵ�Ե�SNN�Ƿ�������ֵ����
                                SnapObject tempp=allObjects.get(onepair.qid);
                                flag.put(tempp.id, true);
                                queue.add(tempp);
                                points.add(tempp);
                                gridIndexcore.delete(tempp);
//                                if(compMeterDistance(head,tempp)<=eps){  //�ж�tempp�Ƿ���head�Ħ������У�������������һ
                                if(coreObjectNeighbors.get(head.id).contains(tempp)){
                                    e2e=new Edge2Edge(head.edgeid,tempp.edgeid);
                                    if(!e2e_path.containsKey(e2e)) {
                                        calE2EPath(head,tempp);
                                    }
                                    pathlist=e2e_path.get(e2e);
                                    Sedge=head.edgeid;
                                    Eedge=tempp.edgeid;
                                    //�õ���Щ·����������֮��ĵ㣬���뵽����
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
//                    System.out.println("�ж���ϣ�  "+ new Date(System.currentTimeMillis()));

//                    System.out.println("�����ж�ʣ����ڽ����ĵ��Ƿ����"+head.id+"ͬ��...  "+ new Date(System.currentTimeMillis()));
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
//                                        System.out.println("��ǰ���Ķ����pq����"+templist.size()+"�ԣ�");
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
                if(points.size()>=mincontain){
                    this.finalClusters.add(new Clusters(num+"", points.get(0).time, points));
                    num++;
                }
                ids=gridIndexcore.pointsHash.keySet();
                break;
            }
        }
//        System.out.println("SNN�Ƚ���ɣ��ܵ���Ҫ����SNN�жϵĺ��ĵ����"+allNeighborCoreAmount+"�ԣ���������һʡ��"+omitCalNCAmount+"�Եļ��㣡"+ new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
//        System.out.println("SNN�Ƚ���ɣ�   "+ new Date(System.currentTimeMillis())+"  "+System.currentTimeMillis());
    }

    /**
     * ��������list����֮���غϵĵ�ĸ���
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
     * ����a��b����list����֮���غϵĵ�ĸ�������������c��
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
     * ���������߼�����·�����뵽HashMap<Edge2Edge,List<String>> e2e_path��
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
     * ��������
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

//��ʾp���Ӧ��һ��(p,q)��
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
