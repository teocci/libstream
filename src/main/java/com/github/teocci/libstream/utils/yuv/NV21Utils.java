package com.github.teocci.libstream.utils.yuv;

/**
 * NV21 yuv pixel format
 * YUV 4:2:0 image with a plane of 8 bit Y samples followed by an interleaved V/U plane
 * containing 8 bit 2x2 subsampled chroma samples.
 * The same as NV12 except the interleave order of U and V is reversed.
 * NV21: YYYYYYYY VU VU =>YUV420SP
 * <p>
 * Created by teocci.
 *
 * @author teocci@yandex.com on 2019-May-28
 */
public class NV21Utils
{
    private static byte[] preAllocatedBufferRotate;
    private static byte[] preAllocatedBufferColor;

    public static void preAllocateBuffers(int length)
    {
        preAllocatedBufferRotate = new byte[length];
        preAllocatedBufferColor = new byte[length];
    }

    /**
     * COLOR_FormatYUV420Planar is I420, which is identical to YV12 except that the U and V plane order is reversed.
     * So, we just have to reverse U and V to convert it.
     * I420: YYYYYYYY UU VV => YUV420P
     *
     * @param input  NV21 yuv pixel format buffer
     * @param width  width of the frame
     * @param height height of the frame
     * @return an I420 yuv pixel format buffer
     */
    public static byte[] toI420(byte[] input, int width, int height)
    {
        final int frameSize = width * height;
        final int qFrameSize = frameSize / 4;

        System.arraycopy(input, 0, preAllocatedBufferColor, 0, frameSize); // Y

        for (int i = 0; i < qFrameSize; i++) {
            int index = frameSize + i * 2;
            int pos = frameSize + i;
            preAllocatedBufferColor[pos] = input[index + 1]; // Cb (U)
            preAllocatedBufferColor[pos + qFrameSize] = input[index]; // Cr (V)
        }
        return preAllocatedBufferColor;
    }

    /**
     * COLOR_FormatYUV420PackedPlanar is identical to YV12
     * YV12: YYYYYYYY VV UU => YUV420PP
     *
     * @param input  a NV21 yuv pixel format buffer
     * @param width  the width of the frame
     * @param height the height of the frame
     * @return YV12 yuv pixel format buffer
     */
    public static byte[] toYV12(byte[] input, int width, int height)
    {
        final int frameSize = width * height;
        final int qFrameSize = frameSize / 4;

        System.arraycopy(input, 0, preAllocatedBufferColor, 0, frameSize); // Y

        for (int i = 0; i < qFrameSize; i++) {
            int index = frameSize + i * 2;
            int pos = frameSize + i;

            preAllocatedBufferColor[pos + qFrameSize] = input[index + 1]; // Cb (U)
            preAllocatedBufferColor[pos] = input[index]; // Cr (V)
        }

        return preAllocatedBufferColor;
    }


    /**
     * COLOR_FormatYUV420SemiPlanar is NV12
     * NV21 is same as NV12 except the interleave order of U and V is reversed.
     * We convert by putting the corresponding U and V bytes together (interleaved).
     * https://stackoverflow.com/questions/15739684
     * NV12: YYYYYYYY UV UV => YUV420SP
     *
     * @param input  NV21 yuv pixel format buffer
     * @param width  the width of the frame
     * @param height the height of the frame
     * @return NV12 yuv pixel format buffer
     */
    public static byte[] toNV12(byte[] input, int width, int height)
    {
        final int frameSize = width * height;
        final int qFrameSize = frameSize / 4;

        System.arraycopy(input, 0, preAllocatedBufferColor, 0, frameSize); // Y

        for (int i = 0; i < qFrameSize; i++) {
            int index = frameSize + i * 2;
            preAllocatedBufferColor[index] = input[index + 1]; // Cb (U)
            preAllocatedBufferColor[index + 1] = input[index]; // Cr (V)
        }

        return preAllocatedBufferColor;
    }

