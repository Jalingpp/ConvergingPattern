import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.csvreader.CsvWriter;

public class RW {

}


class CSVRW {



    public static void writeCSV(String path, List<Clusters> finalClusters) {
        String csvFilePath = path;

        try {

            CsvWriter csvWriter = new CsvWriter(csvFilePath, ',', Charset.forName("UTF-8"));

            String[] headers = {"CID","PID","PLng","Plat","Pedge","Ppos","time"};

            csvWriter.writeRecord(headers);
            for(int i=0;i<finalClusters.size();i++){
                String cid = finalClusters.get(i).id;
                for(int j=0;j<finalClusters.get(i).getPointList().size();j++)
                {
                    String pid = finalClusters.get(i).getPointList().get(j).getId();
                    double plng = finalClusters.get(i).getPointList().get(j).getLng();
                    double plat = finalClusters.get(i).getPointList().get(j).getLat();
                    String time = finalClusters.get(i).getTime();

                    String[] writeLine = new String[7];
                    writeLine[0] = cid;
                    writeLine[1] = pid;
                    writeLine[2] = Double.toString(plng);
                    writeLine[3] = Double.toString(plat);

                    writeLine[6] = time;

                    String Pedge=finalClusters.get(i).getPointList().get(j).getEdgeid();
                    double Ppos=finalClusters.get(i).getPointList().get(j).getPos();
                    writeLine[4]=Pedge;
                    writeLine[5]=Double.toString(Ppos);

                    csvWriter.writeRecord(writeLine);
                }
            }

            csvWriter.close();
//            System.out.println("簇输出完成");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void writeMBRtoCSV(String path, List<Clusters> finalClusters)
    {
        String csvFilePath = path;

        try {


            CsvWriter csvWriter = new CsvWriter(csvFilePath, ',', Charset.forName("UTF-8"));

            String[] headers = {"splng","splat","eplng","eplat"};
            csvWriter.writeRecord(headers);
            for(int i=0;i<finalClusters.size();i++){
                double splng1 = finalClusters.get(i).getMinlng();
                double splat1 = finalClusters.get(i).getMaxlat();
                double eplng1 =finalClusters.get(i).getMaxlng();
                double eplat1 = finalClusters.get(i).getMaxlat();

                String[] writeLine1 = new String[4];
                writeLine1[0] = Double.toString(splng1);
                writeLine1[1] = Double.toString(splat1);
                writeLine1[2] = Double.toString(eplng1);
                writeLine1[3] = Double.toString(eplat1);
                csvWriter.writeRecord(writeLine1);

                double splng2 = finalClusters.get(i).getMaxlng();
                double splat2 = finalClusters.get(i).getMaxlat();
                double eplng2 = finalClusters.get(i).getMaxlng();
                double eplat2 = finalClusters.get(i).getMinlat();

                String[] writeLine2 = new String[4];
                writeLine2[0] = Double.toString(splng2);
                writeLine2[1] = Double.toString(splat2);
                writeLine2[2] = Double.toString(eplng2);
                writeLine2[3] = Double.toString(eplat2);
                csvWriter.writeRecord(writeLine2);

                double splng3 = finalClusters.get(i).getMaxlng();
                double splat3 = finalClusters.get(i).getMinlat();
                double eplng3 = finalClusters.get(i).getMinlng();
                double eplat3 = finalClusters.get(i).getMinlat();

                String[] writeLine3 = new String[4];
                writeLine3[0] = Double.toString(splng3);
                writeLine3[1] = Double.toString(splat3);
                writeLine3[2] = Double.toString(eplng3);
                writeLine3[3] = Double.toString(eplat3);
                csvWriter.writeRecord(writeLine3);

                double splng4 = finalClusters.get(i).getMinlng();
                double splat4 = finalClusters.get(i).getMinlat();
                double eplng4 = finalClusters.get(i).getMinlng();
                double eplat4 = finalClusters.get(i).getMaxlat();

                String[] writeLine4 = new String[4];
                writeLine4[0] = Double.toString(splng4);
                writeLine4[1] = Double.toString(splat4);
                writeLine4[2] = Double.toString(eplng4);
                writeLine4[3] = Double.toString(eplat4);
                csvWriter.writeRecord(writeLine4);

            }
            csvWriter.close();
//            System.out.println("MBR输出完成");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



class JSONRW {
    public static JSONObject readJson(String path){

        File file = new File(path);
        BufferedReader reader = null;

        StringBuffer data = new StringBuffer();

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));

            String temp;
            while((temp = reader.readLine()) != null){
                data.append(temp);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {

            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return JSONObject.parseObject(data.toString());
    }

    public static void writeJson(String path,List<Clusters> finalclusters){
        BufferedWriter writer = null;
        File file = new File(path);


        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //写入
        try {
            writer = new BufferedWriter(new FileWriter(file));


            JSONObject root=new JSONObject();
            if(finalclusters!=null){
                root.put("Time", finalclusters.get(0).getTime());
                root.put("ClusterSize",finalclusters.size());
                JSONArray clustersarray=new JSONArray();
                JSONArray earray = null;
                JSONArray Objectsarray=null;
                JSONObject m = null,n=null;
                for(int i=0;i<finalclusters.size();i++){
                    m=new JSONObject();
                    m.put("CId",finalclusters.get(i).id);
                    List<SnapPoint> list=new ArrayList<SnapPoint>();
                    list=finalclusters.get(i).menberPoints;
                    m.put("Scale", list.size());
                    Objectsarray=new JSONArray();
                    earray=new JSONArray();
                    int tempnum=0;
                    for(int j=0;j<list.size();j++){
                        n=new JSONObject();
                        n.put("OId",list.get(j).id );
                        n.put("Lng",list.get(j).getLng());
                        n.put("Lat",list.get(j).getLat());
                        n.put("e",list.get(j).getEdgeid());
                        if(earray.contains(list.get(j).getEdgeid())==false){
                            earray.add(tempnum++, list.get(j).getEdgeid());
                        }
                        n.put("pos",list.get(j).getPos());
                        Objectsarray.add(n);

                    }
                    m.put("Objects",Objectsarray);
                    m.put("E", earray);
                    clustersarray.add(m);
                }
                root.put("Clusters", clustersarray);
                //  	System.out.println(root.toString());
                writer.write(root.toString());
                root.clear();
                m.clear();
                n.clear();
                clustersarray.clear();
                earray.clear();
                Objectsarray.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(writer != null){
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}




//读Edge.txt文件

class Readedge {
    String temp;
    String[] list=new String[10];
    Adj_Node adjnode=new Adj_Node();
    public HashMap<String,Adj_Node> read(String name) throws IOException
    {
        HashMap<String,Adj_Node> map= new HashMap<String,Adj_Node> ();
        BufferedReader br=new BufferedReader(new FileReader(name));
        temp=br.readLine();
        while(temp!=null){
            list=temp.split(",");
            adjnode=new Adj_Node();
            adjnode.edgeid=list[0];
            adjnode.spid=list[1];
            adjnode.epid=list[2];
            adjnode.length=Double.parseDouble(list[3]);
            map.put(list[0], adjnode);
            temp=br.readLine();
        }
        return map;
    }
}





//读edgeturn.txt文件
class Readedgeturn {
    String list[]=new String[1000];
    LinkedList<each2> linkedlist=new LinkedList<each2>();
    HashMap<String,Etoe> map=new HashMap<String,Etoe>();
    Etoe etoe;
    each2 e;
    String temp;
    String id;
    public HashMap<String,Etoe> read(String name) throws IOException
    {
        BufferedReader br=new BufferedReader(new FileReader(name));
        temp=br.readLine();
        while(temp!=null){
            list=temp.split(",");
            id=list[0];
            etoe=new Etoe(id);
            for(int i=1;i<list.length;i=i+2){
                e=new each2();
                e.id=list[i];
                e.pro=Double.parseDouble(list[i+1]);
                etoe.linkedlist.add(e);
            }
            map.put(id, etoe);
            temp=br.readLine();
        }
        return map;

    }
}

//读nodes.txt文件
class ReadNodes {
    List<SnapPoint> pointslist=new ArrayList<SnapPoint>();
    String temp;
    String[] list=new String[20];

    int num;
    SnapPoint p;

    public List<SnapPoint> read( String spath, HashMap<String,SnapPoint> map) throws IOException{
        String path=spath;
        BufferedReader br=new BufferedReader(new FileReader(path));
        temp=br.readLine();
        num=0;
        while(temp!=null){
            list=temp.split(",");
            p=new SnapPoint();
            p.setId(list[0]);
            p.setLng(Double.parseDouble(list[1]));
            p.setLat(Double.parseDouble(list[2]));
            p.setEdgeid("");
            p.setPos(0);
            p.setTime(null);
            pointslist.add(p);
            map.put(list[0], p);
            num++;
            temp=br.readLine();
        }
        return pointslist;
    }

}

//从轨迹数据文件中读取所有的移动对象
class ReadPoints {
    List<SnapPoint> pointslist=new ArrayList<SnapPoint>();
    String temp;
    String[] list=new String[20];

    int num;
    SnapPoint p;

    public List<SnapPoint> read(String stime,String spath, HashMap<String, ArrayList<SnapPoint>> edge_point,HashMap<String,SnapPoint> pointsidtop) throws IOException{

        String path=spath;
        BufferedReader br=new BufferedReader(new FileReader(path));
        Timestamp timestamp=Timestamp.valueOf(stime);
        ArrayList<SnapPoint> adjlist;

        temp=br.readLine();
        num=0;
        while(temp!=null){
            list=temp.split(",");
            p=new SnapPoint();
            p.setId(list[0]);
            p.setLng(Double.parseDouble(list[1]));
            p.setLat(Double.parseDouble(list[2]));
            p.setEdgeid(list[3]);
            p.setPos(Double.parseDouble(list[4]));
            p.setTime(timestamp);
            pointslist.add(p);
            pointsidtop.put(list[0], p);

            if (edge_point.containsKey(list[3]))
            {
                edge_point.get(list[3]).add(p);
            }
            else
            {
                adjlist = new ArrayList<SnapPoint>();
                adjlist.add(p);
                edge_point.put(list[3], adjlist);
            }

            num++;

            temp=br.readLine();
        }
        return pointslist;
    }


}

//读可达矩阵文件vexarrive.txt
class Readvexarrive {
    String list[]=new String[1000];
    LinkedList<Adj_Node> linkedlist=new LinkedList<Adj_Node>();
    HashMap<String,LinkedList<Adj_Node>> map=new HashMap<String,LinkedList<Adj_Node>>();
    Adj_Node e;
    String temp;
    String spid;
    public HashMap<String,LinkedList<Adj_Node>> read(String name) throws IOException
    {
        BufferedReader br=new BufferedReader(new FileReader(name));
        temp=br.readLine();
        while(temp!=null){
            list=temp.split(",");
            spid=list[0];
            linkedlist=new LinkedList<Adj_Node>();
            for(int i=1;i<list.length;i=i+3){
                e=new Adj_Node();
                e.spid=spid;
                e.epid=list[i];
                e.length=Double.parseDouble(list[i+1]);
                e.edgeid=list[i+2];
                linkedlist.add(e);
            }
            map.put(spid, linkedlist);
            temp=br.readLine();
        }

        return map;
    }

}


//表示簇的类

class Clusters {

    public String id;//同一帧里面的编号
    protected Timestamp time;//时间点
    public List<SnapPoint> menberPoints;//簇中的点


    public Clusters(){
    }

    public Clusters(String id,Timestamp time,List<SnapPoint> points){
        this.id = id;
        this.time = time;
        this.menberPoints = points;
    }

    public String getTime(){
        return this.time+"";
    }

    public double getMinlng(){
        SnapPoint p;
        Double minlng=Double.MAX_VALUE;
        for(int i=0;i<this.menberPoints.size();i++){
            p=menberPoints.get(i);
            if(p.getLng()<minlng){
                minlng=p.getLng();
            }
        }
        return minlng;
    }

    public double getMaxlng(){
        SnapPoint p;
        Double maxlng=Double.MIN_VALUE;
        for(int i=0;i<this.menberPoints.size();i++){
            p=menberPoints.get(i);
            if(p.getLng()>maxlng){
                maxlng=p.getLng();
            }
        }
        return maxlng;
    }

    public double getMinlat(){
        SnapPoint p;
        Double minlat=Double.MAX_VALUE;
        for(int i=0;i<this.menberPoints.size();i++){
            p=menberPoints.get(i);
            if(p.getLat()<minlat){
                minlat=p.getLat();
            }
        }
        return minlat;
    }

    public double getMaxlat(){
        SnapPoint p;
        Double maxlat=Double.MIN_VALUE;
        for(int i=0;i<this.menberPoints.size();i++){
            p=menberPoints.get(i);
            if(p.getLat()>maxlat){
                maxlat=p.getLat();
            }
        }
        return maxlat;
    }

    public Clusters(String id,Timestamp time,SnapPoint p){
        this.id = id;
        this.time = time;
        this.menberPoints = new ArrayList<SnapPoint>();
        this.menberPoints.add(p);
    }

    public void addtocluster(SnapPoint p){
        menberPoints.add(p);
    }

    public List<SnapPoint> getPointList(){
        return this.menberPoints;
    }

}


//表示地图边界的类
class MBR {

    public double minLng;//最小经度
    public double minLat;//最小纬度
    public double maxLng;//最大经度
    public double maxLat;//最大纬度

    /**
     * 构造器
     * @param minLng 最小经度
     * @param minLat 最小纬度
     * @param maxLng 最大经度
     * @param maxLat 最大纬度
     */
    public MBR(double minLng,double minLat,double maxLng,double maxLat){
        this.minLng = minLng;
        this.minLat = minLat;
        this.maxLng = maxLng;
        this.maxLat = maxLat;
    }
}


//表示每一个移动对象的类
class SnapPoint{

    String id;//点的id，对应所属轨迹的id
    double lng;
    double lat;
    String edgeid;
    double pos;
    Timestamp time;
    PointMark mark;//点在聚类过程中的标记


    public SnapPoint(){

    }


    public SnapPoint(String id,double lng, double lat, String edgeid,double pos,Timestamp time) {
        this.id = id;
        this.lat=lat;
        this.lng=lng;
        this.edgeid=edgeid;
        this.pos=pos;
        this.time =time;
        //初始所有点标记为噪声
        this.mark = PointMark.NOISE;
    }



    public double getLng(){
        return this.lng;
    }

    public double getLat(){
        return this.lat;
    }

    public String getEdgeid(){
        return this.edgeid;
    }

    public double getPos(){
        return this.pos;
    }

    /**
     * @return 点的id，对应轨迹id即移动对象id
     */
    public String getId(){
        return this.id;
    }



    public Timestamp getTime(){
        return this.time;
    }


    public PointMark getMark(){
        return this.mark;
    }



    public void setMark(PointMark pm){
        this.mark = pm;
    }


    public void setId(String id){
        this.id=id;
    }

    public void setLat(double lat){
        this.lat=lat;
    }

    public void setPos(double pos){
        this.pos=pos;
    }

    public void setLng(double lng){
        this.lng=lng;
    }

    public void setEdgeid(String edgeid){
        this.edgeid=edgeid;
    }

    public void setTime(Timestamp time){
        this.time=time;
    }

    public boolean equals(Object obj) {
        // TODO Auto-generated method stub
        return obj instanceof SnapPoint &&
                this.id.equals(((SnapPoint)obj).id) ;

    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        int result = 17;
        result = 37*result+id.hashCode();
        return result;
    }


    public String toString() {
        String str = this.getId() + "," + this.getLng() + "," + this.getLat();

        return str;
    }
}


//表示p点对应的一个(p,q)对

class Onepair{
    String qid;
    List<SnapPoint> sharelist;//=new ArrayList<SnapPoint>();
    Onepair(){
        sharelist=new ArrayList<SnapPoint>();
    }
    Onepair(String id,List<SnapPoint> list){
        sharelist=new ArrayList<SnapPoint>();
        this.qid=id;
        this.sharelist=list;
    }

    void setqid(String id){
        this.qid=id;
    }

    void setlist(List<SnapPoint> a){
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


class each2{
    String id;
    double pro;
    each2(){

    }
}

class Etoe{
    String id;
    LinkedList<each2> linkedlist=new LinkedList<each2>();
    Etoe(String id){
        this.id=id;
    }
}


class Adj_Node {
    String spid;
    String epid;
    double length;
    String edgeid;
    Adj_Node(){

    }
    Adj_Node(String sp, String ep, double len, String eid)
    {
        spid = sp;
        epid = ep;
        length = len;
        edgeid = eid;
    }
}


enum PointMark {
    NOISE,//噪声点
    CORE//核心点
}
