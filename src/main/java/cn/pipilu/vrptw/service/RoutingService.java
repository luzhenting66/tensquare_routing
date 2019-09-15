package cn.pipilu.vrptw.service;

import cn.pipilu.vrptw.request.GetRoutingReq;
import cn.pipilu.vrptw.response.GetRoutingResp;

public interface RoutingService {
    GetRoutingResp getRouting(GetRoutingReq reqData);
}
