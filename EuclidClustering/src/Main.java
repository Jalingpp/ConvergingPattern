import java.sql.Timestamp;
import java.util.*;

import Code.*;
import MyClass.*;
import Tools.*;

public class Main {

    public static void main(String[] args) {
        // TODO Auto-generated method stub

        String trajPointInpath = "/root/Converging_jjp/data/trajectories/"; //????????????·??
        String clustersOutpath = "/root/Converging_jjp/data/clusters_euclid/";   //??????·??
        String sTime = "2015-04-11 18:00:00.0";    //?????????
        String eTime = "2015-04-11 20:00:00.0";      //???????????


        //???????????????JSON?????CSV???
        int all = getseconds(sTime, eTime);   //??????
        int count = 0;
        System.out.println("正在聚类！   "+ new Date(System.currentTimeMillis()));
        long startruntime = System.currentTimeMillis();
        for (int i = timetonum_s(sTime); i <= timetonum_s(sTime) + all; i+=10) {
            String _tpInpath = trajPointInpath+numsout(alltotime_s(i).substring(8,14))+".txt";
            String _clustersJsonOutpath = clustersOutpath+"json/euclidClusters"+numsout(alltotime_s(i).substring(8,14))+".json";
            String _clustersCsvOutpath = clustersOutpath+"csv/euclidClusters_"+numsout(alltotime_s(i).substring(8,14))+".csv";
            String _MBRCsvOutpath = clustersOutpath+"csv/euclidClustersMBR_"+numsout(alltotime_s(i).substring(8,14))+".csv";
            NewClusteringDBSCAN.outputClusters(_tpInpath, _clustersJsonOutpath,_clustersCsvOutpath,_MBRCsvOutpath);
            count++;
            if(count%200==0)
                System.out.println("完成"+count+"个时间戳下的聚类！   "+ new Date(System.currentTimeMillis()));
        }
        long endruntime = System.currentTimeMillis();
        System.out.println("聚类完成！聚类总时长为 "+(endruntime-startruntime)+" ms!  "+ new Date(System.currentTimeMillis()));
        //??JSON????????????б?
//            List<NewCluster> clusters = ClustersJSONRW.readClustersJson("F:\\java\\NewClustering\\OutputData\\hongkong182000.json");

    }


    /**
     * ??????????ε?????
     * @param start  ??????
     * @param end    ??????
     * @return
     */
    static int getseconds(String start, String end) {
        Timestamp stimestamp = Timestamp.valueOf(start);
        long t1 = stimestamp.getTime();
        Timestamp etimestamp = Timestamp.valueOf(end);
        long t2 = etimestamp.getTime();
        return (int) ((t2 - t1) / 1000);
    }

    /**
     * ?????(???????)???????????
     * @param stime
     * @return
     */
    static int timetonum_s(String stime) {
        int res;
        String a = stime.substring(11, 13);
        int hours = Integer.parseInt(stime.substring(11, 13));
        int minutes = Integer.parseInt(stime.substring(14, 16));
        int second = Integer.parseInt(stime.substring(17, 19));
        res = hours * 60 * 60 + minutes * 60 + second;
        return res;
    }

    /**
     * ?????????????????????????????????????
     * @param all
     * @return
     */
    static String alltotime_s(int all){
        String res="20150411";
        int hours=all/(60*60);
        if(hours<10){
            res=res+"0"+hours;
        }else{
            res=res+hours;
        }
        int minutes=(all%(60*60))/60;
        if(minutes<10){
            res=res+"0"+minutes;
        }
        else{
            res=res+minutes;
        }
        int seconds=(all%(60*60))%60;
        if(seconds<10){
            res=res+"0"+seconds+"0";
        }else{
            res=res+seconds+"0";
        }

        return res;
    }

    /**
     * ??????????hh:mm:ss???????hhmmss??????????
     * @param str
     * @return
     */
    public static String numsout(String str){
        String res="";
        str=str.trim();
        if(str!=null&&!"".equals(str)){
            for(int i=0;i<str.length();i++){
                if(str.charAt(i)>=48&&str.charAt(i)<=57){
                    res+=str.charAt(i);
                }
            }
        }
        return res;
    }
}

