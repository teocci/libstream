package com.github.teocci.libstream.protocols.rtsp.rtsp;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Base64;
import android.view.SurfaceView;

import com.github.teocci.libstream.BuildConfig;
import com.github.teocci.libstream.clients.RtspDirectEncoder;
import com.github.teocci.libstream.coder.encoder.audio.AudioEncoder;
import com.github.teocci.libstream.coder.encoder.video.VideoEncoder;
import com.github.teocci.libstream.enums.Protocol;
import com.github.teocci.libstream.input.audio.AudioQuality;
import com.github.teocci.libstream.input.audio.AudioStream;
import com.github.teocci.libstream.input.video.VideoStream;
import com.github.teocci.libstream.interfaces.ConnectCheckerRtsp;
import com.github.teocci.libstream.interfaces.SessionCallback;
import com.github.teocci.libstream.interfaces.Stream;
import com.github.teocci.libstream.protocols.rtsp.rtp.packets.AacPacket;
import com.github.teocci.libstream.protocols.rtsp.rtp.packets.H264Packet;
import com.github.teocci.libstream.utils.LogHelper;

import java.io.OutputStream;
import java.nio.ByteBuffer;

import static com.github.teocci.libstream.enums.Protocol.TCP;
import static com.github.teocci.libstream.enums.Protocol.UDP;

/**
 * You should instantiate this class with the {@link SessionBuilder}.<br />
 * This is the class you will want to use to stream audio and or video to some peer using RTP.<br />
 * <p>
 * It holds a {@link VideoStream} and a {@link AudioStream} together and provides
 * synchronous and asynchronous functions to start and stop those steams.
 * You should implement a callback interface {@link SessionCallback} to receive notifications and error reports.<br />
 * <p>
 * If you want to stream to a RTSP server, you will need an instance of this class and hand it to a {@link RtspClient}.
 * <p>
 * If you don't use the RTSP protocol, you will still need to send a session description to the receiver
 * for him to be able to decode your audio/video streams. You can obtain this session description by calling
 * {@link #configure()} or {@link #syncConfigure()} to configure the session with its parameters
 * (audio sampling rate, video resolution) and then {@link Session#createDescription()}.<br />
 * <p>
 * See the example 2 here: https://github.com/fyhertz/libstreaming-examples to
 * see an example of how to get a SDP.<br />
 * <p>
 * See the example 3 here: https://github.com/fyhertz/libstreaming-examples to
 * see an example of how to stream to a RTSP server.<br />
 */
public class Session
{
    private final static String TAG = LogHelper.makeLogTag(Session.class);

    private final static String THREAD_NAME = BuildConfig.APPLICATION_ID + ".Session";

    public final static int STREAM_VIDEO = 0x01;
    public final static int STREAM_AUDIO = 0x00;

    public final static int[] CHANNEL_IDS = {STREAM_AUDIO, STREAM_VIDEO};

    /**
     * Some app is already using a camera (Camera.open() has failed).
     */
    public final static int ERROR_CAMERA_ALREADY_IN_USE = 0x00;

    /**
     * The phone may not support some streaming parameters that you are trying to use (bit rate, frame rate, resolution...).
     */
    public final static int ERROR_CONFIGURATION_NOT_SUPPORTED = 0x01;

    /**
     * The internal storage of the phone is not ready.
     * libstreaming tried to store a test file on the sdcard but couldn't.
     * See H264Stream and AACStream to find out why libstreaming would want to something like that.
     */
    public final static int ERROR_STORAGE_NOT_READY = 0x02;

    /**
     * The phone has no flash.
     */
    public final static int ERROR_CAMERA_HAS_NO_FLASH = 0x03;

    /**
     * The supplied SurfaceView is not a valid surface, or has not been created yet.
     */
    public final static int ERROR_INVALID_SURFACE = 0x04;

    /**
     * The destination set with {@link Session#setDestination(String)} could not be resolved.
     * May mean that the phone has no access to the internet, or that the DNS server could not
     * resolved the host name.
     */
    public final static int ERROR_UNKNOWN_HOST = 0x05;

    /**
     * Some other error occurred !
     */
    public final static int ERROR_OTHER = 0x06;

    public final int trackAudio = 0;
    public final int trackVideo = 1;

    public String user;
    public String password;

    public byte[] sps;
    public byte[] pps;
    public byte[] vps; //For H265

    // Packets
    public H264Packet h264Packet;
    public AacPacket aacPacket;

