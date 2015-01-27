package org.phenotips.studies.family.api;

import org.phenotips.data.Patient;

import org.xwiki.component.annotation.Role;

import java.util.Collection;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

@Role
public interface Family
{
    XWikiDocument getFamilyDoc(Patient patient) throws XWikiException;

    JSONObject getFamily(XWikiDocument doc);

    Collection<String> getRelatives(Patient patient) throws XWikiException;

    void storeFamily(XWikiDocument family, JSON familyContents) throws XWikiException;

    XWikiDocument createFamilyDoc(Patient patient) throws Exception;

    public JSONObject createBlankFamily();
}
