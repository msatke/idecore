/*******************************************************************************
 * Copyright (c) 2015 Salesforce.com, inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Salesforce.com, inc. - initial API and implementation
 ******************************************************************************/
package com.salesforce.ide.apex.core.tooling.systemcompletions;

import org.apache.log4j.Logger;

import com.salesforce.ide.core.project.ForceProject;
import com.salesforce.ide.core.remote.ForceConnectionException;
import com.salesforce.ide.core.remote.HTTPAdapter;
import com.salesforce.ide.core.remote.HTTPAdapter.HTTPMethod;
import com.salesforce.ide.core.remote.HTTPConnection;
import com.salesforce.ide.core.remote.InsufficientPermissionsException;
import com.salesforce.ide.core.remote.PromiseableJob;

/**
 * Centralized (singleton) repository for the completions resources.
 * 
 * Assumptions: There is one global systems completion repository for all projects. This is the assumption that is being
 * made on the server – it returns everything regardless of org perm.
 * 
 * @author nchen
 * 
 */
public class ApexSystemCompletionsRepository {
    private static final Logger logger = Logger.getLogger(ApexSystemCompletionsRepository.class);
    private static Completions NOT_INITIALIZED = null;
    private static final String TOOLING_SERVICE_ENDPOINT = "/services/data/";

    private Completions completions = NOT_INITIALIZED;
    private HTTPConnection toolingRESTConnection;

    public static final ApexSystemCompletionsRepository INSTANCE = new ApexSystemCompletionsRepository();

    private ApexSystemCompletionsRepository() {}

    public synchronized Completions getCompletionsFetchIfNecessary(ForceProject project) {
        if (completions == NOT_INITIALIZED) {
            try {
                initializeConnectionIfNecessary(project);
                PromiseableJob<Completions> job =
                        new SystemCompletionsCommand(new HTTPAdapter<>(Completions.class,
                                new SystemCompletionTransport(toolingRESTConnection), HTTPMethod.GET));
                job.schedule();
                try {
                    job.join();
                    completions = job.getAnswer();
                } catch (InterruptedException e) {
                    completions = NOT_INITIALIZED;
                }

            } catch (InsufficientPermissionsException | ForceConnectionException e) {
                logger.error("Failed to fetch system completions", e);
                completions = NOT_INITIALIZED;
            }
        }
        return completions;
    }

    private void initializeConnectionIfNecessary(ForceProject project) throws InsufficientPermissionsException,
            ForceConnectionException {
        toolingRESTConnection = new HTTPConnection(project, TOOLING_SERVICE_ENDPOINT);
        toolingRESTConnection.initialize();
    }

    public synchronized void clear() {
        completions = NOT_INITIALIZED;
    }
}
