/*
Copyright (c) Members of the EGEE Collaboration. 2004. 
See http://www.eu-egee.org/partners/ for details on the copyright
holders.  

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
 */
package org.glite.security.trustmanager;

import org.glite.security.util.DN;
import org.glite.security.util.FullTrustAnchor;
import org.glite.security.util.proxy.ProxyCertificateInfo;

import java.util.Vector;

/**
 * Simple class to hold the path validator state from one stage to the next.
 * 
 * @author hahkala
 */
public class CertPathValidatorState {

    /** The path limit from the basic constraints, default unlimited. */
    public int m_basicConstraintsPathLimit = Integer.MAX_VALUE;

    /** The path limiter cert DN for the basic constraints limit. For clearer error messages. */
    public DN m_basicConstraintsPathLimiter = null;

    /** The path limit from the proxy info extension, default unlimited. */
    public int m_proxyInfoPathLimit = Integer.MAX_VALUE;

    /** The path limiter cert DN for the proxy info extension limit. For clearer error messages. */
    public DN m_proxyInfoPathLimiter = null;

    /** The type of the proxy. */
    public int m_proxyType = ProxyCertificateInfo.UNDEFINED_TYPE;

    /** the stack of CA parent certs for namespace and CRL checking */
    public Vector<FullTrustAnchor> m_anchorStack = new Vector<FullTrustAnchor>();
}
