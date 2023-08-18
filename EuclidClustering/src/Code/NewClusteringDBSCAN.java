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
     * ��ԭʼʱ�չ켣���ݣ����࣬�����ΪJSON�ļ�
     * @param trajPointsInpath   �����ƶ�������ļ�·��+�ļ���
     * @param clustersJsonOutpath  ���JSON·��+�ļ���
     * @param clustersCsvOutpath  ���CSV·��+�ļ���
     * @param mbrCsvOutpath  ���MBR��CSV·��+�ļ���
     */
    public static void outputClusters(String trajPointsInpath,String clustersJsonOutpath,String clustersCsvOutpath,String mbrCsvOutpath){

        try{
            //��ȡԭʼʱ�չ켣��
            List<NewPoint> ps = readPointsFromfile(trajPointsInpath);
//            System.out.println(ps.get(0).getTime());
//            System.out.println(ps.size());


            //����
            NewDBSCAN subDbscan = new NewDBSCAN(ps, 200, 4, 30, 120, 32.3, 122.3);

            List<NewCluster> clusters = subDbscan.getClusters();

            //���ΪJson�ļ�
            ClustersJSONRW.writeJson(clustersJsonOutpath,clusters);
            //���ΪCSV�ļ�
            //�����б�д��CSV�ļ�
            ClustersCSVRW.writeCSV(clustersCsvOutpath, clusters);
            //���ص�MBRд��CSV�ļ�
            ClustersCSVRW.writeMBRtoCSV(mbrCsvOutpath, clusters);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    /**
     * 组织结果�?
     * @param clusters
     * @return
     */
    public static JSONObject getResultSet(List<Cluster<Point>> clusters){

        JSONObject res = new JSONObject();

        //簇的个数
        res.put("ClusterSize",clusters.size());

        //簇的列表
        JSONArray clusArr = new JSONArray();
        for(int i=0;i<clusters.size();i++){
            JSONArray _c = getClusRes(clusters.get(i));
            clusArr.add(_c);
        }
        res.put("Clusters",clusArr);

        return res;
    }


    /**
     * 组织单个簇的结果
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
     * ���ı��ļ�����һ��ʱ����µĵ㼯List<Point>
     * @param inputPath
     * @return
     * @throws IOException
     */
    public static List<NewPoint> readPointsFromfile(String inputPath) throws IOException {
        List<NewPoint> points = new ArrayList<NewPoint>();
        File file = new File(inputPath);
        if (file.isFile() && file.exists()) { //�ж��ļ��Ƿ����
            InputStreamReader read = new InputStreamReader(new FileInputStream(file), "UTF-8");//���ǵ������ʽ
            BufferedReader bufferedReader = new BufferedReader(read);
            String lineTxt;
            String[] pointInfor;
            while ((lineTxt = bufferedReader.readLine()) != null) {   //��һ������
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
