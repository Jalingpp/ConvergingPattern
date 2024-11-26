package DataProcessing;

import Classes.SnapObject;
import Controls.MiningControlor;
import Tools.DataRw;
import Tools.TimeConversion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: 处理时空轨迹，仅保留增量更新的数据，即删除前后相同的位置记录
 * @Author JJP
 * @Date 2021/11/4 18:59
 */
public class ProcessObjectLocations {
    static HashMap<String, SnapObject> snapshotObjectSet = new HashMap<>(); //移动对象最新位置点集合
    static HashMap<String, SnapObject> incrementalObjects = new HashMap<>(); //增量数据
    static String dataInpath = "E:/1 Research/data/TestDataForConvergingMonitor/";
    static String dataOutpath = "E:/1 Research/data/TestDataForConvergingMonitor/IncrementalData/";
    static String startTime = "2015-04-11 00:00:00.0";
    static String endTime = "2015-04-11 00:05:00.0";
    static int timeInterval = 60000;

    public static void main(String[] args) throws IOException {
        //读入第一片数据
        String firstFileName = TimeConversion.timeToOutpath(startTime);
        System.out.print("Reading "+firstFileName+" ...");
        DataRw.readObjectsFromFile(dataInpath+firstFileName+".txt",snapshotObjectSet);
        System.out.println("finish!");
        //输出第一片数据
        System.out.print("Writing "+firstFileName+" ...");
        DataRw.writeObjectsToFile(dataOutpath+firstFileName+".txt",snapshotObjectSet);
        System.out.println("finish!");
        //循环处理后续数据
        int timeAmount = TimeConversion.getTimestampAmount(startTime,endTime,timeInterval);
        for(int i=1;i<=timeAmount;i++){
            //读入一片数据
            String curFileName = TimeConversion.timeToOutpath(TimeConversion.getNextKTimeString(startTime,i,timeInterval));
            System.out.print("Reading "+curFileName+" ...");
            DataRw.readObjectsFromFile(dataInpath+curFileName+".txt",incrementalObjects);
            System.out.println("finish!");
            //挨个判断是否需要删除
            List<String> objectToDelete = new ArrayList<>(); //待删除对象表
            for(Map.Entry<String,SnapObject> entry:incrementalObjects.entrySet()){
                SnapObject so = entry.getValue();
                SnapObject preSo = snapshotObjectSet.get(so.getId());
                if(preSo==null)
                    continue;
                if(so.getLng()==preSo.getLng()&&so.getLat()==preSo.getLat()){
                    objectToDelete.add(so.getId());
                }else{
                    snapshotObjectSet.put(preSo.getId(),so);
                }
            }
            //删除对象
            System.out.println(objectToDelete.size()+"  to Delete!");
            for(int j=0;j<objectToDelete.size();j++){
                incrementalObjects.remove(objectToDelete.get(j));
            }
            //输出增量数据到文件
            System.out.print("Writing "+curFileName+" ...");
            DataRw.writeObjectsToFile(dataOutpath+curFileName+".txt",incrementalObjects);
            System.out.println("finish!");
            //清空incrementalObjects
            incrementalObjects.clear();
        }
        int a=0;
    }

}
