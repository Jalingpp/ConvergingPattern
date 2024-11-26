## Converging Pattern Introduction
A converging represents a group event that a group of moving objects gradually come to a target area from different directions and eventually form one group. Examples of convergings include traffic jams, celebrations, protests, and so on. The key property of a converging is membership variation, which is the crucial distinction compared to the existing patterns.

Our team is devoted to discovering converging patterns from human trajectories and has successively studied converging pattern mining algorithms both in Euclidean space and road networks. The difference between the two spaces is that the former is a planar space while the later is essentially a non-planar graph, which leads to different distance calculation methods. Therefore, existing pattern mining methods in Euclidean space cannot distinguish the group events with different heights in urban space, e.g. pedestrian overpasses, elevated highways, and the subway network.

**For more information, please read our papers：**    
[1] Bin Zhao, Xintao Liu, Jinping Jia, Genlin Ji, Shengxi Tan, Zhaoyuan Yu. A Framework for Group Converging Pattern Mining using Spatiotemporal Trajectories. GeoInformatica 24(4): 745-776 (2020)   
[2] Jinping Jia, Ying Hu, Bin Zhao, Genlin Ji, Richen Liu. Discovering Collective Converging Groups of Large Scale Moving Objects in Road Networks. DASFAA (2) 2021: 307-324

## Coding List Guidence
**EuclidClustering:**    
A clustering algorithm for moving objects in Euclidean space, which rewrites DBSCAN algorithm to adapt to large-scale Euclidean distance based moving object clustering.

**RoadNetworkClustering:**     
Two clustering algorithms for moving objects in Road Network. GetClusters includes a clustering algorithm based on Grid Index, while GetClustersPro includes a clustering algorithm based on our proposed index called VNIndex, which is detailed in our dasfaa paper.

**MatchingAlgorithms:**     
Three cluster containment join algorithms. MatchingLoopNest is the naive method for cluster containment join. MatchingBreadTraversal is the classic method with a breadth traversal of the road network. MatchingProposed is our proposal based on the road network partitions.

**ConvergingOnlineMining:**  
Three online clustering algorithms for streaming trajectories in Road Network. The MORN index is implemented in the Classes filefold, while the DCRN, DISC and IDCRN algorithmes are implemented in the Clustering filefold.

**ConvergingMonitoring:** 
A lightweight implementation of the Converging Pattern online mining algorithm.

## Other Statements
Due to confidentiality issues, our data and its format are not publicly available. 

Special thanks to Tan Shengxi, Yin Furong, Fang Ziruo and others for their contributions to the code of this warehouse.
