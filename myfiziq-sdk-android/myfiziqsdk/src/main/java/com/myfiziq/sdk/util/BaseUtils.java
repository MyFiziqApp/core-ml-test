package com.myfiziq.sdk.util;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import androidx.annotation.Nullable;
import timber.log.Timber;

/**
 * @hide
 */
public class BaseUtils
{
    final static int BUFFER_SIZE = 10240;

    final static char enctab[] = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', //00..12
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', //13..25
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', //26..38
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', //39..51
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '!', '#', '$', //52..64
            '%', '&', '(', ')', '*', '+', ',', '.', '/', ':', ';', '-', '=', //65..77
            '\\','?', '@', '[', ']', '^', '_', '`', '{', '|', '}', '~', '\'' //78..90
    };

    final static char enctab_str[] = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', //00..12
            'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', //13..25
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', //26..38
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', //39..51
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '!', '#', '$', //52..64
            '%', '&', '(', ')', '*', '+', ',', '.', '/', ':', ';', '<', '=', //65..77
            '>', '?', '@', '[', ']', '^', '_', '`', '{', '|', '}', '~', ' '  //78..90
    };

    final static char dectab[] = {
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //000..015
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //016..031
            91, 62, 91, 63, 64, 65, 66, 90, 67, 68, 69, 70, 71, 76, 72, 73, //032..047 // @34: ", @39: ', @45: -
            52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 74, 75, 91, 77, 91, 79, //048..063 // @60: <, @62: >
            80,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, //064..079
            15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 81, 78, 82, 83, 84, //080..095 // @92: slash
            85, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, //096..111
            41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 86, 87, 88, 89, 91, //112..127
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //128..143
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //144..159
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //160..175
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //176..191
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //192..207
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //208..223
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //224..239
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91  //240..255
    };

    final static char dectab_str[] = {
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //000..015
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //016..031
            90, 62, 91, 63, 64, 65, 66, 91, 67, 68, 69, 70, 71, 91, 72, 73, //032..047 @32 <sp>
            52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 74, 75, 76, 77, 78, 79, //048..063
            80,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, //064..079
            15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 81, 91, 82, 83, 84, //080..095
            85, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, //096..111
            41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 86, 87, 88, 89, 91, //112..127
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //128..143
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //144..159
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //160..175
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //176..191
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //192..207
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //208..223
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, //224..239
            91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91, 91  //240..255
    };

    final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public enum Format
    {
        c,
        string,
        gzip_c,
        gzip_string
    }

    /**
     * Compresses the supplied binary data and then encodes as Base91
     * @param data
     * @param format
     * @return
     */
    public static String compressEncode(String data, Format format)
    {
        return encode(compress(data), format);
    }

    /**
     * Decodes the supplied Base91 String and then decompresses the data.
     * @param data
     * @param format
     * @return
     */
    public static byte[] decodeDecompress(String data, Format format)
    {
        return decompressToBytes(decode(data, format));
    }

    public static String encode(byte[] data, Format format)
    {
        StringBuilder result = new StringBuilder();

        long queue = 0;
        int nbits = 0;
        int ix = 0;
        char[] table;

        switch (format)
        {
            case c:
                table = enctab;
                break;

            default:
            case string:
                table = enctab_str;
                break;

            case gzip_c:
                table = enctab;
                data = compress(data);
                break;

            case gzip_string:
                table = enctab_str;
                data = compress(data);
                break;
        }

        for( long len = data.length; len-- > 0; )
        {
            queue |= (data[ix++] & 255) << nbits;
            nbits += 8;
            if (nbits > 13)
            {   /* enough bits in queue */
                long val = queue & 8191;

                if (val > 88)
                {
                    queue >>= 13;
                    nbits -= 13;
                }
                else
                {    /* we can take 14 bits */
                    val = queue & 16383;
                    queue >>= 14;
                    nbits -= 14;
                }
                result.append(table[(int) (val % 91)]);
                result.append(table[(int) (val / 91)]);
            }
        }

        /* process remaining bits from bit queue; write up to 2 bytes */
        if (nbits > 0)
        {
            result.append(table[(int)(queue % 91)]);
            if (nbits > 7 || queue > 90)
                result.append(table[(int)(queue / 91)]);
        }

        return result.toString();
    }

    public static byte[] decode(String data, Format format)
    {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        int queue = 0;
        int nbits = 0;
        int val = -1;
        int ix = 0;

        char[] table;

        switch (format)
        {
            case c:
            case gzip_c:
                table = dectab;
                break;

            default:
            case string:
            case gzip_string:
                table = dectab_str;
                break;
        }

        for( int len = data.length(); len-- > 0; )
        {
            int d = table[data.charAt(ix++)];
            if (d == 91)
                continue;   /* ignore non-alphabet chars */
            if (val == -1)
                val = d;    /* start next value */
            else
            {
                val += d * 91;
                queue |= val << nbits;
                nbits += (val & 8191) > 88 ? 13 : 14;
                do
                {
                    result.write(queue);
                    queue >>= 8;
                    nbits -= 8;
                } while (nbits > 7);
                val = -1;   /* mark value complete */
            }
        }

        /* process remaining bits; write at most 1 byte */
        if (val != -1)
            result.write( queue | val << nbits );

        switch (format)
        {
            default:
            case string:
            case c:
                return result.toByteArray();

            case gzip_c:
            case gzip_string:
                return decompressToBytes(result.toByteArray());
        }
    }

    @Nullable
    public static byte[] compress(String string)
    {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream(string.length());
             UtilsGZIPOutputStream gos = new UtilsGZIPOutputStream(os);)
        {
            gos.setLevel(Deflater.BEST_COMPRESSION);
            gos.write(string.getBytes());
            gos.close();
            return os.toByteArray();
        }
        catch (Exception e)
        {
            Timber.e(e, "Error occurred when compressing stream from String");
        }

        return null;
    }

    @Nullable
    public static byte[] compress(byte[] data)
    {
        if (data.length == 0)
        {
            Timber.w("Input was empty when trying to compress a byte array");
            return null;
        }


        try (ByteArrayOutputStream os = new ByteArrayOutputStream(data.length);
             GZIPOutputStream gos = new GZIPOutputStream(os);)
        {
            gos.write(data);
            gos.close();
            return os.toByteArray();
        }
        catch (Exception e)
        {
            Timber.e(e, "Error occurred when compressing a stream from byte array");
        }

        return null;
    }

    @Nullable
    public static byte[] compress(ByteBuffer buf)
    {
        if (null == buf || !buf.hasRemaining())
        {
            Timber.w("Input was empty when trying to compress a ByteBuffer");
            return null;
        }


        try (ByteArrayOutputStream os = new ByteArrayOutputStream(buf.capacity());
             GZIPOutputStream gos = new GZIPOutputStream(os);)
        {
            byte[] data = new byte[BUFFER_SIZE];
            buf.rewind();
            while (buf.hasRemaining())
            {
                int read = Math.min(buf.remaining(), BUFFER_SIZE);
                buf.get(data, 0, read);
                gos.write(data, 0, read);
            }
            gos.close();
            return os.toByteArray();
        }
        catch (Exception e)
        {
            Timber.e(e, "Error occurred when compressing a stream from ByteBuffer");
        }

        return null;
    }

    @Nullable
    public static String decompress(byte[] compressed)
    {
        if (compressed.length == 0)
        {
            Timber.w("Input was empty when trying to decompress a stream");
            return null;
        }


        try (ByteArrayInputStream is = new ByteArrayInputStream(compressed);
             GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);)
        {
            StringBuilder string = new StringBuilder();
            byte[] data = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = gis.read(data)) != -1)
            {
                string.append(new String(data, 0, bytesRead));
            }

            return string.toString();
        }
        catch (Exception e)
        {
            Timber.e(e, "Error occurred when decompressing a stream");
        }

        return null;
    }

    @Nullable
    public static String decompress(ByteBuffer dest, byte[] compressed)
    {
        if (compressed.length == 0)
        {
            Timber.w("Input was empty when trying to decompress a stream to a ByteBuffer destination");
            return null;
        }


        try (ByteArrayInputStream is = new ByteArrayInputStream(compressed);
             GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);)
        {
            byte[] data = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = gis.read(data)) != -1)
            {
                dest.put(data, 0, bytesRead);
            }
        }
        catch (Exception e)
        {
            Timber.e(e, "Error occurred when decompressing a stream to a ByteBuffer destination");
        }

        return null;
    }

    @Nullable
    public static byte[] decompressToBytes(byte[] compressed)
    {
        if (compressed.length == 0)
        {
            Timber.w("Input was empty when trying to decompress a stream to bytes");
            return null;
        }


        try (ByteArrayInputStream is = new ByteArrayInputStream(compressed);
             ByteArrayOutputStream os = new ByteArrayOutputStream();
             GZIPInputStream gis = new GZIPInputStream(is, BUFFER_SIZE);)
        {
            byte[] data = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = gis.read(data)) != -1)
            {
                os.write(data, 0, bytesRead);
            }
            return os.toByteArray();
        }
        catch (Exception e)
        {
            Timber.e(e, "Error occurred when decompressing a stream to bytes");
        }

        return null;
    }

    public static String bytesToHex(byte[] bytes)
    {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ )
        {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String toHex(long value, int length)
    {
        StringBuilder buf = new StringBuilder();
        for ( int j = 0; j < length; j++ )
        {
            buf.insert(0, hexArray[(int)(value & 0x0F)]);
            value = value >> 4;
        }
        return buf.toString();
    }
}
