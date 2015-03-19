package org.phenotips.studies.family;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import net.sf.json.JSON;

@Role
public interface Processing
{
    public void processPatientPedigree(JSON json, String patientId) throws XWikiException;

    void storeFamilyRepresentation(XWikiDocument family, JSON familyContents) throws XWikiException;
}
