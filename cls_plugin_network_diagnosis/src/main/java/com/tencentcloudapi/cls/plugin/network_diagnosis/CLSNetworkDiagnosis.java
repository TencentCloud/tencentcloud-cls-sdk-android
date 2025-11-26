package com.tencentcloudapi.cls.plugin.network_diagnosis;

import java.util.Map;

public class CLSNetworkDiagnosis implements INetworkDiagnosis {

    private INetworkDiagnosis networkDiagnosis;

    private static class Holder {
        private static final CLSNetworkDiagnosis INSTANCE = new CLSNetworkDiagnosis();
    }

    public static CLSNetworkDiagnosis getInstance() {
        return Holder.INSTANCE;
    }

    /* package */
    void setNetworkDiagnosis(INetworkDiagnosis networkDiagnosis) {
        this.networkDiagnosis = networkDiagnosis;
    }
    private boolean checkNetworkDiagnosis() {
        return null != this.networkDiagnosis;
    }

    @Override
    public void updateExtensions(Map<String, String> extension) {
        if (checkNetworkDiagnosis()) {
            networkDiagnosis.updateExtensions(extension);
        }
    }


    @Override
    public void ping(PingRequest request) {
        if (checkNetworkDiagnosis()) {
            networkDiagnosis.ping(request);
        }
    }

    @Override
    public void ping(PingRequest request, Callback callback) {
        if (checkNetworkDiagnosis()) {
            networkDiagnosis.ping(request, callback);
        }
    }

    @Override
    public void http(HttpRequest request) {
        if (checkNetworkDiagnosis()) {
            networkDiagnosis.http(request);
        }
    }

    @Override
    public void http(HttpRequest request, Callback callback) {
        if (checkNetworkDiagnosis()) {
            networkDiagnosis.http(request, callback);
        }
    }

    @Override
    public void tcpPing(TcpPingRequest request) {
        if (checkNetworkDiagnosis()) {
            networkDiagnosis.tcpPing(request);
        }
    }

    @Override
    public void tcpPing(TcpPingRequest request, Callback callback) {
        if (checkNetworkDiagnosis()) {
            networkDiagnosis.tcpPing(request, callback);
        }
    }

    @Override
    public void dns(DnsRequest request) {
        if (checkNetworkDiagnosis()) {
            networkDiagnosis.dns(request);
        }
    }

    @Override
    public void dns(DnsRequest request, Callback callback) {
        if (checkNetworkDiagnosis()) {
            networkDiagnosis.dns(request, callback);
        }
    }

    @Override
    public void mtr(MtrRequest request) {
        if (checkNetworkDiagnosis()) {
            networkDiagnosis.mtr(request);
        }
    }

    @Override
    public void mtr(MtrRequest request, Callback callback) {
        if (checkNetworkDiagnosis()) {
            networkDiagnosis.mtr(request, callback);
        }
    }
}