    public Protocol protocol = TCP;

    public ConnectCheckerRtsp connectCheckerRtsp;

    private String origin;
    private String destination;

    // For udp
    private int[] videoPorts = new int[]{5002, 5003};
    private int[] audioPorts = new int[]{6000, 6001};

    // For tcp
    private OutputStream outputStream;

    private AudioQuality audioQuality = AudioQuality.DEFAULT;

    // Default sps and pps to work only audio
//    protected String defaultSPS = "Z0KAHtoHgUZA";
//    protected String defaultPPS = "aM4NiA==";

    private int timeToLive = 64;
    private long timestamp;

    private AudioStream audioStream = null;
    private VideoStream videoStream = null;

    private SessionCallback sessionCallback;

    private Handler mainHandler;
    private Handler handler;


    /**
     * Creates a streaming session that can be customized by adding tracks.
     */
    public Session()
    {
        long uptime = System.nanoTime() / 1000;

        HandlerThread thread = new HandlerThread(THREAD_NAME);
        thread.start();

        this.handler = new Handler(thread.getLooper());
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.timestamp = (uptime / 1000) << 32 & (((uptime - ((uptime / 1000) * 1000)) >> 32) / 1000); // NTP timestamp
        this.origin = "127.0.0.1";
    }

//	/** You probably don't need to use that directly, use the {@link SessionBuilder}. */
//	void addAudioTrack(AudioStream track) {
//		removeAudioTrack();
//		this.audioStream = track;
//	}
//
//	/** You probably don't need to use that directly, use the {@link SessionBuilder}. */
//	void addVideoTrack(VideoStream track) {
//		removeVideoTrack();
//		this.videoStream = track;
//	}

    /**
     * You probably don't need to use that directly, use the {@link SessionBuilder}.
     */
    public void removeAudioTrack()
    {
        if (audioStream != null) {
            audioStream.stop();
            audioStream = null;
        }
    }

    /**
     * You probably don't need to use that directly, use the {@link SessionBuilder}.
     */
    public void removeVideoTrack()
    {
        if (videoStream != null) {
            videoStream.stopPreview();
            videoStream = null;
        }
    }

//	/** Returns the underlying {@link AudioStream} used by the {@link Session}. */
//	public AudioStream getAudioTrack() {
//		return audioStream;
//	}
//
//	/** Returns the underlying {@link VideoStream} used by the {@link Session}. */
//	public VideoStream getVideoTrack() {
//		return videoStream;
//	}
//
//	/**
//	 * Sets the callback interface that will be called by the {@link Session}.
//	 * @param callback The implementation of the {@link SessionCallback} interface
//	 */
//	public void setCallback(SessionCallback callback) {
//		this.sessionCallback = callback;
//	}

    /**
     * The origin address of the session.
     * It appears in the session description.
     *
     * @param origin The origin address
     */
    public void setOrigin(String origin)
    {
        this.origin = origin;
    }

    /**
     * The destination address for all the streams of the session. <br />
     * Changes will be taken into account the next time you start the session.
     *
     * @param destination The destination address
     */
    public void setDestination(String destination)
    {
        this.destination = destination;
    }

    /**
     * Returns the destination set with {@link #setDestination(String)}.
     */
    public String getDestination()
    {
        return destination;
    }

