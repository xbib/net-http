package org.xbib.net.http.server.cookie;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.xbib.net.util.JsonUtil;

public class CookieSignatureUtil {

    private CookieSignatureUtil() {
    }

    public static String hmac(String plainText, String secret, String algo) throws NoSuchAlgorithmException, InvalidKeyException {
        return hmac(plainText.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8), algo);
    }

    public static Map<String, Object> toMap(String string) throws IOException {
        return JsonUtil.toMap(new String(Base64.getDecoder().decode(string), StandardCharsets.UTF_8));
    }

    public static String toString(Map<String, Object> map) throws IOException {
        return new String(Base64.getEncoder().encode(JsonUtil.toString(map).getBytes(StandardCharsets.UTF_8)), StandardCharsets.ISO_8859_1);
    }

    private static String hmac(byte[] plainText, byte[] secret, String algo) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(algo);
        mac.init(new SecretKeySpec(secret, algo));
        return encodeHex(mac.doFinal(plainText));
    }

    private static String encodeHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(Character.forDigit((b & 240) >> 4, 16)).append(Character.forDigit((b & 15), 16));
        }
        return sb.toString();
    }
}
