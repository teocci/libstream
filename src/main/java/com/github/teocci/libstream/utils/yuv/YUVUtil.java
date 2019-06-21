package com.github.teocci.libstream.utils.yuv;

import android.graphics.Bitmap;
import android.media.MediaCodecInfo;
import android.os.Environment;

import com.github.teocci.libstream.enums.FormatVideoEncoder;
import com.github.teocci.libstream.input.video.Frame;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by teocci.
 * <p>
 * Example YUV images 4x4 px.
 * <p>
 * NV21 example:
 * <p>
 * Y1   Y2   Y3   Y4
 * Y5   Y6   Y7   Y8
 * Y9   Y10  Y11  Y12
 * Y13  Y14  Y15  Y16
 * U1   V1   U2   V2
 * U3   V3   U4   V4
 * <p>
 * <p>
 * YV12 example:
 * <p>
 * Y1   Y2   Y3   Y4
 * Y5   Y6   Y7   Y8
 * Y9   Y10  Y11  Y12
 * Y13  Y14  Y15  Y16
 * U1   U2   U3   U4
 * V1   V2   V3   V4
 * <p>
 * <p>
 * YUV420 planar example (I420):
 * <p>
 * Y1   Y2   Y3   Y4
 * Y5   Y6   Y7   Y8
 * Y9   Y10  Y11  Y12
 * Y13  Y14  Y15  Y16
 * V1   V2   V3   V4
 * U1   U2   U3   U4
 * <p>
 * <p>
 * YUV420 semi planar example (NV12):
 * <p>
 * Y1   Y2   Y3   Y4
 * Y5   Y6   Y7   Y8
 * Y9   Y10  Y11  Y12
 * Y13  Y14  Y15  Y16
 * V1   U1   V2   U2
 * V3   U3   V4   U4
 * <p>
 * https://wiki.videolan.org/YUV/#I420
 * https://www.fourcc.org/yuv.php
 *
 * @author teocci@yandex.com on 2017-Jan-14
 */

public class YUVUtil
{
    public static void preAllocateBuffers(int length)
    {
        NV21Utils.preAllocateBuffers(length);
        YV12Utils.preAllocateBuffers(length);
    }

    /**
     * Converts a NV21 frame buffer to the format requested
     *
     * @param input  NV21 yuv pixel format buffer
     * @param width  width of the frame
     * @param height height of the frame
     * @param format yuv pixel format to be converted
     * @return converted yuv pixel format buffer
     */
    public static byte[] NV21toYUV420byColor(byte[] input, int width, int height, FormatVideoEncoder format)
    {
        switch (format) {
            case YUV420PLANAR:
                // YUV420sP is like I420
                return NV21Utils.toI420(input, width, height);
            case YUV420SEMIPLANAR:
                // YUV420SP is like NV12
                return NV21Utils.toNV12(input, width, height);
            case YUV420PACKEDPLANAR:
                // YUV420PP is like YV12
                return NV21Utils.toYV12(input, width, height);
            case YUV420PACKEDSEMIPLANAR:
                // YUV420PSP and NV21 are the same
                return input;
            default:
                return null;
        }
    }

    /**
     * Converts a YV12 frame buffer to the format requested
     *
     * @param input  YV12 yuv pixel format buffer
     * @param width  width of the frame
     * @param height height of the frame
     * @param format yuv pixel format to be converted
     * @return converted yuv pixel format buffer
     */
    public static byte[] YV12toYUV420byColor(byte[] input, int width, int height, FormatVideoEncoder format)
    {
        switch (format) {
            case YUV420PLANAR:
                // YUV420P is like I420
                return YV12Utils.toI420(input, width, height);
            case YUV420SEMIPLANAR:
                // YUV420SP is like NV12
                return YV12Utils.toNV12(input, width, height);
            case YUV420PACKEDPLANAR:
                // YUV420PP is like YV12
                return input;
            case YUV420PACKEDSEMIPLANAR:
                // YUV420PSP and NV21 are the same
                return YV12Utils.toNV21(input, width, height);
            default:
                return null;
        }
    }

    /**
     * @param data     frame buffer
     * @param width    width of the frame
     * @param height   height of the frame
     * @param rotation angle of rotation
     * @return rotated frame buffer
     */
    public static byte[] rotateNV21(byte[] data, int width, int height, int rotation)
    {
        switch (rotation) {
            case 0:
                return data;
            case 90:
                return NV21Utils.rotate90(data, width, height);
            case 180:
                return NV21Utils.rotate180(data, width, height);
            case 270:
                return NV21Utils.rotate270(data, width, height);
            default:
                return null;
        }
    }