    public OutputStream getOutputStream()
    {
        return outputStream;
    }

//	/**
//	 * Set the TTL of all packets sent during the session. <br />
//	 * Changes will be taken into account the next time you start the session.
//	 * @param ttl The Time To Live
//	 */
//	public void setTimeToLive(int ttl) {
//		this.timeToLive = ttl;
//	}
//
//	/**
//	 * Sets the configuration of the stream. <br />
//	 * You can call this method at any time and changes will take
//	 * effect next time you call {@link #configure()}.
//	 * @param quality Quality of the stream
//	 */
//	public void setVideoQuality(VideoQuality quality) {
//		if (videoStream != null) {
//			videoStream.setVideoQuality(quality);
//		}
//	}
//
//	/**
//	 * Sets a Surface to show a preview of recorded media (video). <br />
//	 * You can call this method at any time and changes will take
//	 * effect next time you call {@link #start()} or {@link #startPreview()}.
//	 */
//	public void setSurfaceView(final SurfaceView view) {
//		handler.post(new Runnable() {
//			@Override
//			public void run() {
//				if (videoStream != null) {
//					videoStream.setSurfaceView(view);
//				}
//			}
//		});
//	}
//
//	/**
//	 * Sets the orientation of the preview. <br />
//	 * You can call this method at any time and changes will take
//	 * effect next time you call {@link #configure()}.
//	 * @param orientation The orientation of the preview
//	 */
//	public void setPreviewOrientation(int orientation) {
//		if (videoStream != null) {
//			videoStream.setPreviewOrientation(orientation);
//		}
//	}
//
//	/**
//	 * Sets the configuration of the stream. <br />
//	 * You can call this method at any time and changes will take
//	 * effect next time you call {@link #configure()}.
//	 * @param quality Quality of the stream
//	 */
//	public void setAudioQuality(AudioQuality quality) {
//		if (audioStream != null) {
//			audioStream.setAudioQuality(quality);
//		}
//	}
//
//	/**
//	 * Returns the {@link SessionCallback} interface that was set with
//	 * {@link #setCallback(SessionCallback)} or null if none was set.
//	 */
//	public SessionCallback getCallback() {
//		return sessionCallback;
//	}
//
//	/**
//	 * Returns a Session Description that can be stored in a file or sent to a client with RTSP.
//	 * @return The Session Description.
//	 * @throws IllegalStateException Thrown when {@link #setDestination(String)} has never been called.
//	 */
//	public String getSessionDescription() {
//		StringBuilder sessionDescription = new StringBuilder();
//
//		if (destination==null) {
//			throw new IllegalStateException("setDestination() has not been called !");
//		}
//
//		sessionDescription.append("v=0\r\n");
//		// TODO: Add IPV6 support
//		sessionDescription.append("o=- "+timestamp+" "+timestamp+" IN IP4 "+origin+"\r\n");
//		sessionDescription.append("s=Unnamed\r\n");
//		sessionDescription.append("i=N/A\r\n");
//		sessionDescription.append("c=IN IP4 "+destination+"\r\n");
//		// t=0 0 means the session is permanent (we don't know when it will stop)
//		sessionDescription.append("t=0 0\r\n");
//		sessionDescription.append("a=recvonly\r\n");
//
//		// Prevents two different sessions from using the same peripheral at the same time
//		if (audioStream != null) {
//			sessionDescription.append(audioStream.getSessionDescription());
//			sessionDescription.append("a=control:trackID="+0+"\r\n");
//		}
//		if (videoStream != null) {
//			sessionDescription.append(videoStream.getSessionDescription());
//			sessionDescription.append("a=control:trackID="+1+"\r\n");
//		}
//
//		return sessionDescription.toString();
//	}
//
//	/** Returns an approximation of the bandwidth consumed by the session in bit per second. */
//	public long getBitrate() {
//		long sum = 0;
//		if (audioStream != null) sum += audioStream.getBitrate();
//		if (videoStream != null) sum += videoStream.getBitrate();
//
//		return sum;
//	}

