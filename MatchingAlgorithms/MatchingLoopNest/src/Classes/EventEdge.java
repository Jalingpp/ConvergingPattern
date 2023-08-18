package Classes;

public class EventEdge {

    String id;
    double sp_lat;    //������γ��
    double sp_lng;    //�����ľ���
    double ep_lat;    //���յ��γ��
    double ep_lng;    //���յ�ľ���
    int objectsAmount;  //�����ƶ���������

    public EventEdge(String id,int oA) {
        this.id = id;
        this.objectsAmount = oA;
    }

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public double getSp_lat() {
        return sp_lat;
    }
    public void setSp_lat(double sp_lat) {
        this.sp_lat = sp_lat;
    }
    public double getSp_lng() {
        return sp_lng;
    }
    public void setSp_lng(double sp_lng) {
        this.sp_lng = sp_lng;
    }
    public double getEp_lat() {
        return ep_lat;
    }
    public void setEp_lat(double ep_lat) {
        this.ep_lat = ep_lat;
    }
    public double getEp_lng() {
        return ep_lng;
    }
    public void setEp_lng(double ep_lng) {
        this.ep_lng = ep_lng;
    }
    public int getObjectsAmount() {
        return objectsAmount;
    }
    public void setObjectsAmount(int objectsAmount) {
        this.objectsAmount = objectsAmount;
    }
}
