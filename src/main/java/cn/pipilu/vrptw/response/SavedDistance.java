package cn.pipilu.vrptw.response;

import cn.pipilu.vrptw.request.PointReq;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SavedDistance implements Comparable {
    private PointReq p1;
    private PointReq p2;
    private float savedDistance;

    public SavedDistance(PointReq p1, PointReq p2, float savedDistance) {
        this.p1 = p1;
        this.p2 = p2;
        this.savedDistance = savedDistance;
    }

    @Override
    public String toString() {
        return "SD{" +
                "(" + p1 +
                " -> " + p2 +
                "), saved=" + savedDistance +
                '}';
    }

    @Override
    public int compareTo(Object o) {
        return Float.compare(savedDistance, ((SavedDistance) o).savedDistance);
    }

    public PointReq nodeAt(RouteResp existRoute) throws Exception {
        if (existRoute.getNodes().contains(p1)) {
            return p1;
        } else if (existRoute.getNodes().contains(p2)) {
            return p2;
        }

        throw new Exception("p1:" + p1 + ", p2:" + p2 + ". 均不存在于路径：" + existRoute);
    }
}
