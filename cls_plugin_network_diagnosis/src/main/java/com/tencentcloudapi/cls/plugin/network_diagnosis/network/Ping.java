package com.tencentcloudapi.cls.plugin.network_diagnosis.network;

import com.tencentcloudapi.cls.plugin.network_diagnosis.CLSNetDiagnosis;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;


/**
 * @author farmerx
 */
public final class Ping implements Task {
    private final String address;
    private final int count;
    private final int size;
    private final CLSNetDiagnosis.Output output;
    private final CLSNetDiagnosis.Callback complete;
    private volatile boolean stopped;
    private int interval;


    private Ping(String address, int count, int size, CLSNetDiagnosis.Output output, CLSNetDiagnosis.Callback complete) {
        this(address, count, size, 200, output, complete);
    }

    /**
     * Ping CMD
     * @param address host
     * @param count ping 次数
     * @param size 发包字节数
     * @param interval 步调
     * @param output output callback func
     * @param complete result callback func
     */
    private Ping(String address, int count, int size, int interval, CLSNetDiagnosis.Output output, CLSNetDiagnosis.Callback complete) {
        this.address = address;
        this.count = count;
        this.size = size;
        this.interval = interval;
        this.output = output;
        this.complete = complete;
        this.stopped = false;
    }

    /**
     * @param address host
     * @param output 输出 callback
     * @param complete 回掉 callback
     * @return
     */
    public static Task start(String address, CLSNetDiagnosis.Output output, CLSNetDiagnosis.Callback complete) {
        return start(address, 10, 56, output, complete);
    }

    /**
     * Ping Cmd Task Start
     * @param address host
     * @param count ping 次数
     * @param size 发包字节数
     * @param output output callback func
     * @param complete result callback func
     * @return
     */
    public static Task start(String address, int count, int size, CLSNetDiagnosis.Output output, CLSNetDiagnosis.Callback complete) {
        final Ping p = new Ping(address, count, size, output, complete);
        Utils.runInBack(new Runnable() {
            @Override
            public void run() {
                p.run();
            }
        });
        return p;
    }

    private void run() {
        complete.onComplete(pingCmd().encode());
    }

    private static String getIp(String host) throws UnknownHostException {
        InetAddress i = InetAddress.getByName(host);
        return i.getHostAddress();
    }

