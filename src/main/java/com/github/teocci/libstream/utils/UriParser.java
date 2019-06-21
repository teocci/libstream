package com.github.teocci.libstream.utils;

//import static net.kseek.streaming.SessionBuilder.AUDIO_AAC;
//import static net.kseek.streaming.SessionBuilder.AUDIO_NONE;
//import static net.kseek.streaming.SessionBuilder.VIDEO_H263;
//import static net.kseek.streaming.SessionBuilder.VIDEO_H264;
//import static net.kseek.streaming.SessionBuilder.VIDEO_NONE;

/**
 * This class parses URIs received by the RTSP server and configures a Session accordingly.
 */
public class UriParser
{
//    public final static String TAG = "UriParser";
//
//    /**
//     * Configures a Session according to the given URI.
//     * Here are some examples of URIs that can be used to configure a Session:
//     * <ul><li>rtsp://xxx.xxx.xxx.xxx:8086?h264&flash=on</li>
//     * <li>rtsp://xxx.xxx.xxx.xxx:8086?h263&camera=front&flash=on</li>
//     * <li>rtsp://xxx.xxx.xxx.xxx:8086?h264=200-20-320-240</li>
//     * <li>rtsp://xxx.xxx.xxx.xxx:8086?aac</li></ul>
//     *
//     * @param uri The URI
//     * @return A Session configured according to the URI
//     * @throws IllegalStateException
//     * @throws IOException
//     */
//    public static Session parse(String uri) throws IllegalStateException, IOException
//    {
//        SessionBuilder builder = SessionBuilder.getInstance().clone();
//        byte audioApi = 0, videoApi = 0;
//
//        String query = URI.create(uri).getQuery();
//        String[] queryParams = query == null ? new String[0] : query.split("&");
//        ContentValues params = new ContentValues();
//        for (String param : queryParams) {
//            String[] keyValue = param.split("=");
//            String value = "";
//            try {
//                value = keyValue[1];
//            } catch (ArrayIndexOutOfBoundsException e) {}
//
//            params.put(
//                    URLEncoder.encode(keyValue[0], "UTF-8"), // Name
//                    URLEncoder.encode(value, "UTF-8")  // Value
//            );
//
//        }
//
//        if (params.size() > 0) {
//            builder.setAudioEncoder(AUDIO_NONE).setVideoEncoder(VIDEO_NONE);
//            Set<String> paramKeys = params.keySet();
//            // Those parameters must be parsed first or else they won't necessarily be taken into account
//            for (String paramName : paramKeys) {
//                String paramValue = params.getAsString(paramName);
//
//                // FLASH ON/OFF
//                if (paramName.equalsIgnoreCase("flash")) {
//                    if (paramValue.equalsIgnoreCase("on"))
//                        builder.setFlashEnabled(true);
//                    else
//                        builder.setFlashEnabled(false);
//                }
//
//                // CAMERA -> the client can choose between the front facing camera and the back facing camera
//                else if (paramName.equalsIgnoreCase("camera")) {
//                    if (paramValue.equalsIgnoreCase("back"))
//                        builder.setCamera(CameraInfo.CAMERA_FACING_BACK);
//                    else if (paramValue.equalsIgnoreCase("front"))
//                        builder.setCamera(CameraInfo.CAMERA_FACING_FRONT);
//                }
//
//                // MULTICAST -> the stream will be sent to a multicast group
//                // The default mutlicast address is 228.5.6.7, but the client can specify another
//                else if (paramName.equalsIgnoreCase("multicast")) {
//                    if (paramValue != null) {
//                        try {
//                            InetAddress addr = InetAddress.getByName(paramValue);
//                            if (!addr.isMulticastAddress()) {
//                                throw new IllegalStateException("Invalid multicast address !");
//                            }
//                            builder.setDestination(paramValue);
//                        } catch (UnknownHostException e) {
//                            throw new IllegalStateException("Invalid multicast address !");
//                        }
//                    } else {
//                        // Default multicast address
//                        builder.setDestination("228.5.6.7");
//                    }
//                }
//
//                // UNICAST -> the client can use this to specify where he wants the stream to be sent
//                else if (paramName.equalsIgnoreCase("unicast")) {
//                    if (paramValue != null) {
//                        builder.setDestination(paramValue);
//                    }
//                }
//
//                // VIDEOAPI -> can be used to specify what api will be used to encode video (the MediaRecorder API or the MediaCodec API)
//                else if (paramName.equalsIgnoreCase("videoapi")) {
//                    if (paramValue != null) {
//                        if (paramValue.equalsIgnoreCase("mr")) {
//                            videoApi = MediaStream.MODE_MEDIARECORDER_API;
//                        } else if (paramValue.equalsIgnoreCase("mc")) {
//                            videoApi = MediaStream.MODE_MEDIACODEC_API;
//                        }
//                    }
//                }
//
//                // AUDIOAPI -> can be used to specify what api will be used to encode audio (the MediaRecorder API or the MediaCodec API)
//                else if (paramName.equalsIgnoreCase("audioapi")) {
//                    if (paramValue != null) {
//                        if (paramValue.equalsIgnoreCase("mr")) {
//                            audioApi = MediaStream.MODE_MEDIARECORDER_API;
//                        } else if (paramValue.equalsIgnoreCase("mc")) {
//                            audioApi = MediaStream.MODE_MEDIACODEC_API;
//                        }
//                    }
//                }
//
//                // TTL -> the client can modify the time to live of packets
//                // By default ttl=64
//                else if (paramName.equalsIgnoreCase("ttl")) {
//                    if (paramValue != null) {
//                        try {
//                            int ttl = Integer.parseInt(paramValue);
//                            if (ttl < 0) throw new IllegalStateException();
//                            builder.setTimeToLive(ttl);
//                        } catch (Exception e) {
//                            throw new IllegalStateException("The TTL must be a positive integer !");
//                        }
//                    }
//                }
//
//                // H.264
//                else if (paramName.equalsIgnoreCase("h264")) {
//                    VideoQuality quality = VideoQuality.parseQuality(paramValue);
//                    builder.setVideoQuality(quality).setVideoEncoder(VIDEO_H264);
//                }
//
//                // H.263
//                else if (paramName.equalsIgnoreCase("h263")) {
//                    VideoQuality quality = VideoQuality.parseQuality(paramValue);
//                    builder.setVideoQuality(quality).setVideoEncoder(VIDEO_H263);
//                }
//
//                // AMR
//                else if (paramName.equalsIgnoreCase("amrnb") || paramName.equalsIgnoreCase("amr")) {
//                    AudioQuality quality = AudioQuality.parseQuality(paramValue);
//                    builder.setAudioQuality(quality).setAudioEncoder(AUDIO_AMRNB);
//                }
//
//                // AAC
//                else if (paramName.equalsIgnoreCase("aac")) {
//                    AudioQuality quality = AudioQuality.parseQuality(paramValue);
//                    builder.setAudioQuality(quality).setAudioEncoder(AUDIO_AAC);
//                }
//
//            }
//
//        }
//
//        if (builder.getVideoEncoder() == VIDEO_NONE && builder.getAudioEncoder() == AUDIO_NONE) {
//            SessionBuilder b = SessionBuilder.getInstance();
//            builder.setVideoEncoder(b.getVideoEncoder());
//            builder.setAudioEncoder(b.getAudioEncoder());
//        }
//
//        Session session = builder.build();
//
//        if (videoApi > 0 && session.getVideoTrack() != null) {
//            session.getVideoTrack().setStreamingMethod(videoApi);
//        }
//
//        if (audioApi > 0 && session.getAudioTrack() != null) {
//            session.getAudioTrack().setStreamingMethod(audioApi);
//        }
//
//        return session;
//    }
}
