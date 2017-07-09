package com.rexy.widgets.tools;

import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;

/**
 * TODO:功能说明
 *
 * @author: renzheng
 * @date: 2016-05-04 09:36
 */
public class PaintUtils {
    /**
     * @param color the original color.。
     * @return argb array。
     */
    public static int[] color(int color) {
        int[] r = new int[4];
        r[0] = color >>> 24;
        r[1] = (color & 0x00FF0000) >>> 16;
        r[2] = (color & 0x0000FF00) >>> 8;
        r[3] = color & 0x000000FF;
        return r;
    }

    /**
     * keep the RGB value and return a new color with a new alpha.
     *
     * @param @param color original color with alpha value.
     * @param @param alphaScale between 0 and 1;
     */
    public static int color(int color, int alpha) {
        if (alpha >= 0 && alpha <= 255) {
            return (color & 0x00FFFFFF) | (alpha << 24);
        }
        return color;
    }

    /**
     * @param @param color original color with alpha value.
     * @param @param scale between 0 and 1;
     * @param scaleType 0 for only alpha ,1 for only rgb,other for all.
     */
    public static int color(int color, float scale, int scaleType) {
        if (scale >= 0 && scale <= 1) {
            if (scaleType == 0) {
                int alpha = Math.round(((color >>> 24) * scale));
                return (color & 0x00FFFFFF) | (alpha << 24);
            } else {
                int argb[] = color(color);
                int na = scaleType == 1 ? argb[0] : Math.min(Math.round(argb[0] * scale), 255);
                int nr = Math.min(Math.round(argb[1] * scale), 255);
                int ng = Math.min(Math.round(argb[2] * scale), 255);
                int nb = Math.min(Math.round(argb[3] * scale), 255);
                int ncolor = (na << 24) | (nr << 16) | (ng << 8) | nb;
                return ncolor;
            }
        }
        return color;
    }

    //////////////////////////////////////////////////////////////////////////////
    public static RectF centRect(RectF rectDes, float cenX, float cenY) {
        float dx = cenX - rectDes.centerX();
        float dy = cenY - rectDes.centerY();
        rectDes.offset(dx, dy);
        return rectDes;
    }

    public static Rect centRect(Rect rectDes, int cenX, int cenY) {
        int dx = cenX - rectDes.centerX();
        int dy = cenY - rectDes.centerY();
        rectDes.offset(dx, dy);
        return rectDes;
    }

    public static RectF scaleRect(RectF rectDes, float scale) {
        float cenX = rectDes.centerX();
        float cenY = rectDes.centerY();
        float newWidth = rectDes.width() * scale;
        float newHeight = rectDes.height() * scale;
        rectDes.set(0, 0, newWidth, newHeight);
        return centRect(rectDes, cenX, cenY);
    }

    public static Rect scaleRect(Rect rectDes, float scale) {
        float cenX = rectDes.centerX();
        float cenY = rectDes.centerY();
        int newWidth = Math.round(rectDes.width() * scale);
        int newHeight = Math.round(rectDes.height() * scale);
        rectDes.set(0, 0, newWidth, newHeight);
        return centRect(rectDes, Math.round(cenX), Math.round(cenY));
    }
    //////////////////////////////////////////////////////////////////////////////


    public static boolean pointInCircle(float x, float y, float cenX, float cenY, float r) {
        float dx = x - cenX;
        float dy = y - cenY;
        float dr = r * r;
        return dx * dx / dr + dy * dy / dr < 1;
    }

    public static boolean pointInOval(float x, float y, RectF rect) {
        if (rect.contains(x, y)) {
            float dx = x - rect.centerX();
            float dy = y - rect.centerY();
            float a = rect.width() / 2;
            float b = rect.height() / 2;
            return dx * dx / (a * a) + dy * dy / (b * b) < 1;
        }
        return false;
    }

    /**
     * @param d 弦长
     * @param r 半径
     * @return [0，PI]
     */
    public static double chordToRadian(float d, float r) {
        double result = 0;
        if (r > 0 && d > 0) {
            d = d / 2;
            if (d >= r) {
                result = Math.PI;
            } else {
                result = 2 * Math.asin(d / r);
            }
        }
        return result;
    }

    /**
     * @param d 弦长
     * @param r 半径
     * @return [0，180]
     */
    public static double chordToAngle(float d, float r) {
        double result = 0;
        if (r > 0 && d > 0) {
            d = d / 2;
            if (d >= r) {
                result = 180;
            } else {
                result = 360 * Math.asin(d / r) / Math.PI;
            }
        }
        return result;
    }

