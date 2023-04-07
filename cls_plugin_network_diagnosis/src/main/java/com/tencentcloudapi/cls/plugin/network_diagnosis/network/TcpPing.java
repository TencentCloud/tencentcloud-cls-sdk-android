package com.tencentcloudapi.cls.plugin.network_diagnosis.network;


import com.tencentcloudapi.cls.plugin.network_diagnosis.CLSNetDiagnosis;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public final class TcpPing implements Task {
    public static final int TimeOut = -3;
    public static final int NotReach = -2;
    public static final int UnkownHost = -4;
    public static final int Stopped = -1;
    public static final String Method = "TCPPing";
    private final String host;
    private final int port;
    private final int count;
    private final int timeout;
    private final CLSNetDiagnosis.Callback complete;
    private boolean stopped;
    private CLSNetDiagnosis.Output output;

    /**
     * TCP Ping Method
     * @param host 域名
     * @param port 端口号
     * @param count 次数
     * @param timeout 超时时间
     * @param output output func
     * @param complete callback func
     */
    private TcpPing(String host, int port, int count, int timeout, CLSNetDiagnosis.Output output, CLSNetDiagnosis.Callback complete) {
        this.host = host;
        this.port = port;
        this.count = count;
        this.complete = complete;
        this.output = output;
        this.timeout = timeout;
    }

    public static Task start(String host, CLSNetDiagnosis.Output output, CLSNetDiagnosis.Callback complete) {
        return start(host, 80, 10, 20 * 1000, output, complete);
    }

    public static Task start(String host, int port, int count, int timeout, CLSNetDiagnosis.Output output, CLSNetDiagnosis.Callback complete) {
        final TcpPing t = new TcpPing(host, port, count, timeout, output, complete);
        Utils.runInBack(new Runnable() {
            @Override
            public void run() {
                t.run();
            }
        });
        return t;
    }

    private void run() {
        InetAddress[] addrs = null;
        try {
            addrs = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            if (null != output) {
                output.write("Unknown host: " + host);
            }
            if (null!= complete) {
                Result res= new Result(Method, UnkownHost, host, 0, "", 0,0, 0, 0, 0, 0, 0);
                complete.onComplete(res.encode());
            }
            return;
        }

        final String ip = addrs[0].getHostAddress();
        InetSocketAddress server = new InetSocketAddress(ip, port);
        output.write("connect to " + ip + ":" + port);
        float[] times = new float[count];
        int index = -1;
        int dropped = 0;
        for (int i = 0; i < count && !stopped; i++) {
            long start = System.nanoTime();
            try {
                connect(server, timeout);
            } catch (IOException e) {
                e.printStackTrace();
                output.write(e.getMessage());
                int code = NotReach;
                if (e instanceof SocketTimeoutException) {
                    code = TimeOut;
                }
                if (i == 0) {
                    Result res= new Result(Method, code, host, port, ip, 0,0, 0, 0, 0, 1, 1);
                    complete.onComplete(res.encode());
                    return;
                } else {
                    dropped++;
                }
            }
            long end = System.nanoTime();

            float t = (float) (end-start)/1000000;
            times[i] = t;
            index = i;
            try {
                if (!stopped && 100 > t && t > 0) {
                    Thread.sleep(100 - (int)t);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (index == -1) {
            Result res = new Result(Method, Stopped, host, port, ip, 0, 0, 0, 0, 0, 0, 0);
            complete.onComplete(res.encode());
            return;
        }
        complete.onComplete(buildResult(times, index, ip, dropped).encode());
    }

    private Result buildResult(float[] times, int index, String ip, int dropped) {
        float sum = 0;
        float min = 1000000;
        float max = 0;
        for (int i = 0; i <= index; i++) {
            float t = times[i];
            if (t > max) {
                max = t;
            }
            if (t < min) {
                min = t;
            }
            sum += t;
        }
        return new Result(Method,0,host, port, ip, sum, max, min, sum / (index + 1), 0, index + 1, dropped);
    }


    private void connect(InetSocketAddress socketAddress, int timeOut) throws IOException {
        Socket socket = null;
        try {
            socket = new Socket();
            socket.connect(socketAddress, timeOut);
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void stop() {
        stopped = true;
    }

    public static final class Result {
        public final int code;
        public final String host;
        public final int port;
        public final String method;

        public final String ip;
        public final float maxTime;
        public final float minTime;
        public final float avgTime;
        public final float sumTime;
        public final int stddevTime;
        public final int count;
        public final int dropped;

        public Result(String method,
                      int code,
                      String host,
                      int port,
                      String ip,
                      float sumTime,
                      float maxTime,
                      float minTime,
                      float avgTime,
                      int stddevTime,
                      int count,
                      int dropped
        ) {
            this.method = method;
            this.host = host;
            this.port = port;
            this.code = code;
            this.ip = ip;
            this.maxTime = maxTime;
            this.minTime = minTime;
            this.avgTime = avgTime;
            this.sumTime = sumTime;
            this.stddevTime = stddevTime;
            this.count = count;
            this.dropped = dropped;
        }

        public String encode() {
            JSONObject o = new JSONObject();
            try {
                o.put("method", this.method);
                o.put("ip", this.ip);
                o.put("host", this.host);
                o.put("port", this.port);
                o.put("max", String.format("%.2f", this.maxTime));
                o.put("min", String.format("%.2f", this.minTime));
                o.put("avg", String.format("%.2f", this.avgTime));
                o.put("stddev", this.stddevTime);
                o.putOpt("sum", String.format("%.2f", this.sumTime));

                if (0 == this.count) {
                    o.put("loss", "1");
                } else {
                    o.put("loss",  String.format("%.2f", Float.valueOf(this.dropped) / Float.valueOf(this.count)));
                }
                o.put("count", this.count);
                o.put("timestamp",  System.currentTimeMillis() / 1000);
                o.put("responseNum", this.count-this.dropped);
                return o.toString();
            } catch (JSONException err) {
                err.printStackTrace();
                return null;
            }
        }

    }

}
