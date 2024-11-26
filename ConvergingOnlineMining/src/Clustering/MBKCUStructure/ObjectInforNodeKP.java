package Clustering.MBKCUStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description: �ƶ�����λ����Ϣ�ڵ�
 * @Author JJP
 * @Date 2021/11/16 9:21
 */
public class ObjectInforNodeKP {
    String id;  //�ƶ�����ID
    String type;  //λ�õ����ͣ��������ĵ㣺C���߽�㣺B�������㣺N��δ֪���ͣ�unknown
    boolean isKeyPoint;  //����Ƿ�Ϊ�ؼ��㣬trueΪ�ǣ�falseΪ��
    String cid;  //�ƶ��������ڴ�ID
    List<String> neighbors;  //�����ڵ��ƶ�����

    /**
     * ���캯��
     * @param id
     * @param type
     * @param cid
     */
    public ObjectInforNodeKP(String id, String type, boolean isKeyPoint, String cid) {
        this.id = id;
        this.type = type;
        this.isKeyPoint = isKeyPoint;
        this.cid = cid;
        this.neighbors = new ArrayList<>();
    }

    /**
     * ���캯��
     * @param id
     * @param type
     * @param cid
     * @param neighbors
     */
    public ObjectInforNodeKP(String id, String type, boolean isKeyPoint, String cid, List<String> neighbors) {
        this.id = id;
        this.type = type;
        this.isKeyPoint = isKeyPoint;
        this.cid = cid;
        this.neighbors = neighbors;
    }

    /**
     * �������캯��
     * @param oin
     */
    public ObjectInforNodeKP(ObjectInforNodeKP oin){
        this.id = oin.getId();
        this.type = oin.getType();
        this.cid = oin.getCid();
        this.neighbors = new ArrayList<>();
        for(int i=0;i<oin.getNeighbors().size();i++){
            this.neighbors.add(oin.getNeighbors().get(i));
        }
    }

    /**
     * ����һ���ھ�
     * @param neighborID
     */
    public void addNeighbor(String neighborID){
        this.neighbors.add(neighborID);
    }

    /**
     * ɾ��һ���ھ�
     * @param neighborID
     */
    public void delNeighbor(String neighborID){
        for(int i=neighbors.size()-1;i>=0;i--){
            if(neighbors.get(i).equals(neighborID))
                neighbors.remove(i);
        }
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getCid() {
        return cid;
    }

    public List<String> getNeighbors() {
        return neighbors;
    }

    public boolean isKeyPoint() {
        return isKeyPoint;
    }
}
