/*******************************************************************************
 * Copyright (c) 2008, 2009 SAP AG.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    SAP AG - initial API and implementation
 *******************************************************************************/
package org.eclipse.mat.snapshot;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.mat.snapshot.model.IClass;
import org.eclipse.mat.util.MessageUtil;

/**
 * Factory for often-used OQL queries.
 */
public final class OQL
{

    /**
     * Select object by its address.
     */
    public static final String forAddress(long address)
    {
        return "SELECT * FROM OBJECTS 0x" + Long.toHexString(address);//$NON-NLS-1$
    }

    /**
     * Select object by its object id.
     */
    public static final String forObjectId(int objectId)
    {
        return "SELECT * FROM OBJECTS " + objectId;//$NON-NLS-1$
    }

    /**
     * Select objects by its ids.
     */
    public static String forObjectIds(int[] objectIds)
    {
        if (objectIds.length == 0)
            return null;
        StringBuilder buf = new StringBuilder(512);
        buf.append("SELECT * FROM OBJECTS "); //$NON-NLS-1$

        for (int ii = 0; ii < objectIds.length; ii++)
        {
            if (ii > 0)
                buf.append(","); //$NON-NLS-1$
            buf.append(objectIds[ii]);
        }

        return buf.toString();
    }

    /**
     * Select the retained set of a given OQL query.
     */
    public static final String retainedBy(String oqlQuery)
    {
        return "SELECT AS RETAINED SET * FROM OBJECTS (" + oqlQuery + ")";//$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * Select the retained set of a given object.
     */
    public static String retainedBy(int objectId)
    {
        return "SELECT AS RETAINED SET * FROM OBJECTS " + objectId;//$NON-NLS-1$
    }

    /**
     * All objects of a given class.
     */
    public static final String forObjectsOfClass(IClass clasz)
    {
        return "SELECT * FROM " + clasz.getName();//$NON-NLS-1$
    }

    /**
     * All objects of a class identified by its id.
     */
    public static final String forObjectsOfClass(int classId)
    {
        return "SELECT * FROM " + classId; //$NON-NLS-1$
    }

    /**
     * Create a OQL union statement and append it to the query.
     * Possibly optimize a common prefix.
     * select s.a,s.b,s.c from 1,173 s
     * select s.a,s.b,s.c from 123 s
     * combine to
     * select s.a,s.b,s.c from 1,173,123 s
     */
    public static void union(StringBuilder query, String other)
    {
        if ((query.length() > 0))
        {
            // Match number,number identifier
            String m = "\\s*((\\d+\\s*,\\s*)*\\d+)\\s*([A-Za-z_]+[A-Za-z_0-9]*)?\\s*$"; //$NON-NLS-1$
            Pattern p = Pattern.compile(m);
            Matcher m1 = p.matcher(query);
            Matcher m2 = p.matcher(other);
            if (m1.find() && m2.find())
            {
                int i1 = m1.start();
                int i2 = m2.start();
                // check same prefix and identifier
                if (query.substring(0, i1).equals(other.substring(0, i2)))
                {
                    String id1 = m1.group(3);
                    String id2 = m2.group(3);
                    if (id1 == null ? id2 == null : id1.equals(id2))
                    {
                        query.insert(m1.end(1), ","+m2.group(1)); //$NON-NLS-1$
                        return;
                    }
                }
            }
            // Default
            query.append(" UNION (").append(other).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        else
            query.append(other);
    }

    /**
     * Return all instances of classes matching a given regular expression.
     */
    public static String instancesByPattern(Pattern pattern, boolean includeSubclasses)
    {
        StringBuilder buf = new StringBuilder(256);
        buf.append("SELECT * FROM \""); //$NON-NLS-1$
        if (includeSubclasses)
            buf.append(" INSTANCEOF"); //$NON-NLS-1$
        buf.append(pattern.pattern());
        buf.append("\""); //$NON-NLS-1$

        return buf.toString();
    }

    /**
     * Returns all classes matching a given regular expression.
     */
    public static String classesByPattern(Pattern pattern, boolean includeSubclasses)
    {
        StringBuilder buf = new StringBuilder(256);
        buf.append("SELECT * FROM OBJECTS \""); //$NON-NLS-1$
        if (includeSubclasses)
            buf.append(" INSTANCEOF"); //$NON-NLS-1$
        buf.append(pattern.pattern());
        buf.append("\""); //$NON-NLS-1$

        return buf.toString();
    }

    private static final String OQL_classesByClassLoaderId = "SELECT * FROM java.lang.Class c WHERE c implements " //$NON-NLS-1$
                    + IClass.class.getName() + " and c.@classLoaderId = {0, number, 0}"; //$NON-NLS-1$

    /**
     * Returns an OQL query string to select all objects loaded by the given
     * class loader.
     * 
     * <pre>
     *       select *
     *       from
     *       (
     *            select *
     *            from java.lang.Class c
     *            where
     *                c implements org.eclipse.mat.snapshot.model.IClass
     *                and c.@classLoaderId = {0}
     *       )
     * </pre>
     * 
     * @param classLoaderId
     *            the object id of the class loader
     * @return an OQL query selecting all objects loaded by the class loader
     */
    public static String instancesByClassLoaderId(int classLoaderId)
    {
        StringBuilder buf = new StringBuilder(256);
        buf.append("SELECT * FROM ("); //$NON-NLS-1$
        buf.append(classesByClassLoaderId(classLoaderId));
        buf.append(")"); //$NON-NLS-1$
        return buf.toString();
    }

    /**
     * Returns an OQL query string to select all classes loaded by the given
     * class loader.
     * 
     * <pre>
     *       select *
     *       from java.lang.Class c
     *       where
     *            c implements org.eclipse.mat.snapshot.model.IClass
     *            and c.@classLoaderId = {0}
     * </pre>
     * 
     * @param classLoaderId
     *            the object id of the class loader
     * @return an OQL query selecting all classes loaded by the class loader
     */
    public static String classesByClassLoaderId(int classLoaderId)
    {
        return MessageUtil.format(OQL_classesByClassLoaderId, new Object[] { classLoaderId });
    }

    private OQL()
    {}
}