    /**
     * @param yuv    frame buffer
     * @param width  the width of the frame
     * @param height the height of the frame
     * @return an ARGB frame buffer
     */
    public static int[] toARGB(byte[] yuv, int width, int height)
    {
        final int frameSize = width * height;
        int[] argb = new int[frameSize];
        final int ii = 0;
        final int ij = 0;
        final int di = +1;
        final int dj = +1;
        int index = 0;

        for (int i = 0, ih = ii; i < height; ++i, ih += di) {
            for (int j = 0, iw = ij; j < width; ++j, iw += dj) {
                int y = (0xff & ((int) yuv[ih * width + iw]));
                int v = (0xff & ((int) yuv[frameSize + (ih >> 1) * width + (iw & ~1)]));
                int u = (0xff & ((int) yuv[frameSize + (ih >> 1) * width + (iw & ~1) + 1]));
                y = y < 16 ? 16 : y;

                int r = (int) (1.164f * (y - 16) + 1.596f * (v - 128));
                int g = (int) (1.164f * (y - 16) - 0.813f * (v - 128) - 0.391f * (u - 128));
                int b = (int) (1.164f * (y - 16) + 2.018f * (u - 128));

                r = r < 0 ? 0 : (r > 255 ? 255 : r);
                g = g < 0 ? 0 : (g > 255 ? 255 : g);
                b = b < 0 ? 0 : (b > 255 ? 255 : b);

                argb[index++] = 0xff000000 | (r << 16) | (g << 8) | b;
            }
        }

        return argb;
    }


    public static byte[] rotate90(byte[] data, int imageWidth, int imageHeight)
    {
        // Rotate the Y-Luma
        int i = 0;
        for (int x = 0; x < imageWidth; x++) {
            for (int y = imageHeight - 1; y >= 0; y--) {
                preAllocatedBufferRotate[i++] = data[y * imageWidth + x];
            }
        }

        // Rotate the U and V color components
        int size = imageWidth * imageHeight;
        i = size * 3 / 2 - 1;
        for (int x = imageWidth - 1; x > 0; x = x - 2) {
            for (int y = 0; y < imageHeight / 2; y++) {
                preAllocatedBufferRotate[i--] = data[size + (y * imageWidth) + x];
                preAllocatedBufferRotate[i--] = data[size + (y * imageWidth) + (x - 1)];
            }
        }

        return preAllocatedBufferRotate;
    }

    public static byte[] rotate180(byte[] data, int imageWidth, int imageHeight)
    {
        int size = imageWidth * imageHeight;
        int count = 0;
        for (int i = size - 1; i >= 0; i--) {
            preAllocatedBufferRotate[count++] = data[i];
        }

        for (int i = size * 3 / 2 - 1; i >= size; i -= 2) {
            preAllocatedBufferRotate[count++] = data[i - 1];
            preAllocatedBufferRotate[count++] = data[i];
        }

        return preAllocatedBufferRotate;
    }

    public static byte[] rotate270(byte[] data, int imageWidth, int imageHeight)
    {
        // Rotate the Y-Luma
        int i = 0;
        for (int x = imageWidth - 1; x >= 0; x--) {
            for (int y = 0; y < imageHeight; y++) {
                preAllocatedBufferRotate[i++] = data[y * imageWidth + x];
            }
        }

        // Rotate the U and V color components
        i = imageWidth * imageHeight;
        int uvHeight = imageHeight / 2;
        for (int x = imageWidth - 1; x >= 0; x -= 2) {
            for (int y = imageHeight; y < uvHeight + imageHeight; y++) {
                preAllocatedBufferRotate[i++] = data[y * imageWidth + x - 1];
                preAllocatedBufferRotate[i++] = data[y * imageWidth + x];
            }
        }

        return preAllocatedBufferRotate;
    }

    public static byte[] rotatePixels(byte[] input, int width, int height, int rotation)
    {
        byte[] output = new byte[input.length];

        boolean swap = (rotation == 90 || rotation == 270);
        boolean yflip = (rotation == 90 || rotation == 180);
        boolean xflip = (rotation == 270 || rotation == 180);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int xo = x, yo = y;
                int w = width, h = height;
                int xi = xo, yi = yo;
                if (swap) {
                    xi = w * yo / h;
                    yi = h * xo / w;
                }
                if (yflip) {
                    yi = h - yi - 1;
                }
                if (xflip) {
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

    public static byte[] mirror(byte[] input, int width, int height)
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
}
