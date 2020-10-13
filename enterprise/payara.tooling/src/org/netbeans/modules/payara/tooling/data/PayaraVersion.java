/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.netbeans.modules.payara.tooling.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.apache.maven.artifact.repository.metadata.Metadata;
import org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import org.netbeans.api.annotations.common.CheckForNull;
import org.netbeans.api.annotations.common.NonNull;
import org.openide.util.Exceptions;

/**
 * Payara server version.
 * <p/>
 * @author Tomas Kraus, Peter Benedikovic
 * @author Gaurav Gupta
 */
public class PayaraVersion implements Comparable<PayaraVersion> {

    
    ////////////////////////////////////////////////////////////////////////////
    // Class attributes                                                       //
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Version elements separator character.
     */
    public static final char SEPARATOR = '.';

    /**
     * Version elements separator REGEX pattern.
     */
    public static final String SEPARATOR_PATTERN = "\\.";

    /**
     * Payara Server artifact download url
     */
    private static final String DOWNLOAD_URL = "https://oss.sonatype.org/service/local/repositories/releases/content/fish/payara/distributions/payara/%s/payara-%s.zip"; // NOI18N

    private static final String METADATA_URL = "https://repo1.maven.org/maven2/fish/payara/distributions/payara/maven-metadata.xml"; // NOI18N

    private static final String CDDL_LICENSE = "https://raw.githubusercontent.com/payara/Payara/master/LICENSE.txt"; // NOI18N

    public static final PayaraVersion EMPTY = new PayaraVersion((short) 0, (short) 0, (short) 0, (short) 0, "", "");

    private static PayaraVersion latestVersion;

    private static final Map<String, PayaraVersion> versions = new TreeMap<>();

    public static PayaraVersion getLatestVersion() {
        if (!getVersions().isEmpty()) {
            return latestVersion;
        } else {
            return null;
        }
    }

    public static Map<String, PayaraVersion> getVersionMap() {
        if (!getVersions().isEmpty()) {
            return Collections.unmodifiableMap(versions);
        } else {
            return Collections.emptyMap();
        }
    }

