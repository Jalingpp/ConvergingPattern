package RoadNetWork;

/**
 * @ Class: Edges
 * @ Description:
 * @ Date 2019/10/16 21:50
 * @ Created by bridge
**/

public class Edges {

    private String id;      //唯一标识
    private String sp;      //边的起点id
    private String ep;      //边的终点id
    private double length;  //路的长度

    public Edges() {
        this.id = null;
        this.sp = null;
        this.ep = null;
        this.length = 0.0;
    }

    public String getId() {
        return id;
    }

    public String getSp() {
        return sp;
    }

    public String getEp() {
        return ep;
    }

    public double getLength() {
        return length;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSp(String sp) {
        this.sp = sp;
    }

    public void setEp(String ep) {
        this.ep = ep;
    }

    public void setLength(double length) {
        this.length = length;
    }
}
