package Classes;

/**
 * @Description: 路网边类
 * @Author JJP
 * @Date 2021/5/14 9:36
 */
public class Edge {
    String id;
    double length;
    String sp_id;
    String ep_id;


    public  Edge(){}

    public Edge(String id,double length,String sp_id,String ep_id){
        this.id = id;
        this.length = length;
        this.sp_id = sp_id;
        this.ep_id = ep_id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public String getSp_id() {
        return sp_id;
    }

    public void setSp_id(String sp_id) {
        this.sp_id = sp_id;
    }

    public String getEp_id() {
        return ep_id;
    }

    public void setEp_id(String ep_id) {
        this.ep_id = ep_id;
    }
}
