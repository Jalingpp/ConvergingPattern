package Test;

/**
 * @ Title: Time.java
 * @ Package Test
 * @ Description:
 * @ Date 2019/10/15 18:44
 * @ Created by bridge
 **/

public class Time {

    /**
     * @ Class: Time
     * @ Description: store time information
     * @ Date 2019/10/15 19:03
     * @ Created by bridge
     **/

    private String year;
    private String month;
    private String date;
    private String hour;
    private String minute;
    private String second;

    public Time() {
        year = null;
        month = null;
        date = null;
        hour = null;
        minute = null;
        second = null;
    }

    public Time(String time) {

        /**
         * @ Methond: Time
         * @ Description: Transform string to time
         * @ Date 2019/10/15 19:11
         * @ Created by bridge
         * @ return
         **/

        String[] array =  time.split("[-:.\\s]");
        this.year = array[0];
        this.month = array[1];
        this.date = array[2];
        this.hour = array[3];
        this.minute = array[4];
        this.second = array[5];
    }

    public String getYear() {
        return year;
    }

    public String getMonth() {
        return month;
    }

    public String getDate() {
        return date;
    }

    public String getHour() {
        return hour;
    }

    public String getMinute() {
        return minute;
    }

    public String getSecond() {
        return second;
    }

    public String Operadd(String obj) {
        StringBuffer sb = new StringBuffer(obj);
        int i = Integer.parseInt(sb.substring(0,2));
        i = i + 1;
        String s;
        if(i < 10) {
            s = "0" + i;
        }
        else {
            s = Integer.toString(i);
        }
        String str = s + sb.substring(2,sb.length());
        return str;
    }
}
