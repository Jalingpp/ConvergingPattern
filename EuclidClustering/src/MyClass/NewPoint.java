package MyClass;

import Tools.NewDBSCAN;

public class NewPoint {
	
	String id;
	double lng;
	double lat;
	String time;
	NewDBSCAN.PointMark mark;//点在聚类过程中的标记

	public NewPoint() {
	}

	public NewPoint(String id, double lng, double lat, String time) {
		super();
		this.id = id;
		this.lng = lng;
		this.lat = lat;
		this.time = time;
	}

	public NewDBSCAN.PointMark getMark() {
		return mark;
	}

	public void setMark(NewDBSCAN.PointMark mark) {
		this.mark = mark;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public double getLng() {
		return lng;
	}

	public void setLng(double lng) {
		this.lng = lng;
	}

	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

//	/**
//	 * 点的标记枚举
//	 */
//	enum PointMark {
//		NOISE,//噪声�?
//		BOUNDRY,//边界�?
//		CORE//核心�?
//	}
}