    /**
     * Indicates if a track is currently running.
     */
    public boolean isStreaming()
    {
        return ((aacPacket != null && aacPacket.isStreaming()) ||
                (h264Packet != null && h264Packet.isStreaming()));
    }

//	/**
//	 * Configures all streams of the session.
//	 **/
//	public void configure() {
//		handler.post(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					syncConfigure();
//				} catch (Exception e) {}
//			}
//		});
//	}
//
//	/**
//	 * Does the same thing as {@link #configure()}, but in a synchronous manner. <br />
//	 * Throws exceptions in addition to calling a callback
//	 * {@link SessionCallback#onSessionError(int, int, Exception)} when
//	 * an error occurs.
//	 **/
//	public void syncConfigure()
//			throws
//			RuntimeException,
//			IOException {
//
//		for (int id : CHANNEL_IDS) {
//			Stream stream = id==STREAM_AUDIO ? audioStream : videoStream;
//			if (stream!=null && !stream.isStreaming()) {
//				try {
//					stream.configure();
//				} catch (CameraInUseException e) {
//					postError(ERROR_CAMERA_ALREADY_IN_USE , id, e);
//					throw e;
//				} catch (StorageUnavailableException e) {
//					postError(ERROR_STORAGE_NOT_READY , id, e);
//					throw e;
//				} catch (ConfNotSupportedException e) {
//					postError(ERROR_CONFIGURATION_NOT_SUPPORTED , id, e);
//					throw e;
//				} catch (InvalidSurfaceException e) {
//					postError(ERROR_INVALID_SURFACE , id, e);
//					throw e;
//				} catch (IOException e) {
//					postError(ERROR_OTHER, id, e);
//					throw e;
//				} catch (RuntimeException e) {
//					postError(ERROR_OTHER, id, e);
//					throw e;
//				}
//			}
//		}
//
//		postSessionConfigured();
//	}
//
//	/**
//	 * Asynchronously starts all streams of the session.
//	 **/
//	public void start() {
//		handler.post(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					syncStart();
//				} catch (Exception e) {}
//			}
//		});
//	}
//
//	/**
//	 * Starts a stream in a synchronous manner. <br />
//	 * Throws exceptions in addition to calling a callback.
//	 * @param id The id of the stream to start
//	 **/
//	public void syncStart(int id)
//			throws CameraInUseException,
//			ConfNotSupportedException,
//			InvalidSurfaceException,
//			IOException {
//
//		Stream stream = id==STREAM_AUDIO ? audioStream : videoStream;
//		if (stream!=null && !stream.isStreaming()) {
//			try {
//				InetAddress destinationAddress =  InetAddress.getByName(destination);
//				stream.setTimeToLive(timeToLive);
//				stream.setDestinationAddress(destinationAddress);
//				stream.start();
//				if (getTrack(1-id) == null || getTrack(1-id).isStreaming()) {
//					postSessionStarted();
//				}
//				if (getTrack(1-id) == null || !getTrack(1-id).isStreaming()) {
//					handler.post(mUpdateBitrate);
//				}
//			} catch (UnknownHostException e) {
//				postError(ERROR_UNKNOWN_HOST, id, e);
//				throw e;
//			} catch (CameraInUseException e) {
//				postError(ERROR_CAMERA_ALREADY_IN_USE, id, e);
//				throw e;
//			} catch (StorageUnavailableException e) {
//				postError(ERROR_STORAGE_NOT_READY, id, e);
//				throw e;
//			} catch (ConfNotSupportedException e) {
//				postError(ERROR_CONFIGURATION_NOT_SUPPORTED, id, e);
//				throw e;
//			} catch (InvalidSurfaceException e) {
//				postError(ERROR_INVALID_SURFACE, id, e);
//				throw e;
//			} catch (IOException e) {
//				postError(ERROR_OTHER, id, e);
//				throw e;
//			} catch (RuntimeException e) {
//				postError(ERROR_OTHER, id, e);
//				throw e;
//			}
//		}
//	}
//
//	/**
//	 * Does the same thing as {@link #start()}, but in a synchronous manner. <br />
//	 * Throws exceptions in addition to calling a callback.
//	 **/
//	public void syncStart()
//			throws CameraInUseException,
//			ConfNotSupportedException,
//			InvalidSurfaceException,
//			IOException {
//
//		syncStart(1);
//		try {
//			syncStart(0);
//		} catch (RuntimeException e) {
//			syncStop(1);
//			throw e;
//		} catch (IOException e) {
//			syncStop(1);
//			throw e;
//		}
//	}

    /**
     * Stops all existing streams.
     */
    public void stop()
    {
        handler.post(this::syncStop);
    }

    /**
     * Stops one stream in a synchronous manner.
     *
     * @param id The id of the stream to stop
     **/
    private void syncStop(final int id)
    {
        Stream stream = id == STREAM_AUDIO ? audioStream : videoStream;
        if (stream != null) {
            stream.stop();
        }
    }

