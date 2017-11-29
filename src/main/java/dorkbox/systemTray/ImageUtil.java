/*
 * Copyright 2016 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.systemTray;

import dorkbox.util.LocationResolver;
import dorkbox.util.OS;

import java.io.*;
import java.math.BigInteger;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ImageUtil {

    public static final File TEMP_DIR = new File(System.getProperty("java.io.tmpdir"));

    private static MessageDigest digest;

    private static final Map<String, String> resourceToFilePath = new HashMap<String, String>();
    private static final long runtimeRandom = new SecureRandom().nextLong();

    public static synchronized void init() throws NoSuchAlgorithmException {
        ImageUtil.digest = MessageDigest.getInstance("MD5");
    }

    /**
     * appIndicator/gtk require strings (which is the path)
     * swing version loads as an image (which can be stream or path, we use path)
     */
    public static synchronized String iconPath(String fileName) {
        // if we already have this fileName, reuse it
        final String cachedFile = resourceToFilePath.get(fileName);
        if (cachedFile != null) {
            return cachedFile;
        }

        // is file sitting on drive
        File iconTest = new File(fileName);
        if (iconTest.isFile() && iconTest.canRead()) {
            final String absolutePath = iconTest.getAbsolutePath();

            resourceToFilePath.put(fileName, absolutePath);
            return absolutePath;
        } else {
            // suck it out of a URL/Resource (with debugging if necessary)
            final URL systemResource = LocationResolver.getResource(fileName);
            final String filePath = makeFileViaUrl(systemResource);
            resourceToFilePath.put(fileName, filePath);
            return filePath;
        }
    }

    /**
     * appIndicator/gtk require strings (which is the path)
     * swing version loads as an image (which can be stream or path, we use path)
     */
    public static synchronized String iconPath(final URL fileResource) {
        // if we already have this fileName, reuse it
        final String cachedFile = resourceToFilePath.get(fileResource.getPath());
        if (cachedFile != null) {
            return cachedFile;
        }

        final String filePath = makeFileViaUrl(fileResource);
        resourceToFilePath.put(fileResource.getPath(), filePath);
        return filePath;
    }


    /**
     * appIndicator/gtk require strings (which is the path)
     * swing version loads as an image (which can be stream or path, we use path)
     */
    public static synchronized String iconPath(final String cacheName, final InputStream fileStream) {
        // if we already have this fileName, reuse it
        final String cachedFile = resourceToFilePath.get(cacheName);
        if (cachedFile != null) {
            return cachedFile;
        }

        final String filePath = makeFileViaStream(cacheName, fileStream);
        resourceToFilePath.put(cacheName, filePath);
        return filePath;
    }

    /**
     * NO CACHING OF INPUTSTREAM!
     * <p>
     * appIndicator/gtk require strings (which is the path)
     * swing version loads as an image (which can be stream or path, we use path)
     */
    @Deprecated
    public static synchronized String iconPathNoCache(final InputStream fileStream) {
        return makeFileViaStream(Long.toString(System.currentTimeMillis()), fileStream);
    }


    /**
     * @param resourceUrl the url to copy to a file on disk
     * @return the full path of the resource copied to disk, or null if invalid
     */
    private static String makeFileViaUrl(final URL resourceUrl) {
        if (resourceUrl == null) {
            throw new RuntimeException("resourceUrl is null");
        }

        InputStream inStream;
        try {
            inStream = resourceUrl.openStream();
        } catch (IOException e) {
            String message = "Unable to open icon at '" + resourceUrl + "'";
            SystemTray.logger.error(message, e);
            throw new RuntimeException(message, e);
        }

        // suck it out of a URL/Resource (with debugging if necessary)
        String cacheName = resourceUrl.getPath();
        return makeFileViaStream(cacheName, inStream);
    }

    /**
     * @param cacheName      needs name+extension for the resource
     * @param resourceStream the resource to copy to a file on disk
     * @return the full path of the resource copied to disk, or null if invalid
     */
    private static String makeFileViaStream(final String cacheName, final InputStream resourceStream) {
        if (cacheName == null) {
            throw new RuntimeException("cacheName is null");
        }
        if (resourceStream == null) {
            throw new RuntimeException("resourceStream is null");
        }

        // figure out the fileName
        byte[] bytes = cacheName.getBytes(OS.UTF_8);
        File newFile;

        // can be wimpy, only one at a time
        String hash = hashName(bytes);

        String extension = getExtension(cacheName);
        newFile = new File(TEMP_DIR, "SYSTRAY_" + hash + '.' + extension).getAbsoluteFile();
        if (SystemTray.isKDE) {
            // KDE is unique per run, so this prevents buildup
            newFile.deleteOnExit();
        }

        // copy out to a temp file, as a hash of the file name

        OutputStream outStream = null;
        try {
            outStream = new FileOutputStream(newFile);

            byte[] buffer = new byte[2048];
            int read;
            while ((read = resourceStream.read(buffer)) > 0) {
                outStream.write(buffer, 0, read);
            }
        } catch (IOException e) {
            // Send up exception
            String message = "Unable to copy icon '" + cacheName + "' to temporary location: '" + newFile.getAbsolutePath() + "'";
            SystemTray.logger.error(message, e);
            throw new RuntimeException(message, e);
        } finally {
            try {
                resourceStream.close();
            } catch (Exception ignored) {
            }
            try {
                if (outStream != null) {
                    outStream.close();
                }
            } catch (Exception ignored) {
            }
        }

        return newFile.getAbsolutePath();
    }

    public static String getExtension(final String fileName) {

        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > -1) {
            extension = fileName.substring(dot + 1);
        }
        return extension;
    }

    // must be called from synchronized block
    private static String hashName(byte[] nameChars) {
        digest.reset();
        digest.update(nameChars);

        // For KDE4, it must also be unique across runs
        if (SystemTray.isKDE) {
            byte[] longBytes = new byte[8];
            ByteBuffer wrap = ByteBuffer.wrap(longBytes);
            wrap.putLong(runtimeRandom);
            digest.update(longBytes);
        }

        // convert to alpha-numeric. see https://stackoverflow.com/questions/29183818/why-use-tostring32-and-not-tostring36
        return new BigInteger(1, digest.digest()).toString(32).toUpperCase(Locale.US);
    }
}
