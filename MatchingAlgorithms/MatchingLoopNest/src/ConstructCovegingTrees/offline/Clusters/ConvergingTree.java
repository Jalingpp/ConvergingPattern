package Clusters;

import RoadNetWork.RoadNetWork;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import Tools.*;

//所有汇聚树所构成的最终结果
public class ConvergingTree {

    List<Tree> TotalTree = new ArrayList<>();

    public ConvergingTree() {

    }

    public int inWhichTree(String s) {      //这个簇在哪棵树中
        for(int i = 0; i < TotalTree.size(); i++) {
            if(TotalTree.get(i).isInTree(s))
                return i;
        }
        return -1;   //不在总汇聚树中
    }

    public void createTree(RoadNetWork road,ArrayList<TimeClusters> Tc,int n,String outpath) {                //生成汇聚树
        for(int i = 0; i < n-1; i++) {            //时间片
            for(int j = 0; j < Tc.get(i).Clusters.size(); j++) {     //该时间片下所有的簇
                System.out.println("正在处理"+Tc.get(i).Time+"第"+j+"个簇");
                Cluster c = Tc.get(i).Clusters.get(j);              //返回的是Tc中已有簇的指针
                TimeClusters nextclu = Tc.get(i+1);                 //下一时间片的簇集合
                String filename = outpath+"Edges_Clusters_List/" + new Time(nextclu.Time).retSimple()+".txt";
                HashMap<String,ArrayList<String>> edg_clu = EdgesClustersList.readECList(filename); //查询下一时刻边-簇表
                List<String> candiClusters = Tc.get(i).Clusters.get(j).getCandiClusters(road,nextclu,edg_clu);  //得到候选簇的id列表
                for(int z = 0; z < candiClusters.size(); z++) {
                    if(c.isInclude(candiClusters.get(z),Tc.get(i+1))) {       //簇包含在候选簇中，直接连接
                        c.FatherId = candiClusters.get(z);
                        Tree Tr = new Tree();
                        int number1 = this.inWhichTree(c.Cid);       //这个簇是否存在于一棵树中
                        if(number1 != -1) {                        //存在于树中
                            Tr.converge(this.TotalTree.get(number1));
                            this.TotalTree.remove(number1);
                        }
                        else                 //不存在则构造一棵数
                            Tr.Tree.add(c);     //用这个簇生成一棵数
                        int number = this.inWhichTree(candiClusters.get(z));    //父簇在哪棵树中
                        if(number == -1) {   //父簇不在某棵树中
                            Tr.Tree.add(Tc.get(i+1).getClusters().get(candiClusters.get(z)));//将该簇加入树中
                            this.TotalTree.add(Tr);
                        }
                        else {               //父簇在某棵树中
                            this.TotalTree.get(number).converge(Tr);//将两个树合并
                        }
                        break;
                    }
                }
            }
        }
        for(int i = 0; i < this.TotalTree.size(); i++) {
            Tree t = TotalTree.get(i);
            for(int j = 0; j < t.Tree.size(); j++) {
                System.out.print(t.Tree.get(j).Cid + "--->"+t.Tree.get(j).FatherId+"  ");
            }
            System.out.println("");
        }
        System.out.println("汇聚树棵数：" + this.TotalTree.size());
    }
}