    /**
     * Stops all existing streams in a synchronous manner.
     */
    public void syncStop()
    {
        syncStop(0);
        syncStop(1);
        postSessionStopped();
    }

//	/**
//	 * Asynchronously starts the camera preview. <br />
//	 * You should of course pass a {@link SurfaceView} to {@link #setSurfaceView(SurfaceView)}
//	 * before calling this method. Otherwise, the {@link SessionCallback#onSessionError(int, int, Exception)}
//	 * callback will be called with {@link #ERROR_INVALID_SURFACE}.
//	 */
//	public void startPreview() {
//		handler.post(new Runnable() {
//			@Override
//			public void run() {
//				if (videoStream != null) {
//					try {
//						videoStream.startPreview();
//						postPreviewStarted();
//						videoStream.configure();
//					} catch (CameraInUseException e) {
//						postError(ERROR_CAMERA_ALREADY_IN_USE , STREAM_VIDEO, e);
//					} catch (ConfNotSupportedException e) {
//						postError(ERROR_CONFIGURATION_NOT_SUPPORTED , STREAM_VIDEO, e);
//					} catch (InvalidSurfaceException e) {
//						postError(ERROR_INVALID_SURFACE , STREAM_VIDEO, e);
//					} catch (RuntimeException e) {
//						postError(ERROR_OTHER, STREAM_VIDEO, e);
//					} catch (StorageUnavailableException e) {
//						postError(ERROR_STORAGE_NOT_READY, STREAM_VIDEO, e);
//					} catch (IOException e) {
//						postError(ERROR_OTHER, STREAM_VIDEO, e);
//					}
//				}
//			}
//		});
//	}
//
//	/**
//	 * Asynchronously stops the camera preview.
//	 */
//	public void stopPreview() {
//		handler.post(new Runnable() {
//			@Override
//			public void run() {
//				if (videoStream != null) {
//					videoStream.stopPreview();
//				}
//			}
//		});
//	}
//
//	/**	Switch between the front facing and the back facing camera of the phone. <br />
//	 * If {@link #startPreview()} has been called, the preview will be  briefly interrupted. <br />
//	 * If {@link #start()} has been called, the stream will be  briefly interrupted.<br />
//	 * To find out which camera is currently selected, use {@link #getCamera()}
//	 **/
//	public void switchCamera() {
//		handler.post(new Runnable() {
//			@Override
//			public void run() {
//				if (videoStream != null) {
//					try {
//						videoStream.switchCamera();
//						postPreviewStarted();
//					} catch (CameraInUseException e) {
//						postError(ERROR_CAMERA_ALREADY_IN_USE , STREAM_VIDEO, e);
//					} catch (ConfNotSupportedException e) {
//						postError(ERROR_CONFIGURATION_NOT_SUPPORTED , STREAM_VIDEO, e);
//					} catch (InvalidSurfaceException e) {
//						postError(ERROR_INVALID_SURFACE , STREAM_VIDEO, e);
//					} catch (IOException e) {
//						postError(ERROR_OTHER, STREAM_VIDEO, e);
//					} catch (RuntimeException e) {
//						postError(ERROR_OTHER, STREAM_VIDEO, e);
//					}
//				}
//			}
//		});
//	}
//
//	/**
//	 * Asynchronously zoom the camera preview.
//	 */
//	public void handleZoom(final int newZoom) {
//		handler.post(new Runnable() {
//			@Override
//			public void run() {
//				if (videoStream != null) {
//					videoStream.handleZoom(newZoom);
//				}
//			}
//		});
//	}
//
//	/**
//	 * Asynchronously zoom the camera preview.
//	 */
//	public void handleFocus(final float x, final float y) {
//		handler.post(new Runnable() {
//			@Override
//			public void run() {
//				if (videoStream != null) {
//					videoStream.handleFocus(x, y);
//				}
//			}
//		});
//	}
//
//	/**
//	 * Returns the id of the camera currently selected. <br />
//	 * It can be either {@link CameraInfo#CAMERA_FACING_BACK} or
//	 * {@link CameraInfo#CAMERA_FACING_FRONT}.
//	 */
//	public int getCamera() {
//		return videoStream != null ? videoStream.getCamera() : 0;
//
//	}
//
//	/**
//	 * Toggles the LED of the phone if it has one.
//	 * You can get the current state of the flash with
//	 * {@link Session#getVideoTrack()} and {@link VideoStream#getFlashState()}.
//	 **/
//	public void toggleFlash() {
//		handler.post(new Runnable() {
//			@Override
//			public void run() {
//				if (videoStream != null) {
//					try {
//						videoStream.toggleFlash();
//					} catch (RuntimeException e) {
//						postError(ERROR_CAMERA_HAS_NO_FLASH, STREAM_VIDEO, e);
//					}
//				}
//			}
//		});
//	}

    /**
     * Deletes all existing tracks & release associated resources.
     */
    public void release()
    {
        removeAudioTrack();
        removeVideoTrack();
        handler.getLooper().quit();
    }

