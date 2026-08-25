// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net;


import org.reactivestreams.Publisher;

import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

/**
 * rx 辅助方法包装
 */
public class RxTransformer {


    /**
     * 线程调度  io - main
     */
    public static final FlowableTransformer io_main_flow = new FlowableTransformer() {
        @Override
        public Publisher apply(Flowable upstream) {
            return upstream.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        }
    };


    /**
     * 线程调度  io - main
     */
    public static final FlowableTransformer io_main_base_flow = new FlowableTransformer() {
        @Override
        public Publisher apply(Flowable upstream) {
            return upstream.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).map(new Function() {
                @Override
                public Object apply(Object o) throws Exception {

                    return null;
                }
            });
        }
    };

    /**
     * 统一变换
     *
     * @param <T>
     * @return
     */
    public static <T> FlowableTransformer<BaseResult<T>, T> commonTrans() {
        return new FlowableTransformer<BaseResult<T>, T>() {
            public Publisher<T> apply(Flowable<BaseResult<T>> upstream) {
                return upstream.map(new Function<BaseResult<T>, T>() {
                    public T apply(@NonNull BaseResult<T> tBaseResult) throws Exception {
                        ////fixme 此处不能返回null
                        return tBaseResult.getData();
                    }
                }).compose(io_main_flow);
            }
        };
    }

    public static  FlowableTransformer<ResponseBody, java.lang.String> stringTrans() {
        return new FlowableTransformer<ResponseBody, java.lang.String>() {
            public Publisher<java.lang.String> apply(Flowable<ResponseBody> upstream) {
                return upstream.map(new Function<ResponseBody, java.lang.String>() {
                    public java.lang.String apply(@NonNull ResponseBody tBaseResult) throws Exception {
                        return tBaseResult.string();
                    }
                }).compose(io_main_flow);
            }
        };
    }

    // observable ====================================================================================


    /**
     * 线程调度  io - main
     */
    public static final ObservableTransformer io_main = new ObservableTransformer() {
        @Override
        public ObservableSource apply(Observable upstream) {
            return upstream.subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread());
        }
    };

}
