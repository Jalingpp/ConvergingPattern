package Tools;

import MyClass.NewCluster;
import MyClass.NewPoint;
import traj.util.Cluster;
import traj.util.Point;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * ****************临时建立的聚�?*********************
 * Created by jdxg on 2017/4/8.
 */
public class NewDBSCAN {

    private MBR mapScale;//地图边界经纬�?

    private double eps;//领域半径阈�??
    private int minPts;//密度阈�??

    private List<NewPoint> procPoints;//点集合的副本，用于运算（列表序）
    private GridIndex gridIndex;//网格索引
    public List<NewPoint> boundryPoints;//边界点的集合
    private List<NewPoint> corePoints;//核心点的集合
    List<NewCluster> finalClusters;//计算结果 簇集�?

    /**
     * 构�?�函�?
     * @param points 有待聚类的点�?
     */
    public NewDBSCAN(List<NewPoint> points, double eps, int minPts, double minlat, double minlng, double maxlat, double maxlng){
        this.mapScale = new MBR(minlng,minlat,maxlng,maxlat);
        this.eps = eps;
        this.minPts = minPts;
        //初始化点集合
        procPoints = SpToSnap(points);
        //初始化索�?
        gridIndex = new GridIndex(mapScale,eps,procPoints);
    }

    private List<NewPoint> SpToSnap(List<NewPoint> ps){
        List<NewPoint> points = new ArrayList<>();
        for(int i=0;i<ps.size();i++){
            NewPoint p = ps.get(i);
            String id = p.getId();
            double lng = p.getLng();
            double lat = p.getLat();
            String t = p.getTime();
            if(lng>mapScale.minLng && lng<mapScale.maxLng && lat>mapScale.minLat && lat<mapScale.maxLat){
                points.add(new NewPoint(id,lng,lat,t));
            }
        }
        return points;
    }

    /**
     * 核心功能，聚类运�?
     * @return 聚类得到的簇
     */
    public List<NewCluster> getClusters(){
        finalClusters = new ArrayList<>();

        //给所有点打上标记
        markPoints();
        //初始化核心点集合以及边界点集�?
        initBoundryCore();
        //以核心点为骨架初始化簇集�?
        initCoreClusters();
        //�?簇里面添加剩下的边界�?
        initBounderClusters();

        List<NewCluster> resultSet = new ArrayList<>();
        for(int i=0;i<finalClusters.size();i++){
            resultSet.add(new NewCluster(finalClusters.get(i).getId(),finalClusters.get(i).getPointList()));
        }

        return resultSet;
    }

    /**
     * �?簇里面添加剩下的边界�?
     */
    private void initBounderClusters(){
        gridIndex = new GridIndex(mapScale,eps,boundryPoints);
        for (int i=0;i<finalClusters.size();i++){
            insertBounder(finalClusters.get(i));
        }
    }

    /**
     * �?单个簇里面添加边界点
     * @param cluster
     */
    private void insertBounder(NewCluster cluster){
        List<NewPoint> cores = copyNewPoints(cluster.getPointList());
        for(int i=0;i<cores.size();i++){
            List<NewPoint> ps =  gridIndex.getPointsFromGrids(cores.get(i),1);
            ps = getPointNearBy(cores.get(i),ps);
            for(int j=0;j<ps.size();j++){
                cluster.getPointList().add(ps.get(j));
                gridIndex.delete(ps.get(j));
            }
        }
    }

    /**
     * 复制点集�?
     * @return 复制的点集合
     */
    private   List<NewPoint> copyNewPoints(List<NewPoint> points){
        List<NewPoint> copyPoints = new ArrayList<>();
        for(int i=0;i<points.size();i++){
            copyPoints.add(copyNewPoint(points.get(i)));
        }
        return copyPoints;
    }

    /**
     * 拷贝属�?�相同的�?个点
     * @param point 待拷贝的�?
     * @return 拷贝的点
     */
    private   NewPoint copyNewPoint(NewPoint point){
        double lng = point.getLng();
        double lat = point.getLat();
        String time = point.getTime();
        String id = point.getId();
        NewPoint copyPoint = new NewPoint(id,lng,lat,time);
        return  copyPoint;
    }

