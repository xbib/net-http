package org.xbib.net.http.server.resource.negotiate;

import java.util.ArrayList;
import java.util.List;

public class VariantSpec {

    private final MediaRangeSpec type;

    private final List<MediaRangeSpec> aliases;

    private boolean isDefault;

    public VariantSpec(String mediaType) {
        type = MediaRangeSpec.parseType(mediaType);
        aliases = new ArrayList<>();
        isDefault = false;
    }

    public VariantSpec addAliasMediaType(String mediaType) {
        aliases.add(MediaRangeSpec.parseType(mediaType));
        return this;
    }

    public void makeDefault() {
        isDefault = true;
    }

    public MediaRangeSpec getMediaType() {
        return type;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public List<MediaRangeSpec> getAliases() {
        return aliases;
    }
}
