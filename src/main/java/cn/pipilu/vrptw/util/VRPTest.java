package cn.pipilu.vrptw.util;

import cn.pipilu.vrptw.request.PointReq;
import cn.pipilu.vrptw.response.RouteResp;
import cn.pipilu.vrptw.response.SavedDistance;
import com.alibaba.fastjson.JSON;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

public class VRPTest {
    public static final String KONGGE = " ";
      public static final int FACTOR = 1;
      private int vehicleNumber;
      private int totalPointNumber;
      private LinkedList<Float> vehicleCapacityList = new LinkedList<>();
      private LinkedList<Float> usedVehicleList = new LinkedList<>();
      private List<PointReq> postOfficeList = new ArrayList<>();
      private List<RouteResp> routeList = new ArrayList<>();
      private float[][] distMatrix;
      private List<SavedDistance> savingList = new ArrayList<>();


              public static void main(String[] args) throws Exception {
                 VRPTest vrpTest = new VRPTest();
                 vrpTest.readFromFile("D:\\order.txt");
                 vrpTest.vrp();
             }

        /**
         * 从文件中读取数据
        */
              public void readFromFile(String fileName) {
                 File file = new File(fileName);
                 try {
                         BufferedReader br = new BufferedReader(new FileReader(
                                         file));
                         constructGeneral(br);
                         constructVehicle(br);
                         constructNodes(br);
                     } catch (FileNotFoundException e) {
                         e.printStackTrace();
                     } catch (IOException e) {
                         e.printStackTrace();
                     }
             }

              private void constructGeneral(BufferedReader br) throws IOException {
                 String first = br.readLine().trim();
                 String[] firstLineArr = first.split(KONGGE);
                 vehicleNumber = Integer.parseInt(firstLineArr[0]);
                 totalPointNumber = Integer.parseInt(firstLineArr[1]);
             }

              private void constructVehicle(BufferedReader br) throws IOException {
                 String vehicleCapacity = br.readLine().trim();
                 for (String s : vehicleCapacity.split(KONGGE)) {
                         vehicleCapacityList.add(Float.parseFloat(s));
                     }
             }

              private void constructNodes(BufferedReader br) throws IOException {
                 for (int i = 0; i < totalPointNumber; i++) {
                         String postStr = br.readLine().trim();
                         String[] postArr = postStr.split(KONGGE);
                     String s = postArr[4];
                     PointReq pointReq = PointReq.builder()
                             .orderNo(postArr[0])
                             .address(postArr[1])
                             .latitude(new BigDecimal(postArr[2]))
                             .longitude(new BigDecimal(postArr[3]))

                             .receive(new BigDecimal(s))
                             .sendOut(new BigDecimal(postArr[5]))
                             .earliestTime(Integer.parseInt(postArr[6]))
                             .latestTime(Integer.parseInt(postArr[7]))
                             .duration(Integer.parseInt(postArr[8]))
                             .pointType(isDepot(i))
                             .build();

                     postOfficeList.add(pointReq);
                     }
                  System.err.println(JSON.toJSONString(postOfficeList));
             }

              private String isDepot(int i) {
                 //第一条记录为仓库
                 return i == 0 ? "0" : "1";
             }

              public void vrp() throws Exception {
                 calcDistMatrix();
                 calcSavingMatrix();
                 calcRoute();
                 cwSaving();
                 //optimise
                 twoOptOptimise();
                 capacityOptimise();

                 printGeneral();
                 printRoute();
                 printTimeSchedule();
             }

             /**
       * 计算距离矩阵
       */
             private void calcDistMatrix() {
                 int length = postOfficeList.size();
                 distMatrix = new float[length][length];
                 for (int i = 0; i < totalPointNumber; i++) {
                         for (int j = 0; j < i; j++) {
                                 distMatrix[i][j] = DistanceUtils.distanceTo(postOfficeList.get(i),postOfficeList.get(j));
                                 distMatrix[j][i] = distMatrix[i][j];
                             }
                         distMatrix[i][i] = 0;
                     }
             }

             /**
       * 计算节约距离列表
       */
             private void calcSavingMatrix() {
                 for (int i = 2; i < totalPointNumber; i++) {
                         for (int j = 1; j < i; j++) {
                                 PointReq pi = postOfficeList.get(i);
                             PointReq pj = postOfficeList.get(j);
                             PointReq depot = postOfficeList.get(0);
                                 float dist = DistanceUtils.distanceTo(pi,pj);
                                 float saving =DistanceUtils.distanceTo(pi,depot)+ DistanceUtils.distanceTo(pj,depot) - dist;
                                 savingList.add(new SavedDistance(postOfficeList.get(i), postOfficeList.get(j), saving));
                             }
                     }
                 savingList.sort(Collections.reverseOrder());
             }

             private boolean twoOptOptimise() {
                 for (RouteResp route : routeList) {
                         if (route.twoOptOptimise()) {
                                 return true;
                             }
                     }
                 return false;
             }

             private boolean capacityOptimise() {
                 for (RouteResp route : routeList) {
                         if (route.vehicleOptimise(vehicleCapacityList, usedVehicleList)) {
                                 return true;
                             }
                     }
                 return false;
             }