    /**
     * 以核心点为骨架初始化簇集�?
     */
    private void initCoreClusters(){
        gridIndex = new GridIndex(mapScale,eps,corePoints);

        Set<String> ids = gridIndex.pointsHash.keySet();
        int num = 0;
        while (!ids.isEmpty()){
            for (String id : ids) {
                List<NewPoint> points = new ArrayList<NewPoint>();
                List<NewPoint> ps =  gridIndex.getPointsFromGrids(gridIndex.pointsHash.get(id),1);
                ps = getPointNearBy(gridIndex.pointsHash.get(id),ps);
                NewPoint p = gridIndex.pointsHash.get(id);
                gridIndex.delete(p);
                points = insertCore(p,points);
                this.finalClusters.add(new NewCluster(num+"",points));
                num++;
                ids = gridIndex.pointsHash.keySet();
                break;
            }
        }
    }

    /**
     * 递归添加点集
     * @param core 中心�?
     * @param points
     * @return
     */
    private List<NewPoint> insertCore(NewPoint core,List<NewPoint> points){

        points.add(core);

        List<NewPoint> ps =  gridIndex.getPointsFromGrids(core,1);
        ps = getPointNearBy(core,ps);
        for(int i=0;i<ps.size();i++){
            gridIndex.delete(ps.get(i));
        }
        for(int i=0;i<ps.size();i++){
            points = insertCore(ps.get(i),points);
        }
        return points;
    }

    /**
     * 初始化核心点集合，边界点集合
     */
    private void initBoundryCore(){
        boundryPoints = new ArrayList<NewPoint>();
        corePoints = new ArrayList<NewPoint>();
        for(int i=0;i<procPoints.size();i++){
            if(procPoints.get(i).getMark() == PointMark.BOUNDRY)
                boundryPoints.add(procPoints.get(i));
            if(procPoints.get(i).getMark() == PointMark.CORE)
                corePoints.add(procPoints.get(i));
        }
    }

    /**
     * 标记出所有核心点
     */
    private void markPoints(){
        for(int i=0;i<procPoints.size();i++){
            markPoint(procPoints.get(i));
        }
    }

    /**
     *尝试标记单个点附近的区域
     */
    private void markPoint(NewPoint p){
        List<NewPoint> pointsNearCandi =  gridIndex.getPointsFromGrids(p,1);
        pointsNearCandi = getPointNearBy(p,pointsNearCandi);
        //如果数量大等于密度阈值，那么�?始标�?
        if(pointsNearCandi.size()>=minPts){
            p.setMark(PointMark.CORE);
            for(int i=0;i<pointsNearCandi.size();i++){
                if(pointsNearCandi.get(i).getMark() == PointMark.NOISE)
                    pointsNearCandi.get(i).setMark(PointMark.BOUNDRY);
            }
        }
    }

    /**
     * 获得临近�?(半径为领域密�?)
     * @param core 核心�?
     * @param points 候�?�点
     * @return 邻近的点�?
     */
    private List<NewPoint> getPointNearBy(NewPoint core,List<NewPoint> points){
        if(points.size()<=0)
            return points;
        for(int i = points.size()-1;i>=0;i--){
            //如果距离大于领域半径阈�?�，那么删掉这个�?
            if(compMeterDistance(core,points.get(i))>eps)
                points.remove(i);
        }
        return points;
    }

    /**
     * 点集格式转换
     * @param ps 基本�?
     * @return 扩展�?
     */
    private List<NewPoint> PToSnap(List<NewPoint> ps){
        List<NewPoint> points = new ArrayList<NewPoint>();
        for(int i=0;i<ps.size();i++){
            NewPoint p = ps.get(i);
            String id = p.getId();
            double lng = p.getLng();
            double lat = p.getLat();
            String t = p.getTime();
            points.add(new NewPoint(id,lng,lat,t));
        }
        return points;
    }

