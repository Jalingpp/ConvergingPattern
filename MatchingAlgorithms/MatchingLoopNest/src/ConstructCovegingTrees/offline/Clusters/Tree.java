package Clusters;

import java.util.ArrayList;
import java.util.List;

//生成的汇聚树
public class Tree {

    List<Cluster> Tree = new ArrayList<>();

    public boolean isInTree(String s) {         //簇是否在这颗树中
        for(int i = 0; i < Tree.size(); i++) {
            if(s.equals(Tree.get(i).Cid))
                return true;
        }
        return  false;
    }

    public void converge(Tree t) {             //将两棵树合成一颗树
        for(int i = 0; i < t.Tree.size(); i++) {
            this.Tree.add(t.Tree.get(i));
        }
    }

    int ClusterNumber(String s)                 //返回s所对应的簇在树中的序号
    {
        for(int i =0;i <this.Tree.size();i++){
            if(this.Tree.get(i).equals(s))
                return i;
        }
        return -1;                 //无此簇
    }
}