    /**
     * @param data     frame buffer
     * @param width    width of the frame
     * @param height   height of the frame
     * @param rotation angle of rotation
     * @return rotated frame buffer
     */
    public static byte[] rotateYV12(byte[] data, int width, int height, int rotation)
    {
        switch (rotation) {
            case 0:
                return data;
            case 90:
                return YV12Utils.rotate90(data, width, height);
            case 180:
                return YV12Utils.rotate180(data, width, height);
            case 270:
                return YV12Utils.rotate270(data, width, height);
            default:
                return null;
        }
    }


    /**
     * @param frame       frame buffer
     * @param width       width of the frame
     * @param height      height of the frame
     * @param orientation orientation of the frame
     * @return a bitmap
     */
    public static Bitmap frameToBitmap(Frame frame, int width, int height, int orientation)
    {
        int w = (orientation == 90 || orientation == 270) ? height : width;
        int h = (orientation == 90 || orientation == 270) ? width : height;

        int[] argb = NV21Utils.toARGB(rotateNV21(frame.getBuffer(), width, height, orientation), w, h);

        return Bitmap.createBitmap(argb, w, h, Bitmap.Config.ARGB_8888);
    }

    public static byte[] ARGBtoNV12(int[] input, int width, int height)
    {
        // COLOR_FormatYUV420SemiPlanar is NV12
        final int frameSize = width * height;
        byte[] yuv420sp = new byte[width * height * 3 / 2];
        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                a = (input[index] & 0xff000000) >> 24; // a is not used obviously
                R = (input[index] & 0xff0000) >> 16;
                G = (input[index] & 0xff00) >> 8;
                B = (input[index] & 0xff);

                // Well known RGB to YUV algorithm
                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
                // meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
                // pixel AND every other scan-line.
                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                }

