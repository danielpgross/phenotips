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

import org.phenotips.data.events.PatientChangedEvent;
import org.phenotips.studies.family.content.Family;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.doc.XWikiDocument;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Listens for changes in patient documents to check for new or changed links between patients. Creates family pages and
 * keeps them updated.
 */
@Component
@Singleton
@Named("patientlinklistener")
public class PatientLinkListener implements EventListener
{
    @Inject
    private Logger logger;

    @Inject
    private Family familyUtils;

    @Override
    public String getName()
    {
        return "patientlinklistener";
    }

    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(new PatientChangedEvent());
    }

    /** Receives a {@link org.phenotips.data.Patient} and {@link org.xwiki.users.User} objects. */
    @Override
    public void onEvent(Event event, Object p, Object u)
    {
        try {
            XWikiDocument patient = (XWikiDocument) p;
            Collection<String> relatives = familyUtils.getRelatives(patient);
            if (!relatives.isEmpty()) {
                XWikiDocument familyDoc = familyUtils.getFamilyDoc(patient);
                // if the family is not found, will create a new blank one.
                // todo. if the family contents do exist, but there is a failure to get them, this listener will
                // todo. overwrite the existing pedigree, just to update the relatives.
                JSONObject family = familyUtils.getFamily(familyDoc);
                if (familyDoc == null || familyDoc.isNew()) {
                    familyDoc = familyUtils.createFamilyDoc(patient);
                }
                // replacing whatever relatives were in the list part of the family. Has no effect on the tree/pedigree
                JSONArray updatedList = new JSONArray();
                updatedList.addAll(relatives);
                family.put("list", updatedList);

                familyUtils.storeFamily(familyDoc, updatedList);
            }
        } catch (Exception ex) {
            logger.error("Could not process patient's family information.", ex.getMessage());
        }
    }
}
