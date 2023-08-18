package Tools;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;

import com.csvreader.*;

import MyClass.*;

public class CSVRW {

	/**
	 * 写簇为CSV文件
	 * @param path
	 */
	public static void writeCSV(String path, List<NewCluster> clusters) {
        String csvFilePath = path;
        
        try {
            
            // 创建CSV写对象 例如:CsvWriter(文件路径，分隔符，编码格式);
            CsvWriter csvWriter = new CsvWriter(csvFilePath, ',', Charset.forName("GBK"));
            // 写内容
            String[] headers = {"CID","PID","PLng","Plat","time"};
            csvWriter.writeRecord(headers);
            for(int i=0;i<clusters.size();i++){
            	String cid = ""+i;
            	for(int j=0;j<clusters.get(i).getPointList().size();j++)
            	{
            		String pid = clusters.get(i).getPointList().get(j).getId();
            		double plng = clusters.get(i).getPointList().get(j).getLng();
            		double plat = clusters.get(i).getPointList().get(j).getLat();
            		String time = clusters.get(i).getPointList().get(j).getTime();
            		
            		String[] writeLine = new String[5];
            		writeLine[0] = cid;
            		writeLine[1] = pid;
            		writeLine[2] = Double.toString(plng);
            		writeLine[3] = Double.toString(plat);
            		writeLine[4] = time;
            		System.out.println(writeLine);
                    csvWriter.writeRecord(writeLine);
            	}
            }
                       
            csvWriter.close();
            System.out.println("--------CSV文件已经写入--------");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	/**
	 * 写簇的外接矩形到CSV文件
	 * @param path
	 * @param clusters
	 */
	public static void writeMBRtoCSV(String path, List<NewCluster> clusters) {
		String csvFilePath = path;
        
        try {
            
            // 创建CSV写对象 例如:CsvWriter(文件路径，分隔符，编码格式);
            CsvWriter csvWriter = new CsvWriter(csvFilePath, ',', Charset.forName("GBK"));
            // 写内容
            String[] headers = {"splng","splat","eplng","eplat"};
            csvWriter.writeRecord(headers);
            for(int i=0;i<clusters.size();i++){
            		double splng1 = clusters.get(i).getMinlng();
            		double splat1 = clusters.get(i).getMaxlat();
            		double eplng1 = clusters.get(i).getMaxlng();
            		double eplat1 = clusters.get(i).getMaxlat();
            		
            		String[] writeLine1 = new String[4];
            		writeLine1[0] = Double.toString(splng1);
            		writeLine1[1] = Double.toString(splat1);
            		writeLine1[2] = Double.toString(eplng1);
            		writeLine1[3] = Double.toString(eplat1);
            		System.out.println(writeLine1);
                    csvWriter.writeRecord(writeLine1);
                    
                    double splng2 = clusters.get(i).getMaxlng();
            		double splat2 = clusters.get(i).getMaxlat();
            		double eplng2 = clusters.get(i).getMaxlng();
            		double eplat2 = clusters.get(i).getMinlat();
            		
            		String[] writeLine2 = new String[4];
            		writeLine2[0] = Double.toString(splng2);
            		writeLine2[1] = Double.toString(splat2);
            		writeLine2[2] = Double.toString(eplng2);
            		writeLine2[3] = Double.toString(eplat2);
            		System.out.println(writeLine2);
                    csvWriter.writeRecord(writeLine2);
                    
                    double splng3 = clusters.get(i).getMaxlng();
            		double splat3 = clusters.get(i).getMinlat();
            		double eplng3 = clusters.get(i).getMinlng();
            		double eplat3 = clusters.get(i).getMinlat();
            		
            		String[] writeLine3 = new String[4];
            		writeLine3[0] = Double.toString(splng3);
            		writeLine3[1] = Double.toString(splat3);
            		writeLine3[2] = Double.toString(eplng3);
            		writeLine3[3] = Double.toString(eplat3);
            		System.out.println(writeLine3);
                    csvWriter.writeRecord(writeLine3);
                    
                    double splng4 = clusters.get(i).getMinlng();
            		double splat4 = clusters.get(i).getMinlat();
            		double eplng4 = clusters.get(i).getMinlng();
            		double eplat4 = clusters.get(i).getMaxlat();
            		
            		String[] writeLine4 = new String[4];
            		writeLine4[0] = Double.toString(splng4);
            		writeLine4[1] = Double.toString(splat4);
            		writeLine4[2] = Double.toString(eplng4);
            		writeLine4[3] = Double.toString(eplat4);
            		System.out.println(writeLine4);
                    csvWriter.writeRecord(writeLine4);
       
            }        
            csvWriter.close();
            System.out.println("--------CSV文件已经写入--------");
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
