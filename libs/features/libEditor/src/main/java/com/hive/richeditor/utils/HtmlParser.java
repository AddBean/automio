// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.richeditor.utils;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2017/7/4.
 */

public class HtmlParser {
    /**
     * @param s
     * @return 获得图片
     */
    private static List<String> getImg(String s)
    {
        String regex;
        List<String> list = new ArrayList<String>();
        regex = "src=\"(.*?)\"";
        Pattern pa = Pattern.compile(regex, Pattern.DOTALL);
        Matcher ma = pa.matcher(s);
        while (ma.find())
        {
            list.add(ma.group());
        }
        return list;
    }
    /**
     * 返回存有图片地址的数组
     * @param tar
     * @return
     */
    public static String[] getImgAddress(String tar){
        List<String> imgList = getImg(tar);

        String res[] = new String[imgList.size()];

        if(imgList.size()>0){
            for (int i = 0; i < imgList.size(); i++) {
                int begin = imgList.get(i).indexOf("\"")+1;
                int end = imgList.get(i).lastIndexOf("\"");
                res[i]= imgList.get(i).substring(begin,end);
            }
        }else{
        }
        return res;
    }
}
