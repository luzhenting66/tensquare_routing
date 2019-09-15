package cn.pipilu.vrptw.request;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class OrderReq {
    private String orderNo;
    private String address;
    private BigDecimal longitude;   //经度
    private BigDecimal latitude;    //纬度
    private BigDecimal weight;      //公斤
    private int earliestTime;      //最早到达时间
    private int latestTime;        //最早到达时间
    private int duration;          //卸货时间
    private String type;//订单类型  1-下转移、2-vip 派送、3-普通派送
    private BigDecimal receive;

    private BigDecimal sendOut;
}
