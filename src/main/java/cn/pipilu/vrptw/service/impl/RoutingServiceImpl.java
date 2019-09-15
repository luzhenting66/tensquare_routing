package cn.pipilu.vrptw.service.impl;

import cn.pipilu.vrptw.constant.OrderTypeE;
import cn.pipilu.vrptw.request.GetRoutingReq;
import cn.pipilu.vrptw.request.OrderReq;
import cn.pipilu.vrptw.request.PointReq;
import cn.pipilu.vrptw.response.GetRoutingResp;
import cn.pipilu.vrptw.response.RouteResp;
import cn.pipilu.vrptw.response.SavedDistance;
import cn.pipilu.vrptw.service.RoutingService;
import cn.pipilu.vrptw.util.DistanceUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoutingServiceImpl implements RoutingService {
    public static final int duration_TIME = 15;// 卸货时间 min
    public static final int FACTOR = 1;

    //获取路由
    @Override
    public GetRoutingResp getRouting(GetRoutingReq reqData) {
        //1、获取所有订单信息
        List<OrderReq> orderReqList = reqData.getOrderReqList();
        //2、获取所有的 下转移订单信息，求出最后一个网点信息
        if (CollectionUtils.isEmpty(orderReqList))
            return null;
        List<OrderReq> xzyOrderList = orderReqList.stream().filter(order -> Objects.equals(OrderTypeE.XZY_ORDER.code, order.getType())).collect(Collectors.toList());
        // 下转移网点
        List<PointReq> xzyPointList = getXzyPointList(xzyOrderList);
        PointReq origin = getOriginPoint(reqData);
        List<PointReq> xyz_pointList = new LinkedList<>();
        xyz_pointList.add(origin);
        xyz_pointList.addAll(xzyPointList);
        //得到下转移订单的距离矩阵
        float[][] xzyDistMatrix = getXzyPointDistMatrix(xyz_pointList);
        //节约的距离
        List<SavedDistance> savingList = calcSavingMatrix(xyz_pointList);
        //构建基础路径
        try {
            int xyzPointSize = xyz_pointList.size();
            int carSize = reqData.getCarReqList().size();
            List<RouteResp> routeList = calcRoute(xyz_pointList);
            LinkedList<Float> vehicleCapacityList = new LinkedList<>();
            LinkedList<Float> usedVehicleList = new LinkedList<>();
            cwSaving(savingList, reqData,routeList,vehicleCapacityList,usedVehicleList);

            twoOptOptimise(routeList);
            capacityOptimise(routeList,vehicleCapacityList,usedVehicleList);
            printGeneral(routeList,vehicleCapacityList,usedVehicleList,carSize,xyzPointSize);
            printRoute(routeList);
            printTimeSchedule(routeList);
        }catch (Exception e) {
            e.printStackTrace();
        }

        //3、派送订单信息，时间窗派送
        List<OrderReq> psOrderList = orderReqList.stream().filter(order -> !Objects.equals(OrderTypeE.XZY_ORDER.code, order.getType())).collect(Collectors.toList());
        return null;
    }
    private void printRoute(List<RouteResp> routeList) {
        System.out.println("\n路径: ");
        for (int i = 0; i < routeList.size(); i++) {
            RouteResp r = routeList.get(i);
            System.out.println(i + " " + r.toString());
        }
    }
    private void printTimeSchedule(List<RouteResp> routeList) {
        System.out.println("\n到达时间 ");
        for (int i = 0; i < routeList.size(); i++) {
            RouteResp r = routeList.get(i);
            System.out.println(i + " " + r.timeSchedule());
        }
    }

    private void printGeneral(List<RouteResp> routeList,LinkedList<Float> vehicleCapacityList,LinkedList<Float> usedVehicleList,int carSize,int xyzPointSize) {
        System.out.println("车辆总数: " + carSize);
        System.out.println("邮局总数: " + xyzPointSize);
        System.out.println("使用车辆: " + usedVehicleList);
        System.out.println("剩余车辆: " + vehicleCapacityList);
        //        System.out.println("邮局位置: " + postOfficeList);
        //        if (savingList.size() >= 5) {
        //            System.out.println("\n节约距离 top 5: " + savingList.subList(0, 5).toString());
        //        }
    }
    private void cwSaving(List<SavedDistance> savingList, GetRoutingReq reqData,List<RouteResp> routeList,LinkedList<Float> vehicleCapacityList,LinkedList<Float> usedVehicleList) throws Exception {
        //取出save值最大的路径，尝试加入当前路径
        for (SavedDistance savedDistance : savingList) {
            mergeSavedDistance(savedDistance, reqData,routeList,vehicleCapacityList,usedVehicleList);
        }
    }
    private boolean twoOptOptimise(List<RouteResp> routeList) {
        for (RouteResp route : routeList) {
            if (route.twoOptOptimise()) {
                return true;
            }
        }
        return false;
    }
    private boolean capacityOptimise(List<RouteResp> routeList,LinkedList<Float> vehicleCapacityList,LinkedList<Float> usedVehicleList) {
        for (RouteResp route : routeList) {
            if (route.vehicleOptimise(vehicleCapacityList, usedVehicleList)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 合并路径规则：
     * 两点中有一点在路径尾部，一点在路径头部，并且路径总容积满足车辆容积限制
     * 先单独判断 是为了防止 已经分配了车辆的路径没有充分负载
     *
     * @param savedDistance
     * @param reqData
     */
    private void mergeSavedDistance(SavedDistance savedDistance, GetRoutingReq reqData,List<RouteResp> routeList,LinkedList<Float> vehicleCapacityList,LinkedList<Float> usedVehicleList) throws Exception {
        RouteResp r1 = savedDistance.getP1().getCurrentRoute();
        RouteResp r2 = savedDistance.getP2().getCurrentRoute();
        PointReq p1 = savedDistance.getP1();
        PointReq p2 = savedDistance.getP2();

        if (r1.equals(r2)) return;

        if (r1.getCapacity() != 0) {
            //如果r1已分配车辆， 计算 容积限制
            tryMergeToRoute(savedDistance, r1, r2, p1, p2,routeList,vehicleCapacityList,usedVehicleList);
            return;
        }

        if (r2.getCapacity() != 0) {
            //如果r2已分配车辆，计算 容积限制
            tryMergeToRoute(savedDistance, r2, r1, p2, p1,routeList,vehicleCapacityList,usedVehicleList);
            return;
        }

        //如果都没有分配过车辆, 给r1分配 目前容积最大的车辆
        if (r1.getCapacity() == 0) {
            if (vehicleCapacityList.isEmpty()) throw new Exception("汽车已经分配完了");
            //设置车辆总容积
            Float capacity = vehicleCapacityList.pop();
            usedVehicleList.add(capacity);
            r1.setCapacity(capacity * FACTOR);

            tryMergeToRoute(savedDistance, r1, r2, p1, p2,routeList,vehicleCapacityList,usedVehicleList);
            return;
        }

        //超过r1容积限制，尝试r2。如果没有分配过车辆, 给r2分配 目前容积最大的车辆
        if (r2.getCapacity() == 0) {
            if (vehicleCapacityList.isEmpty()) throw new Exception("汽车已经分配完了");
            //设置车辆总容积
            Float capacity = vehicleCapacityList.pop();
            usedVehicleList.add(capacity);
            r2.setCapacity(capacity * FACTOR);

            tryMergeToRoute(savedDistance, r2, r1, p2, p1,routeList,vehicleCapacityList,usedVehicleList);
        }
    }

    private void tryMergeToRoute(SavedDistance savedDistance, RouteResp existRoute, RouteResp mergedRoute, PointReq existNode, PointReq mergedNode,List<RouteResp> routeList,LinkedList<Float> vehicleCapacityList,LinkedList<Float> usedVehicleList) throws Exception {
        if (appendMergedRoute(existRoute, mergedRoute, existNode, mergedNode)) {
            if (capacityFeasible(existRoute, mergedRoute, false)) {
                //合并到现有路径之后
                if (mergedRoute.hardTimeWindowFeasible(existNode)) {
                    mergeRoute(existRoute, mergedRoute, savedDistance, existNode, false,routeList,vehicleCapacityList,usedVehicleList);
                }
            }
        } else if (insertMergedRoute(existRoute, mergedRoute, existNode, mergedNode)) {
            if (capacityFeasible(existRoute, mergedRoute, true)) {
                //合并到现有路径之前
                if (existRoute.hardTimeWindowFeasible(mergedNode)) {
                    mergeRoute(existRoute, mergedRoute, savedDistance, existNode, true,routeList,vehicleCapacityList,usedVehicleList);
                }
            }
        }
    }
    private boolean insertMergedRoute(RouteResp existRoute, RouteResp mergedRoute, PointReq existNode,PointReq mergedNode) throws Exception {
        if (mergedRoute.getNodes().size() < 3 || existRoute.getNodes().size() < 3)
            throw new Exception("合并路径 节点少于3个");
        return existRoute.getNodes().indexOf(existNode) == 1 && mergedRoute.getNodes().indexOf(mergedNode) == mergedRoute.getNodes().size() - 2;
    }
    private boolean capacityFeasible(RouteResp existRoute, RouteResp mergedRoute, boolean isInsert) throws Exception {
        if (existRoute.getCapacity() > (mergedRoute.getTotalReceive() + existRoute.getTotalReceive())) {
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

    private boolean appendMergedRoute(RouteResp existRoute,RouteResp mergedRoute, PointReq existNode, PointReq mergedNode) throws Exception {
        if (mergedRoute.getNodes().size() < 3 || existRoute.getNodes().size() < 3)
            throw new Exception("合并路径 节点少于3个");
        return existRoute.getNodes().indexOf(existNode) == existRoute.getNodes().size() - 2 && mergedRoute.getNodes().indexOf(mergedNode) == 1;
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
    private void mergeRoute(RouteResp existRoute, RouteResp mergedRoute, SavedDistance savedDistance, PointReq p, Boolean beforeP,List<RouteResp> routeList,LinkedList<Float> vehicleCapacityList,LinkedList<Float> usedVehicleList) throws Exception {
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


    /**
     * 构建基础路径
     *
     * @param xyz_pointList
     * @return
     */
    private List<RouteResp> calcRoute(List<PointReq> xyz_pointList) throws CloneNotSupportedException {
        //将所有点单独与集散中心组成一条路径，路径对象中包含集散中心
        List<RouteResp> routeList = new ArrayList<>();
        PointReq depot = xyz_pointList.get(0);
        for (int i = 1; i < xyz_pointList.size(); i++) {
            RouteResp r = new RouteResp();
            //更新点 所在路径
            PointReq startNode = (PointReq) depot.clone();
            startNode.setCurrentRoute(r);
            PointReq endNode = (PointReq) depot.clone();
            endNode.setCurrentRoute(r);
            xyz_pointList.get(i).setCurrentRoute(r);
            //更新路径 上的点
            r.setNodesAndUpdateLoad(new LinkedList<>(Arrays.asList(startNode, xyz_pointList.get(i), endNode)));

            //更新到达时间
            r.updateArrivedTime();
            //更新路径长度
            r.setLength(r.calcLength(r.getNodes()));
            //更新原路径上点的前后关系
            r.updateNodeTracing();
            //更新载重
            routeList.add(r);
        }
        return routeList;
    }

    /**
     * 计算节约距离列表
     *
     * @param xyz_pointList
     * @return
     */
    private List<SavedDistance> calcSavingMatrix(List<PointReq> xyz_pointList) {
        List<SavedDistance> savingList = new ArrayList<>();
        int size = xyz_pointList.size();
        for (int i = 2; i < size; i++) {
            for (int j = 1; j < i; j++) {
                PointReq pi = xyz_pointList.get(i);
                PointReq pj = xyz_pointList.get(j);
                PointReq depot = xyz_pointList.get(0);
                float dist = DistanceUtils.distanceTo(pi, pj);
                float saving = DistanceUtils.distanceTo(pi, depot) + DistanceUtils.distanceTo(pj, depot) - dist;
                savingList.add(new SavedDistance(xyz_pointList.get(i), xyz_pointList.get(j), saving));
            }
        }
        savingList.sort(Collections.reverseOrder());
        return savingList;
    }

    /**
     * 下转移订单的地点距离矩阵
     *
     * @param xyz_pointList
     * @return
     */
    private float[][] getXzyPointDistMatrix(List<PointReq> xyz_pointList) {
        int length = xyz_pointList.size();
        float[][] distMatrix = new float[length][length];
        for (int i = 0; i < length; i++) {
            for (int j = 0; j < i; j++) {
                distMatrix[i][j] = DistanceUtils.distanceTo(xyz_pointList.get(i), xyz_pointList.get(j));
                distMatrix[j][i] = distMatrix[i][j];
            }
            distMatrix[i][i] = 0;
        }
        return distMatrix;
    }

    private PointReq getOriginPoint(GetRoutingReq reqData) {
        BigDecimal longitude = reqData.getOLongitude();
        BigDecimal latitude = reqData.getOLatitude();
        PointReq origin = PointReq.builder()
                .arriveTime(0)
                .latestTime(0)
                .duration(0)
                .latitude(latitude)
                .longitude(longitude)
                .orderNo("0")
                .receive(BigDecimal.ZERO)
                .sendOut(BigDecimal.ZERO)
                .earliestTime(0)
                .weight(BigDecimal.ZERO)
                .pointType("0")
                .build();
        return origin;
    }

    private List<PointReq> getXzyPointList(List<OrderReq> xzyOrderList) {
        return xzyOrderList.stream().map(order -> {
            PointReq point = PointReq.builder()
                    .orderNo(order.getOrderNo())
                    .orderType(OrderTypeE.XZY_ORDER.code)
                    .duration(duration_TIME)
                    .pointType("1")
                    .latestTime(order.getLatestTime())
                    .earliestTime(order.getEarliestTime())
                    .longitude(order.getLongitude())
                    .latitude(order.getLatitude())
                    .address(order.getAddress())
                    .sendOut(order.getWeight())
                    .build();
            return point;
        }).collect(Collectors.toList());
    }
}
