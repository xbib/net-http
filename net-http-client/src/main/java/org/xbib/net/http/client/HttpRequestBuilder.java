package org.xbib.net.http.client;

import java.nio.ByteBuffer;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import org.xbib.net.ParameterBuilder;
import org.xbib.net.URL;
import org.xbib.net.http.HttpAddress;

public interface HttpRequestBuilder {

    HttpRequestBuilder setAddress(HttpAddress httpAddress);

    HttpRequestBuilder setURL(URL url);

    HttpRequestBuilder setRequestPath(String requestPath);

    HttpRequestBuilder setParameterBuilder(ParameterBuilder parameterBuilder);

    HttpRequestBuilder setBody(ByteBuffer byteBuffer);

    HttpRequest build() throws UnmappableCharacterException, MalformedInputException;
}
