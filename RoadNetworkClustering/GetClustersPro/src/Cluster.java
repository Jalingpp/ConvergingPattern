//表示簇的类

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

class Clusters {

    public String id;//同一帧里面的编号
    protected String time;//时间点
    public List<SnapObject> menberPoints;//簇中的点


    public Clusters(){
    }

    public Clusters(String id,String time,List<SnapObject> points){
        this.id = id;
        this.time = time;
        this.menberPoints = points;
    }

    public String getTime(){
        return this.time+"";
    }

    public double getMinlng(){
        SnapObject p;
        Double minlng=Double.MAX_VALUE;
        for(int i=0;i<this.menberPoints.size();i++){
            p=menberPoints.get(i);
            if(p.lng<minlng){
                minlng=p.lng;
            }
        }
        return minlng;
    }

    public double getMaxlng(){
        SnapObject p;
        Double maxlng=Double.MIN_VALUE;
        for(int i=0;i<this.menberPoints.size();i++){
            p=menberPoints.get(i);
            if(p.lng>maxlng){
                maxlng=p.lng;
            }
        }
        return maxlng;
    }

    public double getMinlat(){
        SnapObject p;
        Double minlat=Double.MAX_VALUE;
        for(int i=0;i<this.menberPoints.size();i++){
            p=menberPoints.get(i);
            if(p.lat<minlat){
                minlat=p.lat;
            }
        }
        return minlat;
    }

    public double getMaxlat(){
        SnapObject p;
        Double maxlat=Double.MIN_VALUE;
        for(int i=0;i<this.menberPoints.size();i++){
            p=menberPoints.get(i);
            if(p.lat>maxlat){
                maxlat=p.lat;
            }
        }
        return maxlat;
    }

    public Clusters(String id,String time,SnapObject p){
        this.id = id;
        this.time = time;
        this.menberPoints = new ArrayList<SnapObject>();
        this.menberPoints.add(p);
    }

    public void addtocluster(SnapObject p){
        menberPoints.add(p);
    }

    public List<SnapObject> getPointList(){
        return this.menberPoints;
    }

}
