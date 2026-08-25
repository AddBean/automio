package org.opencv.core;

import java.util.Arrays;
import java.util.List;

public class MatOfPoint3f extends org.opencv.core.Mat {
    // 32FC3
    private static final int _depth = org.opencv.core.CvType.CV_32F;
    private static final int _channels = 3;

    public MatOfPoint3f() {
        super();
    }

    protected MatOfPoint3f(long addr) {
        super(addr);
        if( !empty() && checkVector(_channels, _depth) < 0 )
            throw new IllegalArgumentException("Incompatible Mat");
        //FIXME: do we need release() here?
    }

    public static MatOfPoint3f fromNativeAddr(long addr) {
        return new MatOfPoint3f(addr);
    }

    public MatOfPoint3f(Mat m) {
        super(m, Range.all());
        if( !empty() && checkVector(_channels, _depth) < 0 )
            throw new IllegalArgumentException("Incompatible Mat");
        //FIXME: do we need release() here?
    }

    public MatOfPoint3f(org.opencv.core.Point3...a) {
        super();
        fromArray(a);
    }

    public void alloc(int elemNumber) {
        if(elemNumber>0)
            super.create(elemNumber, 1, CvType.makeType(_depth, _channels));
    }

    public void fromArray(org.opencv.core.Point3...a) {
        if(a==null || a.length==0)
            return;
        int num = a.length;
        alloc(num);
        float buff[] = new float[num * _channels];
        for(int i=0; i<num; i++) {
            org.opencv.core.Point3 p = a[i];
            buff[_channels*i+0] = (float) p.x;
            buff[_channels*i+1] = (float) p.y;
            buff[_channels*i+2] = (float) p.z;
        }
        put(0, 0, buff); //TODO: check ret val!
    }

    public org.opencv.core.Point3[] toArray() {
        int num = (int) total();
        org.opencv.core.Point3[] ap = new org.opencv.core.Point3[num];
        if(num == 0)
            return ap;
        float buff[] = new float[num * _channels];
        get(0, 0, buff); //TODO: check ret val!
        for(int i=0; i<num; i++)
            ap[i] = new org.opencv.core.Point3(buff[i*_channels], buff[i*_channels+1], buff[i*_channels+2]);
        return ap;
    }

    public void fromList(List<org.opencv.core.Point3> lp) {
        org.opencv.core.Point3 ap[] = lp.toArray(new org.opencv.core.Point3[0]);
        fromArray(ap);
    }

    public List<org.opencv.core.Point3> toList() {
        Point3[] ap = toArray();
        return Arrays.asList(ap);
    }
}