    /**
     * 基于3点钟顺时方向返回角度[0,360].正好和画扇形的方式一致。
     */
    public static float pointToAngle(float x, float y, float cenx, float ceny) {
        float dx = x - cenx;
        float dy = y - ceny;
        if (dx == 0) {
            return (dy == 0 ? -1 : (dy > 0 ? 90 : 270));
        } else {
            if (dx > 0) {
                return (dy < 0 ? 360 : 0) + (float) (180 * Math.atan(dy / dx) / Math.PI);
            } else {
                return 180 + (float) (180 * Math.atan(dy / dx) / Math.PI);
            }
        }
    }

    /**
     * 基于3点钟顺时方向返回弧度[0,2PI].正好和画扇形的方式一致。
     */
    public static double pointToRadian(float x, float y, float cenx, float ceny) {
        float dx = x - cenx;
        float dy = y - ceny;
        double result;
        if (dx == 0) {
            result = dy == 0 ? -1 : Math.PI / 2;
            result += (dy < 0 ? Math.PI : 0);
        } else {
            result = Math.atan(dy / dx);
            if (dx > 0) {
                result += (dy < 0 ? (Math.PI + Math.PI) : 0);
            } else {
                result += Math.PI;
            }
        }
        return result;
    }


    public static PointF angleToPoint(float angle, RectF rect, PointF point) {
        return radianToPoint(angle * Math.PI / 180, rect, point);
    }

    public static PointF radianToPoint(double radian, RectF rect, PointF point) {
        if (point == null) {
            point = new PointF(rect.centerX(), rect.centerY());
        } else {
            point.set(rect.centerX(), rect.centerY());
        }
        point.offset((float) (rect.width() * Math.cos(radian) / 2), (float) (rect.height() * Math.sin(radian) / 2));
        return point;
    }

    /**
     * @param angle outer[left-top] ,inner[right-bottom]
     */
    public static void createSectorRing(Path path, RectF outer, RectF inner, RectF angle, PointF point) {
        path.reset();
        path.addArc(outer, angle.left, angle.top - angle.left);
        PaintUtils.angleToPoint(angle.bottom, inner, point);
        path.lineTo(point.x, point.y);
        path.addArc(inner, angle.bottom, angle.right - angle.bottom);
        PaintUtils.angleToPoint(angle.left, outer, point);
        path.lineTo(point.x, point.y);
    }


    //////////////////////////////////////////////////////////////////////////////
    public static PointF dividePoint(float x1, float y1, float x2, float y2, PointF result, float a) {
        result = result == null ? new PointF() : result;
        float b = a + 1;
        if (b == 0) {
            result.x = 2 * x1 - x2;
            result.y = 2 * y1 - y2;
        } else {
            result.x = (x1 + a * x2) / b;
            result.y = (y1 + a * y2) / b;
        }
        return result;
    }

    public static PointF dividePoint(float x1, float y1, float x2, float y2, float a) {
        return dividePoint(x1, y1, x2, y2, null, a);
    }

    public static PointF dividePoint(PointF p1, PointF p2, PointF result, float a) {
        return dividePoint(p1.x, p1.y, p2.x, p2.y, result, a);
    }

    //////////////////////////////////////////////////////////////////////////////

    private static Rect RECT = new Rect();

    public static float getTextOffsetRelativeBaseline(Paint paint) {
        float d = paint.descent();
        float a = paint.ascent();
        return (d - a) / 2 - d;
    }

    public static Rect getTextBounds(Paint paint, String text) {
        paint.getTextBounds(text, 0, text.length(), RECT);
        return RECT;
    }

    public static float getTextHeight(Paint paint) {
        return paint.descent() - paint.ascent();
    }

    public static float getAdjustTextSize(Paint paint, String text, float minTextSize, float maxTextSize, Rect reactText) {
        float oldTextSize = paint.getTextSize();
        int len = text == null ? 0 : text.length() >> 1;
        if (len == 0 || reactText == null || reactText.isEmpty()) {
            return oldTextSize;
        }
        float rate = 0.95f, w = reactText.width(), h = reactText.height();
        float result = Math.max(Math.min((w + w) / len, h), minTextSize);
        if (maxTextSize > 0) {
            result = Math.min(result, maxTextSize);
        }
        Rect recTest = new Rect();
        paint.setTextSize(result);
        paint.getTextBounds(text, 0, len, recTest);
        while (recTest.width() > w || recTest.height() > h) {
            result *= rate;
            if (result < minTextSize) {
                result = minTextSize;
                break;
            }
            paint.setTextSize(result);
            paint.getTextBounds(text, 0, len, recTest);
        }
        paint.setTextSize(oldTextSize);
        if (minTextSize > 0) {
            result = Math.max(minTextSize, result);
        }
        return result;
    }
}