    /**
     * 点集格式转换
     * @param ps 扩展�?
     * @return 基本�?
     */
    private List<NewPoint> SnapToP(List<NewPoint> ps){
        List<NewPoint> points = new ArrayList<NewPoint>();
        for(int i=0;i<ps.size();i++){
            NewPoint p = ps.get(i);
            String id = p.getId();
            double lng = p.getLng();
            double lat = p.getLat();
            String t = p.getTime();
            points.add(new NewPoint(id,lng,lat,t));
        }
        return points;
    }

    /**
     * 由两点计算球面距�?
     * @param pa 点a
     * @param pb 点b
     * @return 球面距离
     */
    private double compMeterDistance(NewPoint pa,NewPoint pb) {
        double EARTH_RADIUS = 6378137.0D;
        double lon1 = pa.getLng();
        double lat1 = pa.getLat();
        double lon2 = pb.getLng();
        double lat2 = pb.getLat();
        return compMeterDistance(lon1,lat1,lon2,lat2);
    }

    /**
     * 由经纬度计算球面距离
     * @param lon1 经度1
     * @param lat1 维度1
     * @param lon2 经度2
     * @param lat2 维度2
     * @return 球面距离
     */
    private double compMeterDistance(double lon1,double lat1,double lon2,double lat2){
        double EARTH_RADIUS = 6378137.0D;
        if(lon1 >= 1.0E-6D && lon2 >= 1.0E-6D) {
            double radLat1 = lat1 * 3.141592653589793D / 180.0D;
            double radLat2 = lat2 * 3.141592653589793D / 180.0D;
            double a = radLat1 - radLat2;
            double b = (lon1 - lon2) * 3.141592653589793D / 180.0D;
            double s = 2.0D * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2.0D), 2.0D) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2.0D), 2.0D)));
            s *= 6378137.0D;
            return s;
        } else {
            return 0.0D;
        }
    }

