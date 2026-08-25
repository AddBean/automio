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
import com.bumptech.glide.load.resource.gif.GifOptions;
import com.bumptech.glide.util.ByteBufferUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * 解析动态webp和gif图
 *
 * @author liuchun
 */
public class ByteBufferFsDecoder implements ResourceDecoder<ByteBuffer, FrameSequence> {

    /**
     * 是否支持webp动图开关
     */
    public static final Option<Boolean> DISABLE_ANIMATION = Option.memory(
            "com.bumptech.glide.integration.framesequence.ByteBufferFsDecoder.DisableAnimation", false);

    public static final Option<Boolean> DISABLE_WEBP = Option.memory(
            "com.bumptech.glide.integration.framesequence.ByteBufferFsDecoder.DisableWebp", false);

    private final List<ImageHeaderParser> parsers;

    public ByteBufferFsDecoder(List<ImageHeaderParser> parsers) {
        this.parsers = parsers;
    }

    @Override
    public boolean handles(ByteBuffer source, Options options) throws IOException {
        if (options.get(DISABLE_ANIMATION)) {
            return false;
        }
        source.mark();
        ImageHeaderParser.ImageType imageType = ImageHeaderParserUtils.getType(parsers, source);
        source.reset();  // reset the Buffer for twice read
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

        WebpHeaderParser.WebpImageType webpImageType = WebpHeaderParser.getType(source);
        return !options.get(GifOptions.DISABLE_ANIMATION) && WebpHeaderParser.isAnimatedWebpType(webpImageType);
    }

    @Nullable
    @Override
    public Resource<FrameSequence> decode(ByteBuffer source, int width, int height, Options options) throws IOException {
        InputStream inputStream = ByteBufferUtil.toStream(source);
        FrameSequence fs = FrameSequence.decodeStream(inputStream);
        if (fs == null) {
            return null;
        }
        return new FrameSequenceResource(fs);
    }
}
