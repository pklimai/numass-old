package inr.numass;

import hep.dataforge.io.envelopes.*;
import hep.dataforge.values.Value;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * An envelope type for legacy numass tags. Reads legacy tag and writes DF02 tags
 */
public class NumassEnvelopeType implements EnvelopeType {

    public static final byte[] LEGACY_START_SEQUENCE = {'#', '!'};
    public static final byte[] LEGACY_END_SEQUENCE = {'!', '#', '\r', '\n'};

    @Override
    public int getCode() {
        return DefaultEnvelopeType.DEFAULT_ENVELOPE_TYPE;
    }

    @Override
    public String getName() {
        return "numass";
    }

    @Override
    public String description() {
        return "Numass legacy envelope";
    }

    @Override
    public EnvelopeReader getReader(Map<String, String> properties) {
        return new NumassEnvelopeReader();
    }

    @Override
    public EnvelopeWriter getWriter(Map<String, String> properties) {
        return new DefaultEnvelopeWriter(this, MetaType.Companion.resolve(properties));
    }

    public static class LegacyTag extends EnvelopeTag {
        @Override
        protected byte[] getStartSequence() {
            return LEGACY_START_SEQUENCE;
        }

        @Override
        protected byte[] getEndSequence() {
            return LEGACY_END_SEQUENCE;
        }

        /**
         * Get the length of tag in bytes. -1 means undefined size in case tag was modified
         *
         * @return
         */
        public int getLength() {
            return 30;
        }

        /**
         * Read leagscy version 1 tag without leading tag head
         *
         * @param buffer
         * @return
         * @throws IOException
         */
        protected Map<String, Value> readHeader(ByteBuffer buffer) {
            Map<String, Value> res = new HashMap<>();

            int type = buffer.getInt(2);
            res.put(Envelope.Companion.TYPE_PROPERTY, Value.of(type));

            short metaTypeCode = buffer.getShort(10);
            MetaType metaType = MetaType.Companion.resolve(metaTypeCode);

            if (metaType != null) {
                res.put(Envelope.Companion.META_TYPE_PROPERTY, Value.of(metaType.getName()));
            } else {
                LoggerFactory.getLogger(EnvelopeTag.class).warn("Could not resolve meta type. Using default");
            }

            long metaLength = Integer.toUnsignedLong(buffer.getInt(14));
            res.put(Envelope.Companion.META_LENGTH_PROPERTY, Value.of(metaLength));
            long dataLength = Integer.toUnsignedLong(buffer.getInt(22));
            res.put(Envelope.Companion.DATA_LENGTH_PROPERTY, Value.of(dataLength));
            return res;
        }
    }

    private static class NumassEnvelopeReader extends DefaultEnvelopeReader {
        @Override
        protected EnvelopeTag newTag() {
            return new LegacyTag();
        }
    }
}
