package Clusters;

public class Objects {

    String OId;
    double Lng;
    double Lat;
    String e;
    double pos;

    public Objects() {

    }

    public void setOId(String OId) {
        this.OId = OId;
    }

    public void setLng(double lng) {
        Lng = lng;
    }

    public void setLat(double lat) {
        Lat = lat;
    }

    public double getLng() {
        return Lng;
    }

    public double getLat() {
        return Lat;
    }

    public String getOId() {
        return OId;
    }

    public boolean isInCluster(Cluster C) {              //是不是在簇中
        for(int i = 0; i < C.Objects.size(); i++) {
            if(this.OId.equals(C.Objects.get(i).OId)) {
                return true;
            }
        }
        return false;
    }
}
