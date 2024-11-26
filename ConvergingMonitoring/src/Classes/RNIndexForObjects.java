package Classes;

import java.util.HashMap;
import java.util.List;

/**
 * @Description: 为移动对象查询建立的路网索引，用于快速根据边找到其上面的移动对象并做计算
 * @Author JJP
 * @Date 2021/7/2 15:40
 */
public class RNIndexForObjects {
    //路网边和移动对象的索引,String:路网边id;List<String>:边上的移动对象id列表
    HashMap<String, List<String>> rn_objects_index;



}
