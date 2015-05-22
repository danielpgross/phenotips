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
import org.xwiki.environment.Environment;
import owltools.graph.OWLGraphWrapper;
import owltools.sim2.FastOwlSimFactory;
import owltools.sim2.OwlSim;
import owltools.sim2.UnknownOWLClassException;
import org.xwiki.configuration.ConfigurationSource;

import javax.inject.Inject;
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

    @Inject
    private Environment environment; // org.xwiki.environment.Environment

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Override
    public void initialize() throws InitializationException
    {
        initialize(
                new File(new File(this.environment.getPermanentDirectory(), "monarch-scorer"), "all.owl"),
                new File(new File(this.environment.getPermanentDirectory(), "monarch-scorer"), "owlsim.cache").toString(),
                new File(new File(this.environment.getPermanentDirectory(), "monarch-scorer"), "ic-cache.owl")
        );
    }

    public void initialize(File ontologyFile, String lcsCachePath, File icOntologyFile) throws InitializationException
    {
        FastOwlSimFactory fastOwlSimFactory = new FastOwlSimFactory();

        OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = null;

        // Reset to null in case we are re-initializing
        owlSim = null;
        owlGraph = null;
        try {
            ontology = ontologyManager.loadOntologyFromOntologyDocument(ontologyFile);
            owlSim = fastOwlSimFactory.createOwlSim(ontology);
            owlSim.createElementAttributeMapFromOntology();

            owlSim.loadLCSCache(lcsCachePath);

            OWLOntology icOntology = ontologyManager.loadOntologyFromOntologyDocument(icOntologyFile);
            owlSim.setInformationContentFromOntology(icOntology);

            owlGraph = new OWLGraphWrapper(ontology);
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
        Double scaledScore = 0.0;

        Set presentFeatures = new HashSet<OWLClass>();
        // Union of present and absent features
        Set allFeatures = new HashSet<OWLClass>();
        for (Feature f : patient.getFeatures()) {
            if (StringUtils.isNotEmpty(f.getId())) {
                OWLClass featureClass = owlGraph.getOWLClassByIdentifier(f.getId(), true);
                if (featureClass != null) {
                    allFeatures.add(featureClass);
                    if (f.isPresent()) {
                        presentFeatures.add(featureClass);
                    }
                }
            }
        }

        try {
            Double overallPresentFeaturesScore = owlSim.calculateOverallAnnotationSufficiencyForAttributeSet(presentFeatures);
            Double overallFeaturesScore = owlSim.calculateOverallAnnotationSufficiencyForAttributeSet(allFeatures);

            String subgraphRootsStr = this.configuration.getProperty("phenotips.patientScoring.monarch.subgraphRoots", "HP:0000924, HP:0000707, HP:0000152, HP:0001574, HP:0000478, HP:0001626, HP:0001939, HP:0000119, HP:0001438, HP:0003011, HP:0002664, HP:0001871, HP:0002715, HP:0000818, HP:0002086, HP:0000598, HP:0003549, HP:0001197, HP:0001507, HP:0000769");
            Double subgraphAggScore = 0.0;
            Integer n = 0;
            for (String termStr : subgraphRootsStr.split(", ")) {
                OWLClass term = owlGraph.getOWLClassByIdentifier(termStr, true);
                Double subgraphScore = owlSim.calculateSubgraphAnnotationSufficiencyForAttributeSet(allFeatures, term);
                // Add to the average
                subgraphAggScore += (subgraphScore - subgraphAggScore) / ++n;
            }


            Double subgraphAggScoreScalingFactor = Double.parseDouble(this.configuration.getProperty("phenotips.patientScoring.monarch.subgraphAggScoreScalingFactor", "0.5"));

            scaledScore = (overallFeaturesScore + overallPresentFeaturesScore) / 2 + (subgraphAggScore * subgraphAggScoreScalingFactor) / (1 + subgraphAggScoreScalingFactor);
        }
        catch (UnknownOWLClassException e) {
            e.printStackTrace();
        }

        return scaledScore;
    }

    private Date now()
    {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ROOT).getTime();
    }

    /* Below are testing/PoC functions that can be deleted once no longer needed */
    private void reloadDataFromCopy() throws InitializationException
    {
        initialize(
                new File("/Users/dgross/Desktop/monarch-local/phenotype-ontologies-read-only-2/server/all.owl"),
                "/Users/dgross/Desktop/monarch-local/phenotype-ontologies-read-only-2/server/owlsim.cache",
                new File("/Users/dgross/Desktop/monarch-local/phenotype-ontologies-read-only-2/server/ic-cache.owl")
        );
    }
}
