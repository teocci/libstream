package com.github.teocci.libstream.input.video;

import com.github.teocci.libstream.input.media.MediaStream;
import com.github.teocci.libstream.utils.LogHelper;

/**
 * Don't use this class directly.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */
public abstract class VideoStream extends MediaStream
{
    private final static String TAG = LogHelper.makeLogTag(VideoStream.class);

//    protected VideoQuality requestedQuality = VideoQuality.DEFAULT.clone();
//    protected VideoQuality currentQuality = requestedQuality.clone();
//
//    protected Callback surfaceHolderCallback = null;
//    protected SurfaceView surfaceView = null;
//    protected SharedPreferences settings = null;
//    protected int videoEncoder, cameraID = 0;
//    protected int requestedOrientation = 0, orientation = 0;
//    protected Camera camera;
//    protected Thread cameraThread;
//    protected Looper cameraLooper;
//
    protected boolean cameraOpenedManually = true;
//    protected boolean flashEnabled = false;
//    protected boolean surfaceReady = false;
//    protected boolean unlocked = false;
//    protected boolean previewStarted = false;
//    protected boolean updated = false;
//
//    protected String mimeType = "video/avc";
//    protected String encoderName;
//    protected int encoderColorFormat;
//    protected int cameraImageFormat;
//    protected int maxFps = 0;
//
//    protected int currentZoom;
//
//    /**
//     * Don't use this class directly.
//     * Uses CAMERA_FACING_BACK by default.
//     */
//    public VideoStream()
//    {
//        this(CameraInfo.CAMERA_FACING_BACK);
//    }
//
//    /**
//     * Don't use this class directly
//     *
//     * @param camera Can be either CameraInfo.CAMERA_FACING_BACK or CameraInfo.CAMERA_FACING_FRONT
//     */
//    @SuppressLint("InlinedApi")
//    public VideoStream(int camera)
//    {
//        super();
//        setCamera(camera);
//    }
//
//    /**
//     * Sets the camera that will be used to capture video.
//     * You can call this method at any time and changes will take effect next time you start the stream.
//     *
//     * @param camera Can be either CameraInfo.CAMERA_FACING_BACK or CameraInfo.CAMERA_FACING_FRONT
//     */
//    public void setCamera(int camera)
//    {
//        CameraInfo cameraInfo = new CameraInfo();
//        int numberOfCameras = Camera.getNumberOfCameras();
//        for (int i = 0; i < numberOfCameras; i++) {
//            Camera.getCameraInfo(i, cameraInfo);
//            if (cameraInfo.facing == camera) {
//                cameraID = i;
//                break;
//            }
//        }
//    }
//
//    /**
//     * Switch between the front facing and the back facing camera of the phone.
//     * If {@link #startPreview()} has been called, the preview will be  briefly interrupted.
//     * If {@link #start()} has been called, the stream will be  briefly interrupted.
//     * You should not call this method from the main thread if you are already streaming.
//     *
//     * @throws IOException      IOException
//     * @throws RuntimeException RuntimeException
//     **/
//    public void switchCamera() throws RuntimeException, IOException
//    {
//        if (Camera.getNumberOfCameras() == 1) throw new IllegalStateException("Phone only has one camera !");
//        boolean streaming = this.streaming;
//        boolean previewing = camera != null && cameraOpenedManually;
//        cameraID = (cameraID == CameraInfo.CAMERA_FACING_BACK) ? CameraInfo.CAMERA_FACING_FRONT : CameraInfo.CAMERA_FACING_BACK;
//        setCamera(cameraID);
//        stopPreview();
//        flashEnabled = false;
//        if (previewing) startPreview();
//        if (streaming) start();
//    }
//
//    public void setZoom(int newZoom)
//    {
//        // zoom in/zoom out
//        int zoom = newZoom > currentZoom ? 1 : (newZoom < currentZoom ? -1 : 0);
//        currentZoom = newZoom;
//        handleZoom(zoom);
//    }
//
//    public void handleZoom(int newZoom)
//    {
//        Parameters parameters = camera.getParameters();
//        int maxZoom = parameters.getMaxZoom();
//        int zoom = parameters.getZoom();
//        if (newZoom > 0) {
//            // zoom in
//            if (zoom < maxZoom)
//                zoom++;
//        } else if (newZoom < 0) {
//            // zoom out
//            if (zoom > 0)
//                zoom--;
//        }
//
//        parameters.setZoom(zoom);
//        camera.setParameters(parameters);
//    }
//
//    public void handleFocus(float x, float y)
//    {
//        Parameters parameters = camera.getParameters();
//
//        List<String> supportedFocusModes = parameters.getSupportedFocusModes();
//        if (supportedFocusModes != null && supportedFocusModes.contains(Parameters.FOCUS_MODE_AUTO)) {
//            camera.autoFocus((b, camera) -> {
//                // currently set to auto-focus on single touch
//            });
//        }
//    }
//
//    /**
//     * Returns the id of the camera currently selected.
//     * Can be either {@link CameraInfo#CAMERA_FACING_BACK} or
//     * {@link CameraInfo#CAMERA_FACING_FRONT}.
//     */
//    public int getCamera()
//    {
//        return cameraID;
//    }
//
//    /**
//     * Sets a Surface to show a preview of recorded media (video).
//     * You can call this method at any time and changes will take effect next time you call {@link #start()}.
//     */
//    public synchronized void setSurfaceView(SurfaceView view)
//    {
//        if (view == null) {
//            surfaceReady = false;
//            return;
//        }
//
//        surfaceView = view;
//        if (surfaceHolderCallback != null && surfaceView != null && surfaceView.getHolder() != null) {
//            surfaceView.getHolder().removeCallback(surfaceHolderCallback);
//        }
//
//        if (surfaceView.getHolder() != null) {
//            surfaceHolderCallback = new Callback()
//            {
//                @Override
//                public void surfaceDestroyed(SurfaceHolder holder)
//                {
//                    surfaceReady = false;
//                    stopPreview();
//                    Log.d(TAG, "Surface destroyed !");
//                }
//
//                @Override
//                public void surfaceCreated(SurfaceHolder holder)
//                {
//                    surfaceReady = true;
//                }
//
//                @Override
//                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height)
//                {
//                    Log.d(TAG, "Surface Changed !");
//                }
//            };
//            surfaceView.getHolder().addCallback(surfaceHolderCallback);
//            surfaceReady = true;
//        }
//    }
//
//    /**
//     * Turns the LED on or off if phone has one.
//     */
//    public synchronized void setFlashState(boolean state)
//    {
//        // If the camera has already been opened, we apply the change immediately
//        if (camera != null) {
//
//            if (streaming && currentMode == MODE_MEDIARECORDER_API) {
//                lockCamera();
//            }
//
//            Parameters parameters = camera.getParameters();
//
//            // We test if the phone has a flash
//            if (parameters.getFlashMode() == null) {
//                // The phone has no flash or the chosen camera can not toggle the flash
//                throw new RuntimeException("Can't turn the flash on !");
//            } else {
//                parameters.setFlashMode(state ? Parameters.FLASH_MODE_TORCH : Parameters.FLASH_MODE_OFF);
//                try {
//                    camera.setParameters(parameters);
//                    flashEnabled = state;
//                } catch (RuntimeException e) {
//                    flashEnabled = false;
//                    throw new RuntimeException("Can't turn the flash on !");
//                } finally {
//                    if (streaming && currentMode == MODE_MEDIARECORDER_API) {
//                        unlockCamera();
//                    }
//                }
//            }
//        } else {
//            flashEnabled = state;
//        }
//    }
//
//    /**
//     * Toggles the LED of the phone if it has one.
//     * You can get the current state of the flash with {@link VideoStream#getFlashState()}.
//     */
//    public synchronized void toggleFlash()
//    {
//        setFlashState(!flashEnabled);
//    }
//
//    /**
//     * Indicates whether or not the flash of the phone is on.
//     */
//    public boolean getFlashState()
//    {
//        return flashEnabled;
//    }
//
//    /**
//     * Sets the orientation of the preview.
//     *
//     * @param orientation The orientation of the preview
//     */
//    public void setPreviewOrientation(int orientation)
//    {
//        requestedOrientation = orientation;
//        updated = false;
//    }
//
//    /**
//     * Sets the configuration of the stream. You can call this method at any time
//     * and changes will take effect next time you call {@link #configure()}.
//     *
//     * @param videoQuality Quality of the stream
//     */
//    public void setVideoQuality(VideoQuality videoQuality)
//    {
//        Log.e(TAG, "videoQuality");
//        if (!requestedQuality.equals(videoQuality)) {
//            requestedQuality = videoQuality.clone();
//            updated = false;
//        }
//    }
//
//    /**
//     * Returns the currentQuality of the stream.
//     */
//    public VideoQuality getVideoQuality()
//    {
//        return requestedQuality;
//    }
//
//    /**
//     * Some data (SPS and PPS params) needs to be stored when {@link #getSessionDescription()} is called
//     *
//     * @param prefs The SharedPreferences that will be used to save SPS and PPS parameters
//     */
//    public void setPreferences(SharedPreferences prefs)
//    {
//        settings = prefs;
//    }
//
//    /**
//     * Configures the stream. You need to call this before calling {@link #getSessionDescription()}
//     * to apply your configuration of the stream.
//     */
//    public synchronized void configure() throws IllegalStateException, IOException
//    {
//        super.configure();
//        orientation = requestedOrientation;
//        currentQuality = requestedQuality;
//    }
//
//    /**
//     * Starts the stream.
//     * This will also open the camera and display the preview
//     * if {@link #startPreview()} has not already been called.
//     */
//    public synchronized void start() throws IllegalStateException, IOException
//    {
//        if (!previewStarted) cameraOpenedManually = false;
//        super.start();
//        Log.d(TAG, "Stream configuration: FPS: " + currentQuality.fps + " Width: " + currentQuality.resWidth + " Height: " + currentQuality.height);
//    }
//
//    /**
//     * Stops the stream.
//     */
//    public synchronized void stop()
//    {
//        if (camera != null) {
//            if (currentMode == MODE_MEDIACODEC_API) {
//                camera.setPreviewCallbackWithBuffer(null);
//            }
//            if (currentMode == MODE_MEDIACODEC_API_2) {
//                surfaceView.removeMediaCodecSurface();
//            }
//            super.stop();
//            // We need to restart the preview
//            if (!cameraOpenedManually) {
//                destroyCamera();
//            } else {
//                try {
//                    startPreview();
//                } catch (RuntimeException e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//    }
//
//    public synchronized void startPreview()
//            throws CameraInUseException,
//            InvalidSurfaceException,
//            RuntimeException
//    {
//
//        cameraOpenedManually = true;
//        if (!previewStarted) {
//            createCamera();
//            updateCamera();
//        }
//    }

