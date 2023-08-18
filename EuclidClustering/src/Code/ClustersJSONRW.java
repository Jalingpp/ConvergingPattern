package Code;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import Tools.*;

import com.alibaba.fastjson.*;

import MyClass.*;

public class ClustersJSONRW {
	
	 public static List<NewCluster> readClustersJson(String path)
	    {
	    	List<NewCluster> clusters = new ArrayList();
	    	JSONObject rcsjO = JSONRW.readJson(path);
	    	int clusterSize = rcsjO.getIntValue("ClusterSize");
	    	JSONArray rcsjA = rcsjO.getJSONArray("Clusters");
	    	for(int i=0;i<rcsjA.size();i++)
	    	{
	    		NewCluster _c = new NewCluster(""+i);
		    	JSONArray rcjA = rcsjA.getJSONArray(i);
		    	List<NewPoint> _ps = new ArrayList();
		    	for(int j=0;j<rcjA.size();j++)
		    	{
		    		JSONObject rpjO = rcjA.getJSONObject(j);
		    		NewPoint _p = new NewPoint(rpjO.getString("Id"),rpjO.getDoubleValue("Lng"),rpjO.getDoubleValue("Lat"),rpjO.getString("Time"));
		    		_ps.add(_p);
		    	}
		    	_c.setPointList(_ps);
		    	_c.calMBR();
		    	clusters.add(_c);
	    	}
	    	return clusters;
	    }

	public static void writeJson(String path,List<NewCluster> finalclusters){
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
				root.put("Time", finalclusters.get(0).getPointList().get(0).getTime());
				root.put("ClusterSize",finalclusters.size());
				JSONArray clustersarray=new JSONArray();
				JSONArray earray = null;
				JSONArray Objectsarray=null;
				JSONObject m = null,n=null;
				for(int i=0;i<finalclusters.size();i++){
					m=new JSONObject();
					m.put("CId",finalclusters.get(i).getId());
					List<NewPoint> list;
					list=finalclusters.get(i).getPointList();
					m.put("Scale", list.size());
					Objectsarray=new JSONArray();
					earray=new JSONArray();
					for(int j=0;j<list.size();j++){
						n=new JSONObject();
						n.put("OId",list.get(j).getId());
						n.put("Lng",list.get(j).getLng());
						n.put("Lat",list.get(j).getLat());
						n.put("e","none");
						n.put("pos",0);
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
