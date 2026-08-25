// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.net.upload;


import android.os.AsyncTask;
import android.text.TextUtils;

import com.hive.net.interceptor.BaseParamsMap;
import com.hive.utils.debug.DLog;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Admin on 2016/6/25.
 */

public class HttpFilePostRequester {
    public static HttpFilePostRequester httpFilePostRequester;
    private int readTimeOut = 10 * 1000; // 读取超时
    private int connectTimeout = 10 * 1000; // 超时时间
    private int requestTime = 0;
    private final String CHARSET = "utf-8"; // 设置编码
    private final String BOUNDARY = UUID.randomUUID().toString(); // 边界标识 随机生成
    private final String PREFIX = "--";
    private final String LINE_END = "\r\n";
    private final String CONTENT_TYPE = "multipart/form-data"; // 内容类型
    private final int SUCCESS = 0;
    private final int FAILED = 1;

    public static HttpFilePostRequester getInstance() {
        if (null == httpFilePostRequester) {
            httpFilePostRequester = new HttpFilePostRequester();
        }
        return httpFilePostRequester;
    }

    public void post(final String RequestURL, final String cookie, final Map<String, String> param, final List<FormFile> files, final IUploadListener mIUploadListener) {
        new UploadTask() {
            @Override
            protected ResultModel doInBackground(Void... params) {
                return toUploadFiles(this, RequestURL, cookie, param, files);
            }

            @Override
            protected void onProgressUpdate(ProgressModel... values) {
                super.onProgressUpdate(values);
                ProgressModel model = values[0];
                mIUploadListener.onSingleUploadProgress(model.getmIndex(), model.getmFileName(), model.getmSingleCurLen(), model.getmSingleLen());
                mIUploadListener.onAllUploadProgress(model.getmIndex(), model.getmFileName(), model.getmTotalCurLen(), model.getmTotalLen());
                if (model.ismUploaded())
                    mIUploadListener.onSingleUploadSuccess(model.getmIndex(), model.getmFileName());
            }

            @Override
            protected void onPostExecute(ResultModel m) {
                super.onPostExecute(m);
                if (m.getCode() == SUCCESS) {
                    mIUploadListener.onAllUploadSuccess(m.getContent());
                } else if (m.getCode() == FAILED) {
                    mIUploadListener.onAllUploadFailed(m.getContent());
                }
            }
        }.execute();
    }

