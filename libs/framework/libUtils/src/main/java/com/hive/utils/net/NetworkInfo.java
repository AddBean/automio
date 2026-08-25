// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils.net;

public class NetworkInfo {
    public final NetworkSP networkSP;
    public final NetworkType networkType;

    public NetworkInfo(NetworkSP var1, NetworkType var2) {
        this.networkSP = var1;
        this.networkType = var2;
    }

    public enum NetworkType {
        UNKNOWN(1),
        WIFI(2),
        G2(3),
        G3(4),
        G4(5);

        private int code;

        NetworkType(int var3) {
            this.code = var3;
        }

        public final int code() {
            return this.code;
        }
    }

    public enum NetworkSP {
        UNKNOWN(1),
        CHINA_MOBILE(2),
        CHINA_UNICOM(3),
        CHINA_TELECOM(4);

        private int code;

        NetworkSP(int c) {
            this.code = c;
        }

        public final int code() {
            return this.code;
        }
    }
}