    /**
     * Stops the preview.
     */
    public synchronized void stopPreview()
    {
        cameraOpenedManually = false;
        stop();
    }

//    /**
//     * Video encoding is done by a MediaRecorder.
//     */
//    protected void encodeWithMediaRecorder() throws IOException, ConfNotSupportedException
//    {
//
//        Log.d(TAG, "Video encoded using the MediaRecorder API");
//
//        // We need a local socket to forward data output by the camera to the packetizer
//        createSockets();
//
//        // Reopens the camera if needed
//        destroyCamera();
//        createCamera();
//
//        // The camera must be unlocked before the MediaRecorder can use it
//        unlockCamera();
//
//        try {
//            mediaRecorder = new MediaRecorder();
//            mediaRecorder.setCamera(camera);
//            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//            mediaRecorder.setVideoEncoder(videoEncoder);
//            mediaRecorder.setPreviewDisplay(surfaceView.getHolder().getSurface());
//            mediaRecorder.setVideoSize(currentQuality.resWidth, currentQuality.height);
//            mediaRecorder.setVideoFrameRate(currentQuality.fps);
//
//            // The bandwidth actually consumed is often above what was requested
//            mediaRecorder.setVideoEncodingBitRate((int) (currentQuality.bitrate * 0.8));
//
//            // We write the output of the camera in a local socket instead of a file !
//            // This one little trick makes streaming feasible quiet simply: data from the camera
//            // can then be manipulated at the other end of the socket
//            FileDescriptor fd = pipeAPI == PIPE_API_PFD ?
//                    parcelWrite.getFileDescriptor() : sender.getFileDescriptor();
//            mediaRecorder.setOutputFile(fd);
//
//            mediaRecorder.prepare();
//            mediaRecorder.start();
//        } catch (Exception e) {
//            throw new ConfNotSupportedException(e.getMessage());
//        }
//
//        InputStream is = pipeAPI == PIPE_API_PFD ?
//                new ParcelFileDescriptor.AutoCloseInputStream(parcelRead) :
//                receiver.getInputStream();
//
//        // This will skip the MPEG4 header if this step fails we can't stream anything :(
//        try {
//            byte buffer[] = new byte[4];
//            // Skip all atoms preceding mdat atom
//            while (!Thread.interrupted()) {
//                while (is.read() != 'm') ;
//                is.read(buffer, 0, 3);
//                if (buffer[0] == 'd' && buffer[1] == 'a' && buffer[2] == 't') break;
//            }
//        } catch (IOException e) {
//            Log.e(TAG, "Couldn't skip mp4 header :/");
//            stop();
//            throw e;
//        }
//
//        // The packetizer encapsulates the bit stream in an RTP stream and send it over the network
//        packetizer.setInputStream(is);
//        packetizer.start();
//
//        streaming = true;
//    }
//
//
//    /**
//     * Video encoding is done by a MediaCodec.
//     */
//    protected void encodeWithMediaCodec() throws RuntimeException, IOException
//    {
//        if (currentMode == MODE_MEDIACODEC_API_2) {
//            // Uses the method MediaCodec.createInputSurface to feed the encoder
//            encodeWithMediaCodecMethod2();
//        } else {
//            // Uses dequeueInputBuffer to feed the encoder
//            encodeWithMediaCodecMethod1();
//        }
//    }
//
//    /**
//     * Video encoding is done by a MediaCodec.
//     */
//    @SuppressLint("NewApi")
//    protected void encodeWithMediaCodecMethod1() throws RuntimeException, IOException
//    {
//
//        Log.d(TAG, "Video encoded using the MediaCodec API with a buffer");
//
//        // Updates the parameters of the camera if needed
//        createCamera();
//        updateCamera();
//
//        // Estimates the frame rate of the camera
//        measureFramerate();
//
//        // Starts the preview if needed
//        if (!previewStarted) {
//            try {
//                camera.startPreview();
//                previewStarted = true;
//            } catch (RuntimeException e) {
//                destroyCamera();
//                throw e;
//            }
//        }
//
//        EncoderDebugger debugger = EncoderDebugger.debug(settings, currentQuality.resWidth, currentQuality.height);
//        final NV21Convertor convertor = debugger.getNV21Convertor();
//
//        mediaCodec = MediaCodec.createByCodecName(debugger.getEncoderName());
//        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mimeType, currentQuality.resWidth, currentQuality.height);
//        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, currentQuality.bitrate);
//        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, currentQuality.fps);
//        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, debugger.getEncoderColorFormat());
//        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
//        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//        mediaCodec.start();
//
//        Camera.PreviewCallback callback = new Camera.PreviewCallback()
//        {
//            long now = System.nanoTime() / 1000, oldnow = now, i = 0;
//            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
//
//            @Override
//            public void onPreviewFrame(byte[] data, Camera camera)
//            {
////                Log.e(TAG, "inputBuffers: " + inputBuffers.length);
//                oldnow = now;
//                now = System.nanoTime() / 1000;
//                if (i++ > 3) {
//                    i = 0;
//                    //Log.d(TAG,"Measured: "+1000000L/(now-oldnow)+" fps.");
//                }
//                try {
//                    int bufferIndex = mediaCodec.dequeueInputBuffer(500000);
//                    if (bufferIndex >= 0) {
//                        inputBuffers[bufferIndex].clear();
//                        if (data == null) Log.e(TAG, "Symptom of the \"Callback buffer was to small\" problem...");
//                        else convertor.convert(data, inputBuffers[bufferIndex]);
//                        mediaCodec.queueInputBuffer(bufferIndex, 0, inputBuffers[bufferIndex].position(), now, 0);
//                    } else {
//                        Log.e(TAG, "No buffer available !");
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                } finally {
//                    camera.addCallbackBuffer(data);
//                }
//            }
//        };
//
//        for (int i = 0; i < 10; i++) camera.addCallbackBuffer(new byte[convertor.getBufferSize()]);
//        camera.setPreviewCallbackWithBuffer(callback);
//
//        // The packetizer encapsulates the bit stream in an RTP stream and send it over the network
//        packetizer.setInputStream(new MediaCodecInputStream(mediaCodec));
//        packetizer.start();
//
//        streaming = true;
//    }
//
//    /**
//     * Video encoding is done by a MediaCodec.
//     * But here we will use the buffer-to-surface method
//     */
//    @SuppressLint({"InlinedApi", "NewApi"})
//    protected void encodeWithMediaCodecMethod2() throws RuntimeException, IOException
//    {
//
//        Log.d(TAG, "Video encoded using the MediaCodec API with a surface");
//
//        // Updates the parameters of the camera if needed
//        createCamera();
//        updateCamera();
//
//        // Estimates the frame rate of the camera
//        measureFramerate();
//
//        EncoderDebugger debugger = EncoderDebugger.debug(settings, currentQuality.resWidth, currentQuality.height);
//
//        mediaCodec = MediaCodec.createByCodecName(debugger.getEncoderName());
//        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", currentQuality.resWidth, currentQuality.height);
//        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, currentQuality.bitrate);
//        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, currentQuality.fps);
//        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
//        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//        Surface surface = mediaCodec.createInputSurface();
//        surfaceView.addMediaCodecSurface(surface);
//        mediaCodec.start();
//
//        // The packetizer encapsulates the bit stream in an RTP stream and send it over the network
//        packetizer.setInputStream(new MediaCodecInputStream(mediaCodec));
//        packetizer.start();
//
//        streaming = true;
//    }
//
//    /**
//     * Returns a description of the stream using SDP.
//     * This method can only be called after {@link Stream#configure()}.
//     *
//     * @throws IllegalStateException Thrown when {@link Stream#configure()} wa not called.
//     */
//    public abstract String getSessionDescription() throws IllegalStateException;
//
//    /**
//     * Opens the camera in a new Looper thread so that the preview callback is not called from the main thread
//     * If an exception is thrown in this Looper thread, we bring it back into the main thread.
//     *
//     * @throws RuntimeException Might happen if another app is already using the camera.
//     */
//    private void openCamera() throws RuntimeException
//    {
//        final Semaphore lock = new Semaphore(0);
//        final RuntimeException[] exception = new RuntimeException[1];
//        cameraThread = new Thread(new Runnable()
//        {
//            @Override
//            public void run()
//            {
//                Looper.prepare();
//                cameraLooper = Looper.myLooper();
//                try {
//                    camera = Camera.open(cameraID);
//                } catch (RuntimeException e) {
//                    exception[0] = e;
//                } finally {
//                    lock.release();
//                    Looper.loop();
//                }
//            }
//        });
//        cameraThread.start();
//        lock.acquireUninterruptibly();
//        if (exception[0] != null) throw new CameraInUseException(exception[0].getMessage());
//    }
//
//    protected synchronized void createCamera() throws RuntimeException
//    {
//        if (surfaceView == null)
//            throw new InvalidSurfaceException("Invalid surface !");
//        if (surfaceView.getHolder() == null || !surfaceReady)
//            throw new InvalidSurfaceException("Invalid surface !");
//
//        if (camera == null) {
//            openCamera();
//            updated = false;
//            unlocked = false;
//            camera.setErrorCallback(new Camera.ErrorCallback()
//            {
//                @Override
//                public void onError(int error, Camera camera)
//                {
//                    // On some phones when trying to use the camera facing front the media server will die
//                    // Whether or not this callback may be called really depends on the phone
//                    if (error == Camera.CAMERA_ERROR_SERVER_DIED) {
//                        // In this case the application must release the camera and instantiate a new one
//                        Log.e(TAG, "Media server died !");
//                        // We don't know in what thread we are so stop needs to be synchronized
//                        cameraOpenedManually = false;
//                        stop();
//                    } else {
//                        Log.e(TAG, "Error unknown with the camera: " + error);
//                    }
//                }
//            });
//
//            try {
//                // If the phone has a flash, we turn it on/off according to flashEnabled
//                // setRecordingHint(true) is a very nice optimization if you plane to only use the Camera for recording
//                Parameters parameters = camera.getParameters();
//                if (parameters.getFlashMode() != null) {
//                    parameters.setFlashMode(flashEnabled ? Parameters.FLASH_MODE_TORCH : Parameters.FLASH_MODE_OFF);
//                }
//                parameters.setRecordingHint(true);
//                camera.setParameters(parameters);
//                camera.setDisplayOrientation(orientation);
//
//                try {
//                    if (currentMode == MODE_MEDIACODEC_API_2) {
//                        surfaceView.startGLThread();
//                        camera.setPreviewTexture(surfaceView.getSurfaceTexture());
//                    } else {
//                        camera.setPreviewDisplay(surfaceView.getHolder());
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    throw new InvalidSurfaceException("Invalid surface !");
//                }
//
//            } catch (RuntimeException e) {
//                e.printStackTrace();
//                destroyCamera();
//                throw e;
//            }
//
//        }
//    }
//
//    protected synchronized void destroyCamera()
//    {
//        if (camera != null) {
//            if (streaming) super.stop();
//            lockCamera();
//            camera.stopPreview();
//            try {
//                camera.release();
//            } catch (Exception e) {
//                Log.e(TAG, e.getMessage() != null ? e.getMessage() : "unknown error");
//            }
//            camera = null;
//            cameraLooper.quit();
//            unlocked = false;
//            previewStarted = false;
//        }
//    }
//
//    protected synchronized void updateCamera() throws RuntimeException
//    {
//
//        // The camera is already correctly configured
//        if (updated) return;
//
//        if (previewStarted) {
//            previewStarted = false;
//            camera.stopPreview();
//        }
//
//        Parameters parameters = camera.getParameters();
//        currentQuality = VideoQuality.closestSupportedResolution(parameters, currentQuality);
//        Log.e(TAG, "currentQuality: " + currentQuality);
//        int[] max = VideoQuality.maximumSupportedFramerate(parameters);
//
//        double ratio = (double) currentQuality.resWidth / (double) currentQuality.height;
//        surfaceView.requestAspectRatio(ratio);
//
//        parameters.setPreviewFormat(cameraImageFormat);
//        parameters.setPreviewSize(currentQuality.resWidth, currentQuality.height);
//        parameters.setPreviewFpsRange(max[0], max[1]);
//
//        try {
//            camera.setParameters(parameters);
//            camera.setDisplayOrientation(orientation);
//            camera.startPreview();
//            previewStarted = true;
//            updated = true;
//        } catch (RuntimeException e) {
//            destroyCamera();
//            throw e;
//        }
//    }
//
//    protected void lockCamera()
//    {
//        if (unlocked) {
//            Log.d(TAG, "Locking camera");
//            try {
//                camera.reconnect();
//            } catch (Exception e) {
//                Log.e(TAG, e.getMessage());
//            }
//            unlocked = false;
//        }
//    }
//
//    protected void unlockCamera()
//    {
//        if (!unlocked) {
//            Log.d(TAG, "Unlocking camera");
//            try {
//                camera.unlock();
//            } catch (Exception e) {
//                Log.e(TAG, e.getMessage());
//            }
//            unlocked = true;
//        }
//    }
//
//
//    /**
//     * Computes the average frame rate at which the preview callback is called.
//     * We will then use this average frame rate with the MediaCodec.
//     * Blocks the thread in which this function is called.
//     */
//    private void measureFramerate()
//    {
//        final Semaphore lock = new Semaphore(0);
//
//        final Camera.PreviewCallback callback = new Camera.PreviewCallback()
//        {
//            int i = 0, t = 0;
//            long now, oldNow, count = 0;
//
//            @Override
//            public void onPreviewFrame(byte[] data, Camera camera)
//            {
//                i++;
//                now = System.nanoTime() / 1000;
//                if (i > 3) {
//                    t += now - oldNow;
//                    count++;
//                }
//                if (i > 20) {
//                    currentQuality.fps = (int) (1000000 / (t / count) + 1);
//                    lock.release();
//                }
//                oldNow = now;
//            }
//        };
//
//        camera.setPreviewCallback(callback);
//
//        try {
//            lock.tryAcquire(2, TimeUnit.SECONDS);
//            Log.d(TAG, "Actual fps: " + currentQuality.fps);
//            if (settings != null) {
//                Editor editor = settings.edit();
//                editor.putInt(PREF_PREFIX + "fps" + requestedQuality.fps + "," + cameraImageFormat + "," + requestedQuality.resWidth + requestedQuality.height, currentQuality.fps);
//                editor.commit();
//            }
//        } catch (InterruptedException e) {}
//
//        camera.setPreviewCallback(null);
//    }
}
