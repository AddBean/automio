// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.plugin.ocr;

import android.graphics.Point;
import android.graphics.Rect;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OcrResult {
    public List<Block> blocks;

    public static class Block {
        public String text;
        public Rect rect;
        public String language;
        public Point[] points;
        public List<Line> lines;

        @Override
        public String toString() {
            return "Block{" +
                    "text='" + text + '\'' +
                    ", rect=" + rect +
                    ", language='" + language + '\'' +
                    ", points=" + Arrays.toString(points) +
                    ", lines=" + lines +
                    '}';
        }
    }


    public static class Line {
        public String text;
        public Rect rect;
        public String language;
        public Point[] points;
        public Map<String,Rect> findResult;

        @Override
        public String toString() {
            return "Line{" +
                    "text='" + text + '\'' +
                    ", rect=" + rect +
                    ", language='" + language + '\'' +
                    ", points=" + Arrays.toString(points) +
                    ", findResult=" + findResult +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "OcrResult{" +
                "blocks=" + blocks +
                '}';
    }
}
