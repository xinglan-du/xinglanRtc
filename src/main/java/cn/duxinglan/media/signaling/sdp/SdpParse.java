package cn.duxinglan.media.signaling.sdp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * 版权所有 (c) 2025 www.duxinglan.cn
 * <p>
 * 项目名称：xinglanRtc
 * <p>
 * 本文件属于 xinglanRtc 项目的一部分。
 * <p>
 * 本软件依据 XinglanRtc 非商业许可证（XNCL）授权，仅限个人非商业使用。
 * 禁止任何形式的商业用途，包括但不限于：收费安装、收费部署、
 * 收费运维、收费技术支持等行为。
 * <p>
 * 详情请参阅项目根目录下的 LICENSE 文件。
 **/
public class SdpParse {


    public static final String EXPAND_KEY = "a";

    public static final String EXPAND_DELIMITER = ":";

    public static final String MID_KEY = "mid";
    public static final String RTCP_MUX = "rtcp-mux";


    /**
     * 将文本的sdp内容转换为map类型，key是'='前的内容，value是'='后的内容
     *
     * @param sdpStr sdp的文本字符串
     * @return sdp的map数据
     */
    public static Map<String, List<String>> sdpStrToSdpMap(String sdpStr) {
        Map<String, List<String>> sdpMap = new HashMap<>();
        String[] split = sdpStr.split("\n");
        for (String str : split) {
            int i = str.indexOf("=");
            String key = str.substring(0, i);
            String value = str.substring(i + 1);
            sdpMap.compute(key, (string, strings) -> {
                if (strings == null) {
                    strings = new ArrayList<>();
                }
                strings.add(value);
                return strings;
            });
        }

        return sdpMap;
    }

    public static Map<String, List<String>> aExtMapToMap(List<String> aStrList, String delimiter) {
        Map<String, List<String>> aMap = new HashMap<>();
        for (String aStr : aStrList) {
            String key;
            String value;
            int i = aStr.indexOf(delimiter);
            if (i == -1) {
                key = aStr;
                value = null;
            } else {
                key = aStr.substring(0, i);
                value = aStr.substring(i + 1).trim();
            }
            aMap.compute(key, (string, strings) -> {
                if (strings == null) {
                    strings = new ArrayList<>();
                }
                strings.add(value);
                return strings;
            });
        }
        return aMap;
    }
}
