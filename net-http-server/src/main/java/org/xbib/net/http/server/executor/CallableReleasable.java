package org.xbib.net.http.server.executor;

import java.util.concurrent.Callable;
import org.xbib.net.buffer.Releasable;

public interface CallableReleasable<T> extends Callable<T>, Releasable {
}
