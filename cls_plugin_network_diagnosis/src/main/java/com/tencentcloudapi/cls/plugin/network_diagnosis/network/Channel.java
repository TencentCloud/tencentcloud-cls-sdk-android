package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.NetworkInfo.State;
import android.os.ParcelFileDescriptor;
import android.os.Build.VERSION;

import com.tencentcloudapi.cls.android.CLSLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketOption;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class Channel {
    private static final String TAG = "CLSChannel";

    public Channel() {
    }

    public static NetworkState getNetworkState() {
        Application app = Utils.getApplication();
        ConnectivityManagerDelegate cmd = new ConnectivityManagerDelegate(app.getApplicationContext());
        return cmd.getNetworkState();
    }

    public static NetworkState getNetworkState(Network network) {
        Application app = Utils.getApplication();
        ConnectivityManagerDelegate cmd = new ConnectivityManagerDelegate(app.getApplicationContext());
        return cmd.getNetworkState(network);
    }

    public static Network[] getAllNetworks() {
        Application app = Utils.getApplication();
        ConnectivityManagerDelegate cmd = new ConnectivityManagerDelegate(app.getApplicationContext());
        return cmd.getAllNetworks();
    }

    public static String stringConnectionType(ConnectionType type) {
        switch (type) {
            case CONNECTION_2G:
                return "2G";
            case CONNECTION_3G:
                return "3G";
            case CONNECTION_4G:
                return "4G";
            case CONNECTION_5G:
                return "5G";
            case CONNECTION_WIFI:
                return "WIFI";
            case CONNECTION_BLUETOOTH:
                return "BLUETOOTH";
            case CONNECTION_ETHERNET:
                return "ETHERNET";
            case CONNECTION_NONE:
                return "NONE";
            case CONNECTION_UNKNOWN:
                return "UNKNOWN";
            case CONNECTION_VPN:
                return "VPN";
            case CONNECTION_UNKNOWN_CELLULAR:
                return "UNKNOWN_CELLULAR";
            default:
                return "UNKNOWN";
        }
    }

    public static ConnectionType getConnectionType(boolean isConnected, int networkType, int networkSubtype) {
        if (!isConnected) {
            return Channel.ConnectionType.CONNECTION_NONE;
        } else {
            switch (networkType) {
                case 0:
                    switch (networkSubtype) {
                        case 1:
                        case 2:
                        case 4:
                        case 7:
                        case 11:
                        case 16:
                            return Channel.ConnectionType.CONNECTION_2G;
                        case 3:
                        case 5:
                        case 6:
                        case 8:
                        case 9:
                        case 10:
                        case 12:
                        case 14:
                        case 15:
                        case 17:
                            return Channel.ConnectionType.CONNECTION_3G;
                        case 13:
                        case 18:
                            return Channel.ConnectionType.CONNECTION_4G;
                        case 19:
                        default:
                            return Channel.ConnectionType.CONNECTION_UNKNOWN_CELLULAR;
                        case 20:
                            return Channel.ConnectionType.CONNECTION_5G;
                    }
                case 1:
                    return Channel.ConnectionType.CONNECTION_WIFI;
                case 2:
                case 3:
                case 4:
                case 5:
                case 8:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                default:
                    return Channel.ConnectionType.CONNECTION_UNKNOWN;
                case 6:
                    return Channel.ConnectionType.CONNECTION_4G;
                case 7:
                    return Channel.ConnectionType.CONNECTION_BLUETOOTH;
                case 9:
                    return Channel.ConnectionType.CONNECTION_ETHERNET;
                case 17:
                    return Channel.ConnectionType.CONNECTION_VPN;
            }
        }
    }

    void closeDoubleChannel() {
    }

    @SuppressLint({"NewApi"})
    public static long networkToNetId(Network network) {
        return VERSION.SDK_INT >= 23 ? network.getNetworkHandle() : (long) Integer.parseInt(network.toString());
    }

    @SuppressLint({"NewApi"})
    public static long dnsServersForNetwork(Network network) {
        return VERSION.SDK_INT >= 23 ? network.getNetworkHandle() : (long) Integer.parseInt(network.toString());
    }

    public static enum ConnectionType {
        CONNECTION_UNKNOWN,
        CONNECTION_ETHERNET,
        CONNECTION_WIFI,
        CONNECTION_5G,
        CONNECTION_4G,
        CONNECTION_3G,
        CONNECTION_2G,
        CONNECTION_UNKNOWN_CELLULAR,
        CONNECTION_BLUETOOTH,
        CONNECTION_VPN,
        CONNECTION_NONE;

        private ConnectionType() {
        }
    }

    @SuppressLint({"NewApi"})
    public static class NetworkCallbackImpl extends ConnectivityManager.NetworkCallback {
        final ConnectivityManager connectivityManager;

        public NetworkCallbackImpl(ConnectivityManager connectivityManager) {
            this.connectivityManager = connectivityManager;
        }

        public void onAvailable(Network network) {
            CLSLog.d(TAG, "4g通道,已经开启");
        }
    }

    @SuppressLint({"NewApi"})
    private static class SocketOptionImpl implements SocketOption {
        private final ConnectivityManager connectivityManager;
        private final NetworkCallbackImpl networkCallback;
        private final Socket socket;
        private final ParcelFileDescriptor parcelFileDescriptor;

        public SocketOptionImpl(ConnectivityManager connectivityManager, NetworkCallbackImpl networkCallback, Socket socket, ParcelFileDescriptor parcelFileDescriptor) {
            this.connectivityManager = connectivityManager;
            this.networkCallback = networkCallback;
            this.socket = socket;
            this.parcelFileDescriptor = parcelFileDescriptor;
        }

        public void closeSocket() {
            try {
                this.socket.close();
            } catch (IOException e) {
                CLSLog.printStackTrace(e);
            }
            this.parcelFileDescriptor.detachFd();
            try {
                this.parcelFileDescriptor.close();
            } catch (IOException e) {
                CLSLog.printStackTrace(e);
            }

        }

        public void closeDoubleChannel() {
            this.connectivityManager.unregisterNetworkCallback(this.networkCallback);
        }

        public String name() {
            return null;
        }

        public Class type() {
            return null;
        }
    }

    public static class IPAddress {
        public final byte[] address;

        public IPAddress(byte[] address) {
            this.address = address;
        }

        private byte[] getAddress() {
            return this.address;
        }
    }

    public static class NetworkInformation {
        public final String name;
        public final ConnectionType type;
        public final ConnectionType underlyingTypeForVpn;
        public final long handle;
        public final IPAddress[] ipAddresses;

        public NetworkInformation(String name, ConnectionType type, ConnectionType underlyingTypeForVpn, long handle, IPAddress[] addresses) {
            this.name = name;
            this.type = type;
            this.underlyingTypeForVpn = underlyingTypeForVpn;
            this.handle = handle;
            this.ipAddresses = addresses;
        }

        private IPAddress[] getIpAddresses() {
            return this.ipAddresses;
        }

        private ConnectionType getConnectionType() {
            return this.type;
        }

        private ConnectionType getUnderlyingConnectionTypeForVpn() {
            return this.underlyingTypeForVpn;
        }

        private long getHandle() {
            return this.handle;
        }

        private String getName() {
            return this.name;
        }
    }

    static class NetworkState {
        private final boolean connected;
        private final int type;
        private final int subtype;
        private final int underlyingNetworkTypeForVpn;
        private final int underlyingNetworkSubtypeForVpn;

        public NetworkState(boolean connected, int type, int subtype, int underlyingNetworkTypeForVpn, int underlyingNetworkSubtypeForVpn) {
            this.connected = connected;
            this.type = type;
            this.subtype = subtype;
            this.underlyingNetworkTypeForVpn = underlyingNetworkTypeForVpn;
            this.underlyingNetworkSubtypeForVpn = underlyingNetworkSubtypeForVpn;
        }

        public boolean isConnected() {
            return this.connected;
        }

        public int getNetworkType() {
            return this.type;
        }

        public int getNetworkSubType() {
            return this.subtype;
        }

        public int getUnderlyingNetworkTypeForVpn() {
            return this.underlyingNetworkTypeForVpn;
        }

        public int getUnderlyingNetworkSubtypeForVpn() {
            return this.underlyingNetworkSubtypeForVpn;
        }
    }

    static class ConnectivityManagerDelegate {
        private static ConnectivityManager connectivityManager;
        private static final long INVALID_NET_ID = -1L;
        private static Map<String, JSONObject> interfaceIPMap = new HashMap();
        private static Map<String, Integer> interfaceErrorMap = new HashMap();
        private ConnectivityManager.NetworkCallback mobileCallback;
        private ConnectivityManager.NetworkCallback callback;

        ConnectivityManagerDelegate(Context context) {
            connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        ConnectivityManagerDelegate() {
            connectivityManager = null;
        }

        @SuppressLint({"NewApi"})
        NetworkState getNetworkState() {
            if (connectivityManager == null) {
                return new NetworkState(false, -1, -1, -1, -1);
            } else {
                try {
                    Network n = this.getDefaultNetwork();
                    return n == null ? new NetworkState(false, -1, -1, -1, -1) : this.getNetworkState(connectivityManager.getNetworkInfo(n));
                } catch (Throwable e) {
                    CLSLog.e(TAG, "getNetworkState exception: " + e.getMessage() + "\n" + e.toString());
                    return new NetworkState(false, -1, -1, -1, -1);
                }
            }
        }

        NetworkState getNetworkStateBak() {
            return connectivityManager == null ? null : this.getNetworkState(connectivityManager.getActiveNetworkInfo());
        }

        @SuppressLint({"NewApi"})
        NetworkState getNetworkState(Network network) {
            if (network != null && connectivityManager != null) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                if (networkInfo == null) {
                    CLSLog.w(TAG, "Couldn't retrieve information from network " + network.toString());
                    return new NetworkState(false, -1, -1, -1, -1);
                } else if (networkInfo.getType() != 17) {
                    NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
                    return networkCapabilities != null && networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ? new NetworkState(networkInfo.isConnected(), 17, -1, networkInfo.getType(), networkInfo.getSubtype()) : this.getNetworkState(networkInfo);
                } else if (networkInfo.getType() == 17) {
                    if (VERSION.SDK_INT >= 23 && network.equals(connectivityManager.getActiveNetwork())) {
                        NetworkInfo underlyingActiveNetworkInfo = connectivityManager.getActiveNetworkInfo();
                        if (underlyingActiveNetworkInfo != null && underlyingActiveNetworkInfo.getType() != 17) {
                            return new NetworkState(networkInfo.isConnected(), 17, -1, underlyingActiveNetworkInfo.getType(), underlyingActiveNetworkInfo.getSubtype());
                        }
                    }

                    return new NetworkState(networkInfo.isConnected(), 17, -1, -1, -1);
                } else {
                    return this.getNetworkState(networkInfo);
                }
            } else {
                return new NetworkState(false, -1, -1, -1, -1);
            }
        }

        private NetworkState getNetworkState(NetworkInfo networkInfo) {
            return networkInfo != null && networkInfo.isConnected() ? new NetworkState(true, networkInfo.getType(), networkInfo.getSubtype(), -1, -1) : new NetworkState(false, -1, -1, -1, -1);
        }

        @SuppressLint({"NewApi"})
        Network[] getAllNetworks() {
            return connectivityManager == null ? new Network[0] : connectivityManager.getAllNetworks();
        }

        @SuppressLint({"NewApi"})
        Network getDefaultNetwork() {
            return connectivityManager.getActiveNetwork();
        }

        @SuppressLint({"NewApi"})
        long getDefaultNetId() {
            if (!this.supportNetworkCallback()) {
                return -1L;
            } else {
                try {
                    Network network = this.getDefaultNetwork();
                    return !this.hasInternetCapability(network) ? -1L : Channel.networkToNetId(network);
                } catch (Throwable var2) {
                    return -1L;
                }
            }
        }

        @SuppressLint({"NewApi"})
        boolean hasInternetCapability(Network network) {
            if (connectivityManager == null) {
                return false;
            } else {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            }
        }

        private static JSONObject getIpInfoNetwork(Network n) {
//            try {
////                String uri = "/geo?deviceId=" + Utils.getDeviceId() + "&appid=" + Utils.getTencentCloudAppId();
//                String urlStr = "https://cls.tencentcloudapi.com" + uri;
//                URL url = new URL(urlStr);
//                HttpURLConnection conn;
//                if (VERSION.SDK_INT >= 23) {
//                    conn = (HttpURLConnection) n.openConnection(url);
//                } else {
//                    conn = (HttpURLConnection) url.openConnection();
//                }
//
//                conn.setConnectTimeout(10000);
//                conn.setReadTimeout(20000);
//                conn.setUseCaches(false);
//                conn.connect();
//                int code = conn.getResponseCode();
//                CLSLog.i(TAG, "getIpInfoNetwork code " + code);
//                if (code == 200) {
//                    InputStream in = conn.getInputStream();
//                    byte[] buffer = new byte[10240];
//                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                    int len = 0;
//
//                    while ((len = in.read(buffer)) > 0) {
//                        baos.write(buffer, 0, len);
//                    }
//
//                    String returnValue = new String(baos.toByteArray(), StandardCharsets.UTF_8);
//                    CLSLog.d(TAG, "getIpInfoNetwork: " + returnValue);
//                    return new JSONObject(returnValue);
//                }
//            } catch (Exception e) {
//                CLSLog.e(TAG, "getIpInfoNetwork exception: " + e.getMessage() + "\n" + e.toString());
//            }
            return new JSONObject();
        }

        public static synchronized JSONObject getIpInfo(Network n, ConnectionType ctype, long netId) {
            if (n == null) {
                return null;
            } else {
                String code = getNetworkDigest(n);
                if (interfaceIPMap.containsKey(code)) {
                    JSONObject o = (JSONObject) interfaceIPMap.get(code);
                    return o;
                } else {
                    JSONObject o = null;
                    try {
                        o = getIpInfoNetwork(n);
                        JSONObject geo = o.getJSONObject("geo");
                        long ts = System.currentTimeMillis();
                        geo.put("ts", ts);
                        setIpInfo(n, geo);
                        if (Diagnosis.isCellularNetwork(ctype)) {
                            interfaceIPMap.put("cellular", geo);
                        }
                        String countryId = geo.optString("ip_country_id", "CN");
                        Utils.setCountryId(countryId);
                        return geo;
                    } catch (JSONException var12) {
                        if (Diagnosis.isCellularNetwork(ctype) && interfaceIPMap.containsKey("cellular")) {
                            JSONObject cell = (JSONObject) interfaceIPMap.get("cellular");
                            if (null != cell && cell.has("ts")) {
                                try {
                                    long ts = cell.getLong("ts");
                                    long now = System.currentTimeMillis();
                                    if (now < 600000L) {
                                        return cell;
                                    }
                                } catch (JSONException var11) {
                                }
                            }
                        }

                        return detectInterfaceIpInfo(n, ctype);
                    }
                }
            }
        }

        private static synchronized void setIpInfo(Network n, JSONObject info) {
            String code = getNetworkDigest(n);
            interfaceIPMap.put(code, info);
        }

        public static synchronized void setIpInfoWithCode(String code, JSONObject info) {
            interfaceIPMap.put(code, info);
        }

        public static synchronized JSONObject getIpInfoWithCode(String code) {
            return interfaceIPMap.containsKey(code) ? (JSONObject) interfaceIPMap.get(code) : null;
        }

        @SuppressLint({"NewApi"})
        private static JSONObject detectInterfaceIpInfo(Network n, ConnectionType ct) {
            String code = getNetworkDigest(n);
            if (interfaceErrorMap.containsKey(code) && (Integer) interfaceErrorMap.get(code) > 5) {
                return null;
            }
//            String url = "https://hhhha.com" + "/geo?deviceId=" + Utils.getDeviceId() + "&ipaAppId=" + Utils.getTencentCloudAppId();
//            String url = "";
//            try {
//                HttpURLConnection connection = (HttpURLConnection) n.openConnection(new URL(url));
//                connection.setConnectTimeout(3000);
//                connection.connect();
//                if (connection.getResponseCode() == 200) {
//                    byte[] buff = new byte[4096];
//                    InputStream is = connection.getInputStream();
//                    is.read(buff);
//                    String result = new String(buff, StandardCharsets.UTF_8);
//                    JSONObject o = new JSONObject(result);
//                    JSONObject geo = o.getJSONObject("geo");
//                    long ts = System.currentTimeMillis();
//                    geo.put("ts", ts);
//                    setIpInfo(n, geo);
//                    if (Diagnosis.isCellularNetwork(ct)) {
//                        interfaceIPMap.put("cellular", geo);
//                    }
//                    if (geo != null) {
//                        String countryId = geo.optString("ip_country_id", "CN");
//                        Utils.setCountryId(countryId);
//                    }
//                    interfaceErrorMap.remove(code);
//                    return geo;
//                }
//            } catch (Throwable e) {
//                CLSLog.e(TAG, "detectInterfaceIpInfo exception: " + e.getMessage() + "\n" + e.toString());
//            }
//
//            if (interfaceErrorMap.containsKey(code)) {
//                interfaceErrorMap.put(code, (Integer) interfaceErrorMap.get(code) + 1);
//            } else {
//                interfaceErrorMap.put(code, new Integer(1));
//            }

            return null;

        }

        private static String getNetworkDigest(Network n) {
            return n.toString();
        }

        @SuppressLint({"NetApi"})
        JSONObject composeInfo(Network network, String currentNetType, String usedType, ConnectionType ctype, long netId) {
            JSONObject info = new JSONObject();
            try {
                info.put("defaultNet", currentNetType);
                info.put("usedNet", usedType);
                JSONObject geo = getIpInfo(network, ctype, netId);
                if (geo == null) {
                    geo = getIpInfoWithCode("default");
                    info.put("get_ip_by_default", true);
                }
                if (geo != null) {
                    info.put("client_ip", geo.getString("remote_addr"));
                    info.put("country_id", geo.getString("ip_country_id"));
                    info.put("isp_en", geo.getString("ip_isp_en"));
                    info.put("city_en", geo.getString("ip_city_en"));
                    info.put("province_en", geo.getString("ip_region_en"));
                    info.put("country_en", geo.getString("ip_country_en"));
                }
            } catch (Throwable e) {
                CLSLog.e(TAG, "composeInfo exception: " + e.getMessage() + "\n" + e.toString());
            }

            return info;
        }

        @SuppressLint({"NetApi"})
        JSONObject getNetInfo(Network network, long netId) {
            JSONObject info = new JSONObject();
            try {
                NetworkState usedState = this.getNetworkState(network);
                String usedType = Channel.stringConnectionType(Channel.getConnectionType(usedState.isConnected(), usedState.getNetworkType(), usedState.getNetworkSubType()));
                ConnectionType ctype = Channel.getConnectionType(usedState.isConnected(), usedState.getNetworkType(), usedState.getNetworkSubType());
                String currentNetType = Channel.stringConnectionType(ctype);
                info = this.composeInfo(network, currentNetType, usedType, ctype, netId);
            } catch (Throwable e) {
                CLSLog.e(TAG, "getNetInfo exception: " + e.getMessage() + "\n" + e.toString());
            }

            return info;
        }

        JSONObject getNetInfoBak() {
            JSONObject info = new JSONObject();
            Network network = null;
            if (VERSION.SDK_INT >= 21) {
                try {
                    Network[] networks = new Network[0];
                    networks = connectivityManager.getAllNetworks();
                    NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                    if (activeNetworkInfo != null) {
                        for (Network nw : networks) {
                            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(nw);
                            if (networkInfo != null && networkInfo.isConnected() && networkInfo.getState() == State.CONNECTED) {
                                String activeString = activeNetworkInfo.toString();
                                String tmpString = networkInfo.toString();
                                if (tmpString.equals(activeString)) {
                                    network = nw;
                                    break;
                                }
                            }
                        }
                    }
                } catch (Throwable e) {
                    CLSLog.e(TAG, "Low Api getNetInfo exception: " + e.getMessage() + "\n" + e.toString());
                }
                try {
                    NetworkState usedState = this.getNetworkState(network);
                    ConnectionType defaultType = Channel.getConnectionType(usedState.isConnected(), usedState.getNetworkType(), usedState.getNetworkSubType());
                    String defaultTypeStr = Channel.stringConnectionType(defaultType);
                    info = this.composeInfo(network, defaultTypeStr, defaultTypeStr, defaultType, -1L);
                } catch (Throwable e) {
                    CLSLog.e(TAG, "Low Api getNetInfo exception: " + e.getMessage() + "\n" + e.toString());
                }
            }
            return info;
        }

        @SuppressLint({"NewApi"})
        private String getDnsServers(Network network) {
            if (connectivityManager != null && network != null) {
                LinkProperties lp = connectivityManager.getLinkProperties(network);
                if (lp == null) {
                    return null;
                } else {
                    String dnsServers = "";
                    for (InetAddress addr : lp.getDnsServers()) {
                        if (dnsServers.equalsIgnoreCase("")) {
                            dnsServers = dnsServers + addr.getHostAddress();
                        } else {
                            dnsServers = dnsServers + "," + addr.getHostAddress();
                        }
                    }
                    return dnsServers;
                }
            } else {
                return null;
            }
        }

        @SuppressLint({"NewApi"})
        public void registerNetworkCallback(ConnectivityManager.NetworkCallback networkCallback) {
            connectivityManager.registerNetworkCallback((new NetworkRequest.Builder()).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), networkCallback);
        }

        @SuppressLint({"NewApi"})
        public void requestMobileNetwork(ConnectivityManager.NetworkCallback networkCallback) {
            try {
                if (!this.supportNetworkCallback()) {
                    return;
                }
                if (!Utils.canWriteSetting()) {
                    CLSLog.d(TAG, "can write setting false!!");
                }
                NetworkRequest.Builder builder = new NetworkRequest.Builder();
                builder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
                this.mobileCallback = networkCallback;
                CLSLog.d(TAG, "###########requestNetwork with callback " + networkCallback + ", cmd " + this);
                connectivityManager.requestNetwork(builder.build(), networkCallback);
            } catch (Throwable e) {
                CLSLog.e(TAG, "requestMobileNetwork exception: " + e.getMessage() + "\n" + e.toString());
            }

        }

        @SuppressLint({"NewApi"})
        IPAddress[] getIPAddresses(LinkProperties linkProperties) {
            IPAddress[] ipAddresses = new IPAddress[linkProperties.getLinkAddresses().size()];
            int i = 0;

            for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
                ipAddresses[i] = new IPAddress(linkAddress.getAddress().getAddress());
                ++i;
            }

            return ipAddresses;
        }

        @SuppressLint({"NewApi"})
        public void releaseCallback(ConnectivityManager.NetworkCallback networkCallback) {
            try {
                if (this.supportNetworkCallback()) {
                    CLSLog.d(TAG, "Unregister network callback");
                    connectivityManager.unregisterNetworkCallback(networkCallback);
                }
            } catch (Throwable e) {
                CLSLog.e(TAG, "releaseCallback exception: " + e.getMessage());
            }

        }

        @SuppressLint({"NewApi"})
        public void releaseCallback() {
            try {
                if (this.supportNetworkCallback() && this.mobileCallback != null) {
                    CLSLog.d(TAG, "###########unregisterNetworkCallback with " + this.mobileCallback + ", cmd " + this);
                    connectivityManager.unregisterNetworkCallback(this.mobileCallback);
                }
            } catch (Throwable e) {
                CLSLog.e(TAG, "releaseCallback exception: " + e.getMessage());
            }
        }

        public boolean supportNetworkCallback() {
            return VERSION.SDK_INT >= 21 && connectivityManager != null;
        }
    }
}