    //	private void postPreviewStarted() {
//		mainHandler.post(new Runnable() {
//			@Override
//			public void run() {
//				if (sessionCallback != null) {
//					sessionCallback.onPreviewStarted();
//				}
//			}
//		});
//	}
//
//	private void postSessionConfigured() {
//		mainHandler.post(new Runnable() {
//			@Override
//			public void run() {
//				if (sessionCallback != null) {
//					sessionCallback.onSessionConfigured();
//				}
//			}
//		});
//	}
//
    private void postSessionStarted()
    {
        mainHandler.post(() -> {
            if (sessionCallback != null) {
                sessionCallback.onSessionStarted();
            }
        });
    }

    private void postSessionStopped()
    {
        mainHandler.post(() -> {
            if (sessionCallback != null) {
                sessionCallback.onSessionStopped();
            }
        });
    }

    public void updateDestination()
    {
        aacPacket.updateDestination();
        h264Packet.updateDestination();
        connectCheckerRtsp.onConnectionSuccessRtsp();
    }

    public String createDescription()
    {
        if (sps == null && pps == null) return null;

        String sSPS;
        String sPPS;
        sSPS = Base64.encodeToString(sps, 0, sps.length, Base64.NO_WRAP);
        sPPS = Base64.encodeToString(pps, 0, pps.length, Base64.NO_WRAP);
        return "v=0\r\n" +
                "o=- " + timestamp + " " + timestamp + " IN IP4 " + origin + "\r\n" +
                "s=Unnamed\r\n" +
                "i=N/A\r\n" +
                "c=IN IP4 " + destination + "\r\n" +
                "t=0 0\r\n" + // this means the session is permanent
                "a=recvonly\r\n" +
                AudioEncoder.createBody(trackAudio, getAudioPorts()[0], audioQuality) +
                VideoEncoder.createBody(trackVideo, getVideoPorts()[0], sSPS, sPPS);
    }

    private byte[] extractData(ByteBuffer buffer)
    {
        if (buffer == null) return null;

        byte[] chunk = new byte[buffer.capacity() - 4];
        buffer.position(4);
        buffer.get(chunk, 0, chunk.length); // Remove the last 4 bytes

        return chunk;
    }


    // Setters

    public void setOutputStream(OutputStream outputStream)
    {
        this.outputStream = outputStream;
    }

    public void setPSPair(ByteBuffer sps, ByteBuffer pps) throws IllegalAccessException
    {
        if (sps == null) throw new IllegalAccessException("SPS is null.");
        if (pps == null) throw new IllegalAccessException("PPS is null.");

        byte[] mSPS = new byte[sps.capacity() - 4];
        sps.position(4);
        sps.get(mSPS, 0, mSPS.length);

        byte[] mPPS = new byte[pps.capacity() - 4];
        pps.position(4);
        pps.get(mPPS, 0, mPPS.length);

        this.sps = mSPS;
        this.pps = mPPS;
    }

    public void setAVCInfo(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps)
    {
        this.sps = extractData(sps);
        this.pps = extractData(pps);
        this.vps = extractData(vps);  // H264 has no vps so if not null assume H265
    }
    /**
     * Sets the destination ports of the stream.
     *
     * @param rtpPort  Destination port that will be used for RTP
     * @param rtcpPort Destination port that will be used for RTCP
     */
    public void setDestinationPorts(int id, int rtpPort, int rtcpPort)
    {
        if (protocol != UDP) return;
        if (!trackExists(id)) return;
        if (id == 0) {
            setAudioPorts(rtpPort, rtcpPort);
        } else {
            setVideoPorts(rtpPort, rtcpPort);
        }
    }

    /**
     * Sets the destination ports of the stream.
     * If an odd number is supplied for the destination port then the next
     * lower even number will be used for RTP and it will be used for RTCP.
     * If an even number is supplied, it will be used for RTP and the next odd
     * number will be used for RTCP.
     *
     * @param dport The destination port
     */
    public void setAudioPorts(int dport)
    {
        if (dport % 2 == 1) {
            audioPorts = new int[]{dport - 1, dport};
        } else {
            audioPorts = new int[]{dport, dport + 1};
        }
    }

    /**
     * Sets the destination ports of the stream.
     * If an odd number is supplied for the destination port then the next
     * lower even number will be used for RTP and it will be used for RTCP.
     * If an even number is supplied, it will be used for RTP and the next odd
     * number will be used for RTCP.
     *
     * @param dport The destination port
     */
    public void setVideoPorts(int dport)
    {
        if (dport % 2 == 1) {
            videoPorts = new int[]{dport - 1, dport};
        } else {
            videoPorts = new int[]{dport, dport + 1};
        }
    }

