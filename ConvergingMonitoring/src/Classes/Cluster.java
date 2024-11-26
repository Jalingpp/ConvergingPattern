package Classes;//��ʾ�ص���

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JJP on 20200225
 * ���մ�
 */
public class Cluster {

    private String id;//ͬһ֡����ı��
    private String timestamp;//ʱ���
    private String timeNo;//����ʱ�䴰���е����
    private MBR mbr;   //�ص�MBR
    private List<SnapObject> menberPoints;//���еĵ�

    /**
     * ���캯��
     * @param id
     * @param timestamp
     * @param timeNo
     */
    public Cluster(String id, String timestamp, String timeNo) {
        this.id = id;
        this.timestamp = timestamp;
        this.timeNo = timeNo;
        this.mbr = new MBR();
        this.menberPoints = new ArrayList<>();
    }

    /**
     * �������캯��
     * @param cluster
     */
    public Cluster(Cluster cluster){
        this.id = cluster.getId();
        this.timestamp = cluster.getTimestamp();
        this.timeNo = cluster.getTimeNo();
        this.mbr = new MBR(cluster.getMbr());
        this.menberPoints = new ArrayList<>();
        for(int i=0;i<cluster.getMenberPoints().size();i++){
            SnapObject so = new SnapObject(cluster.getMenberPoints().get(i));
            this.menberPoints.add(so);
        }
    }

    /**
     * �������һ����
     * @param snapObject
     */
    public void addPoint(SnapObject snapObject){
        SnapObject so = new SnapObject(snapObject);
        menberPoints.add(so);
        ajustMbr(so);
    }

    /**
     * �����¼���ĵ�����ص�MBR
     * @param snapObject
     */
    public void ajustMbr(SnapObject snapObject){
        if(snapObject.getLat()>mbr.getMaxLat())
            mbr.setMaxLat(snapObject.getLat());
        if(snapObject.getLat()<mbr.getMinLat())
            mbr.setMinLat(snapObject.getLat());

        if(snapObject.getLng()>mbr.getMaxLng())
            mbr.setMaxLng(snapObject.getLng());
        if(snapObject.getLng()<mbr.getMinLng())
            mbr.setMinLng(snapObject.getLng());
    }

    /**
     * ��ǰ���Ƿ񱻴�c����
     * @param c
     * @return ��������true,����������false
     */
    public boolean isCluded(Cluster c){
        for(int i=0;i<menberPoints.size();i++){
            boolean included = false;
            for(int j=0;j<c.getMenberPoints().size();j++){
                if(c.getMenberPoints().get(j).getId().equals(this.getMenberPoints().get(i).getId())) {
                    included = true;
                    break;
                }
            }
            if(included==false)
                return false;
        }
        return true;
    }

    /**
     * �жϵ�ǰ���Ƿ�����ƶ�����id
     * @param snapObjectID
     * @return ��������true������������false
     */
    public boolean isContain(String snapObjectID){
        for(int i=0;i<this.getMenberPoints().size();i++){
            if(this.getMenberPoints().get(i).getId().equals(snapObjectID)){
                return true;
            }
        }
        return false;
    }

    /**
     * ���ش��е��ƶ�����ʵ��
     * @param snapObjectID
     * @return
     */
    public SnapObject getSnapObject(String snapObjectID){
        for(int i=0;i<this.getMenberPoints().size();i++){
            if(this.getMenberPoints().get(i).getId().equals(snapObjectID)){
                return this.getMenberPoints().get(i);
            }
        }
        return null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimeNo() {
        return timeNo;
    }

    public void setTimeNo(String timeNo) {
        this.timeNo = timeNo;
    }

    public MBR getMbr() {
        return mbr;
    }

    public void setMbr(MBR mbr) {
        this.mbr = mbr;
    }

    public List<SnapObject> getMenberPoints() {
        return menberPoints;
    }

    public void setMenberPoints(List<SnapObject> menberPoints) {
        this.menberPoints = menberPoints;
    }
}