                index++;
            }
        }

        return yuv420sp;
    }

    public static byte[] CropYUV(int srcFormat, byte[] srcYUV, int srcWidth, int srcHeight, int dstWidth, int dstHeight)
    {
        if (srcYUV == null) return null;
        byte[] dstYUV;

        // Simple implementation: copy the corner
        if (srcWidth == dstWidth && srcHeight == dstHeight) {
            dstYUV = srcYUV;
        } else {
            dstYUV = new byte[(int) (dstWidth * dstHeight * 1.5)];
            switch (srcFormat) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar: // I420
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar: // YV12
                    // Copy Y
                    int srcYOffset = 0;
                    int dstYOffset = 0;
                    for (int i = 0; i < dstHeight; i++) {
                        System.arraycopy(srcYUV, srcYOffset, dstYUV, dstYOffset, dstWidth);
                        srcYOffset += srcWidth;
                        dstYOffset += dstWidth;
                    }

                    // Copy u
                    int srcUOffset = 0;
                    int dstUOffset = 0;
                    srcYOffset = srcWidth * srcHeight;
                    dstYOffset = dstWidth * dstHeight;
                    for (int i = 0; i < dstHeight / 2; i++) {
                        int srcPos = srcYOffset + srcUOffset;
                        int dstPos = dstYOffset + dstUOffset;
                        System.arraycopy(srcYUV, srcPos, dstYUV, dstPos, dstWidth / 2);
                        srcUOffset += srcWidth / 2;
                        dstUOffset += dstWidth / 2;
                    }

                    // Copy v
                    int srcVOffset = 0;
                    int dstVOffset = 0;
                    srcUOffset = srcWidth * srcHeight + srcWidth * srcHeight / 4;
                    dstUOffset = dstWidth * dstHeight + dstWidth * dstHeight / 4;
                    for (int i = 0; i < dstHeight / 2; i++) {
                        int srcPos = srcUOffset + srcVOffset;
                        int dstPos = dstUOffset + dstVOffset;
                        System.arraycopy(srcYUV, srcPos, dstYUV, dstPos, dstWidth / 2);
                        srcVOffset += srcWidth / 2;
                        dstVOffset += dstWidth / 2;
                    }

                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar: // NV12
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar: // NV21
                case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                case MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar:
                    // copy Y
                    srcYOffset = 0;
                    dstYOffset = 0;
                    for (int i = 0; i < dstHeight; i++) {
                        System.arraycopy(srcYUV, srcYOffset, dstYUV, dstYOffset, dstWidth);
                        srcYOffset += srcWidth;
                        dstYOffset += dstWidth;
                    }

                    // copy u and v
                    srcUOffset = 0;
                    dstUOffset = 0;
                    srcYOffset = srcWidth * srcHeight;
                    dstYOffset = dstWidth * dstHeight;
                    for (int i = 0; i < dstHeight / 2; i++) {
                        int srcPos = srcYOffset + srcUOffset;
                        int dstPos = dstYOffset + dstUOffset;
                        System.arraycopy(srcYUV, srcPos, dstYUV, dstPos, dstWidth);
                        srcUOffset += srcWidth;
                        dstUOffset += dstWidth;
                    }

                    break;
                default:
                    dstYUV = null;
                    break;
            }
        }

        return dstYUV;
    }

    public void dumpYUVData(byte[] buffer, int len, String name)
    {
        File f = new File(Environment.getExternalStorageDirectory().getPath() + "/tmp/", name);
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            out.write(buffer);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] rotatePixelsNV21(byte[] input, int width, int height, int rotation)
    {
        byte[] output = new byte[input.length];

        boolean swap = (rotation == 90 || rotation == 270);
        boolean yFlip = (rotation == 90 || rotation == 180);
        boolean xFlip = (rotation == 270 || rotation == 180);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int xo = x, yo = y;
                int w = width, h = height;
                int xi = xo, yi = yo;
                if (swap) {
                    xi = w * yo / h;
                    yi = h * xo / w;
                }
                if (yFlip) {
                    yi = h - yi - 1;
                }
                if (xFlip) {
                    xi = w - xi - 1;
                }
                output[w * yo + xo] = input[w * yi + xi];
                int fs = w * h;
                int qs = (fs >> 2);
                xi = (xi >> 1);
                yi = (yi >> 1);
                xo = (xo >> 1);
                yo = (yo >> 1);
                w = (w >> 1);
                h = (h >> 1);
                // adjust for interleave here
                int ui = fs + (w * yi + xi) * 2;
                int uo = fs + (w * yo + xo) * 2;
                // and here
                int vi = ui + 1;
                int vo = uo + 1;
                output[uo] = input[ui];
                output[vo] = input[vi];
            }
        }
        return output;
    }

    public static byte[] mirrorNV21(byte[] input, int width, int height)
    {
        byte[] output = new byte[input.length];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int xo = x, yo = y;
                int w = width, h = height;
                int xi = xo, yi = yo;
                yi = h - yi - 1;
                output[w * yo + xo] = input[w * yi + xi];
                int fs = w * h;
                int qs = (fs >> 2);
                xi = (xi >> 1);
                yi = (yi >> 1);
                xo = (xo >> 1);
                yo = (yo >> 1);
                w = (w >> 1);
                h = (h >> 1);

                // Adjust for interleave here
                int ui = fs + (w * yi + xi) * 2;
                int uo = fs + (w * yo + xo) * 2;
                // And here
                int vi = ui + 1;
                int vo = uo + 1;
                output[uo] = input[ui];
                output[vo] = input[vi];
            }
        }

        return output;
    }


    /**
     * For the vbuffer for YV12(android YUV), @see below:
     * https://developer.android.com/reference/android/hardware/Camera.Parameters.html#setPreviewFormat(int)
     * https://developer.android.com/reference/android/graphics/ImageFormat.html#YV12
     */
    public static int getYuvBufferSize(int width, int height)
    {
        // stride = ALIGN(width, 16)
        // ySize = stride * height
        // cStride = ALIGN(stride/2, 16)
        // cSize = cStride * height/2
        // size = ySize + cSize * 2
        int stride = (int) Math.ceil(width / 16.0) * 16;
        int ySize = stride * height;
        int cStride = (int) Math.ceil(width / 32.0) * 16;
        int cSize = cStride * height / 2;
        return ySize + cSize * 2;
    }

    public static byte[] bitmapToNV21(int inputWidth, int inputHeight, Bitmap bitmap)
    {
        int[] argb = new int[inputWidth * inputHeight];
        bitmap.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);
        byte[] yuv = ARGBtoNV12(argb, inputWidth, inputHeight);
        bitmap.recycle();
        return yuv;
    }
}