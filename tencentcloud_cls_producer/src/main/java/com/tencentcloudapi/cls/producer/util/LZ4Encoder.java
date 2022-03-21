package com.tencentcloudapi.cls.producer.util;

import com.tencentcloudapi.cls.producer.common.LogException;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Exception;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

/**
 * @author farmerx
 */
public class LZ4Encoder {
    public LZ4Encoder() {
    }

    public static byte[] compressToLhLz4Chunk(byte[] data) throws LogException {
        int rawSize = data.length;
        LZ4Factory factory = LZ4Factory.fastestInstance();
        LZ4Compressor compressor = factory.fastCompressor();
        int maxCompressedLength = compressor.maxCompressedLength(rawSize);
        byte[] rawCompressed = new byte[maxCompressedLength];

        int encodingSize;
        try {
            encodingSize = compressor.compress(data, 0, rawSize, rawCompressed, 0, maxCompressedLength);
        } catch (LZ4Exception var8) {
            throw new LogException("CompressException", var8.getMessage());
        }

        if (encodingSize <= 0) {
            throw new LogException("CompressException", "Invalid encoding size");
        } else {
            byte[] ret = new byte[encodingSize];
            System.arraycopy(rawCompressed, 0, ret, 0, encodingSize);
            return ret;
        }
    }

    public static byte[] decompressFromLhLz4Chunk(byte[] compressedData, int rawSize) throws LogException {
        LZ4Factory factory = LZ4Factory.fastestInstance();
        LZ4FastDecompressor decompressor = factory.fastDecompressor();
        byte[] restored = new byte[rawSize];

        try {
            decompressor.decompress(compressedData, 0, restored, 0, rawSize);
            return restored;
        } catch (LZ4Exception var6) {
            throw new LogException("DecompressException", var6.getMessage());
        }
    }
}
