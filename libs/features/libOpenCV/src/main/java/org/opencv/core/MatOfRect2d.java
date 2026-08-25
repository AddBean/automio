package org.opencv.core;

import java.util.Arrays;
import java.util.List;


public class MatOfRect2d extends org.opencv.core.Mat {
    // 64FC4
    private static final int _depth = org.opencv.core.CvType.CV_64F;
    private static final int _channels = 4;

    public MatOfRect2d() {
        super();
    }

    protected MatOfRect2d(long addr) {
        super(addr);
        if( !empty() && checkVector(_channels, _depth) < 0 )
            throw new IllegalArgumentException("Incompatible Mat");
        //FIXME: do we need release() here?
    }

    public static MatOfRect2d fromNativeAddr(long addr) {
        return new MatOfRect2d(addr);
    }

    public MatOfRect2d(Mat m) {
        super(m, Range.all());
        if( !empty() && checkVector(_channels, _depth) < 0 )
            throw new IllegalArgumentException("Incompatible Mat");
        //FIXME: do we need release() here?
    }

    public MatOfRect2d(org.opencv.core.Rect2d...a) {
        super();
        fromArray(a);
    }

    public void alloc(int elemNumber) {
        if(elemNumber>0)
            super.create(elemNumber, 1, CvType.makeType(_depth, _channels));
    }

    public void fromArray(org.opencv.core.Rect2d...a) {
        if(a==null || a.length==0)
            return;
        int num = a.length;
        alloc(num);
        double buff[] = new double[num * _channels];
        for(int i=0; i<num; i++) {
            org.opencv.core.Rect2d r = a[i];
            buff[_channels*i+0] = (double) r.x;
            buff[_channels*i+1] = (double) r.y;
            buff[_channels*i+2] = (double) r.width;
            buff[_channels*i+3] = (double) r.height;
        }
        put(0, 0, buff); //TODO: check ret val!
    }


    public org.opencv.core.Rect2d[] toArray() {
        int num = (int) total();
        org.opencv.core.Rect2d[] a = new org.opencv.core.Rect2d[num];
        if(num == 0)
            return a;
        double buff[] = new double[num * _channels];
        get(0, 0, buff); //TODO: check ret val!
        for(int i=0; i<num; i++)
            a[i] = new org.opencv.core.Rect2d(buff[i*_channels], buff[i*_channels+1], buff[i*_channels+2], buff[i*_channels+3]);
        return a;
    }
    public void fromList(List<org.opencv.core.Rect2d> lr) {
        org.opencv.core.Rect2d ap[] = lr.toArray(new org.opencv.core.Rect2d[0]);
        fromArray(ap);
    }

    public List<org.opencv.core.Rect2d> toList() {
        Rect2d[] ar = toArray();
        return Arrays.asList(ar);
    }
}