    /**
     * Sets the audio destination ports of the stream.
     *
     * @param rtpPort  Destination port that will be used for RTP
     * @param rtcpPort Destination port that will be used for RTCP
     */
    public void setAudioPorts(int rtpPort, int rtcpPort)
    {
        audioPorts = new int[]{rtpPort, rtcpPort};
    }

    /**
     * Sets the video destination ports of the stream.
     *
     * @param rtpPort  Destination port that will be used for RTP
     * @param rtcpPort Destination port that will be used for RTCP
     */
    public void setVideoPorts(int rtpPort, int rtcpPort)
    {
        videoPorts = new int[]{rtpPort, rtcpPort};
    }

    /**
     * Sets the audio destination ports of the stream.
     *
     * @param ports Destination ports that will be used for RTP and RTCP
     */
    public void setAudioPorts(int[] ports)
    {
        audioPorts = ports;
    }

    /**
     * Sets the video destination ports of the stream.
     *
     * @param ports Destination ports that will be used for RTP and RTCP
     */
    public void setVideoPorts(int[] ports)
    {
        videoPorts = ports;
    }

    public void setProtocol(Protocol protocol)
    {
        this.protocol = protocol;
    }

    public void setSampleRate(int sampleRate)
    {
        audioQuality.sampleRate = sampleRate;
    }

    public void setChannel(int channel)
    {
        audioQuality.channel = channel;
    }

    public void setConnectCheckerRtsp(ConnectCheckerRtsp connectCheckerRtsp)
    {
        this.connectCheckerRtsp = connectCheckerRtsp;
    }

    public void setZoom(int newZoom) {}

//	private void postError(final int reason, final int streamType,final Exception e) {
//		mainHandler.post(new Runnable() {
//			@Override
//			public void run() {
//				if (sessionCallback != null) {
//					sessionCallback.onSessionError(reason, streamType, e);
//				}
//			}
//		});
//	}
//
//	private void postBitRate(final long bitrate) {
//		mainHandler.post(new Runnable() {
//			@Override
//			public void run() {
//				if (sessionCallback != null) {
//					sessionCallback.onBitrateUpdate(bitrate);
//				}
//			}
//		});
//	}
//
//	private Runnable mUpdateBitrate = new Runnable() {
//		@Override
//		public void run() {
//			if (isStreaming()) {
//				postBitRate(getBitrate());
//				handler.postDelayed(mUpdateBitrate, 500);
//			} else {
//				postBitRate(0);
//			}
//		}
//	};
//
//
//	public boolean trackExists(int id) {
//		return (id==0) ? audioStream!=null :videoStream!=null;
//	}
//
//	public Stream getTrack(int id) {
//		return (id==0) ? audioStream : videoStream;
//	}


    // Getters

    public ConnectCheckerRtsp getConnectCheckerRtsp()
    {
        return connectCheckerRtsp;
    }

    /**
     * Returns a pair of destination ports, the first one is the
     * one used for RTP and the second one is used for RTCP.
     **/
    public int[] getDestinationPorts(int id)
    {
        if (protocol != UDP) return null;
        if (!trackExists(id)) return null;
        return id == 0 ? getAudioPorts() : getVideoPorts();
    }

    /**
     * Returns a pair of video destination ports, the first one is the
     * one used for RTP and the second one is used for RTCP.
     **/
    public int[] getVideoPorts()
    {
        return videoPorts;
    }

    /**
     * Returns a pair of audio destination ports, the first one is the
     * one used for RTP and the second one is used for RTCP.
     **/
    public int[] getAudioPorts()
    {
        return audioPorts;
    }

    public int getSSRC(int id)
    {
        if (!trackExists(id)) return -1;
        return id == 0 ? aacPacket.getSSRC() : h264Packet.getSSRC();
    }

    /**
     * Returns a pair of source ports, the first one is the
     * one used for RTP and the second one is used for RTCP.
     **/
    public int[] getLocalPorts(int id)
    {
        if (protocol != UDP) return null;
        if (!trackExists(id)) return null;
        return id == 0 ? aacPacket.getLocalPorts() : h264Packet.getLocalPorts();
    }

    public int getSampleRate()
    {
        return audioQuality.sampleRate;
    }


    // Booleans

    public boolean trackExists(int id)
    {
        return (id == 0) ? aacPacket != null : h264Packet != null;
    }

    public boolean isTCP()
    {
        return protocol == TCP;
    }
}
