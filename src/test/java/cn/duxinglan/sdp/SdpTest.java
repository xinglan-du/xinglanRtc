package cn.duxinglan.sdp;

import cn.duxinglan.sdp.entity.SessionDescription;
import cn.duxinglan.sdp.parse.SdpParser;

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
public class SdpTest {

    private static final String SDP_STRING = """
            v=0
            o=- 4884528416432785153 2 IN IP4 127.0.0.1
            s=-
            t=0 0
            a=group:BUNDLE 0 1
            a=extmap-allow-mixed
            a=msid-semantic: WMS 7f855a09-53c9-4d61-82bb-ba6b2ca69706
            m=audio 9 UDP/TLS/RTP/SAVPF 111 63 9 0 8 13 110 126
            c=IN IP4 0.0.0.0
            a=rtcp:9 IN IP4 0.0.0.0
            a=candidate:312432146 1 udp 2122194687 192.168.64.1 60207 typ host generation 0 network-id 1
            a=candidate:2917354289 1 udp 2122063615 10.240.1.51 51080 typ host generation 0 network-id 3 network-cost 10
            a=candidate:3349596158 1 udp 2121998079 172.16.0.223 51654 typ host generation 0 network-id 5 network-cost 50
            a=candidate:3431748403 1 udp 2122265343 fd1e:c273:37cf:685f:461:27a2:489e:cdd0 50177 typ host generation 0 network-id 2
            a=candidate:1965722208 1 udp 2122131711 2408:821b:439:7f21:159c:380:46e:cdfd 63554 typ host generation 0 network-id 4 network-cost 10
            a=candidate:3962929798 1 tcp 1518214911 192.168.64.1 9 typ host tcptype active generation 0 network-id 1
            a=candidate:1397333925 1 tcp 1518083839 10.240.1.51 9 typ host tcptype active generation 0 network-id 3 network-cost 10
            a=candidate:957092714 1 tcp 1518018303 172.16.0.223 9 typ host tcptype active generation 0 network-id 5 network-cost 50
            a=candidate:841385895 1 tcp 1518285567 fd1e:c273:37cf:685f:461:27a2:489e:cdd0 9 typ host tcptype active generation 0 network-id 2
            a=candidate:2340445940 1 tcp 1518151935 2408:821b:439:7f21:159c:380:46e:cdfd 9 typ host tcptype active generation 0 network-id 4 network-cost 10
            a=ice-ufrag:jrN1
            a=ice-pwd:6uYHAWHR0nTOJX8qUMR3MNrM
            a=ice-options:trickle
            a=fingerprint:sha-256 CC:97:32:60:42:B5:94:AA:C8:51:F5:E0:B2:E8:39:BC:5E:F5:12:C6:16:D4:8F:0A:CF:4D:70:C9:19:6A:E8:C6
            a=setup:actpass
            a=mid:0
            a=extmap:1 urn:ietf:params:rtp-hdrext:ssrc-audio-level
            a=extmap:2 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
            a=extmap:3 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01
            a=extmap:4 urn:ietf:params:rtp-hdrext:sdes:mid
            a=sendrecv
            a=msid:7f855a09-53c9-4d61-82bb-ba6b2ca69706 5e9a8d0e-aba0-464f-875d-c1a25c3b7d58
            a=rtcp-mux
            a=rtcp-rsize
            a=rtpmap:111 opus/48000/2
            a=rtcp-fb:111 transport-cc
            a=fmtp:111 minptime=10;useinbandfec=1
            a=rtpmap:63 red/48000/2
            a=fmtp:63 111/111
            a=rtpmap:9 G722/8000
            a=rtpmap:0 PCMU/8000
            a=rtpmap:8 PCMA/8000
            a=rtpmap:13 CN/8000
            a=rtpmap:110 telephone-event/48000
            a=rtpmap:126 telephone-event/8000
            a=ssrc:2926456319 cname:L4BKH0FDlGDAAth7
            a=ssrc:2926456319 msid:7f855a09-53c9-4d61-82bb-ba6b2ca69706 5e9a8d0e-aba0-464f-875d-c1a25c3b7d58
            m=video 9 UDP/TLS/RTP/SAVPF 96 97 103 104 107 108 109 114 115 116 117 118 39 40 45 46 98 99 100 101 119 120 49 50 123 124 125
            c=IN IP4 0.0.0.0
            a=rtcp:9 IN IP4 0.0.0.0
            a=candidate:312432146 1 udp 2122194687 192.168.64.1 60207 typ host generation 0 network-id 1
            a=candidate:2917354289 1 udp 2122063615 10.240.1.51 51080 typ host generation 0 network-id 3 network-cost 10
            a=candidate:3349596158 1 udp 2121998079 172.16.0.223 51654 typ host generation 0 network-id 5 network-cost 50
            a=candidate:3431748403 1 udp 2122265343 fd1e:c273:37cf:685f:461:27a2:489e:cdd0 50177 typ host generation 0 network-id 2
            a=candidate:1965722208 1 udp 2122131711 2408:821b:439:7f21:159c:380:46e:cdfd 63554 typ host generation 0 network-id 4 network-cost 10
            a=candidate:3962929798 1 tcp 1518214911 192.168.64.1 9 typ host tcptype active generation 0 network-id 1
            a=candidate:1397333925 1 tcp 1518083839 10.240.1.51 9 typ host tcptype active generation 0 network-id 3 network-cost 10
            a=candidate:957092714 1 tcp 1518018303 172.16.0.223 9 typ host tcptype active generation 0 network-id 5 network-cost 50
            a=candidate:841385895 1 tcp 1518285567 fd1e:c273:37cf:685f:461:27a2:489e:cdd0 9 typ host tcptype active generation 0 network-id 2
            a=candidate:2340445940 1 tcp 1518151935 2408:821b:439:7f21:159c:380:46e:cdfd 9 typ host tcptype active generation 0 network-id 4 network-cost 10
            a=ice-ufrag:jrN1
            a=ice-pwd:6uYHAWHR0nTOJX8qUMR3MNrM
            a=ice-options:trickle
            a=fingerprint:sha-256 CC:97:32:60:42:B5:94:AA:C8:51:F5:E0:B2:E8:39:BC:5E:F5:12:C6:16:D4:8F:0A:CF:4D:70:C9:19:6A:E8:C6
            a=setup:actpass
            a=mid:1
            a=extmap:14 urn:ietf:params:rtp-hdrext:toffset
            a=extmap:2 http://www.webrtc.org/experiments/rtp-hdrext/abs-send-time
            a=extmap:13 urn:3gpp:video-orientation
            a=extmap:3 http://www.ietf.org/id/draft-holmer-rmcat-transport-wide-cc-extensions-01
            a=extmap:5 http://www.webrtc.org/experiments/rtp-hdrext/playout-delay
            a=extmap:6 http://www.webrtc.org/experiments/rtp-hdrext/video-content-type
            a=extmap:7 http://www.webrtc.org/experiments/rtp-hdrext/video-timing
            a=extmap:8 http://www.webrtc.org/experiments/rtp-hdrext/color-space
            a=extmap:4 urn:ietf:params:rtp-hdrext:sdes:mid
            a=extmap:10 urn:ietf:params:rtp-hdrext:sdes:rtp-stream-id
            a=extmap:11 urn:ietf:params:rtp-hdrext:sdes:repaired-rtp-stream-id
            a=sendrecv
            a=msid:7f855a09-53c9-4d61-82bb-ba6b2ca69706 550b75ac-1343-4f36-8941-d88a3d51f61b
            a=rtcp-mux
            a=rtcp-rsize
            a=rtpmap:96 VP8/90000
            a=rtcp-fb:96 goog-remb
            a=rtcp-fb:96 transport-cc
            a=rtcp-fb:96 ccm fir
            a=rtcp-fb:96 nack
            a=rtcp-fb:96 nack pli
            a=rtpmap:97 rtx/90000
            a=fmtp:97 apt=96
            a=rtpmap:103 H264/90000
            a=rtcp-fb:103 goog-remb
            a=rtcp-fb:103 transport-cc
            a=rtcp-fb:103 ccm fir
            a=rtcp-fb:103 nack
            a=rtcp-fb:103 nack pli
            a=fmtp:103 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42001f
            a=rtpmap:104 rtx/90000
            a=fmtp:104 apt=103
            a=rtpmap:107 H264/90000
            a=rtcp-fb:107 goog-remb
            a=rtcp-fb:107 transport-cc
            a=rtcp-fb:107 ccm fir
            a=rtcp-fb:107 nack
            a=rtcp-fb:107 nack pli
            a=fmtp:107 level-asymmetry-allowed=1;packetization-mode=0;profile-level-id=42001f
            a=rtpmap:108 rtx/90000
            a=fmtp:108 apt=107
            a=rtpmap:109 H264/90000
            a=rtcp-fb:109 goog-remb
            a=rtcp-fb:109 transport-cc
            a=rtcp-fb:109 ccm fir
            a=rtcp-fb:109 nack
            a=rtcp-fb:109 nack pli
            a=fmtp:109 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f
            a=rtpmap:114 rtx/90000
            a=fmtp:114 apt=109
            a=rtpmap:115 H264/90000
            a=rtcp-fb:115 goog-remb
            a=rtcp-fb:115 transport-cc
            a=rtcp-fb:115 ccm fir
            a=rtcp-fb:115 nack
            a=rtcp-fb:115 nack pli
            a=fmtp:115 level-asymmetry-allowed=1;packetization-mode=0;profile-level-id=42e01f
            a=rtpmap:116 rtx/90000
            a=fmtp:116 apt=115
            a=rtpmap:117 H264/90000
            a=rtcp-fb:117 goog-remb
            a=rtcp-fb:117 transport-cc
            a=rtcp-fb:117 ccm fir
            a=rtcp-fb:117 nack
            a=rtcp-fb:117 nack pli
            a=fmtp:117 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=4d001f
            a=rtpmap:118 rtx/90000
            a=fmtp:118 apt=117
            a=rtpmap:39 H264/90000
            a=rtcp-fb:39 goog-remb
            a=rtcp-fb:39 transport-cc
            a=rtcp-fb:39 ccm fir
            a=rtcp-fb:39 nack
            a=rtcp-fb:39 nack pli
            a=fmtp:39 level-asymmetry-allowed=1;packetization-mode=0;profile-level-id=4d001f
            a=rtpmap:40 rtx/90000
            a=fmtp:40 apt=39
            a=rtpmap:45 AV1/90000
            a=rtcp-fb:45 goog-remb
            a=rtcp-fb:45 transport-cc
            a=rtcp-fb:45 ccm fir
            a=rtcp-fb:45 nack
            a=rtcp-fb:45 nack pli
            a=fmtp:45 level-idx=5;profile=0;tier=0
            a=rtpmap:46 rtx/90000
            a=fmtp:46 apt=45
            a=rtpmap:98 VP9/90000
            a=rtcp-fb:98 goog-remb
            a=rtcp-fb:98 transport-cc
            a=rtcp-fb:98 ccm fir
            a=rtcp-fb:98 nack
            a=rtcp-fb:98 nack pli
            a=fmtp:98 profile-id=0
            a=rtpmap:99 rtx/90000
            a=fmtp:99 apt=98
            a=rtpmap:100 VP9/90000
            a=rtcp-fb:100 goog-remb
            a=rtcp-fb:100 transport-cc
            a=rtcp-fb:100 ccm fir
            a=rtcp-fb:100 nack
            a=rtcp-fb:100 nack pli
            a=fmtp:100 profile-id=2
            a=rtpmap:101 rtx/90000
            a=fmtp:101 apt=100
            a=rtpmap:119 H264/90000
            a=rtcp-fb:119 goog-remb
            a=rtcp-fb:119 transport-cc
            a=rtcp-fb:119 ccm fir
            a=rtcp-fb:119 nack
            a=rtcp-fb:119 nack pli
            a=fmtp:119 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=64001f
            a=rtpmap:120 rtx/90000
            a=fmtp:120 apt=119
            a=rtpmap:49 H265/90000
            a=rtcp-fb:49 goog-remb
            a=rtcp-fb:49 transport-cc
            a=rtcp-fb:49 ccm fir
            a=rtcp-fb:49 nack
            a=rtcp-fb:49 nack pli
            a=fmtp:49 level-id=156;profile-id=1;tier-flag=0;tx-mode=SRST
            a=rtpmap:50 rtx/90000
            a=fmtp:50 apt=49
            a=rtpmap:123 red/90000
            a=rtpmap:124 rtx/90000
            a=fmtp:124 apt=123
            a=rtpmap:125 ulpfec/90000
            a=ssrc-group:FID 2013921525 512224715
            a=ssrc:2013921525 cname:L4BKH0FDlGDAAth7
            a=ssrc:2013921525 msid:7f855a09-53c9-4d61-82bb-ba6b2ca69706 550b75ac-1343-4f36-8941-d88a3d51f61b
            a=ssrc:512224715 cname:L4BKH0FDlGDAAth7
            a=ssrc:512224715 msid:7f855a09-53c9-4d61-82bb-ba6b2ca69706 550b75ac-1343-4f36-8941-d88a3d51f61b
            """;

    static void main() {
        SessionDescription parse = SdpParser.parse(SDP_STRING);
        System.out.println(parse);
    }
}
