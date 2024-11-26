package Classes;

import java.util.*;

/**
 * @Description: TODO
 * @Author lenovo
 * @Date 2021/5/14 9:38
 */
public class Vertex {
    String id;
    double lng;
    double lat;
    List<String> adjEdges;  //邻接边

    public Vertex() {
        adjEdges = new ArrayList<>();
    }

    public Vertex(Vertex vertex){
        this.id = vertex.id;
        this.lng = vertex.lng;
        this.lat = vertex.lat;
        this.adjEdges = new ArrayList<>();
        this.adjEdges.addAll(vertex.adjEdges);
    }

    public Vertex(String id,double lng,double lat){
        this.id = id;
        this.lng = lng;
        this.lat = lat;
        this.adjEdges = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public List<String> getAdjEdges() {
        return adjEdges;
    }

    public void setAdjEdges(List<String> adjEdges) {
        this.adjEdges = adjEdges;
    }
    public void addAdjEdge(String newEdge){
        this.adjEdges.add(newEdge);
    }
}
