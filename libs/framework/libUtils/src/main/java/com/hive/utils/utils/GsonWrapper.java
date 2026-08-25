// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.utils;


import android.text.TextUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class GsonWrapper {
    private static final TypeAdapter<Boolean> booleanAsIntAdapter = new TypeAdapter<Boolean>() {
        public void write(JsonWriter out, Boolean value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value);
            }

        }

        public Boolean read(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            switch(peek) {
                case BOOLEAN:
                    return in.nextBoolean();
                case NULL:
                    in.nextNull();
                    return false;
                case NUMBER:
                    return in.nextInt() != 0;
                case STRING:
                    String val = in.nextString();
                    if (TextUtils.equals("1", val)) {
                        return true;
                    } else {
                        if (TextUtils.equals("0", val)) {
                            return false;
                        }

                        return Boolean.parseBoolean(val);
                    }
                default:
                    throw new IllegalStateException("Expected BOOLEAN or NUMBER but was " + peek);
            }
        }
    };
    private static final TypeAdapter<Integer> IntAdapter = new TypeAdapter<Integer>() {
        public void write(JsonWriter out, Integer value) throws IOException {
            if (value == null) {
                out.value(0L);
            } else {
                out.value(value);
            }

        }

        public Integer read(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            switch(peek) {
                case NULL:
                    in.nextNull();
                    return 0;
                case NUMBER:
                    return in.nextInt();
                case STRING:
                    int value = 0;
                    String val = in.nextString();

                    try {
                        value = Integer.parseInt(val);
                    } catch (NumberFormatException var6) {
                        ;
                    }

                    return value;
                default:
                    throw new IllegalStateException("Expected Integer but was " + peek);
            }
        }
    };
    private static final TypeAdapter<Long> LongAdapter = new TypeAdapter<Long>() {
        public void write(JsonWriter out, Long value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value);
            }

        }

        public Long read(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            switch(peek) {
                case NULL:
                    in.nextNull();
                    return null;
                case NUMBER:
                    return in.nextLong();
                case STRING:
                    long value = 0L;
                    String val = in.nextString();

                    try {
                        value = Long.parseLong(val);
                    } catch (NumberFormatException var7) {
                        ;
                    }

                    return value;
                default:
                    throw new IllegalStateException("Expected Long but was " + peek);
            }
        }
    };
    private static final TypeAdapter<java.time.OffsetDateTime> offsetDateTimeAdapter = new TypeAdapter<java.time.OffsetDateTime>() {
        @Override
        public void write(JsonWriter out, java.time.OffsetDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value));
            }
        }

        @Override
        public java.time.OffsetDateTime read(JsonReader in) throws IOException {
            JsonToken peek = in.peek();
            if (peek == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            if (peek == JsonToken.NUMBER) {
                return OffsetDateTime.ofInstant(Instant.ofEpochMilli(in.nextLong()), java.time.ZoneOffset.UTC);
            }
            return OffsetDateTime.parse(in.nextString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    };
    private static Gson mGson;

    public GsonWrapper() {
    }

    public static Gson buildGson() {
        if (null == mGson) {
            Class var0 = GsonWrapper.class;
            synchronized(GsonWrapper.class) {
                if (null == mGson) {
                    mGson = create();
                }
            }
        }

        return mGson;
    }

    private static Gson create() {
        Gson gson = (new GsonBuilder())
                .registerTypeAdapter(Boolean.TYPE, booleanAsIntAdapter)
                .registerTypeAdapter(Boolean.class, booleanAsIntAdapter)
                .registerTypeAdapter(Integer.TYPE, IntAdapter)
                .registerTypeAdapter(Integer.class, IntAdapter)
                .registerTypeAdapter(Long.TYPE, LongAdapter)
                .registerTypeAdapter(Long.class, LongAdapter)
                .registerTypeAdapter(OffsetDateTime.class, offsetDateTimeAdapter)
                .create();
        return gson;
    }
}