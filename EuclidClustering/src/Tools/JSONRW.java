package Tools;

import java.io.*;
import java.util.*;

import com.alibaba.fastjson.JSONObject;

import MyClass.NewCluster;

/**
 * Created by jdxg on 2017/4/20.
 * �? json的读写类
 */
public class JSONRW {

    /**
     * 从给定位置读取Json文件
     * @param path 文件�?
     * @return json对象
     */
    public static JSONObject readJson(String path){
        //从给定位置获取文�?
        File file = new File(path);
        BufferedReader reader = null;
        //返回�?,使用StringBuffer
        StringBuffer data = new StringBuffer();
        //
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(file),"UTF-8"));
            //每次读取文件的缓�?
            String temp;
            while((temp = reader.readLine()) != null){
                data.append(temp);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            //关闭文件�?
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

    /**
     * 给定路径与Json文件，存储到硬盘
     * @param path 保存路径
     * @param json 待保存的json对象
     * @param fileName 保存的文件名
     */
    public static void writeJson(String path,Object json,String fileName){
        BufferedWriter writer = null;
        File file = new File(path  + ".json");
//        System.out.println(path + fileName + ".json");
        //如果文件不存在，则新建一�?
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
            writer.write(json.toString());
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
//        System.out.println("文件写入成功�?");
    }
    
   
    
}
