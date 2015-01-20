/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.phenotips.studies.family.listener;

import org.phenotips.data.Patient;
import org.phenotips.data.events.PatientChangedEvent;
import org.phenotips.data.events.PatientCreatedEvent;
import org.phenotips.studies.family.api.Family;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Listens for changes in patient documents to check for new or changed links between patients.
 * Creates family pages and keeps them updated.
 */
@Component
@Singleton
@Named("patientlinklistener")
public class PatientLinkListener implements EventListener
{
    @Inject
    private Family familyUtils;

    @Override
    public String getName() { return "patientlinklistener"; }

    public List<Event> getEvents() {
        return Arrays.<Event>asList(new PatientCreatedEvent(), new PatientChangedEvent());
    }

    /** Receives a {@link org.phenotips.data.Patient} and {@link org.xwiki.users.User} objects. */
    @Override
    public void onEvent(Event event, Object p, Object u)
    {
        try {
            Patient patient = (Patient) p;
            familyUtils.getFamily(patient);
        } catch (Exception ex) {
            // todo.
        }
    }
}
