package cn.pipilu.vrptw.request;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.List;

@Data
@ToString
public class GetRoutingReq {
    private BigDecimal oLongitude;//原点x
    private BigDecimal oLatitude;//原点y
    private List<OrderReq> orderReqList;
    private List<CarReq> carReqList;

}
