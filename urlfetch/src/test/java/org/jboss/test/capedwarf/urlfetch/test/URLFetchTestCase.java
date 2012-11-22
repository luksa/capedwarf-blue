/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.test.capedwarf.urlfetch.test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.test.capedwarf.common.support.All;
import org.jboss.test.capedwarf.common.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
@RunWith(Arquillian.class)
@Category(All.class)
public class URLFetchTestCase extends BaseTest {

    @Deployment
    public static Archive getDeployment() {
        return getCapedwarfDeployment();
    }

    /**
     * Dummy check if we're available.
     *
     * @param url the url to check against
     * @return true if available, false otherwise
     */
    private static boolean available(URL url) {
        InputStream stream = null;
        try {
            stream = url.openStream();
            int x = stream.read();
            Assert.assertFalse(x == -1);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static URL findAvailableUrl(String... urls) throws Exception {
        for (String s : urls) {
            URL url = new URL(s);
            if (available(url))
                return url;
        }
        throw new IllegalArgumentException("No available url: " + Arrays.toString(urls));
    }


    @Test
    @InSequence(1)
    public void testAsyncOps() throws Exception {
        URLFetchService service = URLFetchServiceFactory.getURLFetchService();

        URL adminConsole = findAvailableUrl("http://localhost:9990", "http://localhost:8080/_ah/admin", "http://capedwarf-test.appspot.com/index.html");
        Future<HTTPResponse> response = service.fetchAsync(adminConsole);
        printResponse(response.get(5, TimeUnit.SECONDS));

        URL jbossOrg = new URL("http://www.jboss.org");
        if (available(jbossOrg)) {
            response = service.fetchAsync(jbossOrg);
            printResponse(response.get(30, TimeUnit.SECONDS));
        }

        sync(5000L); // wait a bit for async to finish
    }

    @Test
    @InSequence(2)
    public void testBasicOps() throws Exception {
        URLFetchService service = URLFetchServiceFactory.getURLFetchService();

        URL adminConsole = findAvailableUrl("http://localhost:9990", "http://localhost:8080/_ah/admin", "http://capedwarf-test.appspot.com/index.html");
        HTTPResponse response = service.fetch(adminConsole);
        printResponse(response);

        URL jbossOrg = new URL("http://www.jboss.org");
        if (available(jbossOrg)) {
            response = service.fetch(jbossOrg);
            printResponse(response);
        }
    }

    private void printResponse(HTTPResponse response) throws Exception {
        System.out.println("response = " + new String(response.getContent()));
    }
}