//    /**
//     * �?
//     * Created by jdxg on 2017/3/2.
//     */
//    private class Clusters {
//
//        private String id;//同一帧里面的编号
//        protected Timestamp time;//时间�?
//        public List<SnapPoint> menberPoints;//簇中的点
//        private SnapPoint center;//中心�?
//
//        /**
//         * 构�?�器
//         * @param time 时间�?
//         */
//        public Clusters(String id,Timestamp time,List<SnapPoint> points){
//            this.id = id;
//            this.time = time;
//            this.menberPoints = points;
//        }
//    }

    /**
     * 用来表示二维的索引号
     * Created by jdxg on 2017/3/4.
     */
    private class IndexPoint {

        public int indexLng;//经度索引�?
        public int indexLat;//纬度索引�?

        /**
         * 空的构�?�器
         */
        public IndexPoint(){

        }
        /**
         * 构�?�器
         * @param indexLng 经度方向的下�?
         * @param indexLat 纬度方向的下�?
         */
        public IndexPoint(int indexLng,int indexLat){
            this.indexLng = indexLng;
            this.indexLat = indexLat;
        }

        public String getIndexString(){
            String str = indexLng+","+indexLat;
            return str;
        }
    }

    /**
     * 基于点的网格索引
     * Created by jdxg on 2017/3/4.
     */
    private class GridIndex {
        /**
         * �?小经纬度
         * �?大经纬度
         */
        private double minLng,minLat;
        private double maxLng,maxLat;
        /**
         * 地图宽（东西方向），高（南北方向�?
         */
        private double width,height;
        /**
         * 经度与纬度方向需要划分为多少格，�?共多少格
         */
        private int lngAmount , latAmount , alAmount;
        /**
         * 每个格子的边长（米）
         */
        private double side;
        /**
         * 索引包含的所有点集合（哈希表）（引用�?
         */
        public HashMap<String,NewPoint> pointsHash;
        /**
         * 索引包含的所有点集合（列表）（引用）
         */
        private List<NewPoint> pointsList;
        /**
         * 初始化网格的线程规模，默认为16
         */
        private int listThreaScale = 16;
        /**
         * 填充网格的线程规模，默认�?16
         */
        private int fillThreadScale = 16;
        /**
         * 网格列表,行表示经度划分，列表示经度划�?
         */
        private Grid[][] grids;

        /**
         * 构�?�器
         * @param side 网格边长（米为单位）
         * @param ps �?要建立索引的点集
         */
        public GridIndex(MBR mapScale,double side, List<NewPoint> ps){
            this.side = side;
            //将点的引用加进来
            pointsHash = new HashMap<String,NewPoint>();
            for(int i=0;i<ps.size();i++){
                pointsHash.put(ps.get(i).getId(),ps.get(i));
            }
            pointsList = ps;
            //初始化经纬度边界
            initLngLat(mapScale);

            //初始化网格数�?
            initGrids();
        }

        /**
         * 初始化经纬度边界
         */
        private void initLngLat(MBR mapScale){
            this.minLng = mapScale.minLng;
            this.minLat = mapScale.minLat;
            this.maxLng = mapScale.maxLng;
            this.maxLat = mapScale.maxLat;
        }

        /**
         * 初始化网格数�?
         */
        private void initGrids(){

            //计算平面长宽
            double disLng = compMeterDistance(minLng,minLat,maxLng,minLat);
            double disLat = compMeterDistance(minLng,minLat,minLng,maxLat);
            //计算长宽划分网格数量
            this.lngAmount = ((int)(disLng/side))+1;
            this.latAmount = ((int)(disLat/side))+1;
            this.alAmount = lngAmount*latAmount;
            //建立网格二位数组
            this.grids = new Grid[lngAmount][latAmount];

            //初始化网格列�?,无用网格太多，�?�择不初始化
            //initGridsSub();
            //填充网格
            fillGrids();
        }

        /**
         * 由经纬度计算球面距离
         * @param lon1 经度1
         * @param lat1 维度1
         * @param lon2 经度2
         * @param lat2 维度2
         * @return 球面距离
         */
        public double compMeterDistance(double lon1,double lat1,double lon2,double lat2){
            double EARTH_RADIUS = 6378137.0D;
            if(lon1 >= 1.0E-6D && lon2 >= 1.0E-6D) {
                double radLat1 = lat1 * 3.141592653589793D / 180.0D;
                double radLat2 = lat2 * 3.141592653589793D / 180.0D;
                double a = radLat1 - radLat2;
                double b = (lon1 - lon2) * 3.141592653589793D / 180.0D;
                double s = 2.0D * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2.0D), 2.0D) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2.0D), 2.0D)));
                s *= 6378137.0D;
                return s;
            } else {
                return 0.0D;
            }
        }

        /**
         * 初始化网格的列表
         */
        private void initGridsSub(){
//        //初始化线程池
//        int threadScale = listThreaScale;
//        if(threadScale >= alAmount)
//            threadScale = alAmount-1;
//        initListThread[] threads = new initListThread[threadScale];
//        int taskAmount = alAmount/(threadScale-1);
//        for(int i=0;i<threadScale;i++) {
//            threads[i] = new initListThread(i*taskAmount,(i+1)*taskAmount-1);
//        }
//        //启动线程
//        for(int i=0;i<threadScale;i++) {
//            threads[i].start();
//        }
//        //等待线程
//        try {
//            for (int i=0;i<threadScale;i++) {
//                threads[i].join();
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

            for(int i=0;i<lngAmount;i++){
                for(int j=0;j<latAmount;j++){
                    grids[i][j] = new Grid();
                }
            }
        }

        /**
         * 根据索引获得相应网格中的点集�?
         * @param indexLng 经度索引
         * @param indexLat 纬度索引
         * @return 集合副本
         */
        public List<NewPoint> getPointsFromGrid(int indexLng,int indexLat){
            List<NewPoint> points = new ArrayList<>();
            //溢出�?�?
            if(indexLng<0 || indexLng>= lngAmount || indexLat<0 || indexLat>=latAmount)
                return points;
            if( grids[indexLng][indexLat] == null)
                return points;
            List<String> sb = grids[indexLng][indexLat].substances;
            for(int i=0;i<sb.size();i++){
                points.add(pointsHash.get(sb.get(i)));
            }
            return points;
        }

        /**
         * 搜索给定点附近的网格中的�?
         * @param p 搜索中心�?
         * @param n 搜索范围 n不能为负
         * @return 附近的点集合
         */
        public List<NewPoint> getPointsFromGrids(NewPoint p,int n){
            List<NewPoint> points = new ArrayList<>();
            //溢出�?�?
            if(n<0)
                return points;
            //获取网格�?
            IndexPoint indexPoint = getIndex(p);
            int indexLng = indexPoint.indexLng;
            int indexLat = indexPoint.indexLat;
            //�?始搜�?
            for(int i=indexLng-n;i<=indexLng+n;i++){
                for(int j=indexLat-n;j<=indexLat+n;j++){
                    List<NewPoint> ps = getPointsFromGrid(i,j);
                    for(int x=0;x<ps.size();x++){
                        points.add(ps.get(x));
                    }
                }
            }
            return points;
        }

        /**
         * 将点填入网格�?
         */
        private void fillGrids(){
//        int threadScale = fillThreadScale;
//        if(threadScale >= pointsList.size())
//            threadScale = pointsList.size()-1;
//        fillGridThread[] threads = new fillGridThread[threadScale];
//        int taskAmount = pointsList.size()/(threadScale-1);
//        for(int i=0;i<threadScale;i++) {
//            threads[i] = new fillGridThread(i*taskAmount,(i+1)*taskAmount-1);
//        }
//        //启动线程
//        for(int i=0;i<threadScale;i++) {
//            threads[i].start();
//        }
//        //等待线程
//        try {
//            for (int i=0;i<threadScale;i++) {
//                threads[i].join();
//            }
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
            for(int i=0;i<pointsList.size();i++){
                IndexPoint indexPoint = getIndex(pointsList.get(i));
                if( indexPoint != null)
                    if(grids[indexPoint.indexLng][indexPoint.indexLat] == null)
                        grids[indexPoint.indexLng][indexPoint.indexLat] = new Grid();
                grids[indexPoint.indexLng][indexPoint.indexLat].substances.add(pointsList.get(i).getId());
            }
        }

        /**
         * 删去�?个点
         * @param p 要删去的�?
         */
        public void delete(NewPoint p){
            IndexPoint indexPoint = getIndex(p);
            //删去网格中的索引
            List<String> sub = grids[indexPoint.indexLng][indexPoint.indexLat].substances;
            for(int i=0;i<sub.size();i++){
                if(sub.get(i).equals(p.getId()))
                    sub.remove(i);
            }
            //删去字典中的这个�?
            pointsHash.remove(p.getId());
        }

        /**
         * 计算�?属下标的�?
         * @param point �?要计算所属下标的�?
         * @return 下标
         */
        public IndexPoint getIndex(NewPoint point){
            if(point.getLng()<minLng || point.getLat()<minLat || point.getLng()>maxLng || point.getLat()>maxLat)
                return null;
            return getIndex(point.getLng(),point.getLat());
        }

        /**
         * * 计算�?属下标的�?
         * @param lng 经度
         * @param lat 纬度
         * @return 下标
         */
        public IndexPoint getIndex(double lng,double lat){
            if(lng<minLng || lat<minLat || lng>maxLng || lat>maxLat)
                return null;
            IndexPoint indexPoint = new IndexPoint();
            double disLng = compMeterDistance(lng,minLat,minLng,minLat);
            double disLat = compMeterDistance(minLng,lat,minLng,minLat);
            indexPoint.indexLng = (int)(disLng/side);
            indexPoint.indexLat = (int)(disLat/side);
            return indexPoint;
        }

        /**
         * 设置初始化列表线程规�?
         */
        public void setListThreaScale(int s){
            this.listThreaScale = s;
        }

        /**
         * @return
         */
        public int getListThreaScale(){
            return this.listThreaScale;
        }

        /**
         * 设置填充线程规模
         */
        public void setFillThreadScale(int s){
            this.fillThreadScale = s;
        }

        /**
         * 获得填充线程规模
         * @return
         */
        public int getFillThreadScale(){
            return this.fillThreadScale;
        }

        /**
         * 获取�?小经�?
         * @return
         */
        public double getMinLng(){
            return this.minLng;
        }

        /**
         * 获取�?小纬�?
         * @return
         */
        public double getMinLat(){
            return this.minLat;
        }

        /**
         * 获取�?大经�?
         * @return
         */
        public double getMaxLng(){
            return  this.maxLng;
        }

        /**
         * 获取�?大纬�?
         * @return
         */
        public double getMaxLat(){
            return  this.maxLat;
        }


        /**
         * 基于点的网格�?
         */
        class Grid{
            /**
             * 网格包含的点的id集合
             */
            public List<String> substances;

            public Grid(){
                this.substances = new ArrayList<String>();
            }

        }

        /**
         * 初始化表格的线程
         */
        class initListThread extends Thread{
            /**
             * 处理网格下标的头和尾（包含）
             */
            int begin , end;

            public initListThread(int begin,int end) {
                this.begin = begin;
                this.end = end;
            }

            @Override
            public void run(){
                for(int i=begin;i<=end;i++){
                    //溢出�?�?
                    if(i>= alAmount)
                        break;;
                    int indlng = i/latAmount;
                    int indLat = i%latAmount;
                    grids[indlng][indLat] = new Grid();
                }

            }
        }
        /**
         * 填充表格的线�?
         */
        class fillGridThread extends Thread{
            /**
             * 处理点集的头和尾（包含）
             */
            int begin , end;

            /**
             * 构�?�器
             * @param begin 处理列表的头
             * @param end 处理列表的尾
             */
            public fillGridThread(int begin,int end){
                this.begin = begin;
                this.end = end;
            }

            @Override
            public void run(){
                for(int i=begin;i<=end;i++){
                    //溢出�?�?
                    if(i>=pointsList.size())
                        break;
                    IndexPoint indexPoint = getIndex(pointsList.get(i));
                    grids[indexPoint.indexLng][indexPoint.indexLat].substances.add(pointsList.get(i).getId());
                }
            }
        }
    }

    /**
     * �?小外包矩�?
     */
    private static class MBR {

        public double minLng;//�?小经�?
        public double minLat;//�?小纬�?
        public double maxLng;//�?大经�?
        public double maxLat;//�?大纬�?

        /**
         * 构�?�器
         * @param minLng �?小经�?
         * @param minLat �?小纬�?
         * @param maxLng �?大经�?
         * @param maxLat �?大纬�?
         */
        public MBR(double minLng,double minLat,double maxLng,double maxLat){
            this.minLng = minLng;
            this.minLat = minLat;
            this.maxLng = maxLng;
            this.maxLat = maxLat;
        }
    }

