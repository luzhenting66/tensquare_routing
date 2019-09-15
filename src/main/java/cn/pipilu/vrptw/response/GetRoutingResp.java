package cn.pipilu.vrptw.response;

import cn.pipilu.vrptw.request.OrderReq;
import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class GetRoutingResp {
    private String routing;//线路
    private List<OrderReq> datails;
}
