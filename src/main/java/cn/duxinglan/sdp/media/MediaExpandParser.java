package cn.duxinglan.sdp.media;

import cn.duxinglan.media.signaling.sdp.MediaDescription;
import cn.duxinglan.sdp.media.parser.*;

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
public class MediaExpandParser extends MediaLineParser {


    private static final Map<String, MediaLineParser> parsers = new ConcurrentHashMap<>();

    static {
        addParser(new RtcpConnectionLineParser());
        addParser(new IceInfoConnectionLineParser());
        addParser(new FingerprintLineParser());
        addParser(new SetupLineParser());
        addParser(new MIdLineParser());
        addParser(new ExtMapLineParser());
        addParser(new MediaDirectionLineParser());
    }

    public static void addParser(MediaLineParser mediaLineParser) {
        for (String s : mediaLineParser.getLineStartWith()) {
            parsers.put(s, mediaLineParser);
        }
    }

    public static final String KEY = "a=";


    @Override
    public String[] getLineStartWith() {
        return new String[]{KEY};
    }

    @Override
    protected void parse(MediaDescription mediaDescription, String key, String value) {
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

        MediaLineParser mediaLineParser = parsers.get(expandKey);
        if (mediaLineParser == null) {
            return;
        }
        mediaLineParser.onParse(mediaDescription, expandKey, expandValue);
    }


}
