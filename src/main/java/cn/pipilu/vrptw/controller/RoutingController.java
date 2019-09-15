package cn.pipilu.vrptw.controller;

import cn.pipilu.plus.common.request.Request;
import cn.pipilu.plus.common.response.Response;
import cn.pipilu.plus.common.util.ResponseUtil;
import cn.pipilu.vrptw.request.GetRoutingReq;
import cn.pipilu.vrptw.response.GetRoutingResp;
import cn.pipilu.vrptw.service.RoutingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/tensquare-routing/routing")
public class RoutingController {
    @Autowired
    private RoutingService routingService;
    @RequestMapping("/getRouting")
    public Response<GetRoutingResp> getRouting(@RequestBody Request<GetRoutingReq> reqData){

        Response<GetRoutingResp> response = new Response<>();
        try {
            response.setRespData(routingService.getRouting(reqData.getReqData()));
            ResponseUtil.setRespParam(response);
        }catch (Exception e){
            ResponseUtil.setRespParam(response,e);
        }

        return response;

    }

}
