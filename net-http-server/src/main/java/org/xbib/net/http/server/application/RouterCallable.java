package org.xbib.net.http.server.application;

import java.util.concurrent.Callable;
import org.xbib.net.buffer.Releasable;

public interface RouterCallable extends Callable<Boolean>, Releasable {
}
