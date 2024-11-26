package Classes;//表示簇的类

import java.util.ArrayList;
import java.util.List;

/**
 * Created by JJP on 20200225
 * 快照簇
 */
public class Cluster {

    private String id;//同一帧里面的编号
    private String timestamp;//时间点
    private String timeNo;//簇在时间窗口中的序号
    private MBR mbr;   //簇的MBR
    private List<SnapObject> menberPoints;//簇中的点

    /**
     * 构造函数
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
     * 拷贝构造函数
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
     * 簇中添加一个点
     * @param snapObject
     */
    public void addPoint(SnapObject snapObject){
        SnapObject so = new SnapObject(snapObject);
        menberPoints.add(so);
        ajustMbr(so);
    }

    /**
     * 根据新加入的点调整簇的MBR
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
     * 当前簇是否被簇c包含
     * @param c
     * @return 包含返回true,不包含返回false
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
     * 判断当前簇是否包含移动对象id
     * @param snapObjectID
     * @return 包含返回true，不包含返回false
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
     * 返回簇中的移动对象实体
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
