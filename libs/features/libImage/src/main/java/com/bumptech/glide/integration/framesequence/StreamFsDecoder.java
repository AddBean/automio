package com.bumptech.glide.integration.framesequence;

import android.support.rastermillv2.FrameSequence;

import androidx.annotation.Nullable;

import com.bumptech.glide.integration.webp.WebpHeaderParser;
import com.bumptech.glide.load.ImageHeaderParser;
import com.bumptech.glide.load.ImageHeaderParserUtils;
import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.engine.bitmap_recycle.ArrayPool;
import com.bumptech.glide.load.resource.gif.GifOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 解析动态webp和gif图
 *
 * @author liuchun
 */
public class StreamFsDecoder implements ResourceDecoder<InputStream, FrameSequence> {

    public static final Option<Boolean> DISABLE_ANIMATION = Option.memory(
            "com.bumptech.glide.integration.framesequence.StreamFsDecoder.DisableAnimation", false);

    public static final Option<Boolean> DISABLE_WEBP = Option.memory(
            "com.bumptech.glide.integration.framesequence.StreamFsDecoder.DisableWebp", false);

    private final List<ImageHeaderParser> parsers;
    private final ArrayPool byteArrayPool;

    public StreamFsDecoder(List<ImageHeaderParser> parsers, ArrayPool byteArrayPool) {
        this.parsers = parsers;
        this.byteArrayPool = byteArrayPool;
    }

    @Override
    public boolean handles(InputStream source, Options options) throws IOException {
        if (options.get(DISABLE_ANIMATION)) {
            return false;
        }

        ImageHeaderParser.ImageType imageType = ImageHeaderParserUtils.getType(parsers, source, byteArrayPool);
        if (imageType == ImageHeaderParser.ImageType.GIF) {
            // GIF
            return !options.get(GifOptions.DISABLE_ANIMATION);
        }

        if (options.get(DISABLE_WEBP) ||
                (imageType != ImageHeaderParser.ImageType.WEBP
                        && imageType != ImageHeaderParser.ImageType.WEBP_A)) {
            // Non Webp
            return false;
        }

        WebpHeaderParser.WebpImageType webpImageType = WebpHeaderParser.getType(source, byteArrayPool);
        return !options.get(GifOptions.DISABLE_ANIMATION) && WebpHeaderParser.isAnimatedWebpType(webpImageType);
    }

    @Nullable
    @Override
    public Resource<FrameSequence> decode(InputStream source, int width, int height, Options options) throws IOException {

        FrameSequence fs = FrameSequence.decodeStream(source);
        if (fs == null) {
            return null;
        }
        return new FrameSequenceResource(fs);
    }
}
