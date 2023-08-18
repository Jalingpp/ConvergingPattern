package Test;
import Tools.JSONRW;
import com.alibaba.fastjson.JSONObject;

public class TestJsonrw {
    public TestJsonrw(String path) {
        JSONObject js = JSONRW.readJson(path);
        for(int i = 0; i <js.getJSONArray("nodes").size(); i++)
        {
            JSONObject temp = (JSONObject)js.getJSONArray("nodes").get(i);
            String id = temp.get("id").toString();
            double lat = Double.parseDouble(temp.get("lng").toString());
        }
    }
}