    /**
     * exec ping cmd
     * @return
     */
    private Ping.Result pingCmd() {
        String ip;
        try {
            // 超时应该在3钞以上
            int timeOut = 3000;
            // 当返回值是true时，说明host是可用的，false则不可
            Boolean status = InetAddress.getByName(address).isReachable(timeOut);
            if (null == status && false == status) {
                return new Result("", address, "", 0, 0);
            }
            ip = getIp(address);
        } catch (UnknownHostException e) {
            if (null != output) {
                output.write(e.getMessage());
            } else {
                e.printStackTrace();
            }
            return new Result("", address, "", 0, 0);
        } catch (IOException e) {
            if (null != output) {
                output.write(e.getMessage());
            } else {
                e.printStackTrace();
            }
            return new Result("", address, "", 0, 0);
        } catch (Exception e) {
            if (null != output) {
                output.write(e.getMessage());
            } else {
                e.printStackTrace();
            }
            return new Result("", address, "", 0, 0);
        }

        String cmdPing = "ping";
        if (null!=ip && ip.contains(":")) {
            cmdPing = "ping6";
        }
        String cmd = String.format(Locale.getDefault(), "%s -n -i %f -s %d -c %d %s", cmdPing, ((double) interval / 1000), size, count, ip);
        Process process = null;
        StringBuilder str = new StringBuilder();
        BufferedReader reader = null;
        BufferedReader errorReader = null;
        try {
            process = Runtime.getRuntime().exec(cmd);
            if (null == process) {
                return new Result(str.toString(), address, ip, size, interval);
            }
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = reader.readLine()) != null) {
                str.append(line).append("\n");
                if (null != output) {
                    output.write(line);
                }

            }
            while ((line = errorReader.readLine()) != null) {
                str.append(line);
                if (null != output) {
                    output.write(line);
                }
            }
            reader.close();
            errorReader.close();
            process.waitFor();

        } catch (IOException e) {
            if (null != output) {
                output.write(e.getMessage());
            } else {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            if (null != output) {
                output.write(e.getMessage());
            } else {
                e.printStackTrace();
            }
        } catch (Exception e) {
            if (null != output) {
                output.write(e.getMessage());
            } else {
                e.printStackTrace();
            }
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (process != null) {
                    process.destroy();
                }
            } catch (Exception e) {
                if (null != output) {
                    output.write(e.getMessage());
                } else {
                    e.printStackTrace();
                }
            }
        }
        return new Result(str.toString(), address, ip, size, interval);
    }

    @Override
    public void stop() {
        stopped = true;
    }

    public interface Callback {
        void complete(Result r);
    }

    public static class Result {
        public final String result;
        public final String host;
        public final String ip;
        public final int size;
        public final int interval;
        private final String lastLinePrefix = "rtt min/avg/max/mdev = ";
        private final String packetWords = " packets transmitted";
        private final String receivedWords = " received";
        public int sent;
        public int dropped;
        public float max;
        public float min;
        public float avg;
        public float stddev;
        public int count;

        Result(String result, String host, String ip, int size, int interval) {
            this.result = result;
            this.ip = ip;
            this.size = size;
            this.interval = interval;
            this.host = host;
            parseResult();
        }

        static String trimNoneDigital(String s) {
            if (s == null || s.length() == 0) {
                return "";
            }
            char[] v = s.toCharArray();
            char[] v2 = new char[v.length];
            int j = 0;
            for (char aV : v) {
                if ((aV >= '0' && aV <= '9') || aV == '.') {
                    v2[j++] = aV;
                }
            }
            return new String(v2, 0, j);
        }

        private void parseRttLine(String s) {
            String s2 = s.substring(lastLinePrefix.length(), s.length() - 3);
            String[] l = s2.split("/");
            if (l.length != 4) {
                return;
            }
            min = Float.parseFloat(trimNoneDigital(l[0]));
            avg = Float.parseFloat(trimNoneDigital(l[1]));
            max = Float.parseFloat(trimNoneDigital(l[2]));
            stddev = Float.parseFloat(trimNoneDigital(l[3]));
        }

        private void parsePacketLine(String s) {
            String[] l = s.split(",");
            if (l.length != 4) {
                return;
            }
            if (l[0].length() > packetWords.length()) {
                String s2 = l[0].substring(0, l[0].length() - packetWords.length());
                count = Integer.parseInt(s2);
            }
            if (l[1].length() > receivedWords.length()) {
                String s3 = l[1].substring(0, l[1].length() - receivedWords.length());
                sent = Integer.parseInt(s3.trim());
            }
            dropped = count - sent;
        }

        private void parseResult() {
            String[] rs = result.split("\n");
            try {
                for (String s : rs) {
                    if (s.contains(packetWords)) {
                        parsePacketLine(s);
                    } else if (s.contains(lastLinePrefix)) {
                        parseRttLine(s);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public String encode() {
            JSONObject o = new JSONObject();
            try {
                o.put("method", "ping");
                o.put("ip", this.ip);
                o.put("host", this.host);
                o.put("max", String.format("%.2f", this.max));
                o.put("min", String.format("%.2f", this.min));
                o.put("avg", String.format("%.2f", this.avg));
                o.put("stddev", String.format("%.2f", this.stddev));
                if (0 == this.count) {
                    o.put("loss", "1");
                } else {
                    o.put("loss", String.format("%.2f", Float.valueOf(this.dropped) / Float.valueOf(this.count)));
                }
                o.put("count", this.count);
                o.put("size", this.size);
                o.put("responseNum", this.sent);
                o.put("interval", this.interval);
                o.put("timestamp",  System.currentTimeMillis() / 1000);
                return o.toString();
            } catch (JSONException err) {
                err.printStackTrace();
                return null;
            }
        }
    }

}
