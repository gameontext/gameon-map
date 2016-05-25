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
package org.gameontext.signed;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

public interface SignedRequestMap {

    public void add(String key, String value);

    public String getFirst(String key);

    public String getAll(String key, String defaultValue);


    /**
     * Wrap around a MultivaluedMap<String, Object>
     */
    static class MVSO_StringMap implements SignedRequestMap {

        final MultivaluedMap<String, Object> mvso;

        public MVSO_StringMap(MultivaluedMap<String, Object> mvso) {
            this.mvso = mvso;
        }

        @Override
        public void add(String key, String value) {
            mvso.remove(key);
            mvso.add(key, value);
        }

        @Override
        public String getFirst(String key) {
            Object value = mvso.getFirst(key);
            return (String) value;
        }

        @Override
        public String getAll(String key, String defaultValue) {
            List<Object> values = mvso.get(key);
            if ( values != null ) {
                StringBuilder builder = new StringBuilder();
                for ( Object value : values ) {
                    builder.append(value);
                }
                return builder.toString();
            }
            return defaultValue;
        }

        @Override
        public String toString() {
            return mvso.toString();
        }
    }

    /**
     * Wrap around a MultivaluedMap<String, String>
     */
    static class MVSS_StringMap implements SignedRequestMap {

        final MultivaluedMap<String, String> mvss;

        public MVSS_StringMap(MultivaluedMap<String, String> mvss) {
            this.mvss = mvss;
        }

        @Override
        public void add(String key, String value) {
            mvss.remove(key);
            mvss.add(key, value);
        }

        @Override
        public String getFirst(String key) {
            return mvss.getFirst(key);
        }

        @Override
        public String getAll(String key, String defaultValue) {
            List<String> values = mvss.get(key);
            if ( values != null ) {
                StringBuilder builder = new StringBuilder();
                for ( String value : values ) {
                    builder.append(value);
                }
                return builder.toString();
            }
            return defaultValue;
        }

        @Override
        public String toString() {
            return mvss.toString();
        }
    }

    static class QueryParameterMap implements SignedRequestMap {
        final String queryString;
        MVSS_StringMap mvss = null;

        public QueryParameterMap(String queryString) {
            this.queryString = queryString;
        }

        @Override
        public void add(String key, String value) {
            throw new IllegalStateException("Read-only map");
        }

        @Override
        public String getFirst(String key) {
            if ( mvss == null )
                unpack();
            return mvss.getFirst(key);
        }

        @Override
        public String getAll(String key, String defaultValue) {
            if ( mvss == null )
                unpack();
            return mvss.getAll(key, defaultValue);
        }

        private void unpack() {
            MultivaluedMap<String, String> internal = new MultivaluedHashMap<>();
            mvss = new MVSS_StringMap(internal);
        }
    }
    
    /**
     * Wrap around a MultivaluedMap<String, String>
     */
    static class MLS_StringMap implements SignedRequestMap {

        final Map<String, List<String>> mls;

        public MLS_StringMap(Map<String, List<String>> mls) {
            this.mls = mls;
        }

        @Override
        public void add(String key, String value) {
            List<String> list = mls.get(key);
            if( list == null ) {
                list = new ArrayList<String>();
                mls.put(key, list);
            }
            list.add(value);
        }

        @Override
        public String getFirst(String key) {
            List<String> value = mls.get(key);
            if ( value == null || value.isEmpty())
                return null;
            
            return value.get(0);
        }

        @Override
        public String getAll(String key, String defaultValue) {
            List<String> values = mls.get(key);
            if ( values != null ) {
                StringBuilder builder = new StringBuilder();
                for ( String value : values ) {
                    builder.append(value);
                }
                return builder.toString();
            }
            return defaultValue;
        }

        @Override
        public String toString() {
            return mls.toString();
        }
    }
}
