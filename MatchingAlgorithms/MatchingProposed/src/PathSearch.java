import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

public class PathSearch {


    /**
     * @ Class: PathSearch
     * @ Description: Find path from one edge to another edge in a road network
     * @ Date 2019/10/31 19:11
     * @ Created by bridge
    **/

    public Stack<Edges> stack = new Stack<Edges>();  //保存边序列的栈
    public ArrayList<Path> ret = new ArrayList<Path>(); //保存路径序列

    public PathSearch() {

    }

    public boolean isEdgeInStack(Edges edge) {
        Iterator<Edges> iter = stack.iterator();
        while (iter.hasNext()) {
            Edges e = (Edges)iter.next();
            if (edge.equals(e))
                return true;
        }
        return false;
    }

    public void SavePath() {
        Path temp = new Path();
        Object[] obj = stack.toArray();
        for (int i = 0; i < obj.length; i++) {
            temp.path.add(((Edges)obj[i]).getId());
        }
        ret.add(temp);
    }

    public boolean getPaths(Edges cEdge, Edges pEdge, Edges sEdge, Edges eEdge) {
         //cEdge: 当前的起始边 pEdge: 当前起始边的上一条边 sEdge：最初的起始边 eEdge: 终边
        Edges nedge = null;
        if (cEdge != null && pEdge != null && cEdge == pEdge)  //查找失败
            return false;
        if (cEdge != null) {
            int i = 0;
            stack.push(cEdge);  //起始边入栈
            if (cEdge == eEdge) {    //如果该边是终边，则保存路径
                SavePath();
                return true;
            }
            else {
                if(cEdge.getAccedge().size() == 0)
                    nedge = null;
                else
                    nedge = cEdge.getAccedge().get(i);
                while (nedge != null) {    //如果nedge是最初的起始边，或者nedge就是cedge的上一条边，或者nedge已经在栈中,应重新在与当前起始边有连接关系的边集中寻找nedge
                    if (pEdge != null && (nedge == sEdge || nedge == pEdge || isEdgeInStack(nedge))) {
                        i++;
                        if (i >= cEdge.getAccedge().size())
                            nedge = null;
                        else
                            nedge = cEdge.getAccedge().get(i);
                        continue;
                    }
                    if (getPaths(nedge, cEdge, sEdge, eEdge))   ///以nedge为新的起始边，当前起始边cedge为上一条边，递归调用
                    {
                        //找到一条路径
                        stack.pop();
                    }
                    //继续在与cedge有连接关系的边集中测试nedge
                    i++;
                    if (i >= cEdge.getAccedge().size())
                        nedge = null;
                    else
                        nedge = cEdge.getAccedge().get(i);
                }
                //以cedge为起始边到终边的路径已经全部找到
                stack.pop();
                return false;
            }
        } else
            return false;
    }
}