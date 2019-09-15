package cn.pipilu.vrptw.request;

import cn.pipilu.vrptw.response.RouteResp;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.Objects;

@Data
@ToString
@Builder
public class PointReq implements Cloneable {
    private String orderNo;
    private String address;
    private BigDecimal longitude;   //经度
    private BigDecimal latitude;    //纬度
    private BigDecimal weight;      //公斤
    private int earliestTime;      //最早到达时间
    private int latestTime;        //最早到达时间
    private int duration;          //卸货时间
    private String orderType;//订单类型  1-下转移、2-vip 派送、3-普通派送
    private String pointType;//0-原点，
    private BigDecimal receive;

    private BigDecimal sendOut;
    private BigDecimal cargoWeight;//货物重量

    private int arriveTime;//到达时间

    private RouteResp currentRoute;

    private PointReq previousNode;

    private PointReq nextNode;

    @Override
    public Object clone() throws CloneNotSupportedException {
        PointReq clone = (PointReq) super.clone();
        clone.setCurrentRoute(Objects.isNull(currentRoute) ? null : (RouteResp) currentRoute.clone());
        return clone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointReq that = (PointReq) o;
        return that.longitude.compareTo(longitude) == 0 && that.latitude.compareTo(latitude) == 0;
    }

    @Override
    public String toString() {

        return new StringBuilder().append("point{")
                .append(orderNo)
                .append(" (")
                .append(", ")
                .append(latitude)
                .append("),")
                .append(address)
                .append("}").toString();

    }

    public String getTimeInterval() {
        return orderNo + " [到达时间：" + convertHHmm(arriveTime) +
                ", 出发时间：" + convertHHmm(getDepartTime()) +
                "]";
    }

    public String convertHHmm(int mins) {
        return (mins < 60 ? "0:" : mins / 60 + ":") + mins % 60 + "";
    }

    public String getCoordinate() {
        return orderNo + " [" + longitude + ", " + latitude + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(latitude, longitude);
    }

    public int getDepartTime() {
        return arriveTime + duration;
    }
}
