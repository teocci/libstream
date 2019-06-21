package com.github.teocci.libstream.utils.yuv;

/**
 * YV12 is exactly like I420, but the order of the U and V planes is reversed.
 * In the name, "YV" refers to the plane order: Y, then V (then U).
 * "12" refers to the pixel depth: 12-bits per pixel as for I420.
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-May-28
 */
public class YV12Utils
{
    private static byte[] preAllocatedBufferRotate;
    private static byte[] preAllocatedBufferColor;

    public static void preAllocateBuffers(int length)
    {
        preAllocatedBufferRotate = new byte[length];
        preAllocatedBufferColor = new byte[length];
    }

    /**
     * COLOR_FormatYUV420Planar is I420 which is like YV12, but with U and V reversed.
     * So we just have to reverse U and V.
     * <p>
     * I420: YYYYYYYY UU VV => YUV420P
     * Reference https://stackoverflow.com/questions/15739684
     *
     *
     * @param input  YV12 yuv pixel format buffer
     * @param width  width of the frame
     * @param height height of the frame
     * @return I420 yuv pixel format buffer
     */
    public static byte[] toI420(byte[] input, int width, int height)
    {
        final int frameSize = width * height;
        final int qFrameSize = frameSize / 4;
        final int pos = frameSize + qFrameSize;

        System.arraycopy(input, 0, preAllocatedBufferColor, 0, frameSize); // Y
        System.arraycopy(input, pos, preAllocatedBufferColor, frameSize, qFrameSize); // Cb (U)
        System.arraycopy(input, frameSize, preAllocatedBufferColor, pos, qFrameSize); // Cr (V)

        return preAllocatedBufferColor;
    }

    /**
     * COLOR_FormatYUV420SemiPlanar is NV12
     * We convert by putting the corresponding U and V bytes together (interleaved).
     * <p>
     * NV12: YYYYYYYY UV UV => YUV420SP
     * Reference https://stackoverflow.com/questions/15739684
     *
     * @param input  YV12 yuv pixel format buffer
     * @param width  width of the frame
     * @param height height of the frame
     * @return NV12 yuv pixel format buffer
     */
    public static byte[] toNV12(byte[] input, int width, int height)
    {
        final int frameSize = width * height;
        final int qFrameSize = frameSize / 4;

        System.arraycopy(input, 0, preAllocatedBufferColor, 0, frameSize); // Y
        for (int i = 0; i < qFrameSize; i++) {
            int index = frameSize + i * 2;
            preAllocatedBufferColor[index] = input[frameSize + i + qFrameSize]; // Cb (U)
            preAllocatedBufferColor[index + 1] = input[frameSize + i]; // Cr (V)
        }

        return preAllocatedBufferColor;
    }

    /**
     * COLOR_TI_FormatYUV420PackedSemiPlanar is NV21
     * We convert by putting the corresponding U and V bytes together (interleaved).
     * Reference https://stackoverflow.com/questions/15739684
     *
     * @param input  YV12 yuv pixel format buffer
     * @param width  width of the frame
     * @param height height of the frame
     * @return NV21 yuv pixel format buffer
     */
    public static byte[] toNV21(byte[] input, int width, int height)
    {
        final int frameSize = width * height;
        final int qFrameSize = frameSize / 4;

        System.arraycopy(input, 0, preAllocatedBufferColor, 0, frameSize); // Y

        for (int i = 0; i < qFrameSize; i++) {
            int index = frameSize + i * 2;
            preAllocatedBufferColor[index + 1] = input[frameSize + i + qFrameSize]; // Cb (U)
            preAllocatedBufferColor[index] = input[frameSize + i]; // Cr (V)
        }

        return preAllocatedBufferColor;
    }

    public static byte[] rotate90(byte[] data, int imageWidth, int imageHeight)
    {
        // Rotate the Y luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                preAllocatedBufferRotate[i++] = data[y * imageWidth + x];
            }
        }
        final int size = imageWidth * imageHeight;
        final int colorSize = size / 4;
        final int colorHeight = colorSize / imageWidth;
        // Rotate the U and V color components
        for (int x = 0; x < imageWidth / 2; x++) {
            for (int y = colorHeight - 1; y >= 0; y--) {
                //V
                preAllocatedBufferRotate[i + colorSize] =
                        data[colorSize + size + (imageWidth * y) + x + (imageWidth / 2)];
                preAllocatedBufferRotate[i + colorSize + 1] = data[colorSize + size + (imageWidth * y) + x];
                //U
                preAllocatedBufferRotate[i++] = data[size + (imageWidth * y) + x + (imageWidth / 2)];
                preAllocatedBufferRotate[i++] = data[size + (imageWidth * y) + x];
            }
        }

        return preAllocatedBufferRotate;
    }

    public static byte[] rotate180(byte[] data, int imageWidth, int imageHeight)
    {
        int count = 0;
        final int size = imageWidth * imageHeight;
        for (int i = size - 1; i >= 0; i--) {
            preAllocatedBufferRotate[count++] = data[i];
        }
        final int midColorSize = size / 4;
        //U
        for (int i = size + midColorSize - 1; i >= size; i--) {
            preAllocatedBufferRotate[count++] = data[i];
        }
        //V
        for (int i = data.length - 1; i >= imageWidth * imageHeight + midColorSize; i--) {
            preAllocatedBufferRotate[count++] = data[i];
        }

        return preAllocatedBufferRotate;
    }

    public static byte[] rotate270(byte[] data, int imageWidth, int imageHeight)
    {
        // Rotate the Y luma
        int i = 0;
        for (int x = imageWidth - 1; x >= 0; x--) {
            for (int y = 0; y < imageHeight; y++) {
                preAllocatedBufferRotate[i++] = data[y * imageWidth + x];
            }
        }

        // Rotate the U and V color components
        final int size = imageWidth * imageHeight;
        final int colorSize = size / 4;
        final int colorHeight = colorSize / imageWidth;

        for (int x = 0; x < imageWidth / 2; x++) {
            for (int y = 0; y < colorHeight; y++) {
                //V
                int colorConstant = colorSize + size + (imageWidth * y) - x;
                preAllocatedBufferRotate[i + colorSize] =
                        data[colorConstant + (imageWidth / 2) - 1];
                preAllocatedBufferRotate[i + colorSize + 1] =
                        data[colorConstant + imageWidth - 1];
                //U
                preAllocatedBufferRotate[i++] = data[size + (imageWidth * y) - x + (imageWidth / 2) - 1];
                preAllocatedBufferRotate[i++] = data[size + (imageWidth * y) - x + imageWidth - 1];
            }
        }

        return preAllocatedBufferRotate;
    }
}
