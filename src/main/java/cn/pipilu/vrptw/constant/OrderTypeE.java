package cn.pipilu.vrptw.constant;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public enum OrderTypeE {
    XZY_ORDER("1","下转移订单"),
    VIP_USER_ORDER("2","VIP用户派送订单"),
    NORMAL_USER_ORDER("3","普通派送订单");
    public String code;
    public String label;

    OrderTypeE(String code,String label){
        this.code = code;
        this.label = label;
    }

    public static OrderTypeE getOrderType(String code){
        if (StringUtils.isBlank(code))
            return null;
        OrderTypeE[] typeES = OrderTypeE.values();
        for (OrderTypeE typeE : typeES) {
            if (Objects.equals(code,typeE.code)){
                return typeE;
            }
        }
        return null;
    }

    public static String getOrderTypeLabel(String code){
        OrderTypeE typeE = getOrderType(code);
        return Objects.isNull(typeE)?"订单类型不确定":typeE.label;
    }

}
