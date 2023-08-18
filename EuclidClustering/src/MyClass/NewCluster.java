package MyClass;

import java.util.*;

public class NewCluster {
	
	String id;
	List<NewPoint> pointList;
	
	double minlng,maxlat,maxlng,minlat;
	
	public NewCluster(String id) {
		super();
		this.id = id;
		this.pointList = null;
	}
	public NewCluster(String id,List<NewPoint> pointList) {
		super();
		this.id = id;
		this.pointList = pointList;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<NewPoint> getPointList() {
		return pointList;
	}

	public void setPointList(List<NewPoint> pointList) {
		this.pointList = new ArrayList();
		for(int i=0;i<pointList.size();i++)
		{
			NewPoint p = new NewPoint(pointList.get(i).id,pointList.get(i).lng,pointList.get(i).lat,pointList.get(i).time);
			this.pointList.add(p);
		}
	}
	
	
	
	public double getMinlng() {
		return minlng;
	}

	public void setMinlng(double minlng) {
		this.minlng = minlng;
	}

	public double getMaxlat() {
		return maxlat;
	}

	public void setMaxlat(double maxlat) {
		this.maxlat = maxlat;
	}

	public double getMaxlng() {
		return maxlng;
	}

	public void setMaxlng(double maxlng) {
		this.maxlng = maxlng;
	}

	public double getMinlat() {
		return minlat;
	}

	public void setMinlat(double minlat) {
		this.minlat = minlat;
	}

	public void calMBR()
	{
		this.maxlat = this.pointList.get(0).lat;
		this.minlat = this.pointList.get(0).lat;
		this.minlng = this.pointList.get(0).lng;
		this.maxlng = this.pointList.get(0).lng;
		for(int i=1;i<this.pointList.size();i++)
		{
			if(this.pointList.get(i).lat>maxlat)
				this.maxlat = this.pointList.get(i).lat;
			else if(this.pointList.get(i).lat<minlat)
				this.minlat = this.pointList.get(i).lat;
			
			if(this.pointList.get(i).lng>maxlng)
				this.maxlng = this.pointList.get(i).lng;
			else if(this.pointList.get(i).lng<minlng)
				this.minlng = this.pointList.get(i).lng;
			
		}
	}

}
