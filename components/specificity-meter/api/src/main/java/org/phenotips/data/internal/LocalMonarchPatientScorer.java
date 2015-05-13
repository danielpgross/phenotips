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
package org.phenotips.data.internal;

import org.apache.commons.lang3.StringUtils;
import org.phenotips.data.Feature;
import org.phenotips.data.Patient;
import org.phenotips.data.PatientScorer;
import org.phenotips.data.PatientSpecificity;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import owltools.graph.OWLGraphWrapper;
import owltools.sim2.FastOwlSimFactory;
import owltools.sim2.OwlSim;
import owltools.sim2.UnknownOWLClassException;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.util.*;

/**
 * Patient scorer that locally integrates the MONARCH initiative's scorer.
 */
@Component
@Named("monarch")
@Singleton
public class LocalMonarchPatientScorer implements PatientScorer, Initializable
{
    private static final String SCORER_NAME = "monarchinitiative.org";

    private OwlSim owlSim;
    private OWLGraphWrapper owlGraph;

    @Override
    public void initialize() throws InitializationException
    {
        FastOwlSimFactory fastOwlSimFactory = new FastOwlSimFactory();

        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = null;
        try {
            ontology = ontologyManager.loadOntologyFromOntologyDocument(new File("/Users/dgross/Desktop/monarch-local/phenotype-ontologies-read-only/server/all.owl"));
            this.owlSim = fastOwlSimFactory.createOwlSim(ontology);
            owlSim.createElementAttributeMapFromOntology();

            owlSim.loadLCSCache("/Users/dgross/Desktop/monarch-local/phenotype-ontologies-read-only/server/owlsim.cache");

            OWLOntology icOntology = ontologyManager.loadOntologyFromOntologyDocument(new File("/Users/dgross/Desktop/monarch-local/phenotype-ontologies-read-only/server/ic-cache.owl"));
            owlSim.setInformationContentFromOntology(icOntology);

            this.owlGraph = new OWLGraphWrapper(ontology);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public PatientSpecificity getSpecificity(Patient patient)
    {
        return new PatientSpecificity(getScore(patient), now(), SCORER_NAME);
    }

    @Override
    public double getScore(Patient patient)
    {
        Set atts = new HashSet<OWLClass>();
        for (Feature f : patient.getFeatures()) {
            if (StringUtils.isNotEmpty(f.getId())) {
                OWLClass featureClass = owlGraph.getOWLClassByIdentifier(f.getId(), true);
                if (featureClass != null) {
                    atts.add(featureClass);
                }
            }
        }

        // Use the root term of the HPO
        OWLClass simRoot = owlGraph.getOWLClassByIdentifier("HP:0000118", true);

        Double score = null;
        try {
            score = this.owlSim.calculateSubgraphAnnotationSufficiencyForAttributeSet(atts, simRoot);
        } catch (UnknownOWLClassException e) {
            e.printStackTrace();
        }

        return score;
    }

    private Date now()
    {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).getTime();
    }
}