    public static List<PayaraVersion> getVersions() {
        if (versions.isEmpty()) {
            InputStream input = null;
            try {
                MetadataXpp3Reader reader = new MetadataXpp3Reader();
                input = new URL(METADATA_URL).openStream();
                Metadata data = reader.read(new InputStreamReader(input));
                versions.clear();
                for (String version : data.getVersioning().getVersions()) {
                    if (version.contains("Alpha") || version.contains("Beta")) { // NOI18N
                        continue;
                    }
                    PayaraVersion payaraVersion = PayaraVersion.toValue(version);
                    versions.put(version, payaraVersion);
                    if (version.equals(data.getVersioning().getLatest())) {
                        latestVersion = payaraVersion;
                    }
                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                try {
                    input.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        return new ArrayList<>(versions.values());
    }

    /**
     * Returns a <code>PayaraVersion</code> with a value represented by the
     * specified <code>String</code>. The <code>PayaraVersion</code> returned
     * represents existing value only if specified <code>String</code> matches
     * any <code>String</code> returned by <code>toString</code> method.
     * Otherwise <code>null</code> value is returned.
     * <p/>
     * @param versionStr Value containing version <code>String</code>
     * representation.
     * @return <code>PayaraVersion</code> value represented by
     * <code>String</code> or <code>null</code> if value was not recognized.
     */
    @CheckForNull
    public static PayaraVersion toValue(@NonNull final String versionStr) {
        if(versionStr.trim().isEmpty()) {
            return EMPTY;
        }
        PayaraVersion version
                = versions.get(versionStr.toUpperCase(Locale.ENGLISH));
        if (version == null) {
            String[] versionComps = versionStr.split(SEPARATOR_PATTERN);

            short major = Short.valueOf(versionComps[0]);
            short minor = Short.valueOf(versionComps[1]);
            short update = 0, build = 0;
            if (versionComps.length > 2) {
                update = Short.valueOf(versionComps[2]);
            }
            if (versionComps.length > 3) {
                build = Short.valueOf(versionComps[3]);
            }
            version = new PayaraVersion(
                    major, minor, update, build,
                    major >= 5 ? "deployer:pfv5ee8" : "deployer:pfv4ee7",
                    versionStr
            );
        }
        return version;
    }
    ////////////////////////////////////////////////////////////////////////////
    // Instance attributes                                                    //
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Major version number.
     */
    private final short major;

    /**
     * Minor version number.
     */
    private final short minor;

    /**
     * Update version number.
     */
    private final short update;

    /**
     * Build version number.
     */
    private final short build;

    private final String uriFragment;

    private final String indirectUrl;

    private final String directUrl;

    private final String value;

    ////////////////////////////////////////////////////////////////////////////
    // Constructors                                                           //
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Constructs an instance of Payara server version.
     * <p/>
     * @param major Major version number.
     * @param minor Minor version number.
     * @param update Update version number.
     * @param build Build version number.
     */
    private PayaraVersion(final short major, final short minor,
            final short update, final short build, String uriFragment, final String value) {
        this.major = major;
        this.minor = minor;
        this.update = update;
        this.build = build;
        this.uriFragment = uriFragment;
        this.value = value;
        this.indirectUrl = null;
        this.directUrl = String.format(DOWNLOAD_URL, value, value);
    }

    ////////////////////////////////////////////////////////////////////////////
    // Getters                                                                //
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Get major version number.
     *
     * @return Major version number.
     */
    public short getMajor() {
        return major;
    }

    /**
     * Get minor version number.
     * <p/>
     * @return Minor version number.
     */
    public short getMinor() {
        return minor;
    }

    /**
     * Get update version number.
     * <p/>
     * @return Update version number.
     */
    public short getUpdate() {
        return update;
    }

    /**
     * Get build version number.
     * <p/>
     * @return Build version number.
     */
    public short getBuild() {
        return build;
    }

    public String getUriFragment() {
        return uriFragment;
    }

    public String getDirectUrl() {
        return directUrl;
    }

    public String getIndirectUrl() {
        return indirectUrl;
    }

    public String getLicenseUrl() {
        return CDDL_LICENSE;
    }

    public boolean isMinimumSupportedVersion() {
        return major >= 4;
    }

    public boolean isEE7Supported() {
        return major >= 4;
    }

    public boolean isEE8Supported() {
        return major >= 5;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Methods                                                                //
    ////////////////////////////////////////////////////////////////////////////
    /**
     * Compare major and minor parts of version number <code>String</code>s.
     * <p/>
     * @param version Payara server version to compare with this object.
     * @return Value of <code>true</code> when major and minor parts of version
     * numbers are the same or <code>false</code> otherwise.
     */
    public boolean equalsMajorMinor(final PayaraVersion version) {
        if (version == null) {
            return false;
        } else {
            return this.major == version.major && this.minor == version.minor;
        }
    }

    /**
     * Compare all parts of version number <code>String</code>s.
     * <p/>
     * @param version Payara server version to compare with this object.
     * @return Value of <code>true</code> when all parts of version numbers are
     * the same or <code>false</code> otherwise.
     */
    @SuppressWarnings("AccessingNonPublicFieldOfAnotherObject")
    public boolean equals(final PayaraVersion version) {
        if (version == null) {
            return false;
        } else {
            return this.major == version.major
                    && this.minor == version.minor
                    && this.update == version.update
                    && this.build == version.build;
        }
    }

    /**
     * Convert <code>PayaraVersion</code> value to <code>String</code>.
     * <p/>
     * @return A <code>String</code> representation of the value of this object.
     */
    @Override
    public String toString() {
        return value;
    }

    /**
     * Convert <code>PayaraVersion</code> value to <code>String</code>
     * containing all version numbers.
     * <p/>
     * @return A <code>String</code> representation of the value of this object
     * containing all version numbers.
     */
    public String toFullString() {
        StringBuilder sb = new StringBuilder(8);
        sb.append(Integer.toString(major));
        sb.append(SEPARATOR);
        sb.append(Integer.toString(minor));
        sb.append(SEPARATOR);
        sb.append(Integer.toString(update));
        sb.append(SEPARATOR);
        sb.append(Integer.toString(build));
        return sb.toString();
    }

    @Override
    public int compareTo(PayaraVersion o) {
        return Comparator.comparing(PayaraVersion::getMajor)
                .thenComparing(PayaraVersion::getMinor)
                .thenComparingInt(PayaraVersion::getUpdate)
                .thenComparingInt(PayaraVersion::getBuild)
                .compare(this, o);
    }

}
