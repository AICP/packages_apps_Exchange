/* Copyright (C) 2010 The Android Open Source Project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.exchange.adapter;

import com.android.emailcommon.service.PolicySet;
import com.android.exchange.EasSyncService;
import com.android.exchange.ExchangeService;
import com.android.exchange.R;
import com.android.exchange.SecurityPolicyDelegate;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.content.Context;
import android.content.res.Resources;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Parse the result of the Provision command
 *
 * Assuming a successful parse, we store the PolicySet and the policy key
 */
public class ProvisionParser extends Parser {
    private EasSyncService mService;
    PolicySet mPolicySet = null;
    String mPolicyKey = null;
    boolean mRemoteWipe = false;
    boolean mIsSupportable = true;
    // An array of string resource id's describing policies that are unsupported by the device/app
    String[] mUnsupportedPolicies;
    boolean smimeRequired = false;

    public ProvisionParser(InputStream in, EasSyncService service) throws IOException {
        super(in);
        mService = service;
    }

    public PolicySet getPolicySet() {
        return mPolicySet;
    }

    public String getPolicyKey() {
        return mPolicyKey;
    }

    public boolean getRemoteWipe() {
        return mRemoteWipe;
    }

    public boolean hasSupportablePolicySet() {
        return (mPolicySet != null) && mIsSupportable;
    }

    public void clearUnsupportedPolicies() {
        mPolicySet = SecurityPolicyDelegate.clearUnsupportedPolicies(mService.mContext, mPolicySet);
        mIsSupportable = true;
        mUnsupportedPolicies = null;
    }

    public String[] getUnsupportedPolicies() {
        return mUnsupportedPolicies;
    }

