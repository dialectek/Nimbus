// "Radar" view.
// From: https://stackoverflow.com/questions/25169231/radar-animation-android

package com.dialectek.nimbus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.google.android.material.slider.Slider;

import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

public class RadarView extends View {
   private final float ID_RADIUS = 15.0f;

   private int     fps         = 100;

   public RadarView(Context context)
   {
      this(context, null);
   }

   public RadarView(Context context, AttributeSet attrs)
   {
      this(context, attrs, 0);
   }

   public RadarView(Context context, AttributeSet attrs, int defStyleAttr)
   {
      super(context, attrs, defStyleAttr);
   }

   android.os.Handler mHandler = new android.os.Handler();
   Runnable           mTick    = new Runnable()
   {
      @Override
      public void run()
      {
         invalidate();
         mHandler.postDelayed(this, 1000 / fps);
      }
   };

   public void startUpdate()
   {
      mHandler.removeCallbacks(mTick);
      mHandler.post(mTick);
   }

   public void stopUpdate()
   {
      mHandler.removeCallbacks(mTick);
   }

   public void setFrameRate(int fps) { this.fps = fps; }
   public int getFrameRate() { return(this.fps); }

   @Override
   protected void onDraw(Canvas canvas)
   {
      super.onDraw(canvas);

      int width  = getWidth();
      int height = getHeight();
      int r = Math.min(width, height);

      Paint blackPaint = new Paint();
      blackPaint.setColor(Color.BLACK);
      blackPaint.setStyle(Paint.Style.STROKE);
      blackPaint.setStrokeWidth(1.0F);

      int   i          = r / 2;
      int   j          = i - 1;
      canvas.drawCircle(i, i, j, blackPaint);
      canvas.drawCircle(i, i, j, blackPaint);
      canvas.drawCircle(i, i, j * 3 / 4, blackPaint);
      canvas.drawCircle(i, i, j >> 1, blackPaint);
      canvas.drawCircle(i, i, j >> 2, blackPaint);
      canvas.drawLine(0, i, r, i, blackPaint);
      canvas.drawLine(i, 0, i, r, blackPaint);

      float r2 = (float)r / 2.0f;
      float bearing = MainActivity.CompassBearing;
      canvas.rotate( (float)((int)bearing), r2, r2);

      Paint colorPaint = new Paint();
      colorPaint.setStyle(Paint.Style.FILL);
      float range = MainActivity.RangeSlider.getValue();
      for (Map.Entry<String, ID> entry : MainActivity.DiscoveredIDs.entrySet())
      {
         ID data = entry.getValue();
         if (data.distance >= 0.0f && data.distance <= range)
         {
            float x = (data.xDist / range) * r2;
            float y = (data.yDist / range) * r2;
            if (Math.abs(x) <= r2 && Math.abs(y) <= r2) {
               colorPaint.setColor(data.color);
               canvas.drawCircle(r2 + x, r2 - y, ID_RADIUS, colorPaint);
            }
         }
      }
   }
}
