package o;

import java.io.Serializable;

public class ld implements Serializable {
    private static final long serialVersionUID = -5500582875379116646L;

    public final double ˋ;
    public final double ˎ;

    public ld(double lat, double lng) {
        this.ˋ = Math.toRadians(lat);
        this.ˎ = Math.toRadians(lng);
    }
}
