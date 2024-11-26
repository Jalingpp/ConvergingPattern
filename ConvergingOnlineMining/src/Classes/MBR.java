package Classes;

import Tools.TimeConversion;

/**
 * Created by JJP on 20200224
 * 最小外接矩形
 */
public class MBR {

    private double maxLat;
    private double maxLng;
    private double minLat;
    private double minLng;

    public MBR() {
        maxLat = 0.0;
        maxLng = 0.0;
        minLat = 90.0;
        minLng = 180.0;
    }
    public MBR(MBR mbr){
        this.maxLng = mbr.maxLng;
        this.minLng = mbr.minLng;
        this.maxLat = mbr.maxLat;
        this.minLat = mbr.minLat;
    }

    public MBR(SnapObject object){
        this.minLng = object.getLng();
        this.minLat = object.getLat();
        this.maxLat = object.getLat();
        this.maxLng = object.getLng();
    }

    public MBR(double maxLat, double maxLng, double minLat, double minLng) {
        this.maxLat = maxLat;
        this.maxLng = maxLng;
        this.minLat = minLat;
        this.minLng = minLng;
    }

    /**
     * 判断MBR是否包含某个位置点
     * @param location
     * @return
     */
    public boolean isIncludeLocation(Location location){
        if(location.getLat()>=this.getMinLat()&&location.getLat()<=this.getMaxLat()
                &&location.getLng()>=this.getMinLng()&&location.getLng()<=this.getMaxLng())
            return true;
        return false;
    }

    /**
     * 计算MBR的面积
     * @return
     */
    public double getArea(){
        return TimeConversion.getDistance(maxLat,minLng,maxLat,maxLng)*TimeConversion.getDistance(maxLat,minLng,minLat,minLng);
    }

    /**
     * 获取两个MBR的交集
     * @param mbr
     * @return
     */
    public MBR insectWith(MBR mbr){
        if(isOverlap(mbr)){
            double newMaxLng = this.maxLng<mbr.maxLng? this.maxLng:mbr.maxLng;
            double newMinLng = this.minLng>mbr.minLng? this.minLng:mbr.minLng;
            double newMaxLat = this.maxLat<mbr.maxLat? this.maxLat:mbr.maxLat;
            double newMinLat = this.minLat>mbr.minLat? this.minLat:mbr.minLat;
            return new MBR(newMaxLat,newMaxLng,newMinLat,newMinLng);
        }
        return new MBR(0,0,0,0);
    }

    /**
     * 获取两个MBR的并MBR
     * @param mbr 与该MBR的相合并的MBR
     * @return  两个MBR的并
     */
    public MBR combineWith(MBR mbr){
        double maxLat = this.maxLat>mbr.maxLat? this.maxLat:mbr.maxLat;
        double minLat = this.minLat<mbr.minLat? this.minLat:mbr.minLat;
        double maxLng = this.maxLng>mbr.maxLng? this.maxLng:mbr.maxLng;
        double minLng = this.minLng<mbr.minLng? this.minLng:mbr.minLng;
        return new MBR(maxLat,maxLng,minLat,minLng);
    }

    /**
     * 判断两个mbr是否存在重叠
     * @param mbr
     * @return  重叠返回true，否则返回false
     */
    public boolean isOverlap(MBR mbr){
        if(this.maxLat<mbr.minLat)
            return false;
        else if(this.minLat>mbr.maxLat)
            return false;
        else if(this.minLng>mbr.maxLng)
            return false;
        else if(this.maxLng<mbr.minLng)
            return false;
        else
            return true;
    }


    public double getMaxLat() {
        return maxLat;
    }

    public void setMaxLat(double maxLat) {
        this.maxLat = maxLat;
    }

    public double getMaxLng() {
        return maxLng;
    }

    public void setMaxLng(double maxLng) {
        this.maxLng = maxLng;
    }

    public double getMinLat() {
        return minLat;
    }

    public void setMinLat(double minLat) {
        this.minLat = minLat;
    }

    public double getMinLng() {
        return minLng;
    }

    public void setMinLng(double minLng) {
        this.minLng = minLng;
    }


    public static void main(String[] args){
        MBR mbr1 = new MBR(31.2,121.2,31.1,121.1);
        MBR mbr2 = new MBR(35.5,126.6,29.9,119.9);
        MBR mbr3 = mbr1.combineWith(mbr2);

        int a=0;
    }
}
