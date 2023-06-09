package org.xbib.net.http.server.session;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.http.HttpResponseStatus;
import org.xbib.net.http.server.HttpException;
import org.xbib.net.http.server.HttpHandler;
import org.xbib.net.http.server.persist.Codec;
import org.xbib.net.http.server.route.HttpRouterContext;

public class PersistSessionHandler implements HttpHandler {

    private static final Logger logger = Logger.getLogger(PersistSessionHandler.class.getName());

    private final Codec<Session> sessionCodec;

    public PersistSessionHandler(Codec<Session> sessionCodec) {
        this.sessionCodec = sessionCodec;
    }

    @Override
    public void handle(HttpRouterContext context) throws IOException {
        Session session = context.getAttributes().get(Session.class, "session");
        if (session != null) {
            try {
                logger.log(Level.FINEST, "writing session id " + session.id() + " keys = " + session.keySet());
                sessionCodec.write(session.id(), session);
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                throw new HttpException("unable to create session data for cookie", context, HttpResponseStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }
}