    private void parseProvisionDocWbxml() throws IOException {
        int minPasswordLength = 0;
        int passwordMode = PolicySet.PASSWORD_MODE_NONE;
        int maxPasswordFails = 0;
        int maxScreenLockTime = 0;
        int passwordExpirationDays = 0;
        int passwordHistory = 0;
        int passwordComplexChars = 0;
        boolean encryptionRequired = false;
        boolean encryptionRequiredExternal = false;
        ArrayList<Integer> unsupportedList = new ArrayList<Integer>();

        while (nextTag(Tags.PROVISION_EAS_PROVISION_DOC) != END) {
            boolean tagIsSupported = true;
            switch (tag) {
                case Tags.PROVISION_DEVICE_PASSWORD_ENABLED:
                    if (getValueInt() == 1) {
                        if (passwordMode == PolicySet.PASSWORD_MODE_NONE) {
                            passwordMode = PolicySet.PASSWORD_MODE_SIMPLE;
                        }
                    }
                    break;
                case Tags.PROVISION_MIN_DEVICE_PASSWORD_LENGTH:
                    minPasswordLength = getValueInt();
                    break;
                case Tags.PROVISION_ALPHA_DEVICE_PASSWORD_ENABLED:
                    if (getValueInt() == 1) {
                        passwordMode = PolicySet.PASSWORD_MODE_STRONG;
                    }
                    break;
                case Tags.PROVISION_MAX_INACTIVITY_TIME_DEVICE_LOCK:
                    // EAS gives us seconds, which is, happily, what the PolicySet requires
                    maxScreenLockTime = getValueInt();
                    break;
                case Tags.PROVISION_MAX_DEVICE_PASSWORD_FAILED_ATTEMPTS:
                    maxPasswordFails = getValueInt();
                    break;
                case Tags.PROVISION_DEVICE_PASSWORD_EXPIRATION:
                    passwordExpirationDays = getValueInt();
                    break;
                case Tags.PROVISION_DEVICE_PASSWORD_HISTORY:
                    passwordHistory = getValueInt();
                    break;
                case Tags.PROVISION_ALLOW_SIMPLE_DEVICE_PASSWORD:
                    // Ignore this unless there's any MSFT documentation for what this means
                    // Hint: I haven't seen any that's more specific than "simple"
                    getValue();
                    break;
                // The following policies, if false, can't be supported at the moment
                case Tags.PROVISION_ATTACHMENTS_ENABLED:
                case Tags.PROVISION_ALLOW_STORAGE_CARD:
                case Tags.PROVISION_ALLOW_CAMERA:
                case Tags.PROVISION_ALLOW_UNSIGNED_APPLICATIONS:
                case Tags.PROVISION_ALLOW_UNSIGNED_INSTALLATION_PACKAGES:
                case Tags.PROVISION_ALLOW_WIFI:
                case Tags.PROVISION_ALLOW_TEXT_MESSAGING:
                case Tags.PROVISION_ALLOW_POP_IMAP_EMAIL:
                case Tags.PROVISION_ALLOW_IRDA:
                case Tags.PROVISION_ALLOW_HTML_EMAIL:
                case Tags.PROVISION_ALLOW_BROWSER:
                case Tags.PROVISION_ALLOW_CONSUMER_EMAIL:
                case Tags.PROVISION_ALLOW_INTERNET_SHARING:
                    if (getValueInt() == 0) {
                        tagIsSupported = false;
                        int res = 0;
                        switch(tag) {
                            case Tags.PROVISION_ATTACHMENTS_ENABLED:
                                res = R.string.policy_dont_allow_attachments;
                                break;
                            case Tags.PROVISION_ALLOW_STORAGE_CARD:
                                res = R.string.policy_dont_allow_storage_cards;
                                break;
                            case Tags.PROVISION_ALLOW_CAMERA:
                                res = R.string.policy_dont_allow_camera;
                                break;
                            case Tags.PROVISION_ALLOW_UNSIGNED_APPLICATIONS:
                                res = R.string.policy_dont_allow_unsigned_apps;
                                break;
                            case Tags.PROVISION_ALLOW_UNSIGNED_INSTALLATION_PACKAGES:
                                res = R.string.policy_dont_allow_unsigned_installers;
                                break;
                            case Tags.PROVISION_ALLOW_WIFI:
                                res = R.string.policy_dont_allow_wifi;
                                break;
                            case Tags.PROVISION_ALLOW_TEXT_MESSAGING:
                                res = R.string.policy_dont_allow_text_messaging;
                                break;
                            case Tags.PROVISION_ALLOW_POP_IMAP_EMAIL:
                                res = R.string.policy_dont_allow_pop_imap;
                                break;
                            case Tags.PROVISION_ALLOW_IRDA:
                                res = R.string.policy_dont_allow_irda;
                                break;
                            case Tags.PROVISION_ALLOW_HTML_EMAIL:
                                res = R.string.policy_dont_allow_html;
                                break;
                            case Tags.PROVISION_ALLOW_BROWSER:
                                res = R.string.policy_dont_allow_browser;
                                break;
                            case Tags.PROVISION_ALLOW_CONSUMER_EMAIL:
                                res = R.string.policy_dont_allow_consumer_email;
                                break;
                            case Tags.PROVISION_ALLOW_INTERNET_SHARING:
                                res = R.string.policy_dont_allow_internet_sharing;
                                break;
                        }
                        if (res > 0) {
                            unsupportedList.add(res);
                        }
                    }
                    break;
                // Bluetooth: 0 = no bluetooth; 1 = only hands-free; 2 = allowed
                case Tags.PROVISION_ALLOW_BLUETOOTH:
                    if (getValueInt() != 2) {
                        tagIsSupported = false;
                        unsupportedList.add(R.string.policy_bluetooth_restricted);
                    }
                    break;
                // We may now support device (internal) encryption; we'll check this capability
                // below with the call to SecurityPolicy.isSupported()
                case Tags.PROVISION_REQUIRE_DEVICE_ENCRYPTION:
                    if (getValueInt() == 1) {
                        encryptionRequired = true;
                    }
                    break;
                // We may now support SD card (external) encryption; we'll check this capability
                // below with the call to SecurityPolicy.isSupported().  Note, despite the name,
                // PROVISION_DEVICE_ENCRYPTION_ENABLED really does refer to external storage.
                case Tags.PROVISION_DEVICE_ENCRYPTION_ENABLED:
                    if (getValueInt() == 1) {
                        encryptionRequiredExternal = true;
                    }
                    break;
                // The following policies, if true, can't be supported at the moment
                case Tags.PROVISION_PASSWORD_RECOVERY_ENABLED:
                case Tags.PROVISION_REQUIRE_SIGNED_SMIME_MESSAGES:
                case Tags.PROVISION_REQUIRE_ENCRYPTED_SMIME_MESSAGES:
                case Tags.PROVISION_REQUIRE_SIGNED_SMIME_ALGORITHM:
                case Tags.PROVISION_REQUIRE_ENCRYPTION_SMIME_ALGORITHM:
                case Tags.PROVISION_REQUIRE_MANUAL_SYNC_WHEN_ROAMING:
                    if (getValueInt() == 1) {
                        tagIsSupported = false;
                        int res = 0;
                        switch(tag) {
                            case Tags.PROVISION_PASSWORD_RECOVERY_ENABLED:
                                res = R.string.policy_enable_password_recovery;
                                break;
                            case Tags.PROVISION_REQUIRE_SIGNED_SMIME_ALGORITHM:
                            case Tags.PROVISION_REQUIRE_ENCRYPTION_SMIME_ALGORITHM:
                            case Tags.PROVISION_REQUIRE_ENCRYPTED_SMIME_MESSAGES:
                            case Tags.PROVISION_REQUIRE_SIGNED_SMIME_MESSAGES:
                                if (!smimeRequired) {
                                    res = R.string.policy_require_smime;
                                    smimeRequired = true;
                                }
                                break;
                            case Tags.PROVISION_REQUIRE_MANUAL_SYNC_WHEN_ROAMING:
                                res = R.string.policy_require_manual_sync_roaming;
                                break;
                        }
                        if (res > 0) {
                            unsupportedList.add(res);
                        }
                    }
                    break;
                // The following, if greater than zero, can't be supported at the moment
                case Tags.PROVISION_MAX_ATTACHMENT_SIZE:
                    if (getValueInt() > 0) {
                        tagIsSupported = false;
                        unsupportedList.add(R.string.policy_max_attachment_size);
                    }
                    break;
                // Complex characters are supported
                case Tags.PROVISION_MIN_DEVICE_PASSWORD_COMPLEX_CHARS:
                    passwordComplexChars = getValueInt();
                    break;
                // The following policies are moot; they allow functionality that we don't support
                case Tags.PROVISION_ALLOW_DESKTOP_SYNC:
                case Tags.PROVISION_ALLOW_SMIME_ENCRYPTION_NEGOTIATION:
                case Tags.PROVISION_ALLOW_SMIME_SOFT_CERTS:
                case Tags.PROVISION_ALLOW_REMOTE_DESKTOP:
                    skipTag();
                    break;
                // We don't handle approved/unapproved application lists
                case Tags.PROVISION_UNAPPROVED_IN_ROM_APPLICATION_LIST:
                case Tags.PROVISION_APPROVED_APPLICATION_LIST:
                    // Parse and throw away the content
                    if (specifiesApplications(tag)) {
                        tagIsSupported = false;
                        if (tag == Tags.PROVISION_UNAPPROVED_IN_ROM_APPLICATION_LIST) {
                            unsupportedList.add(R.string.policy_app_blacklist);
                        } else {
                            unsupportedList.add(R.string.policy_app_whitelist);
                        }
                    }
                    break;
                // NOTE: We can support these entirely within the email application if we choose
                case Tags.PROVISION_MAX_CALENDAR_AGE_FILTER:
                case Tags.PROVISION_MAX_EMAIL_AGE_FILTER:
                    // 0 indicates no specified filter
                    if (getValueInt() != 0) {
                        tagIsSupported = false;
                        if (tag == Tags.PROVISION_MAX_CALENDAR_AGE_FILTER) {
                            unsupportedList.add(R.string.policy_max_calendar_age);
                        } else {
                            unsupportedList.add(R.string.policy_max_email_age);
                        }
                     }
                    break;
                // NOTE: We can support these entirely within the email application if we choose
                case Tags.PROVISION_MAX_EMAIL_BODY_TRUNCATION_SIZE:
                case Tags.PROVISION_MAX_EMAIL_HTML_BODY_TRUNCATION_SIZE:
                    String value = getValue();
                    // -1 indicates no required truncation
                    if (!value.equals("-1")) {
                        tagIsSupported = false;
                        if (tag == Tags.PROVISION_MAX_EMAIL_BODY_TRUNCATION_SIZE) {
                            unsupportedList.add(R.string.policy_text_truncation);
                        } else {
                            unsupportedList.add(R.string.policy_html_truncation);
                        }
                    }
                    break;
                default:
                    skipTag();
            }

            if (!tagIsSupported) {
                log("Policy not supported: " + tag);
                mIsSupportable = false;
            }
        }

        mPolicySet = new PolicySet(minPasswordLength, passwordMode,
                maxPasswordFails, maxScreenLockTime, true, passwordExpirationDays, passwordHistory,
                passwordComplexChars, encryptionRequired, encryptionRequiredExternal);

        // We can only determine whether encryption is supported on device by using isSupported here
        if (!SecurityPolicyDelegate.isSupported(mService.mContext, mPolicySet)) {
            log("SecurityPolicy reports PolicySet not supported.");
            mIsSupportable = false;
            unsupportedList.add(R.string.policy_require_encryption);
        }

        if (!unsupportedList.isEmpty()) {
            mUnsupportedPolicies = new String[unsupportedList.size()];
            int i = 0;
            Context context = ExchangeService.getContext();
            if (context != null) {
                Resources resources = context.getResources();
                for (int res: unsupportedList) {
                    mUnsupportedPolicies[i++] = resources.getString(res);
                }
            }
        }
    }

