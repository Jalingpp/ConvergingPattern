package DataProcessing;

import Classes.SnapObject;
import Tools.DataRw;
import Tools.TimeConversion;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description: ���Է���Ϊ��������ݻ���Ϊ����Ϊ�����ÿ�����1/60������
 * @Author JJP
 * @Date 2021/11/25 13:42
 */
public class SplitData {
    static HashMap<String, SnapObject> snapshotObjectSet = new HashMap<>(); //�ƶ���������λ�õ㼯��
    static HashMap<String, SnapObject> incrementalObjects = new HashMap<>(); //��������
    static String dataInpath = "E:/1 Research/data/TestDataForConvergingMonitor/LepsIncrementalData/";
    static String dataOutpath_all = "E:/1 Research/data/TestDataForConvergingMonitor/LepsDataPerSecond/";
    static String dataOutpath_inc = "E:/1 Research/data/TestDataForConvergingMonitor/LepsIncrementalDataPerSecond/";
    static String startTime = "2015-04-11 00:00:00.0";
    static String endTime = "2015-04-11 00:05:00.0";
    static int timeInterval = 60000;

    public static void main(String[] args) throws IOException {
        //�����һƬ����
        String firstFileName = TimeConversion.timeToOutpath(startTime);
        System.out.print("Reading "+firstFileName+" ...");
        DataRw.readObjectsFromFile(dataInpath+firstFileName+".txt",snapshotObjectSet);
        System.out.println("finish!");
        //�����һƬ����
        System.out.print("Writing "+firstFileName+" ...");
        DataRw.writeObjectsToFile(dataOutpath_all+firstFileName+".txt",snapshotObjectSet);
        DataRw.writeObjectsToFile(dataOutpath_inc+firstFileName+".txt",snapshotObjectSet);
        System.out.println("finish!");
        //ѭ�������������
        int timeAmount = TimeConversion.getTimestampAmount(startTime,endTime,timeInterval);
        for(int i=1;i<=timeAmount;i++){
            //����һƬ����
            String preFileName = TimeConversion.timeToOutpath(TimeConversion.getNextKTimeString(startTime,i-1,timeInterval));
            String curFileName = TimeConversion.timeToOutpath(TimeConversion.getNextKTimeString(startTime,i,timeInterval));
            System.out.print("Reading "+curFileName+" ...");
            DataRw.readObjectsFromFile(dataInpath+curFileName+".txt",incrementalObjects);
            System.out.println("finish!");
            //���������ݲ�ֳ�60��
            int amountPerPart = incrementalObjects.size()/60;
            //����ǰ59��
            for(int j=1;j<60;j++){
                HashMap<String,SnapObject> incPerPart = new HashMap<>();
                //�����������и�amountPerPart���㵽IncPerPart��
                int num = 0;
                for(Map.Entry<String,SnapObject> entry:incrementalObjects.entrySet()){
                    if(num==amountPerPart)
                        break;
                    incPerPart.put(entry.getKey(),entry.getValue());
                    snapshotObjectSet.put(entry.getKey(),entry.getValue());
                    num++;
                }
                //��incPerPart�еĵ�����е�������������ļ���
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
                //ɾ�����������е�ǰpart
                for(Map.Entry<String,SnapObject> entry: incPerPart.entrySet()){
                    incrementalObjects.remove(entry.getKey());
                }
            }
            //����ʣ���һ��
            //���µ㼯
            for(Map.Entry<String,SnapObject> entry:incrementalObjects.entrySet()){
                snapshotObjectSet.put(entry.getKey(),entry.getValue());
            }
            //������һpart�����е㵽�ļ�
            System.out.print("Writing "+curFileName+" ...");
            DataRw.writeObjectsToFile(dataOutpath_inc+curFileName+".txt",incrementalObjects);
            DataRw.writeObjectsToFile(dataOutpath_all+curFileName+".txt",snapshotObjectSet);
            System.out.println("finish!");
        }
    }

}
