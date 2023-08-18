import java.util.*;

public class NeighborRange {

    List<String> safeEdges;
    List<Double> safeLength;
    List<String> unsafeEdges;
    List<Double> unsafeLength;
    HashMap<String,Double> allEdges;

    public NeighborRange() {
        this.safeEdges = new ArrayList<>();
        this.safeLength = new ArrayList<>();
        this.unsafeEdges = new ArrayList<>();
        this.unsafeLength = new ArrayList<>();
        this.allEdges = new HashMap<>();
    }

}

