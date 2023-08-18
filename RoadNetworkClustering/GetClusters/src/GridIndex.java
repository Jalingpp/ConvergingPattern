import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



/**
 * 用来表示二维的索引号
 */
class IndexPoint {

    public int indexLng;//经度索引号
    public int indexLat;//纬度索引号

    /**
     * 空的构造器
     */
    public IndexPoint(){

    }
    /**
     * 构造器
     * @param indexLng 经度方向的下标
     * @param indexLat 纬度方向的下标
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
 */
class GridIndex {
    /**
     * 最小经纬度
     * 最大经纬度
     */
    private double minLng,minLat;
    private double maxLng,maxLat;
    /**
     * 地图宽（东西方向），高（南北方向）
     */
    private double width,height;
    /**
     * 经度与纬度方向需要划分为多少格，一共多少格
     */
    private int lngAmount , latAmount , alAmount;
    /**
     * 每个格子的边长（米）
     */
    private double side;
    /**
     * 索引包含的所有点集合（哈希表）（引用）
     */
    public HashMap<String,SnapPoint> pointsHash;
    /**
     * 索引包含的所有点集合（列表）（引用）
     */
    private List<SnapPoint> pointsList;
    /**
     * 网格列表,行表示经度划分，列表示经度划分
     */
    private Grid[][] grids;



    public void clear(){
        pointsHash.clear();
        pointsList.clear();
    }

    /**
     * 构造器
     * @param side 网格边长（米为单位）
     * @param ps 需要建立索引的点集
     */
    public GridIndex(MBR mapScale,double side, List<SnapPoint> ps){
        this.side = side;
        //将点的引用加进来
        pointsHash = new HashMap<String,SnapPoint>();
        for(int i=0;i<ps.size();i++){
            pointsHash.put(ps.get(i).getId(),ps.get(i));
        }
        pointsList = ps;
        //初始化经纬度边界
        initLngLat(mapScale);

        //初始化网格数组
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
     * 初始化网格数组
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

        //初始化网格列表,无用网格太多，选择不初始化
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
        for(int i=0;i<lngAmount;i++){
            for(int j=0;j<latAmount;j++){
                grids[i][j] = new Grid();
            }
        }
    }

    /**
     * 根据索引获得相应网格中的点集合
     * @param indexLng 经度索引
     * @param indexLat 纬度索引
     * @return 集合副本
     */
    public List<SnapPoint> getPointsFromGrid(int indexLng,int indexLat){
        List<SnapPoint> points = new ArrayList<SnapPoint>();
        //溢出检查
//        if(indexLng<0 || indexLng>= lngAmount || indexLat<0 || indexLat>=latAmount)
//            return points;
        if( grids[indexLng][indexLat] == null)
            return points;
        List<String> sb = grids[indexLng][indexLat].substances;
        for(int i=0;i<sb.size();i++){
            points.add(pointsHash.get(sb.get(i)));
        }
        return points;
    }

    /**
     * 搜索给定点附近的网格中的点
     * @param p 搜索中心点
     * @param n 搜索范围 n不能为负
     * @return 附近的点集合
     */
    public List<SnapPoint> getPointsFromGrids(SnapPoint p,int n){
        List<SnapPoint> points = new ArrayList<SnapPoint>();
        //溢出检查
        if(n<0)
            return points;
        //获取网格号
        IndexPoint indexPoint = getIndex(p);
        int indexLng = indexPoint.indexLng;
        int indexLat = indexPoint.indexLat;
        //开始搜索
        for(int i=indexLng-n;i<=indexLng+n;i++){
            for(int j=indexLat-n;j<=indexLat+n;j++){
                List<SnapPoint> ps = getPointsFromGrid(i,j);
                for(int x=0;x<ps.size();x++){
                    if(!ps.get(x).equals(p)){
                        points.add(ps.get(x));
                    }
                }
            }
        }
        return points;
    }

    /**
     * 将点填入网格中
     */
    private void fillGrids(){
        for(int i=0;i<pointsList.size();i++){
            IndexPoint indexPoint = getIndex(pointsList.get(i));
            if( indexPoint != null)
                if(grids[indexPoint.indexLng][indexPoint.indexLat] == null)
                    grids[indexPoint.indexLng][indexPoint.indexLat] = new Grid();
            grids[indexPoint.indexLng][indexPoint.indexLat].substances.add(pointsList.get(i).getId());
        }
    }

    /**
     * 删去一个点
     * @param p 要删去的点
     */
    public void delete(SnapPoint p){
        IndexPoint indexPoint = getIndex(p);
        //删去网格中的索引
        List<String> sub = grids[indexPoint.indexLng][indexPoint.indexLat].substances;
        for(int i=0;i<sub.size();i++){
            if(sub.get(i).equals(p.getId()))
                sub.remove(i);
        }
        //删去字典中的这个点
        pointsHash.remove(p.getId());
        sub = grids[indexPoint.indexLng][indexPoint.indexLat].substances;
    }

    /**
     * 计算所属下标的点
     * @param point 需要计算所属下标的点
     * @return 下标
     */
    public IndexPoint getIndex(SnapPoint point){
//        if(point.getLng()<minLng || point.getLat()<minLat || point.getLng()>maxLng || point.getLat()>maxLat)
//            return null;
        return getIndex(point.getLng(),point.getLat());
    }

    /**
     * * 计算所属下标的点
     * @param lng 经度
     * @param lat 纬度
     * @return 下标
     */
    public IndexPoint getIndex(double lng,double lat){
//        if(lng<minLng || lat<minLat || lng>maxLng || lat>maxLat)
//            return null;
        IndexPoint indexPoint = new IndexPoint();
        double disLng = compMeterDistance(lng,minLat,minLng,minLat);
        double disLat = compMeterDistance(minLng,lat,minLng,minLat);
        indexPoint.indexLng = (int)(disLng/side);
        indexPoint.indexLat = (int)(disLat/side);
        return indexPoint;
    }


    /**
     * 获取最小经度
     * @return
     */
    public double getMinLng(){
        return this.minLng;
    }

    /**
     * 获取最小纬度
     * @return
     */
    public double getMinLat(){
        return this.minLat;
    }

    /**
     * 获取最大经度
     * @return
     */
    public double getMaxLng(){
        return  this.maxLng;
    }

    /**
     * 获取最大纬度
     * @return
     */
    public double getMaxLat(){
        return  this.maxLat;
    }


    /**
     * 基于点的网格类
     */
    class Grid{
        /**
         * 网格包含的点的id集合
         */
        public List<String> substances;

        public Grid(){
            this.substances = new ArrayList<String>();
        }

        public void clear(){
            if(substances!=null)
                this.substances.clear();
        }

    }
}


