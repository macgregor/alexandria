package com.github.macgregor.alexandria.remotes;

import com.github.macgregor.alexandria.DocumentMetadata;
import okhttp3.Request;

import java.io.IOException;
import java.net.URI;

public interface Remote {
    boolean exists(DocumentMetadata documentMetadata) throws IOException;
    URI create(DocumentMetadata documentMetadata) throws IOException;
    URI update(DocumentMetadata documentMetadata) throws IOException;
    Request.Builder authenticated(Request.Builder builder);

    
}
