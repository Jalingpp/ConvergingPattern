import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.csvreader.CsvWriter;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

class CSVRW {



    public static void writeCSV(String path, List<Clusters> finalClusters) {
        String csvFilePath = path;

        try {

            CsvWriter csvWriter = new CsvWriter(csvFilePath, ',', Charset.forName("UTF-8"));

            String[] headers = {"CID","PID","PLng","Plat","Pedge","Ppos","time"};

            csvWriter.writeRecord(headers);
            for(int i=0;i<finalClusters.size();i++){
                String cid = finalClusters.get(i).id;
                for(int j=0;j<finalClusters.get(i).getPointList().size();j++)
                {
                    String pid = finalClusters.get(i).getPointList().get(j).id;
                    double plng = finalClusters.get(i).getPointList().get(j).lng;
                    double plat = finalClusters.get(i).getPointList().get(j).lat;
                    String time = finalClusters.get(i).getTime();

                    String[] writeLine = new String[7];
                    writeLine[0] = cid;
                    writeLine[1] = pid;
                    writeLine[2] = Double.toString(plng);
                    writeLine[3] = Double.toString(plat);

                    writeLine[6] = time;

                    String Pedge=finalClusters.get(i).getPointList().get(j).edgeid;
                    double Ppos=finalClusters.get(i).getPointList().get(j).pos;
                    writeLine[4]=Pedge;
                    writeLine[5]=Double.toString(Ppos);

                    csvWriter.writeRecord(writeLine);
                }
            }

            csvWriter.close();
//            System.out.println("簇输出完成");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void writeMBRtoCSV(String path, List<Clusters> finalClusters)
    {
        String csvFilePath = path;

        try {


            CsvWriter csvWriter = new CsvWriter(csvFilePath, ',', Charset.forName("UTF-8"));

            String[] headers = {"splng","splat","eplng","eplat"};
            csvWriter.writeRecord(headers);
            for(int i=0;i<finalClusters.size();i++){
                double splng1 = finalClusters.get(i).getMinlng();
                double splat1 = finalClusters.get(i).getMaxlat();
                double eplng1 =finalClusters.get(i).getMaxlng();
                double eplat1 = finalClusters.get(i).getMaxlat();

                String[] writeLine1 = new String[4];
                writeLine1[0] = Double.toString(splng1);
                writeLine1[1] = Double.toString(splat1);
                writeLine1[2] = Double.toString(eplng1);
                writeLine1[3] = Double.toString(eplat1);
                csvWriter.writeRecord(writeLine1);

                double splng2 = finalClusters.get(i).getMaxlng();
                double splat2 = finalClusters.get(i).getMaxlat();
                double eplng2 = finalClusters.get(i).getMaxlng();
                double eplat2 = finalClusters.get(i).getMinlat();

                String[] writeLine2 = new String[4];
                writeLine2[0] = Double.toString(splng2);
                writeLine2[1] = Double.toString(splat2);
                writeLine2[2] = Double.toString(eplng2);
                writeLine2[3] = Double.toString(eplat2);
                csvWriter.writeRecord(writeLine2);

                double splng3 = finalClusters.get(i).getMaxlng();
                double splat3 = finalClusters.get(i).getMinlat();
                double eplng3 = finalClusters.get(i).getMinlng();
                double eplat3 = finalClusters.get(i).getMinlat();

                String[] writeLine3 = new String[4];
                writeLine3[0] = Double.toString(splng3);
                writeLine3[1] = Double.toString(splat3);
                writeLine3[2] = Double.toString(eplng3);
                writeLine3[3] = Double.toString(eplat3);
                csvWriter.writeRecord(writeLine3);

                double splng4 = finalClusters.get(i).getMinlng();
                double splat4 = finalClusters.get(i).getMinlat();
                double eplng4 = finalClusters.get(i).getMinlng();
                double eplat4 = finalClusters.get(i).getMaxlat();

                String[] writeLine4 = new String[4];
                writeLine4[0] = Double.toString(splng4);
                writeLine4[1] = Double.toString(splat4);
                writeLine4[2] = Double.toString(eplng4);
                writeLine4[3] = Double.toString(eplat4);
                csvWriter.writeRecord(writeLine4);

            }
            csvWriter.close();
//            System.out.println("MBR输出完成");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}



class ClustersJSONRW {
    public static JSONObject readJson(String path){

        File file = new File(path);
        BufferedReader reader = null;

        StringBuffer data = new StringBuffer();

        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));

            String temp;
            while((temp = reader.readLine()) != null){
                data.append(temp);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {

            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return JSONObject.parseObject(data.toString());
    }

    public static void writeJson(String path,List<Clusters> finalclusters){
        BufferedWriter writer = null;
        File file = new File(path);


        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //写入
        try {
            writer = new BufferedWriter(new FileWriter(file));


            JSONObject root=new JSONObject();
            if(finalclusters!=null){
                root.put("Time", finalclusters.get(0).getTime());
                root.put("ClusterSize",finalclusters.size());
                JSONArray clustersarray=new JSONArray();
                JSONArray earray = null;
                JSONArray Objectsarray=null;
                JSONObject m = null,n=null;
                for(int i=0;i<finalclusters.size();i++){
                    m=new JSONObject();
                    m.put("CId",finalclusters.get(i).id);
                    List<SnapObject> list;
                    list=finalclusters.get(i).menberPoints;
                    m.put("Scale", list.size());
                    Objectsarray=new JSONArray();
                    earray=new JSONArray();
                    int tempnum=0;
                    for(int j=0;j<list.size();j++){
                        n=new JSONObject();
                        n.put("OId",list.get(j).id );
                        n.put("Lng",list.get(j).lng);
                        n.put("Lat",list.get(j).lat);
                        n.put("e",list.get(j).edgeid);
                        if(earray.contains(list.get(j).edgeid)==false){
                            earray.add(tempnum++, list.get(j).edgeid);
                        }
                        n.put("pos",list.get(j).pos);
                        Objectsarray.add(n);

                    }
                    m.put("Objects",Objectsarray);
                    m.put("E", earray);
                    clustersarray.add(m);
                }
                root.put("Clusters", clustersarray);
                //  	System.out.println(root.toString());
                writer.write(root.toString());
                root.clear();
                m.clear();
                n.clear();
                clustersarray.clear();
                earray.clear();
                Objectsarray.clear();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(writer != null){
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
