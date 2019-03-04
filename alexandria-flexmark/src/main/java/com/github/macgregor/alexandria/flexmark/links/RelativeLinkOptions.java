package com.github.macgregor.alexandria.flexmark.links;

import com.vladsch.flexmark.util.options.DataHolder;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class RelativeLinkOptions {
    public final boolean disableRendering;

    public RelativeLinkOptions(){
        disableRendering = false;
    }

    public RelativeLinkOptions(DataHolder options){
        this.disableRendering = RelativeLinkExtension.DISABLE_RENDERING.getFrom(options);
    }
}
