package cn.pipilu.vrptw.response;

import cn.pipilu.vrptw.constant.OrderTypeE;
import cn.pipilu.vrptw.request.PointReq;
import cn.pipilu.vrptw.util.DistanceUtils;
import lombok.Data;
import lombok.ToString;

import java.util.*;
import java.util.stream.Collectors;
@Data
@ToString
public class RouteResp implements Cloneable {
    public static final double DEFAULT_DELTA = 0.0001;
    private LinkedList<PointReq> nodes;

    private Float capacity = 0f;

    private Float totalReceive = 0f;

    private Float totalSendOut = 0f;

    private Float length = 0f;

    /**
     * 公里每分钟
     */
    private static double xzy_speed = 0.66d;
    private static double normal_speed = 0.33d;

    public void setNodesAndUpdateLoad(List<PointReq> nodes) {
        this.nodes = new LinkedList<>(nodes);
    }


    public void addReceive(Float receive) {
        totalReceive += receive;
    }

    public void addSendOut(Float sendOut) {
        totalSendOut += sendOut;
    }

    public Float calcLength(LinkedList<PointReq> nodes) {
        Float length = 0f;
        if (!nodes.isEmpty()) {
            PointReq firstNode = nodes.getFirst();
            for (int i = 1; i < nodes.size(); i++) {
                PointReq next = nodes.get(i);
                length += DistanceUtils.distanceTo(next,firstNode);
                firstNode = next;
            }
        }
        return length;
    }

    public boolean twoOptOptimise() {
        //交换中间路径 任意两点，尝试优化路径
        boolean optimised = false;
        for (int i = 1; i < nodes.size() - 1; i++) {
            for (int j = i + 1; j < nodes.size() - 1; j++) {
                LinkedList<PointReq> tempList = (LinkedList<PointReq>) nodes.clone();
                int k = i, l = j;
                while (k < l) {
                    Collections.swap(tempList, k, l);
                    k++;
                    l--;
                }
                Float tempLength = calcLength(tempList);
                if (length - tempLength > DEFAULT_DELTA) {
                    //优化成功
                    nodes = tempList;
                    length = tempLength;
                    updateNodeTracing();
                    updateArrivedTime();
                    optimised = true;
                }
            }
        }
        return optimised;
    }

    /**
     * 更新路径上点的前后关系
     */
    public void updateNodeTracing() {
        PointReq previous = nodes.get(0);
        for (int i = 1; i < nodes.size(); i++) {
            PointReq node = nodes.get(i);
            //设置点的前后关系
            node.setPreviousNode(previous);
            previous.setNextNode(node);
            previous = node;
        }
    }

    public void updateArrivedTime() {
        PointReq previous = nodes.get(0);
        previous.setArriveTime(previous.getEarliestTime());
        double speed;
        for (int i = 1; i < nodes.size(); i++) {
            PointReq node = nodes.get(i);
            // 节点到达时间为 离开上一节点时间加上路程时间
            if (Objects.equals(OrderTypeE.XZY_ORDER.code,node.getPointType())){
                speed = xzy_speed;
            }else {
                speed = normal_speed;

            }
            int arrivedTime =previous.getDepartTime() + (int) (DistanceUtils.distanceTo(node,previous) / speed);
            node.setArriveTime(arrivedTime);
            previous = node;
        }

    }

    @Override
    public String toString() {
        return (nodes == null ? "[]" : nodes.stream().map(PointReq::getCoordinate).collect(Collectors.toList()).toString()) +
                ", 车辆载重=" + capacity +
                ", 总送达=" + totalReceive +
                ", 总收寄=" + totalSendOut +
                ", 总长度=" + length + "公里";
    }

    public String timeSchedule() {
        return "到达时间{邮局=" + (nodes == null ? "[]" : nodes.stream().map(PointReq::getTimeInterval).collect(Collectors.toList()).toString());
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * 硬时间窗限制
     * 到达时间 不能早于最早时间，不能晚于最晚时间
     *
     * @param p1
     * @return
     */
    public boolean hardTimeWindowFeasible(PointReq p1) {
        PointReq previous = p1;
        int lastDepart = previous.getDepartTime();
        double speed;
        for (PointReq node : nodes) {
            if (Objects.equals(OrderTypeE.XZY_ORDER.code,node.getPointType())){
                speed = xzy_speed;
            }else {
                speed = normal_speed;
            }
            int arrivedTime = lastDepart + (int) ((DistanceUtils.distanceTo(node,previous)) / speed);
            if (arrivedTime < node.getEarliestTime() || arrivedTime > node.getLatestTime()) {
                return false;
            }
            lastDepart = arrivedTime + node.getDuration();
            previous = node;
        }
        return true;
    }
    public boolean vehicleOptimise(LinkedList<Float> vehicleCapacityList, LinkedList<Float> usedVehicleList) {

                 vehicleCapacityList.sort(Comparator.naturalOrder());
                 for (Float temp : vehicleCapacityList) {
                         if (temp < this.capacity) {
                                 if (temp > this.totalReceive) {
                                         Float curLoad = totalReceive;
                                         boolean cando = true;
                                         for (PointReq node : nodes) {
                                                 if ( curLoad - node.getReceive().floatValue() + node.getSendOut().floatValue() > temp) {
                                                         cando = false;
                                                         break;
                                                     }
                                                 curLoad = curLoad - node.getReceive().floatValue() + node.getSendOut().floatValue();
                                             }
                                         if (cando) {
                                                 vehicleCapacityList.remove(temp);
                                                 vehicleCapacityList.add(capacity);
                                                 usedVehicleList.remove(capacity);
                                                 usedVehicleList.add(temp);
                                                 this.capacity = temp;
                                                 return true;
                                             }
                                     }

                             }
                     }
                 return false;
             }
}
