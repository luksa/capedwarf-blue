/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.capedwarf.tasks;

import java.util.Enumeration;

import javax.jms.Message;
import javax.jms.QueueBrowser;

import com.google.appengine.api.taskqueue.TaskAlreadyExistsException;
import org.jboss.capedwarf.common.jms.JmsAdapter;

/**
 *
 * @author <a href="mailto:mluksa@redhat.com">Marko Luksa</a>
 */
class PushQueueChecker extends JmsAdapter {

    public void checkTaskNameNotDuplicate(String taskName) {
        try {
            QueueBrowser browser = getBrowser();
            try {
                Enumeration enumeration = browser.getEnumeration();
                while (enumeration.hasMoreElements()) {
                    System.out.println("***************************************");
                    System.out.println("***************************************");
                    System.out.println("***************************************");
                    System.out.println("");
                    Message message = (Message) enumeration.nextElement();
                    String name = message.getStringProperty("TaskName");
                    if (taskName.equals(name)) {
                        throw new TaskAlreadyExistsException("Duplicate task name: " + taskName);
                    }
                }
            } finally {
                browser.close();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void checkTaskNameNotDuplicate2(String taskName) {
        try {
            QueueBrowser browser = getSession().createBrowser(getQueue(), "TaskName='" + taskName + "'");
            try {
                Enumeration enumeration = browser.getEnumeration();
                if (enumeration.hasMoreElements()) {
                    throw new TaskAlreadyExistsException("Duplicate task name: " + taskName);
                }
            } finally {
                browser.close();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
