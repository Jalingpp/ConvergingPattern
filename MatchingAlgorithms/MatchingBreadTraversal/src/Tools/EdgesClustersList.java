package Tools;

import Clusters.Time;
import RoadNetWork.Edges;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @ Class: EdgesClustersList
 * @ Description: 读写边-簇查询表
 * @ Date 2019/10/22 14:07
 * @ Created by bridge
**/

public class EdgesClustersList {

    public static HashMap<String,ArrayList<String>> readECList(String filename){
        /** edg_clu格式： key:eid  ， value: array[time,length,size,cid1,cid2……] **/
        HashMap<String,ArrayList<String>> edg_clu = new HashMap<String, ArrayList<String>>();
        BufferedReader reader = null;
        File file = new File(filename);
        //读文件
        try {
                reader = new BufferedReader(new FileReader(file));
                String str = null;
                while((str = reader.readLine()) != null) {
                    if(str == null)
                        break;
                    String[] array = str.split("\\s");
                    ArrayList<String> temp = new ArrayList<String>();
                    for(int i = 0; i < array.length; i++) {
                        if(i != 1) {
                            temp.add(array[i]);
                        }
                    }
                    edg_clu.put(array[1],temp);
                }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return edg_clu;
    }

    public static void writeECList(String filepath, HashMap<String,HashMap<String, ArrayList<String>>> edg_clu, HashMap<String, Edges> edges) throws IOException {
        /** 写入文档格式： time eid length size cid1 cid2…… **/
        BufferedWriter writer = null;
        File file = new File(filepath);
        //写入
        try {
            writer = new BufferedWriter(new FileWriter(file));
            for(Map.Entry<String,HashMap<String, ArrayList<String>>> entry : edg_clu.entrySet()) {
                //获取一个时间戳下的边-簇表
                HashMap<String, ArrayList<String>> _entryec = entry.getValue();
                for(Map.Entry<String, ArrayList<String>> entry1 : _entryec.entrySet())
                {
                    String time = entry.getKey();
                    String eid = entry1.getKey();
                    double length = edges.get(eid).getLength();
                    int size = entry1.getValue().size();
                    String content = time+" "+eid+" "+length+" "+size+" ";
                    for(int i=0;i<size;i++)
                        content = content+entry1.getValue().get(i)+" ";
                    writer.write(content);
                    writer.newLine();
                    writer.flush();
                }

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
