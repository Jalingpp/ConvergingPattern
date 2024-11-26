package Tools;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/**
 * Created by JJP on 20200224
 * 时间字符串转换工具类
 */
public class TimeConversion {

    /**
     * 获取当前时间戳后k个时间戳的String
     * @param nowStamp  当前时间戳
     * @param k  后k个
     * @param timeInterval  时间间隔
     * @return
     */
    public static String getNextKTimeString(String nowStamp,int k,int timeInterval){
        Timestamp nStamp = Timestamp.valueOf(nowStamp);
        long rs = nStamp.getTime()+k*timeInterval;
        SimpleDateFormat timeformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.0");
        return timeformat.format(new Date(rs));
    }

    /**
     * 根据时间字符串，获取两个时刻戳相差的时间片数
     * @param start
     * @param end
     * @param timeInterval
     * @return
     */
    public static int getTimestampAmount(String start,String end,int timeInterval){
        Timestamp stimestamp=Timestamp.valueOf(start);
        long t1=stimestamp.getTime();
        Timestamp etimestamp=Timestamp.valueOf(end);
        long t2=etimestamp.getTime();
        return (int)((t2-t1)/timeInterval);
    }

    /**
     * 根据时间字符串转化为输入输出路径
     * @param str
     * @return
     */
    public static String timeToOutpath(String str){
        String[] splitStr= str.split(" ");
        String res = "";
        str=splitStr[1].substring(0,8).trim();
        if(str!=null&&!"".equals(str)){
            for(int i=0;i<str.length();i++){
                if(str.charAt(i)>=48&&str.charAt(i)<=57){
                    res+=str.charAt(i);
                }
            }
        }
        return res;
    }

    /**
     * 判断t1是否在t2之后
     * @param t1
     * @param t2
     * @return true：t1在t2之后
     */
    public static boolean isAfter(String t1,String t2){
        Timestamp ts1 = Timestamp.valueOf(t1);
        Timestamp ts2 = Timestamp.valueOf(t2);
        if(ts1.after(ts2))
            return true;
        return false;
    }


    private static double EARTH_RADIUS = 6378.137;
    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }
    /**
     * 通过经纬度获取距离(单位：米)
     * @param lat1
     * @param lng1
     * @param lat2
     * @param lng2
     * @return
     */
    public static double getDistance(double lat1, double lng1, double lat2,double lng2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000d) / 10000d;
        s = s*1000;
        return s;
    }
}
