package Clustering.MBCCUStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 移动对象位置信息节点
 * @Author JJP
 * @Date 2021/11/16 9:21
 */
public class ObjectInforNode {
    String id;  //移动对象ID
    String type;  //位置点类型，包括核心点：C、边界点：B、噪声点：N，未知类型：unknown
    String cid;  //移动对象所在簇ID
    List<String> neighbors;  //邻域内的移动对象

    /**
     * 构造函数
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
     * 构造函数
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
     * 拷贝构造函数
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
     * 加入一个邻居
     * @param neighborID
     */
    public void addNeighbor(String neighborID){
        this.neighbors.add(neighborID);
    }

    /**
     * 删除一个邻居
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
