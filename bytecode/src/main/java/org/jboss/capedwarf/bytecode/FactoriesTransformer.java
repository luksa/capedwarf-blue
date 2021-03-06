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

package org.jboss.capedwarf.bytecode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author <a href="mailto:marko.luksa@gmail.com">Marko Luksa</a>
 */
public class FactoriesTransformer implements ClassFileTransformer {

    private static Map<String, ClassFileTransformer> transformers = new HashMap<String, ClassFileTransformer>();

    // -- Keep lexicographical order --

    static {
        transformers.put("com.google.appengine.api.blobstore.BlobstoreServiceFactory", new BlobstoreServiceFactoryTransformer());
        transformers.put("com.google.appengine.api.capabilities.CapabilitiesServiceFactory", new CapabilitiesServiceFactoryTransformer());
        transformers.put("com.google.appengine.api.datastore.DatastoreServiceFactory", new DatastoreServiceFactoryTransformer());
        transformers.put("com.google.appengine.api.datastore.Entity", new EntityTransformer());
        transformers.put("com.google.appengine.api.datastore.Key", new KeyTransformer());
        transformers.put("com.google.appengine.api.files.FileServiceFactory", new FileServiceFactoryTransformer());
        transformers.put("com.google.appengine.api.images.ImagesServiceFactory", new ImagesServiceFactoryTransformer());
        transformers.put("com.google.appengine.api.mail.MailServiceFactory", new MailServiceFactoryTransformer());
        transformers.put("com.google.appengine.api.memcache.MemcacheServiceFactory", new MemcacheServiceFactoryTransformer());
        transformers.put("com.google.appengine.api.urlfetch.URLFetchServiceFactory", new URLFetchServiceFactoryTransformer());
        transformers.put("com.google.appengine.api.xmpp.XMPPServiceFactory", new XMPPServiceFactoryTransformer());
        transformers.put("com.google.appengine.api.users.UserServiceFactory", new UserServiceFactoryTransformer());
        transformers.put("com.google.apphosting.api.ApiProxy", new ApiProxyTransformer());
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        ClassFileTransformer cft = transformers.get(className);
        if (cft != null)
            return cft.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);

        return classfileBuffer;
    }
}
