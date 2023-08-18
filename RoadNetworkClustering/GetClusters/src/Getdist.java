import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;



public class Getdist {

    /**
     * @param args
     */
    double inf = Double.MAX_VALUE;

    private HashMap<String, LinkedList<Adj_Node> > graph; //可达矩阵
    private HashMap<String, Adj_Node> edges;  //边到顶点的映射
    private HashMap<String, Etoe> map;  //转向矩阵
    private HashMap<String,SnapPoint> nodesid_p = new HashMap<String,SnapPoint>();
    private int vexnum;   //顶点个数
    //	private BufferedWriter bw = new BufferedWriter(new FileWriter(new File("path.txt")));
    private List<SnapPoint> nodepoints=new ArrayList<SnapPoint>();
    private GridIndex gridIndexnode; //顶点索引

    private HashMap<Integer, Node_Intid> intid2str;
    private HashMap<String, Node_Intid> strid2int;


    Getdist(MBR mapScale,
            double eps,
            HashMap<String, LinkedList<Adj_Node> > _gra,
            HashMap<String, Adj_Node> _edg,
            HashMap<String, Etoe> _map,
            HashMap<String,SnapPoint> _nodesid_p,
            int _vex,
            List<SnapPoint> _nodepoints,
            GridIndex _gridIndexnode){

        graph = _gra; //可达矩阵
        edges =_edg;  //边到顶点的映射
        map = _map;  //转向矩阵
        nodesid_p = _nodesid_p;
        vexnum = _vex;   //顶点个数

        nodepoints = _nodepoints;
        gridIndexnode = _gridIndexnode; //顶点索引





    }





    public HashMap<String, LinkedList<Adj_Node> > getgraph()
    {
        return graph;
    }


    //得到图的大小（顶点个数
    public int graphsize()
    {
        return this.vexnum;
    }

    //获得边的长度
    public double getlen(String edgeid)
    {
        return edges.get(edgeid).length;
    }

    //获得边的终点id
    public String getepid(String edgeid)
    {
        return edges.get(edgeid).epid;
    }

    //获得边的起点id
    public String getspid(String edgeid)
    {
        Adj_Node idnode = edges.get(edgeid);
        return idnode.spid;
    }

    //获得映射




    public double new_Dijkstra(String start,
                               String end,
                               String edge1,
                               String edge2,
                               ArrayList<String> e2elist){

        Edge2Edge e2e;

        if (start.equals(end))
        {
            if (isarrivable(edge1, edge2))
            {
                return 0;
            }
        }
        Queue<Pair> dist = new PriorityQueue<Pair>(10, new Comparator<Pair>(){
            public int compare(Pair i1,Pair i2){
                if (i1.length < i2.length)
                {
                    return -1;
                }
                else if (i1.length == i2.length)
                {
                    return i1.id - i1.id;
                }
                else
                {
                    return 1;
                }
            }});

        List<SnapPoint> points = gridIndexnode.getPointsFromGrids(nodesid_p.get(start), 1);
        int num = points.size()+1;
        int path[] = new int[num];		//路径path[i]为i的前一个顶点的下标
        double dArray[] = new double[num];
        boolean vis[] = new boolean[num];		//visited数组

        strid2int = new HashMap<String, Node_Intid>(num);
        intid2str = new HashMap<Integer, Node_Intid>(num);
        boolean able = false;
        Node_Intid point;
        point=new Node_Intid();

        point.i_id = 0;
        point.id = start;
        strid2int.put(point.id, point);
        intid2str.put(point.i_id, point);
        for (int i = 1; i < num; i++)
        {
            point = new Node_Intid();
            point.i_id = i;
            point.id = points.get(i - 1).id;
            strid2int.put(point.id, point);
            intid2str.put(point.i_id, point);
            if (point.id.equals(end))
            {
                able = true;
            }
        }





        if (!able)
        {
            return Double.MAX_VALUE;
        }

        for (int i = 0; i < num; i++)
        {
            path[i] = -1;
            dArray[i] = inf;
        }
        int a=strid2int.get(start).i_id;
        dArray[strid2int.get(start).i_id] = 0;
        vis[strid2int.get(start).i_id] = true;

        LinkedList<Adj_Node> list = graph.get(start);
        Adj_Node node;
        Pair pair;
        boolean j1 = false;
        for (int i = 0; i < list.size(); i++)
        {
            node = list.get(i);
            j1 = isarrivable(edge1, node.edgeid);
            if (strid2int.containsKey(node.epid) && j1)
            {
                dist.add(new Pair(strid2int.get(node.epid).i_id, node.length));
                path[strid2int.get(node.epid).i_id] = strid2int.get(start).i_id;
                dArray[strid2int.get(node.epid).i_id] = node.length;
            }
        }

//		System.out.println(end + " " + intid2str.get(1).id);
        Node_Intid t = strid2int.get(end);
        int minindex = t.i_id;
        boolean turnjudge = false;
        HashMap<String, String> pathlist = new HashMap<String, String>();

        while (!dist.isEmpty())
        {
            pair = dist.poll();
            minindex = pair.id;

            if (minindex == strid2int.get(end).i_id)
            {
                turnjudge = turnadj_2(intid2str.get(path[minindex]).id, end, edge2);
                if(turnjudge)
                {
                    e2e = new Edge2Edge(edge1, edge2);
//					get_path(path, strid2int.get(end).i_id, pathlist);
                    if (path[path[minindex]] != -1)
                    {
                        pathlist.put(getedgeid(intid2str.get(path[minindex]).id, intid2str.get(minindex).id),
                                getedgeid(intid2str.get(path[path[minindex]]).id, intid2str.get(path[minindex]).id));
                    }
                    pathlist.put(edge2, getedgeid(intid2str.get(path[minindex]).id, intid2str.get(minindex).id));
                    getpath(e2elist, pathlist, edge1, edge2);
                    return dArray[minindex];
                }
            }



            list = graph.get(intid2str.get(minindex).id);
            if (list == null)
            {
                continue;
            }
            for (int i = 0; i < list.size(); i++)
            {
                node = list.get(i);
                if (strid2int.containsKey(node.epid))
                {
                    int ep = strid2int.get(node.epid).i_id;

                    double temp = dArray[ep];
                    if (vis[ep] == false &&
                            node.length != inf &&
                            pair.length + node.length < temp &&
                            turnadj_2(intid2str.get(path[minindex]).id, intid2str.get(minindex).id, node.edgeid))
                    {
                        dArray[ep] = pair.length + node.length;
                        if (!dist.contains(new Pair(ep, temp)))
                        {
                            dist.add(new Pair(ep, dArray[ep]));        //添加到ep的距离
                        }
                        else
                        {
                            dist.remove(new Pair(ep, temp));
                            dist.add(new Pair(ep, dArray[ep]));        //更新到ep的距离
                        }
                        path[ep] = minindex;
                        pathlist.put(node.edgeid, getedgeid(intid2str.get(path[minindex]).id, intid2str.get(minindex).id));
                    }
                }
            }
            if (minindex == strid2int.get(end).i_id && !turnjudge)
            {
                dArray[minindex] = inf;
            }
            else
            {
                vis[minindex] = true;
            }
        }
        return Double.MAX_VALUE;
    }


