import Controls.MiningControlor;
import Tools.TimeConversion;

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
//        String settings = "/home/dm/codes/ConvergingOnlineMiningRW/Runtime/shanghai/eps/100/SCVCU/conf_converging.properties";
        String settings = "C:\\Users\\lenovo\\Desktop\\\u8DEF\u7F51\u53D7\u9650\u7684\u6C47\u805A\u6A21\u5F0F\\\u6BD5\u4E1A\u8BBA\u6587\\\u5B9E\u9A8C\\ConvergingOnlineMining\\src\\conf_converging.properties";
        MiningControlor miningControlor = new MiningControlor(settings);  //内含读数据
        miningControlor.miningConverging();

        int a=0;
    }

}
