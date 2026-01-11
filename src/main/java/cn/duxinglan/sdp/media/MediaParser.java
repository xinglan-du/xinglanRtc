package cn.duxinglan.sdp.media;

import cn.duxinglan.media.signaling.sdp.MediaDescription;
import cn.duxinglan.sdp.media.parser.ConnectionLineParser;
import cn.duxinglan.sdp.media.parser.MediaInfoLineParser;

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
public class MediaParser {


    private static final Map<String, MediaLineParser> parsers = new ConcurrentHashMap<>();

    static {
        addParser(new MediaInfoLineParser());
        addParser(new ConnectionLineParser());
        addParser(new MediaExpandParser());

    }

    public static void addParser(MediaLineParser mediaLineParser) {
        for (String s : mediaLineParser.getLineStartWith()) {
            parsers.put(s, mediaLineParser);
        }
    }

    public static boolean parse(MediaDescription mediaDescription, String line) {
        MediaLineParser parser = parsers.get(line.substring(0, 2));
        if (parser == null) {
            return false;
        }
        return parser.onParse(mediaDescription, line);
    }
}
