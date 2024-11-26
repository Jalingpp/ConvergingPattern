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
 * @Description: ����ʱ�չ켣���������������µ����ݣ���ɾ��ǰ����ͬ��λ�ü�¼
 * @Author JJP
 * @Date 2021/11/4 18:59
 */
public class ProcessObjectLocations {
    static HashMap<String, SnapObject> snapshotObjectSet = new HashMap<>(); //�ƶ���������λ�õ㼯��
    static HashMap<String, SnapObject> incrementalObjects = new HashMap<>(); //��������
    static String dataInpath = "E:/1 Research/data/TestDataForConvergingMonitor/";
    static String dataOutpath = "E:/1 Research/data/TestDataForConvergingMonitor/IncrementalData/";
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
        DataRw.writeObjectsToFile(dataOutpath+firstFileName+".txt",snapshotObjectSet);
        System.out.println("finish!");
        //ѭ�������������
        int timeAmount = TimeConversion.getTimestampAmount(startTime,endTime,timeInterval);
        for(int i=1;i<=timeAmount;i++){
            //����һƬ����
            String curFileName = TimeConversion.timeToOutpath(TimeConversion.getNextKTimeString(startTime,i,timeInterval));
            System.out.print("Reading "+curFileName+" ...");
            DataRw.readObjectsFromFile(dataInpath+curFileName+".txt",incrementalObjects);
            System.out.println("finish!");
            //�����ж��Ƿ���Ҫɾ��
            List<String> objectToDelete = new ArrayList<>(); //��ɾ�������
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
            //ɾ������
            System.out.println(objectToDelete.size()+"  to Delete!");
            for(int j=0;j<objectToDelete.size();j++){
                incrementalObjects.remove(objectToDelete.get(j));
            }
            //����������ݵ��ļ�
            System.out.print("Writing "+curFileName+" ...");
            DataRw.writeObjectsToFile(dataOutpath+curFileName+".txt",incrementalObjects);
            System.out.println("finish!");
            //���incrementalObjects
            incrementalObjects.clear();
        }
        int a=0;
    }

}