    public boolean turnadj_1(String edgeid, String nodeid1, String nodeid2)
    {
        String edgeid2 = getedgeid(nodeid1, nodeid2);
        return isarrivable(edgeid, edgeid2);
    }


    public boolean turnadj_2(String nodeid1, String nodeid2, String edgeid2)
    {
        String edgeid1 = getedgeid(nodeid1, nodeid2);
        return isarrivable(edgeid1, edgeid2);
    }


    public boolean turnadj_3(String pre_min_id, String min_id, String ep_id)
    {
        String edgeid1 = getedgeid(pre_min_id, min_id);
        String edgeid2 = getedgeid(min_id, ep_id);

        return isarrivable(edgeid1, edgeid2);
    }


    public boolean isarrivable(String edgeid1, String edgeid2)
    {
        Etoe list = map.get(edgeid1);
        int i = 0;
        if (list != null)
        {
            for (i = 0; i < list.linkedlist.size(); i++)
            {
                if (list.linkedlist.get(i).id.equals(edgeid2) && list.linkedlist.get(i).pro > 0)
                {
                    return true;
                }
            }
        }
        return false;
    }

    public void getpath(ArrayList<String> list, HashMap<String, String> pathlist, String start, String end)
    {
        String str = pathlist.get(end);
//		System.out.print(start + "->" + end);
        while ((!start.equals(str)) && str != null)
        {
            list.add(str);
//			System.out.print(" " + str);
            str = pathlist.get(str);
        }
//		System.out.println();
    }




    public String getedgeid(String spid, String epid)
    {
        LinkedList<Adj_Node> list = graph.get(spid);
        if (list != null)
        {
            for (Adj_Node a:list)
            {
                if (a.epid.equals(epid))
                {
                    return a.edgeid;
                }
            }
        }
        return null;
    }

    public double getedgelen(String spid, String epid)
    {
        LinkedList<Adj_Node> list = graph.get(spid);
        if (list != null)
        {
            for (Adj_Node a:list)
            {
                if (a.epid.equals(epid))
                {
                    return a.length;
                }
            }
        }
        return 0;
    }


    private int str2int(String str)
    {
        return Integer.parseInt(str);
    }

    private String int2str(int a)
    {
        return String.valueOf(a);
    }



}



class Pair {
    int id;
    double length;

    Pair(int _id, double len)
    {
        id = _id;
        length = len;
    }

}

class Node_Intid{
    String id;
    int i_id;

}
