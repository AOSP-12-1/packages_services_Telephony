/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.services.telephony;

import android.annotation.NonNull;
import android.telecom.Conference;
import android.telecom.Connection;
import android.telecom.PhoneAccountHandle;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for the various Telephony {@link Conference} implementations ({@link CdmaConference},
 * {@link TelephonyConference}, and {@link ImsConference}).  Adds some common listener code which
 * all of these conferences use.
 */
public class TelephonyConferenceBase extends Conference {
    /**
     * Listener for conference events.
     */
    public abstract static class TelephonyConferenceListener {
        /**
         * Listener called when a connection is added or removed from a conference.
         * @param connection The connection.
         */
        public void onConferenceMembershipChanged(Connection connection) {};
    }

    private final Set<TelephonyConferenceListener> mListeners = Collections.newSetFromMap(
            new ConcurrentHashMap<>(8, 0.9f, 1));

    /**
     * Adds a listener to this conference.
     * @param listener The listener.
     */
    public void addListener(@NonNull TelephonyConferenceListener listener) {
        mListeners.add(listener);
    }

    /**
     * Removes a listener from this conference.
     * @param listener The listener.
     */
    public void removeListener(@NonNull TelephonyConferenceListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Constructs a new Conference with a mandatory {@link PhoneAccountHandle}
     *
     * @param phoneAccount The {@code PhoneAccountHandle} associated with the conference.
     */
    public TelephonyConferenceBase(PhoneAccountHandle phoneAccount) {
        super(phoneAccount);
    }

    /**
     * Adds a connection to this {@link Conference}.
     * <p>
     * Should be used in place of {@link Conference#addConnection(Connection)} to ensure
     * {@link TelephonyConferenceListener}s are informed of the change.
     *
     * @param connection The connection.
     */
    public void addTelephonyConnection(@NonNull Connection connection) {
        addConnection(connection);
        notifyConferenceMembershipChanged(connection);
    }

    /**
     * Removes a {@link Connection} from this {@link Conference}.
     * <p>
     * Should be used instead of {@link Conference#removeConnection(Connection)} to ensure
     * {@link TelephonyConferenceListener}s are notified of the change.
     *
     * @param connection The connection.
     */
    public void removeTelephonyConnection(@NonNull Connection connection) {
        removeConnection(connection);
        notifyConferenceMembershipChanged(connection);
    }

    /**
     * Destroys the current {@link Conference} and notifies {@link TelephonyConferenceListener}s of
     * the change to conference membership.
     * <p>
     * Should be used instead of {@link Conference#destroy()} to ensure telephony listeners are
     * notified.
     */
    public void destroyTelephonyConference() {
        // Conference#removeConnection modifies the list of participants, so we need to use an
        // iterator here to ensure all participants are removed.
        // Technically Conference#destroy does this, but we want to notify listeners of the state
        // change so we'll do it here first.
        Iterator<Connection> connectionIterator = getConnections().iterator();
        while (connectionIterator.hasNext()) {
            removeTelephonyConnection(connectionIterator.next());
        }
        destroy();
    }

    /**
     * Notifies {@link TelephonyConferenceListener}s of a connection being added or removed from
     * the conference.
     * @param connection The conference.
     */
    private void notifyConferenceMembershipChanged(@NonNull Connection connection) {
        for (TelephonyConferenceListener listener : mListeners) {
            listener.onConferenceMembershipChanged(connection);
        }
    }
}