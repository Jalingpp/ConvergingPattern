package Clustering.MBCCUStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description: �ƶ�����λ����Ϣ�ڵ�
 * @Author JJP
 * @Date 2021/11/16 9:21
 */
public class ObjectInforNode {
    String id;  //�ƶ�����ID
    String type;  //λ�õ����ͣ��������ĵ㣺C���߽�㣺B�������㣺N��δ֪���ͣ�unknown
    String cid;  //�ƶ��������ڴ�ID
    List<String> neighbors;  //�����ڵ��ƶ�����

    /**
     * ���캯��
     * @param id
     * @param type
     * @param cid
     */
    public ObjectInforNode(String id, String type, String cid) {
        this.id = id;
        this.type = type;
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
    public ObjectInforNode(String id, String type, String cid, List<String> neighbors) {
        this.id = id;
        this.type = type;
        this.cid = cid;
        this.neighbors = neighbors;
    }

    /**
     * �������캯��
     * @param oin
     */
    public ObjectInforNode(ObjectInforNode oin){
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
}