    private ResultModel toUploadFiles(UploadTask task, String RequestURL, String cookie, Map<String, String> param, List<FormFile> files) {
        String result = null;
        requestTime = 0;
        long requestTime = System.currentTimeMillis();
        long responseTime = 0;
        try {
            URL url = new URL(RequestURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(readTimeOut);
            conn.setConnectTimeout(connectTimeout);
            conn.setDoInput(true); // 允许输入流
            conn.setDoOutput(true); // 允许输出流
            conn.setUseCaches(false); // 不允许使用缓存
            conn.setRequestMethod("POST"); // 请求方式
            conn.setRequestProperty("Charset", CHARSET); // 设置编码
            conn.setRequestProperty("connection", "keep-alive");
            if (!TextUtils.isEmpty(cookie))
                conn.setRequestProperty("Cookie", cookie);
            if (BaseParamsMap.get() != null) {
                for (String key : BaseParamsMap.get().keySet()) {
                    String value = BaseParamsMap.get(key);
                    if (!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                        conn.setRequestProperty(key, value);
                    }
                }
            }
            conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1)");
            conn.setRequestProperty("Content-Type", CONTENT_TYPE + ";boundary=" + BOUNDARY);
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            StringBuffer sb = null;
            String params = "";
            if (param != null && param.size() > 0) {
                Iterator<String> it = param.keySet().iterator();
                while (it.hasNext()) {
                    sb = new StringBuffer();
                    String key = it.next();
                    String value = param.get(key);
                    sb.append(PREFIX).append(BOUNDARY).append(LINE_END);
                    sb.append("Content-Disposition: form-data; name=\"").append(key).append("\"").append(LINE_END).append(LINE_END);
                    sb.append(value).append(LINE_END);
                    params = sb.toString();
                    dos.write(params.getBytes());
                }
            }
            /**
             * 这里重点注意： name里面的值为服务器端需要key 只有这个key 才可以得到对应的文件
             * filename是文件的名字，包含后缀名的 比如:abc.png
             */
            long totalLen = 0;//所有文件大小；
            for (FormFile file : files) {
                totalLen = totalLen + file.getFile().length();
            }
            long totalCurLen = 0;
            for (int i = 0; i < files.size(); i++) {
                FormFile file = files.get(i);
                sb = new StringBuffer();
                sb.append(PREFIX).append(BOUNDARY).append(LINE_END);
                sb.append("Content-Disposition: form-data; name=\"" + file.getParameterName() + "\"; filename=\"" + file.getFilname() + "\"" + LINE_END);
                sb.append("Content-Type:" + file.getContentType() + LINE_END);
                sb.append(LINE_END);
                params = sb.toString();
                dos.write(params.getBytes(CHARSET));
                InputStream is = new FileInputStream(file.getFile());
                byte[] bytes = new byte[1024];
                int len = 0;
                long curLen = 0;
                while ((len = is.read(bytes)) != -1) {
                    curLen += len;
                    totalCurLen += len;
                    dos.write(bytes, 0, len);
                    task.upadata(new ProgressModel(i, file.getFilname(), curLen, file.getFile().length(), totalCurLen, totalLen, false));
                }
                is.close();
                dos.write(LINE_END.getBytes());
                task.upadata(new ProgressModel(i, file.getFilname(), curLen, file.getFile().length(), totalCurLen, totalLen, true));
            }
            byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINE_END).getBytes();
            dos.write(end_data);
            dos.flush();
            int res = conn.getResponseCode();
            responseTime = System.currentTimeMillis();
            DLog.e("response code:" + res);
            DLog.e("response time:" + (responseTime - requestTime));
            if (res == 201 || res == 200) {
                DLog.e("request success");
                result = readStream(conn.getInputStream());
                DLog.e("result : " + result);
                return new ResultModel(SUCCESS, result);
            } else {
                result = readStream(conn.getErrorStream());
                DLog.e("request error, code=" + res + ", result=" + result);
                return new ResultModel(FAILED, !TextUtils.isEmpty(result) ? result : ("http code=" + res));
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return new ResultModel(FAILED, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            return new ResultModel(FAILED, e.getMessage());
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        if (inputStream == null) return null;
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, CHARSET));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        reader.close();
        return builder.toString();
    }

    public class UploadTask extends AsyncTask<Void, ProgressModel, ResultModel> {

        @Override
        protected ResultModel doInBackground(Void... params) {
            return null;
        }

        public void upadata(ProgressModel... values) {
            this.publishProgress(values);
        }
    }

    public class ResultModel {
        private int code;
        private String content;

        public ResultModel(int code, String content) {
            this.code = code;
            this.content = content;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    public class ProgressModel {
        private int mIndex = 0;
        private String mFileName;
        private long mSingleCurLen;
        private long mTotalCurLen;
        private long mSingleLen;
        private long mTotalLen;
        private boolean mUploaded = false;

        public ProgressModel(int mIndex, String mFileName, long mSingleCurLen, long mSingleLen, long mTotalCurLen, long mTotalLen, boolean mUploaded) {
            this.mIndex = mIndex;
            this.mFileName = mFileName;
            this.mSingleCurLen = mSingleCurLen;
            this.mTotalCurLen = mTotalCurLen;
            this.mSingleLen = mSingleLen;
            this.mTotalLen = mTotalLen;
            this.mUploaded = mUploaded;
        }

        public boolean ismUploaded() {
            return mUploaded;
        }

        public void setmUploaded(boolean mUploaded) {
            this.mUploaded = mUploaded;
        }

        public int getmIndex() {
            return mIndex;
        }

        public void setmIndex(int mIndex) {
            this.mIndex = mIndex;
        }

        public String getmFileName() {
            return mFileName;
        }

        public void setmFileName(String mFileName) {
            this.mFileName = mFileName;
        }

        public long getmSingleCurLen() {
            return mSingleCurLen;
        }

        public void setmSingleCurLen(long mSingleCurLen) {
            this.mSingleCurLen = mSingleCurLen;
        }

        public long getmTotalCurLen() {
            return mTotalCurLen;
        }

        public void setmTotalCurLen(long mTotalCurLen) {
            this.mTotalCurLen = mTotalCurLen;
        }

        public long getmSingleLen() {
            return mSingleLen;
        }

        public void setmSingleLen(long mSingleLen) {
            this.mSingleLen = mSingleLen;
        }

        public long getmTotalLen() {
            return mTotalLen;
        }

        public void setmTotalLen(long mTotalLen) {
            this.mTotalLen = mTotalLen;
        }
    }
}
