package Code;

import MyClass.NewCluster;
import MyClass.NewPoint;
import Tools.*;

import java.io.*;
import java.sql.Timestamp;
import java.util.*;

import com.alibaba.fastjson.*;

import sun.applet.Main;
import traj.util.Cluster;
import traj.util.Point;

public class NewClusteringDBSCAN {

    /**
     * 读原始时空轨迹数据，聚类，输出簇为JSON文件
     * @param trajPointsInpath   输入移动对象点文件路径+文件名
     * @param clustersJsonOutpath  输出JSON路径+文件名
     * @param clustersCsvOutpath  输出CSV路径+文件名
     * @param mbrCsvOutpath  输出MBR的CSV路径+文件名
     */
    public static void outputClusters(String trajPointsInpath,String clustersJsonOutpath,String clustersCsvOutpath,String mbrCsvOutpath){

        try{
            //读取原始时空轨迹点
            List<NewPoint> ps = readPointsFromfile(trajPointsInpath);
//            System.out.println(ps.get(0).getTime());
//            System.out.println(ps.size());


            //聚类
            NewDBSCAN subDbscan = new NewDBSCAN(ps, 200, 4, 30, 120, 32.3, 122.3);

            List<NewCluster> clusters = subDbscan.getClusters();

            //输出为Json文件
            ClustersJSONRW.writeJson(clustersJsonOutpath,clusters);
            //输出为CSV文件
            //将簇列表写成CSV文件
            ClustersCSVRW.writeCSV(clustersCsvOutpath, clusters);
            //将簇的MBR写成CSV文件
            ClustersCSVRW.writeMBRtoCSV(mbrCsvOutpath, clusters);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * 缁勭粐缁撴灉闆?
     * @param clusters
     * @return
     */
    public static JSONObject getResultSet(List<Cluster<Point>> clusters){

        JSONObject res = new JSONObject();

        //绨囩殑涓暟
        res.put("ClusterSize",clusters.size());

        //绨囩殑鍒楄〃
        JSONArray clusArr = new JSONArray();
        for(int i=0;i<clusters.size();i++){
            JSONArray _c = getClusRes(clusters.get(i));
            clusArr.add(_c);
        }
        res.put("Clusters",clusArr);

        return res;
    }


    /**
     * 缁勭粐鍗曚釜绨囩殑缁撴灉
     * @param cluster
     * @return
     */
    public static JSONArray getClusRes(Cluster<Point> cluster){

        List<Point> ps = cluster.getSpatialObjectList();

        JSONArray clusJson = new JSONArray();
        for(int i=0;i<ps.size();i++){
            JSONObject _p = new JSONObject();
            _p.put("Id",ps.get(i).getId());
            _p.put("Lat",ps.get(i).getLat());
            _p.put("Lng",ps.get(i).getLng());
            _p.put("Time",ps.get(i).getTime().toString());
            clusJson.add(_p);
        }

        return clusJson;

    }

    /**
     * 从文本文件读入一个时间戳下的点集List<Point>
     * @param inputPath
     * @return
     * @throws IOException
     */
    public static List<NewPoint> readPointsFromfile(String inputPath) throws IOException {
        List<NewPoint> points = new ArrayList<NewPoint>();
        File file = new File(inputPath);
        if (file.isFile() && file.exists()) { //判断文件是否存在
            InputStreamReader read = new InputStreamReader(new FileInputStream(file), "UTF-8");//考虑到编码格式
            BufferedReader bufferedReader = new BufferedReader(read);
            String lineTxt;
            String[] pointInfor;
            while ((lineTxt = bufferedReader.readLine()) != null) {   //读一行数据
                pointInfor = lineTxt.split(",");
                NewPoint _p = new NewPoint();
                _p.setId(pointInfor[0]);
                _p.setLng(Double.parseDouble(pointInfor[1]));
                _p.setLat(Double.parseDouble(pointInfor[2]));
                _p.setTime(pointInfor[5]);
                points.add(_p);
            }
            read.close();
        }
        return points;
    }

}
