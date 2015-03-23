package org.phenotips.studies.family;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiException;

@Role
public interface Validation
{
    boolean isInFamily(String familyAnchor, String otherId) throws XWikiException;

    int canAddToFamily(String familyAnchor, String patientId) throws XWikiException;
}
