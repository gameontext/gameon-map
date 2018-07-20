/*******************************************************************************
 * Copyright (c) 2016 IBM Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.gameontext.map.db;

import java.net.MalformedURLException;
import java.util.logging.Level;

import javax.annotation.Resource;
import javax.enterprise.inject.Produces;

import org.ektorp.CouchDbConnector;
import org.ektorp.CouchDbInstance;
import org.ektorp.DbAccessException;
import org.ektorp.http.HttpClient;
import org.ektorp.http.StdHttpClient;
import org.ektorp.impl.StdCouchDbInstance;
import org.gameontext.map.Log;

public class CouchInjector {

    @Resource(lookup="couchdb/username")
    protected String username;

    @Resource(lookup="couchdb/password")
    protected String password;

    @Resource(lookup="couchdb/url")
    protected String url;

    public static final String DB_NAME = "map_repository";

    @Produces
    public CouchDbConnector expose() {

        try {
            HttpClient authenticatedHttpClient = new StdHttpClient.Builder()
                    .url(url)
                    .username(username)
                    .password(password)
                    .build();

            CouchDbInstance dbi = new StdCouchDbInstance(authenticatedHttpClient);

            // Connect to the database with the specified instance
            CouchDbConnector dbc = dbi.createConnector(DB_NAME, false);
            Log.log(Level.FINER, this, "Connected to {0}", DB_NAME);
            return dbc;
        } catch (DbAccessException | MalformedURLException e) {
            // throw to prevent this class from going into service,
            // which will prevent injection to the Health check, which will make the app stay down.
            throw new javax.enterprise.inject.CreationException("Unable to connect to database " + DB_NAME, e);
        }
    }
}
