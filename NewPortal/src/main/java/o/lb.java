package o;

import java.io.Serializable;
public class lb implements Serializable {
      private static final long serialVersionUID = -1803945517509665335L;
      public final double ˋ;
      public final double ˎ;
      public lb(double lat, double lng){
        this.ˋ = Math.toRadians(lat);
        this.ˎ = Math.toRadians(lng);
      }
    }