             /**
       * 构建基础路径
       */
             private void calcRoute() throws CloneNotSupportedException {
                 //将所有点单独与集散中心组成一条路径，路径对象中包含集散中心
                 PointReq depot = postOfficeList.get(0);
                 for(int i = 1 ; i<postOfficeList.size(); i++) {
                         RouteResp r = new RouteResp();
                         //更新点 所在路径
                     PointReq startNode = (PointReq) depot.clone();
                         startNode.setCurrentRoute(r);
                     PointReq endNode = (PointReq) depot.clone();
                         endNode.setCurrentRoute(r);
                         postOfficeList.get(i).setCurrentRoute(r);
                         //更新路径 上的点
                         r.setNodesAndUpdateLoad(new LinkedList<>(Arrays.asList(startNode, postOfficeList.get(i), endNode)));

                         //更新到达时间
                         r.updateArrivedTime();
                         //更新路径长度
                         r.setLength(r.calcLength(r.getNodes()));
                         //更新原路径上点的前后关系
                         r.updateNodeTracing();
                         //更新载重
                         routeList.add(r);
                     }
             }

             /**
       * CW节约算法构建路程
       * @throws Exception
       */
             private void cwSaving() throws Exception {
                 //取出save值最大的路径，尝试加入当前路径
                 for (SavedDistance savedDistance : savingList) {
                         mergeSavedDistance(savedDistance);
                     }
             }

             /**
       * 合并路径规则：
       * 两点中有一点在路径尾部，一点在路径头部，并且路径总容积满足车辆容积限制
       * 先单独判断 是为了防止 已经分配了车辆的路径没有充分负载
       * @param savedDistance
       */
             private void mergeSavedDistance(SavedDistance savedDistance) throws Exception {
                 RouteResp r1 = savedDistance.getP1().getCurrentRoute();
                 RouteResp r2 = savedDistance.getP2().getCurrentRoute();
                 PointReq p1 = savedDistance.getP1();
                 PointReq p2 = savedDistance.getP2();

                 if (r1.equals(r2)) return;

                 if (r1.getCapacity() != 0 ) {
                         //如果r1已分配车辆， 计算 容积限制
                         tryMergeToRoute(savedDistance, r1, r2, p1, p2);
                         return;
                     }

                 if (r2.getCapacity() != 0) {
                         //如果r2已分配车辆，计算 容积限制
                         tryMergeToRoute(savedDistance, r2, r1, p2, p1);
                         return;
                     }

                 //如果都没有分配过车辆, 给r1分配 目前容积最大的车辆
                 if (r1.getCapacity() == 0) {
                         if (vehicleCapacityList.isEmpty()) throw new Exception("汽车已经分配完了");
                         //设置车辆总容积
                         Float capacity = vehicleCapacityList.pop();
                         usedVehicleList.add(capacity);
                         r1.setCapacity(capacity * FACTOR);

                         tryMergeToRoute(savedDistance, r1, r2, p1, p2);
                         return;
                     }

                 //超过r1容积限制，尝试r2。如果没有分配过车辆, 给r2分配 目前容积最大的车辆
                 if (r2.getCapacity() == 0) {
                         if (vehicleCapacityList.isEmpty()) throw new Exception("汽车已经分配完了");
                         //设置车辆总容积
                         Float capacity = vehicleCapacityList.pop();
                         usedVehicleList.add(capacity);
                         r2.setCapacity(capacity * FACTOR);

                         tryMergeToRoute(savedDistance, r2, r1, p2, p1);
                     }
             }

             private void tryMergeToRoute(SavedDistance savedDistance,
                                          RouteResp existRoute,
                                          RouteResp mergedRoute,
                                          PointReq existNode,
                                          PointReq mergedNode) throws Exception {
                 if (appendMergedRoute(existRoute, mergedRoute, existNode,
                                 mergedNode)) {
                         if (capacityFeasible(existRoute, mergedRoute, false)) {
                                 //合并到现有路径之后
                                 if (mergedRoute.hardTimeWindowFeasible(existNode)) {
                                         mergeRoute(existRoute, mergedRoute, savedDistance
                                                         , existNode, false);
                                     }
                             }
                     } else if (insertMergedRoute(existRoute, mergedRoute,
                                 existNode, mergedNode)) {
                         if (capacityFeasible(existRoute, mergedRoute, true)) {
                                 //合并到现有路径之前
                                 if (existRoute.hardTimeWindowFeasible(mergedNode)) {
                                         mergeRoute(existRoute, mergedRoute, savedDistance
                                                         , existNode, true);
                                     }
                             }
                     }
             }

             private boolean insertMergedRoute(RouteResp existRoute,
                                               RouteResp mergedRoute,
                                               PointReq existNode,
                                               PointReq mergedNode) throws Exception {
                 if (mergedRoute.getNodes().size() < 3 || existRoute.getNodes().size() < 3)
                         throw new Exception("合并路径 节点少于3个");
                 return existRoute.getNodes().indexOf(existNode) == 1 && mergedRoute.getNodes().indexOf(mergedNode) == mergedRoute.getNodes().size() - 2;
             }

