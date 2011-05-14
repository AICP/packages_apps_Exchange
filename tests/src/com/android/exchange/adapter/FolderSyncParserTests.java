/*
 * Copyright (C) 2010 The Android Open Source Project
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

import com.android.exchange.EasSyncService;
import com.android.exchange.provider.EmailContentSetupUtils;

import android.test.suitebuilder.annotation.MediumTest;

import java.io.IOException;
import java.util.HashMap;

/**
 * You can run this entire test case with:
 *   runtest -c com.android.exchange.adapter.FolderSyncParserTests exchange
 */
@MediumTest
public class FolderSyncParserTests extends SyncAdapterTestCase<EmailSyncAdapter> {

    public FolderSyncParserTests() {
        super();
    }

    public void testIsValidMailFolder() throws IOException {
        EasSyncService service = getTestService();
        EmailSyncAdapter adapter = new EmailSyncAdapter(service);
        FolderSyncParser parser = new FolderSyncParser(getTestInputStream(), adapter);
        HashMap<String, Mailbox> mailboxMap = new HashMap<String, Mailbox>();
        // The parser needs the mAccount set
        parser.mAccount = mAccount;
        mAccount.save(getContext());

        // Don't save the box; just create it, and give it a server id
        Mailbox boxMailType = EmailContentSetupUtils.setupMailbox("box1", mAccount.mId, false,
                mProviderContext, Mailbox.TYPE_MAIL);
        boxMailType.mServerId = "__1:1";
        // Automatically valid since TYPE_MAIL
        assertTrue(parser.isValidMailFolder(boxMailType, mailboxMap));

        Mailbox boxCalendarType = EmailContentSetupUtils.setupMailbox("box", mAccount.mId, false,
                mProviderContext, Mailbox.TYPE_CALENDAR);
        Mailbox boxContactsType = EmailContentSetupUtils.setupMailbox("box", mAccount.mId, false,
                mProviderContext, Mailbox.TYPE_CONTACTS);
        Mailbox boxTasksType = EmailContentSetupUtils.setupMailbox("box", mAccount.mId, false,
                mProviderContext, Mailbox.TYPE_TASKS);
        // Automatically invalid since TYPE_CALENDAR and TYPE_CONTACTS
        assertFalse(parser.isValidMailFolder(boxCalendarType, mailboxMap));
        assertFalse(parser.isValidMailFolder(boxContactsType, mailboxMap));
        assertFalse(parser.isValidMailFolder(boxTasksType, mailboxMap));

        // Unknown boxes are invalid unless they have a parent that's valid
        Mailbox boxUnknownType = EmailContentSetupUtils.setupMailbox("box", mAccount.mId, false,
                mProviderContext, Mailbox.TYPE_UNKNOWN);
        assertFalse(parser.isValidMailFolder(boxUnknownType, mailboxMap));
        boxUnknownType.mParentServerId = boxMailType.mServerId;
        // We shouldn't find the parent yet
        assertFalse(parser.isValidMailFolder(boxUnknownType, mailboxMap));
        // Put the mailbox in the map; the unknown box should now be valid
        mailboxMap.put(boxMailType.mServerId, boxMailType);
        assertTrue(parser.isValidMailFolder(boxUnknownType, mailboxMap));

        // Clear the map, but save away the parent box
        mailboxMap.clear();
        assertFalse(parser.isValidMailFolder(boxUnknownType, mailboxMap));
        boxMailType.save(mProviderContext);
        // The box should now be valid
        assertTrue(parser.isValidMailFolder(boxUnknownType, mailboxMap));

        // Somewhat harder case.  The parent will be in the map, but also unknown.  The parent's
        // parent will be in the database.
        Mailbox boxParentUnknownType = EmailContentSetupUtils.setupMailbox("box", mAccount.mId,
                false, mProviderContext, Mailbox.TYPE_UNKNOWN);
        assertFalse(parser.isValidMailFolder(boxParentUnknownType, mailboxMap));
        // Give the unknown type parent a parent (boxMailType)
        boxParentUnknownType.mServerId = "__1:2";
        boxParentUnknownType.mParentServerId = boxMailType.mServerId;
        // Give our unknown box an unknown parent
        boxUnknownType.mParentServerId = boxParentUnknownType.mServerId;
        // Confirm the box is still invalid
        assertFalse(parser.isValidMailFolder(boxUnknownType, mailboxMap));
        // Put the unknown type parent into the mailbox map
        mailboxMap.put(boxParentUnknownType.mServerId, boxParentUnknownType);
        // Our unknown box should now be valid, because 1) the parent is unknown, BUT 2) the
        // parent's parent is a mail type
        assertTrue(parser.isValidMailFolder(boxUnknownType, mailboxMap));
    }
}
