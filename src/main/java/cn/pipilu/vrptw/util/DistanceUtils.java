package cn.pipilu.vrptw.util;

import cn.pipilu.vrptw.request.PointReq;

import java.math.BigDecimal;

public class DistanceUtils {

    private DistanceUtils() {
    }

    /**
     * 两个收获点的距离
     *
     * @param order1
     * @param order2
     * @return
     */
    public static float distanceTo(PointReq order1, PointReq order2) {
        return distanceTo(order1.getLatitude(), order1.getLongitude(), order2.getLatitude(), order2.getLongitude(), 'K');
    }

    private static float distanceTo(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2, char unit) {
        return distanceTo(lat1.doubleValue(), lon1.doubleValue(), lat2.doubleValue(), lon2.doubleValue(), unit);
    }

    private static float distanceTo(double lat1, double lon1, double lat2, double lon2, char unit) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        if (unit == 'K') {
            dist = dist * 1.609344;
        }
        return (float) (dist);
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private static double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }

}
