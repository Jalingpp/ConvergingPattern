import java.sql.Timestamp;

public class Event {

    String id;
    int scale;   //汇聚事件包含的移动对象数
    double centerlng;   //汇聚事件中心的经度
    double centerlat;   //汇聚事件中心的纬度
    String startTime;   //汇聚事件开始事件
    String endTime;     //汇聚事件结束时间
    String duration;    //汇聚事件持续时间
    Cluster  rootCluster;    //汇聚事件的根节点

    public Event(String id, String sTime,String eTime, Cluster root) {
        this.id = id;
        this.startTime = sTime ;
        this.endTime = eTime;
        this.rootCluster = root;
   }

    /**
     * 计算汇聚事件的各个属性
     */
    public void calProperties() {
        this.scale = rootCluster.getScale();
        this.centerlat = calCenterLat();
        this.centerlng = calCenterLng();
        int timeLen = getseconds(startTime,endTime);
        int seconds = timeLen%60;
        int minutes = (timeLen%3600)/60;
        int hours = timeLen/3600;
        this.duration = String.format("%02d",hours)+":"+String.format("%02d",minutes)+":"+String.format("%02d",seconds);
    }

    /**
     * 获取所给时间段的秒数
     * @param start  起始时间
     * @param end    终止时间
     * @return
     */
    static int getseconds(String start, String end) {
        Timestamp stimestamp = Timestamp.valueOf(start);
        long t1 = stimestamp.getTime();
        Timestamp etimestamp = Timestamp.valueOf(end);
        long t2 = etimestamp.getTime();
        return (int) ((t2 - t1) / 1000);
    }

    /**
     * 计算根节点簇的中心经度
     * @return
     */
    public double calCenterLng(){
        double minlng = 180;
        double maxlng = 0;
        for(int i=0;i<rootCluster.getScale();i++)
        {
            Objects _o = rootCluster.getObjects().get(i);
            if(_o.getLng()<minlng)
                minlng = _o.getLng();
            if(_o.getLng()>maxlng)
                maxlng = _o.getLng();
        }
        return (minlng+maxlng)/2;
    }

    /**
     * 计算根节点簇的中心纬度
     * @return
     */
    public double calCenterLat(){
        double minlat = 90;
        double maxlat = 0;
        for(int i=0;i<rootCluster.getScale();i++)
        {
            Objects _o = rootCluster.getObjects().get(i);
            if(_o.getLat()<minlat)
                minlat = _o.getLat();
            if(_o.getLat()>maxlat)
                maxlat = _o.getLat();
        }
        return (minlat+maxlat)/2;
    }


    public Cluster getRootCluster() {
        return rootCluster;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getId() {
        return id;
    }

    public double getCenterlng() {
        return centerlng;
    }

    public double getCenterlat() {
        return centerlat;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getScale() {
        return scale;
    }

    public String getDuration() {
        return duration;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

}
