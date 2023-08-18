import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;



/**
 * ������ʾ��ά��������
 */
class IndexPoint {

    public int indexLng;//����������
    public int indexLat;//γ��������

    /**
     * �յĹ�����
     */
    public IndexPoint(){

    }
    /**
     * ������
     * @param indexLng ���ȷ�����±�
     * @param indexLat γ�ȷ�����±�
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
 * ���ڵ����������
 */
class GridIndex {
    /**
     * ��С��γ��
     * ���γ��
     */
    private double minLng,minLat;
    private double maxLng,maxLat;
    /**
     * ��ͼ���������򣩣��ߣ��ϱ�����
     */
    private double width,height;
    /**
     * ������γ�ȷ�����Ҫ����Ϊ���ٸ�һ�����ٸ�
     */
    private int lngAmount , latAmount , alAmount;
    /**
     * ÿ�����ӵı߳����ף�
     */
    private double side;
    /**
     * �������������е㼯�ϣ���ϣ�������ã�
     */
    public HashMap<String,SnapPoint> pointsHash;
    /**
     * �������������е㼯�ϣ��б������ã�
     */
    private List<SnapPoint> pointsList;
    /**
     * �����б�,�б�ʾ���Ȼ��֣��б�ʾ���Ȼ���
     */
    private Grid[][] grids;



    public void clear(){
        pointsHash.clear();
        pointsList.clear();
    }

    /**
     * ������
     * @param side ����߳�����Ϊ��λ��
     * @param ps ��Ҫ���������ĵ㼯
     */
    public GridIndex(MBR mapScale,double side, List<SnapPoint> ps){
        this.side = side;
        //��������üӽ���
        pointsHash = new HashMap<String,SnapPoint>();
        for(int i=0;i<ps.size();i++){
            pointsHash.put(ps.get(i).getId(),ps.get(i));
        }
        pointsList = ps;
        //��ʼ����γ�ȱ߽�
        initLngLat(mapScale);

        //��ʼ����������
        initGrids();
    }

    /**
     * ��ʼ����γ�ȱ߽�
     */
    private void initLngLat(MBR mapScale){
        this.minLng = mapScale.minLng;
        this.minLat = mapScale.minLat;
        this.maxLng = mapScale.maxLng;
        this.maxLat = mapScale.maxLat;
    }

    /**
     * ��ʼ����������
     */
    private void initGrids(){

        //����ƽ�泤��
        double disLng = compMeterDistance(minLng,minLat,maxLng,minLat);
        double disLat = compMeterDistance(minLng,minLat,minLng,maxLat);
        //���㳤������������
        this.lngAmount = ((int)(disLng/side))+1;
        this.latAmount = ((int)(disLat/side))+1;
        this.alAmount = lngAmount*latAmount;
        //���������λ����
        this.grids = new Grid[lngAmount][latAmount];

        //��ʼ�������б�,��������̫�࣬ѡ�񲻳�ʼ��
        //initGridsSub();
        //�������
        fillGrids();
    }

    /**
     * �ɾ�γ�ȼ����������
     * @param lon1 ����1
     * @param lat1 ά��1
     * @param lon2 ����2
     * @param lat2 ά��2
     * @return �������
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
     * ��ʼ��������б�
     */
    private void initGridsSub(){
        for(int i=0;i<lngAmount;i++){
            for(int j=0;j<latAmount;j++){
                grids[i][j] = new Grid();
            }
        }
    }

    /**
     * �������������Ӧ�����еĵ㼯��
     * @param indexLng ��������
     * @param indexLat γ������
     * @return ���ϸ���
     */
    public List<SnapPoint> getPointsFromGrid(int indexLng,int indexLat){
        List<SnapPoint> points = new ArrayList<SnapPoint>();
        //������
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
     * ���������㸽���������еĵ�
     * @param p �������ĵ�
     * @param n ������Χ n����Ϊ��
     * @return �����ĵ㼯��
     */
    public List<SnapPoint> getPointsFromGrids(SnapPoint p,int n){
        List<SnapPoint> points = new ArrayList<SnapPoint>();
        //������
        if(n<0)
            return points;
        //��ȡ�����
        IndexPoint indexPoint = getIndex(p);
        int indexLng = indexPoint.indexLng;
        int indexLat = indexPoint.indexLat;
        //��ʼ����
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
     * ��������������
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
     * ɾȥһ����
     * @param p Ҫɾȥ�ĵ�
     */
    public void delete(SnapPoint p){
        IndexPoint indexPoint = getIndex(p);
        //ɾȥ�����е�����
        List<String> sub = grids[indexPoint.indexLng][indexPoint.indexLat].substances;
        for(int i=0;i<sub.size();i++){
            if(sub.get(i).equals(p.getId()))
                sub.remove(i);
        }
        //ɾȥ�ֵ��е������
        pointsHash.remove(p.getId());
        sub = grids[indexPoint.indexLng][indexPoint.indexLat].substances;
    }

    /**
     * ���������±�ĵ�
     * @param point ��Ҫ���������±�ĵ�
     * @return �±�
     */
    public IndexPoint getIndex(SnapPoint point){
//        if(point.getLng()<minLng || point.getLat()<minLat || point.getLng()>maxLng || point.getLat()>maxLat)
//            return null;
        return getIndex(point.getLng(),point.getLat());
    }

    /**
     * * ���������±�ĵ�
     * @param lng ����
     * @param lat γ��
     * @return �±�
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
     * ��ȡ��С����
     * @return
     */
    public double getMinLng(){
        return this.minLng;
    }

    /**
     * ��ȡ��Сγ��
     * @return
     */
    public double getMinLat(){
        return this.minLat;
    }

    /**
     * ��ȡ��󾭶�
     * @return
     */
    public double getMaxLng(){
        return  this.maxLng;
    }

    /**
     * ��ȡ���γ��
     * @return
     */
    public double getMaxLat(){
        return  this.maxLat;
    }


    /**
     * ���ڵ��������
     */
    class Grid{
        /**
         * ��������ĵ��id����
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


