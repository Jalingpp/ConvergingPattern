import Controls.MiningControlor;

import java.io.IOException;

/**
 * @Description: 汇聚模式在线挖掘算法
 * @DataPathForRN: E:\1 Research\data\RoadNetwork_shanghai
 * @DataPathForTraj: E:\1 Research\data\MatchingDataInShanghai\MatchingData\trajectories
 * @Author JJP
 * @Date 2021/5/14 9:33
 */

public class Main {

    public static void main(String[] args) throws IOException {
        String settings = "C:\\Users\\lenovo\\Desktop\\\u8DEF\u7F51\u53D7\u9650\u7684\u6C47\u805A\u6A21\u5F0F\\\u5B9E\u9A8C\\\u4EE3\u7801\\ConvergingMonitoring\\src\\conf_converging.properties";
        MiningControlor miningControlor = new MiningControlor(settings);  //内含读数据
        miningControlor.miningConverging();

        int a=0;
    }

}