    /**
     * Return whether or not either of the application list tags specifies any applications
     * @param endTag the tag whose children we're walking through
     * @return whether any applications were specified (by name or by hash)
     * @throws IOException
     */
    private boolean specifiesApplications(int endTag) throws IOException {
        boolean specifiesApplications = false;
        while (nextTag(endTag) != END) {
            switch (tag) {
                case Tags.PROVISION_APPLICATION_NAME:
                case Tags.PROVISION_HASH:
                    specifiesApplications = true;
                    break;
                default:
                    skipTag();
            }
        }
        return specifiesApplications;
    }

    class ShadowPolicySet {
        int mMinPasswordLength = 0;
        int mPasswordMode = PolicySet.PASSWORD_MODE_NONE;
        int mMaxPasswordFails = 0;
        int mMaxScreenLockTime = 0;
        int mPasswordExpiration = 0;
        int mPasswordHistory = 0;
        int mPasswordComplexChars = 0;
    }

    /*package*/ void parseProvisionDocXml(String doc) throws IOException {
        ShadowPolicySet sps = new ShadowPolicySet();

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            parser.setInput(new ByteArrayInputStream(doc.getBytes()), "UTF-8");
            int type = parser.getEventType();
            if (type == XmlPullParser.START_DOCUMENT) {
                type = parser.next();
                if (type == XmlPullParser.START_TAG) {
                    String tagName = parser.getName();
                    if (tagName.equals("wap-provisioningdoc")) {
                        parseWapProvisioningDoc(parser, sps);
                    }
                }
            }
        } catch (XmlPullParserException e) {
           throw new IOException();
        }