//    /**
//     * 扩展�?
//     */
//    private class NewPoint extends Point {
//
//        protected String id;//点的id，对应所属轨迹的id
//        protected PointMark mark;//点在聚类过程中的标记
//
//        /**
//         *
//         * @param lng 经度
//         * @param lat 维度
//         * @param time 位于时间�?
//         * @param id id
//         */
//        public NewPoint(double lng, double lat, String time, String id) {
//            Timestamp t = Timestamp.valueOf(time);
//            super(lng,lat,t);
//            this.id = id;
//            //初始�?有点标记为噪�?
//            this.mark = PointMark.NOISE;
//        }
//
//        /**
//         * @return 点的id，对应轨迹id即移动对象id
//         */
//        public String getId(){
//            return this.id;
//        }
//
//        public String toString() {
//            String str = this.getId() + "," + this.getLng() + "," + this.getLat();
//            if(this.getTime() != null) {
//                str = str + "," + this.getTime().toString();
//            }
//
//            if(this.getReservation() != null) {
//                str = str + "," + this.getReservation();
//            }
//
//            return str;
//        }
//
//        /**
//         * 设置标记
//         * @param pm 标记
//         */
//        public void setMark(PointMark pm){
//            this.mark = pm;
//        }
//
//        /**
//         * @return 标记
//         */
//        public PointMark getMark(){
//            return this.mark;
//        }
//
//    }
//

    /**
     * 点的标记枚举
     */
    public enum PointMark {
        NOISE,//噪声�?
        BOUNDRY,//边界�?
        CORE//核心�?
    }

}
