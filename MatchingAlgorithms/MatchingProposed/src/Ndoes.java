import java.util.ArrayList;

/**
 * @ Title: Nodes.java
 * @ Package offline
 * @ Description:
 * @ Date 2019/10/16 20:37
 * @ Created by bridge
 **/

class Nodes {

    private String id;        //唯一标识
    private double lat;       //经度
    private double lng;       //维度
    private ArrayList<String> eEdges = new ArrayList<String>();     //以该点为终点的边集
    private ArrayList<String> sEdges = new ArrayList<String>();     //以该点为起点的边集

    public Nodes() {
        this.id = null;
        this.lat = 0;
        this.lng = 0;
    }

    public String getId() {
        return id;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public ArrayList<String> geteEdges() {
        return eEdges;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public void seteEdges(ArrayList<String> eEdges) {
        this.eEdges = eEdges;
    }

    public ArrayList<String> getsEdges() {
        return sEdges;
    }

    public void setsEdges(ArrayList<String> sEdges) {
        this.sEdges = sEdges;
    }
}