        mPolicySet = new PolicySet(sps.mMinPasswordLength, sps.mPasswordMode, sps.mMaxPasswordFails,
                sps.mMaxScreenLockTime, true, sps.mPasswordExpiration, sps.mPasswordHistory,
                sps.mPasswordComplexChars, false, false);
    }

    /**
     * Return true if password is required; otherwise false.
     */
    private boolean parseSecurityPolicy(XmlPullParser parser, ShadowPolicySet sps)
            throws XmlPullParserException, IOException {
        boolean passwordRequired = true;
        while (true) {
            int type = parser.nextTag();
            if (type == XmlPullParser.END_TAG && parser.getName().equals("characteristic")) {
                break;
            } else if (type == XmlPullParser.START_TAG) {
                String tagName = parser.getName();
                if (tagName.equals("parm")) {
                    String name = parser.getAttributeValue(null, "name");
                    if (name.equals("4131")) {
                        String value = parser.getAttributeValue(null, "value");
                        if (value.equals("1")) {
                            passwordRequired = false;
                        }
                    }
                }
            }
        }
        return passwordRequired;
    }

    private void parseCharacteristic(XmlPullParser parser, ShadowPolicySet sps)
            throws XmlPullParserException, IOException {
        boolean enforceInactivityTimer = true;
        while (true) {
            int type = parser.nextTag();
            if (type == XmlPullParser.END_TAG && parser.getName().equals("characteristic")) {
                break;
            } else if (type == XmlPullParser.START_TAG) {
                if (parser.getName().equals("parm")) {
                    String name = parser.getAttributeValue(null, "name");
                    String value = parser.getAttributeValue(null, "value");
                    if (name.equals("AEFrequencyValue")) {
                        if (enforceInactivityTimer) {
                            if (value.equals("0")) {
                                sps.mMaxScreenLockTime = 1;
                            } else {
                                sps.mMaxScreenLockTime = 60*Integer.parseInt(value);
                            }
                        }
                    } else if (name.equals("AEFrequencyType")) {
                        // "0" here means we don't enforce an inactivity timeout
                        if (value.equals("0")) {
                            enforceInactivityTimer = false;
                        }
                    } else if (name.equals("DeviceWipeThreshold")) {
                        sps.mMaxPasswordFails = Integer.parseInt(value);
                    } else if (name.equals("CodewordFrequency")) {
                        // Ignore; has no meaning for us
                    } else if (name.equals("MinimumPasswordLength")) {
                        sps.mMinPasswordLength = Integer.parseInt(value);
                    } else if (name.equals("PasswordComplexity")) {
                        if (value.equals("0")) {
                            sps.mPasswordMode = PolicySet.PASSWORD_MODE_STRONG;
                        } else {
                            sps.mPasswordMode = PolicySet.PASSWORD_MODE_SIMPLE;
                        }
                    }
                }
            }
        }
    }

    private void parseRegistry(XmlPullParser parser, ShadowPolicySet sps)
            throws XmlPullParserException, IOException {
      while (true) {
          int type = parser.nextTag();
          if (type == XmlPullParser.END_TAG && parser.getName().equals("characteristic")) {
              break;
          } else if (type == XmlPullParser.START_TAG) {
              String name = parser.getName();
              if (name.equals("characteristic")) {
                  parseCharacteristic(parser, sps);
              }
          }
      }
    }

    private void parseWapProvisioningDoc(XmlPullParser parser, ShadowPolicySet sps)
            throws XmlPullParserException, IOException {
        while (true) {
            int type = parser.nextTag();
            if (type == XmlPullParser.END_TAG && parser.getName().equals("wap-provisioningdoc")) {
                break;
            } else if (type == XmlPullParser.START_TAG) {
                String name = parser.getName();
                if (name.equals("characteristic")) {
                    String atype = parser.getAttributeValue(null, "type");
                    if (atype.equals("SecurityPolicy")) {
                        // If a password isn't required, stop here
                        if (!parseSecurityPolicy(parser, sps)) {
                            return;
                        }
                    } else if (atype.equals("Registry")) {
                        parseRegistry(parser, sps);
                        return;
                    }
                }
            }
        }
    }

    private void parseProvisionData() throws IOException {
        while (nextTag(Tags.PROVISION_DATA) != END) {
            if (tag == Tags.PROVISION_EAS_PROVISION_DOC) {
                parseProvisionDocWbxml();
            } else {
                skipTag();
            }
        }
    }

    private void parsePolicy() throws IOException {
        String policyType = null;
        while (nextTag(Tags.PROVISION_POLICY) != END) {
            switch (tag) {
                case Tags.PROVISION_POLICY_TYPE:
                    policyType = getValue();
                    mService.userLog("Policy type: ", policyType);
                    break;
                case Tags.PROVISION_POLICY_KEY:
                    mPolicyKey = getValue();
                    break;
                case Tags.PROVISION_STATUS:
                    mService.userLog("Policy status: ", getValue());
                    break;
                case Tags.PROVISION_DATA:
                    if (policyType.equalsIgnoreCase(EasSyncService.EAS_2_POLICY_TYPE)) {
                        // Parse the old style XML document
                        parseProvisionDocXml(getValue());
                    } else {
                        // Parse the newer WBXML data
                        parseProvisionData();
                    }
                    break;
                default:
                    skipTag();
            }
        }
    }

    private void parsePolicies() throws IOException {
        while (nextTag(Tags.PROVISION_POLICIES) != END) {
            if (tag == Tags.PROVISION_POLICY) {
                parsePolicy();
            } else {
                skipTag();
            }
        }
    }

    private void parseDeviceInformation() throws IOException {
        while (nextTag(Tags.SETTINGS_DEVICE_INFORMATION) != END) {
            if (tag == Tags.SETTINGS_STATUS) {
                mService.userLog("DeviceInformation status: " + getValue());
            } else {
                skipTag();
            }
        }
    }

    @Override
    public boolean parse() throws IOException {
        boolean res = false;
        if (nextTag(START_DOCUMENT) != Tags.PROVISION_PROVISION) {
            throw new IOException();
        }
        while (nextTag(START_DOCUMENT) != END_DOCUMENT) {
            switch (tag) {
                case Tags.PROVISION_STATUS:
                    int status = getValueInt();
                    mService.userLog("Provision status: ", status);
                    res = (status == 1);
                    break;
                case Tags.SETTINGS_DEVICE_INFORMATION:
                    parseDeviceInformation();
                    break;
                case Tags.PROVISION_POLICIES:
                    parsePolicies();
                    break;
                case Tags.PROVISION_REMOTE_WIPE:
                    // Indicate remote wipe command received
                    mRemoteWipe = true;
                    break;
                default:
                    skipTag();
            }
        }
        return res;
    }
}

