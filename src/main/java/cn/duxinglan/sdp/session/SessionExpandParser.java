package cn.duxinglan.sdp.session;

import cn.duxinglan.media.signaling.sdp.SessionDescription;
import cn.duxinglan.sdp.session.parser.ExtMapAllowMixedExpandParser;
import cn.duxinglan.sdp.session.parser.GroupExpandParser;
import cn.duxinglan.sdp.session.parser.MSidExpandParser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
public class SessionExpandParser extends SessionLineParser {


    private static final Map<String, SessionLineParser> parsers = new ConcurrentHashMap<>();

    static {
        addParser(new GroupExpandParser());
        addParser(new ExtMapAllowMixedExpandParser());
        addParser(new MSidExpandParser());
    }

    public static void addParser(SessionLineParser sessionLineParser) {
        parsers.put(sessionLineParser.getLineStartWith(), sessionLineParser);
    }

    public static final String KEY = "a=";


    @Override
    public String getLineStartWith() {
        return KEY;
    }

    @Override
    protected boolean parse(SessionDescription sessionDescription, String key, String value) {
        //扩展需要重新进行分组处理
        int i = value.indexOf(":");
        String expandKey;
        String expandValue;
        if (i == -1) {
            expandValue = value;
            expandKey = value;
        } else {
            expandKey = value.substring(0, i).trim();
            expandValue = value.substring(i + 1).trim();
        }

        SessionLineParser sessionLineParser = parsers.get(expandKey);
        if (sessionLineParser == null) {
            return false;
        }
        return sessionLineParser.onParse(sessionDescription, expandKey, expandValue);
    }
}
