// ID.

package com.dialectek.nimbus;

import android.graphics.Color;
import java.time.Instant;

public class ID {
   public String name;
   public int color;
   public Instant time;
   public float distance;
   public float xDist;
   public float yDist;
   public double latitude;
   public double longitude;

   public ID(String name, int color, float distance, float xDist, float yDist,
             double latitude, double longitude, Instant time) {
      this.name = name;
      this.color = color;
      this.distance = distance;
      this.xDist = xDist;
      this.yDist = yDist;
      this.latitude = latitude;
      this.longitude = longitude;
      this.time = time;
   }
}
