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
import org.phenotips.studies.family.FamilyUtils;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DBStringListProperty;

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
    private FamilyUtils familyUtilsImpl;

    @Inject
    Provider<XWikiContext> provider;

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
            Collection<String> relatives = familyUtilsImpl.getRelatives(patient);
            if (!relatives.isEmpty()) {
                XWikiDocument familyDoc = familyUtilsImpl.getFamilyDoc(patient);
                BaseObject familyObject;
                // if the family is not found, will create a new blank one.
                // todo. if the family contents do exist, but there is a failure to get them, this listener will
                // todo. overwrite the existing pedigree, just to update the relatives.
                // todo. should I check for isNew?
                if (familyDoc == null) {
                    familyDoc = familyUtilsImpl.createFamilyDoc(patient);
                    familyObject = familyDoc.newXObject(FamilyUtils.FAMILY_CLASS, provider.get());
                } else {
                    familyObject = familyDoc.getXObject(FamilyUtils.FAMILY_CLASS);
                }

                // replacing whatever relatives were in the list part of the family. Has no effect on the tree/pedigree
                DBStringListProperty xwikiRelativesList = (DBStringListProperty) familyObject.get("members");
                Set<String> updatedSet = new HashSet<>();
                updatedSet.addAll(xwikiRelativesList.getList());
                updatedSet.addAll(relatives);
                List<String> transferList = new LinkedList<>();
                transferList.addAll(updatedSet);

                familyObject.set("members", transferList, provider.get());
                provider.get().getWiki().saveDocument(familyDoc, provider.get());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Could not process patient's family information. {}", ex.getMessage());
        }
    }
}