             private boolean appendMergedRoute(RouteResp existRoute,
                                               RouteResp mergedRoute,
                                               PointReq existNode,
                                               PointReq mergedNode) throws Exception {
                 if (mergedRoute.getNodes().size() < 3 || existRoute.getNodes().size() < 3)
                         throw new Exception("合并路径 节点少于3个");
                 return existRoute.getNodes().indexOf(existNode) == existRoute.getNodes().size() - 2 && mergedRoute.getNodes().indexOf(mergedNode) == 1;
             }

             private boolean capacityFeasible(RouteResp existRoute,
                                              RouteResp mergedRoute,
                                      boolean isInsert) throws Exception {
                 if (existRoute.getCapacity() > (mergedRoute.getTotalReceive() + existRoute.getTotalReceive()) ) {
                         if (isInsert) {
                                 Float curLoad = mergedRoute.getTotalSendOut() + existRoute.getTotalReceive();
                                 for (PointReq node : existRoute.getNodes()) {
                                         if (curLoad - node.getReceive().floatValue() + node.getSendOut().floatValue() > existRoute.getCapacity()) {
                                                 return false;
                                             }
                                         curLoad = curLoad - node.getReceive().floatValue() + node.getSendOut().floatValue();
                                         if (curLoad < 0)
                                                 throw new Exception("isInsert=true, 当前载重出错，小于0");
                                     }
                             } else {
                                 Float curLoad = existRoute.getTotalSendOut() + mergedRoute.getTotalReceive();
                                 for (PointReq node : mergedRoute.getNodes()) {
                                         if (curLoad - node.getReceive().floatValue() + node.getSendOut().floatValue() > existRoute.getCapacity()) {
                                                 return false;
                                             }
                                         curLoad = curLoad - node.getReceive().floatValue() + node.getSendOut().floatValue();
                                         if (curLoad < 0)
                                                 throw new Exception("isInsert=false, 当前载重出错，小于0");
                                     }
                             }
                         return true;
                     }

                 return false;
             }

             /**
       * 合并路径 算法
       * @param existRoute
       * @param mergedRoute
       * @param savedDistance
       * @param p
       * @param beforeP
       * @throws Exception
       */
             private void mergeRoute(RouteResp existRoute, RouteResp mergedRoute,
                             SavedDistance savedDistance, PointReq p, Boolean beforeP) throws Exception {
                 //合并点在p1之前
                 LinkedList<PointReq> mergedNodes = mergedRoute.getNodes();
                 mergedNodes.removeFirst();
                 mergedNodes.removeLast();

                 //从合并处 插入 被合并路径中所有营业点
                 existRoute.getNodes().addAll(existRoute.getNodes().indexOf(p) + (beforeP ? 0 : 1), mergedRoute.getNodes());
                 //更新 原有路径上所有营业点 所在路径
                 mergedNodes.forEach(node -> {
                         node.setCurrentRoute(existRoute);
                     });
                 //更新原路径上点的前后关系
                 existRoute.updateNodeTracing();
                 //更新到达时间
                 existRoute.updateArrivedTime();
                 //更新载重
                 existRoute.addReceive(mergedRoute.getTotalReceive());
                 existRoute.addSendOut(mergedRoute.getTotalSendOut());
                 //更新路径长度
                 existRoute.setLength(existRoute.calcLength(existRoute.getNodes()));
                 //清除 被合并路径
                 if (mergedRoute.getCapacity() != 0f) {
                         vehicleCapacityList.push(mergedRoute.getCapacity() / FACTOR);
                         vehicleCapacityList.sort(Comparator.reverseOrder());
                         usedVehicleList.remove(mergedRoute.getCapacity() / FACTOR);
                     }
                 routeList.remove(mergedRoute);
             }



             private void printGeneral() {
                 System.out.println("车辆总数: " + vehicleNumber);
                 System.out.println("邮局总数: " + totalPointNumber);
                 System.out.println("使用车辆: " + usedVehicleList);
                 System.out.println("剩余车辆: " + vehicleCapacityList);
         //        System.out.println("邮局位置: " + postOfficeList);
         //        if (savingList.size() >= 5) {
         //            System.out.println("\n节约距离 top 5: " + savingList.subList(0, 5).toString());
         //        }
             }

             private void printRoute() {
                 System.out.println("\n路径: ");
                 for (int i = 0; i < routeList.size(); i++) {
                     RouteResp r = routeList.get(i);
                         System.out.println(i + " " + r.toString());
                     }
             }

             private void printTimeSchedule() {
                 System.out.println("\n到达时间 ");
                 for (int i = 0; i < routeList.size(); i++) {
                     RouteResp r = routeList.get(i);
                         System.out.println(i + " " + r.timeSchedule());
                     }
             }


             public int getTotalPointNumber() {
                 return totalPointNumber;
             }


             public LinkedList<Float> getUsedVehicleList() {
                 return usedVehicleList;
             }

             public List<PointReq> getPostOfficeList() {
                 return postOfficeList;
             }

             public List<RouteResp> getRouteList() {
                 return routeList;
             }
}
