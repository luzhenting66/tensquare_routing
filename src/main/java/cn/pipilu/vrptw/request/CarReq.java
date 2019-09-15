package cn.pipilu.vrptw.request;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class CarReq {
    private String carNo;
    private BigDecimal carCap;//车容量载重
    private BigDecimal carLength;//车长
    private BigDecimal carHigh;//车高度
    private BigDecimal carDepth;//车深度
}
