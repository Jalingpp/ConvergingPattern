package DataProcessing;

import Classes.SnapObject;
import Tools.DataRw;
import Tools.TimeConversion;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: 将以分钟为间隔的数据划分为以秒为间隔，每秒更新1/60的数据
 * @Author JJP
 * @Date 2021/11/25 13:42
 */
public class SplitData {
    static HashMap<String, SnapObject> snapshotObjectSet = new HashMap<>(); //移动对象最新位置点集合
    static HashMap<String, SnapObject> incrementalObjects = new HashMap<>(); //增量数据
    static String dataInpath = "E:/1 Research/data/TestDataForConvergingMonitor/LepsIncrementalData/";
    static String dataOutpath_all = "E:/1 Research/data/TestDataForConvergingMonitor/LepsDataPerSecond/";
    static String dataOutpath_inc = "E:/1 Research/data/TestDataForConvergingMonitor/LepsIncrementalDataPerSecond/";
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
        DataRw.writeObjectsToFile(dataOutpath_all+firstFileName+".txt",snapshotObjectSet);
        DataRw.writeObjectsToFile(dataOutpath_inc+firstFileName+".txt",snapshotObjectSet);
        System.out.println("finish!");
        //循环处理后续数据
        int timeAmount = TimeConversion.getTimestampAmount(startTime,endTime,timeInterval);
        for(int i=1;i<=timeAmount;i++){
            //读入一片数据
            String preFileName = TimeConversion.timeToOutpath(TimeConversion.getNextKTimeString(startTime,i-1,timeInterval));
            String curFileName = TimeConversion.timeToOutpath(TimeConversion.getNextKTimeString(startTime,i,timeInterval));
            System.out.print("Reading "+curFileName+" ...");
            DataRw.readObjectsFromFile(dataInpath+curFileName+".txt",incrementalObjects);
            System.out.println("finish!");
            //将增量数据拆分成60份
            int amountPerPart = incrementalObjects.size()/60;
            //处理前59份
            for(int j=1;j<60;j++){
                HashMap<String,SnapObject> incPerPart = new HashMap<>();
                //从增量数据中搞amountPerPart个点到IncPerPart中
                int num = 0;
                for(Map.Entry<String,SnapObject> entry:incrementalObjects.entrySet()){
                    if(num==amountPerPart)
                        break;
                    incPerPart.put(entry.getKey(),entry.getValue());
                    snapshotObjectSet.put(entry.getKey(),entry.getValue());
                    num++;
                }
                //将incPerPart中的点和所有点输出到单独的文件中
                String currPartFileName = preFileName.substring(0,4);
                if(j<10){
                    currPartFileName = currPartFileName+"0"+j;
                }else{
                    currPartFileName = currPartFileName+j;
                }
                System.out.print("Writing "+currPartFileName+" ...");
                DataRw.writeObjectsToFile(dataOutpath_inc+currPartFileName+".txt",incPerPart);
                DataRw.writeObjectsToFile(dataOutpath_all+currPartFileName+".txt",snapshotObjectSet);
                System.out.println("finish!");
                //删除增量数据中当前part
                for(Map.Entry<String,SnapObject> entry: incPerPart.entrySet()){
                    incrementalObjects.remove(entry.getKey());
                }
            }
            //处理剩余的一份
            //更新点集
            for(Map.Entry<String,SnapObject> entry:incrementalObjects.entrySet()){
                snapshotObjectSet.put(entry.getKey(),entry.getValue());
            }
            //输出最后一part和所有点到文件
            System.out.print("Writing "+curFileName+" ...");
            DataRw.writeObjectsToFile(dataOutpath_inc+curFileName+".txt",incrementalObjects);
            DataRw.writeObjectsToFile(dataOutpath_all+curFileName+".txt",snapshotObjectSet);
            System.out.println("finish!");
        }
    }

}
