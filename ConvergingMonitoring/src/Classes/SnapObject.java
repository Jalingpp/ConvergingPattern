package Classes;

/**
 * Created by JJP on 20200224
 * øÏ’’µ„
 */
public class SnapObject extends GeoObject {

    private String id;
    private double lng;
    private double lat;
    private String edgeid;
    private double pos;
    private String time;

    public SnapObject(){
    }

    public SnapObject(String id){
        this.id = id;
    }

    public SnapObject(SnapObject snapObject){
        this.id = snapObject.getId();
        this.lng = snapObject.getLng();
        this.lat = snapObject.getLat();
        this.edgeid = snapObject.getEdgeid();
        this.pos = snapObject.getPos();
        this.time = snapObject.getTime();
    }

    public MBR getMBR(){
        return new MBR(lat,lng,lat,lng);
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

    public String getEdgeid() {
        return edgeid;
    }

    public void setEdgeid(String edgeid) {
        this.edgeid = edgeid;
    }

    public double getPos() {
        return pos;
    }

    public void setPos(double pos) {
        this.pos = pos;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
