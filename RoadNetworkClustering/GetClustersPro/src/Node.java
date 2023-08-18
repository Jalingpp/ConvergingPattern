import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Node {

    String id;
    double lng;
    double lat;
    List<String> sEdges;
    List<String> eEdges;

    public Node() {
        sEdges = new ArrayList<>();
        eEdges = new ArrayList<>();
    }

}
