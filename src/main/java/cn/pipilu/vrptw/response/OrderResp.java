package cn.pipilu.vrptw.response;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class OrderResp {
    private String orderNo;
    private String address;
    private String arriveTime;
    private String earliestTime;
    private String latestTime;
    private int duration;
    private String orderType;
}
