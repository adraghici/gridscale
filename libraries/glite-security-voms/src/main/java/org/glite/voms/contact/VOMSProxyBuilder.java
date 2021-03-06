/**
 * *******************************************************************
 *
 * Authors:
 *
 * Vincenzo Ciaschini - vincenzo.ciaschini@cnaf.infn.it Andrea Ceccanti -
 * andrea.ceccanti@cnaf.infn.it
 *
 * Uses some code originally developed by: Gidon Moont - g.moont@imperial.ac.uk
 *
 * Copyright (c) 2002, 2003, 2004, 2005, 2006 INFN-CNAF on behalf of the EGEE
 * project.
 *
 * For license conditions see LICENSE
 *
 * Parts of this code may be based upon or even include verbatim pieces,
 * originally written by other people, in which case the original header
 * follows.
 *
 ********************************************************************
 */
/*
 This file is licensed under the terms of the Globus Toolkit Public
 License, found at http://www.globus.org/toolkit/download/license.html.
 */
package org.glite.voms.contact;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameStyle;
import org.bouncycastle.asn1.x500.style.RFC4519Style;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.jce.X509Principal;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.glite.voms.ac.AttributeCertificate;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.GSIConstants.CertificateType;
import org.globus.gsi.GSIConstants.DelegationType;
import org.globus.gsi.X509Credential;
import org.globus.gsi.bc.BouncyCastleCertProcessingFactory;
import org.globus.gsi.proxy.ext.ProxyPolicy;
import org.globus.gsi.util.ProxyCertificateUtil;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 *
 * This class implements VOMS X509 proxy certificates creation.
 *
 * @author Andrea Ceccanti
 *
 *
 */
public class VOMSProxyBuilder {

    private static final Logger log = Logger.getLogger(VOMSProxyBuilder.class);
    public static final CertificateType GT2_PROXY = CertificateType.GSI_2_PROXY;
    public static final CertificateType GT3_PROXY = CertificateType.GSI_3_IMPERSONATION_PROXY;
    public static final CertificateType GT4_PROXY = CertificateType.GSI_4_IMPERSONATION_PROXY;
    public static final CertificateType DEFAULT_PROXY_TYPE = CertificateType.GSI_2_PROXY;
    public static final DelegationType DEFAULT_DELEGATION_TYPE = DelegationType.FULL;
    public static final int DEFAULT_PROXY_LIFETIME = 86400;
    public static final int DEFAULT_PROXY_SIZE = 1024;
    private static final String PROXY_CERT_INFO_V3_OID = "1.3.6.1.4.1.3536.1.222";
    private static final String PROXY_CERT_INFO_V4_OID = "1.3.6.1.5.5.7.1.14";

    /**
     *
     * This methods buils an {@link AttributeCertificate} (AC) object starting
     * from an array of bytes.
     *
     * @param acBytes, the byte array containing the attribute certificate.
     * @return the {@link AttributeCertificate} object
     * @throws VOMSException, in case of parsing errors.
     */
    public static AttributeCertificate buildAC(byte[] acBytes) {

        ByteArrayInputStream bai = new ByteArrayInputStream(acBytes);
        AttributeCertificate ac;

        try {
            ac = AttributeCertificate.getInstance(bai);
            return ac;

        } catch (IOException e) {
            log.error("Error parsing attribute certificate:" + e.getMessage());

            if (log.isDebugEnabled()) {
                log.error(e.getMessage(), e);
            }

            throw new VOMSException(e);

        }
    }

    /**
     * This methods creates a non-voms proxy using the {@link UserCredentials}
     * passed as arguments. The proxy is created with a default lifetime of 3600
     * seconds.
     *
     * @param cred, the {@link UserCredentials} from which the proxy must be
     * created.
     * @return a {@link GlobusCredential} object that represents the proxy.
     */
    public static X509Credential buildProxy(UserCredentials cred) {
        // return buildProxy( cred, 3600, DEFAULT_PROXY_TYPE );
        return buildProxy(cred, 3600, DEFAULT_DELEGATION_TYPE);
    }

    /**
     * This methods creates a non-voms proxy using the {@link UserCredentials}
     * passed as arguments.
     *
     * @param cred, the {@link UserCredentials} from which the proxy must be
     * created.
     * @param lifetime, the lifetime (in seconds) of the generated proxy.
     * @return a {@link GlobusCredential} object that represents the proxy.
     */
    public static X509Credential buildProxy(UserCredentials cred, int lifetime, GSIConstants.DelegationType delegationMode) {

        BouncyCastleCertProcessingFactory factory = BouncyCastleCertProcessingFactory
                .getDefault();

        try {

            // factory.createCredential(cred.getUserChain(),  cred.getUserKey().getPrivateKey(),512, lifetime, GSIConstants.DELEGATION_FULL );
            return factory.createCredential(
                    cred.getUserChain(),
                    cred.getUserKey(),
                    512,
                    lifetime,
                    delegationMode);

        } catch (GeneralSecurityException e) {
            log.error("Error creating temp proxy: " + e.getMessage());

            if (log.isDebugEnabled()) {
                log.error(e.getMessage(), e);
            }

            throw new VOMSException(e);
        } catch (Throwable e) {

            log.error("Error creating temp proxy: " + e.getMessage());
            if (log.isDebugEnabled()) {
                log.error(e.getMessage(), e);
            }

            throw new VOMSException(e.getMessage(), e.getCause());

        }
    }

     /**
     * This methods creates a non-voms proxy using the {@link UserCredentials}
     * passed as arguments.
     *
     * @param cred, the {@link UserCredentials} from which the proxy must be
     * created.
     * @param lifetime, the lifetime (in seconds) of the generated proxy.
     * @return a {@link GlobusCredential} object that represents the proxy.
     */
    public static X509Credential buildProxy(UserCredentials cred, int lifetime, CertificateType certype) {

        BouncyCastleCertProcessingFactory factory = BouncyCastleCertProcessingFactory
                .getDefault();

        try {

            // factory.createCredential(cred.getUserChain(),  cred.getUserKey().getPrivateKey(),512, lifetime, GSIConstants.DELEGATION_FULL );
            return factory.createCredential(
                    cred.getUserChain(),
                    cred.getUserKey(),
                    512,
                    lifetime,
                    certype);

        } catch (GeneralSecurityException e) {
            log.error("Error creating temp proxy: " + e.getMessage());

            if (log.isDebugEnabled()) {
                log.error(e.getMessage(), e);
            }

            throw new VOMSException(e);
        } catch (Throwable e) {

            log.error("Error creating temp proxy: " + e.getMessage());
            if (log.isDebugEnabled()) {
                log.error(e.getMessage(), e);
            }

            throw new VOMSException(e.getMessage(), e.getCause());

        }
    }
    
    
    
    /**
     *
     * This method is used to create a VOMS proxy starting from the
     * {@link UserCredentials} passed as arguments and including a list of
     * {@link AttributeCertificate} objects that will be included in the proxy.
     *
     * @param cred, the {@link UserCredentials} from which the proxy must be
     * created.
     * @param ACs, the list of {@link AttributeCertificate} objects.
     * @param lifetime, the lifetime in seconds of the generated proxy.
     * @return a {@link GlobusCredential} object that represents the proxy.
     * @throws {@link VOMSException}, if something goes wrong.
     *
     * @author Andrea Ceccanti
     * @author Gidon Moont
     *
     *
     */
    public static X509Credential buildProxy(UserCredentials cred,
            List ACs, int lifetime) {
        try {
            return buildProxy(cred, ACs, lifetime, DEFAULT_PROXY_SIZE, DEFAULT_PROXY_TYPE,
                    DEFAULT_DELEGATION_TYPE, null);
        } catch (Throwable e) {

            log.error("Error creating proxy: " + e.getMessage());
            if (log.isDebugEnabled()) {
                log.error(e.getMessage(), e);
            }

            throw new VOMSException(e.getMessage(), e.getCause());

        }

    }

    /**
     *
     * This method is used to create a VOMS proxy starting from the
     * {@link UserCredentials} passed as arguments and including a list of
     * {@link AttributeCertificate} objects that will be included in the proxy.
     *
     * @param cred, the {@link UserCredentials} from which the proxy must be
     * created.
     * @param ACs, the list of {@link AttributeCertificate} objects.
     * @param lifetime, the lifetime in seconds of the generated proxy.
     * @param version, the version of globus to which the proxy conforms
     * @return a {@link GlobusCredential} object that represents the proxy.
     * @throws {@link VOMSException}, if something goes wrong.
     *
     * @author Vincenzo Ciaschini
     * @author Andrea Ceccanti
     *
     *
     */
    public static X509Credential buildProxy(UserCredentials cred,
            List ACs, int bits, int lifetime, CertificateType gtVersion, DelegationType delegType,
            String policyType) {

        if (ACs.isEmpty()) {
            throw new VOMSException("Please specify a non-empty list of attribute certificate to build a voms-proxy.");
        }

        Iterator i = ACs.iterator();

        DEREncodableVector acVector = new DEREncodableVector();

        while (i.hasNext()) {
            acVector.add((ASN1Encodable) i.next());
        }

        HashMap extensions = new HashMap();

        if (ACs.size() != 0) {
            DERSequence seqac = new DERSequence(acVector);
            DERSequence seqacwrap = new DERSequence(seqac);
            extensions.put("1.3.6.1.4.1.8005.100.100.5",
                    ExtensionData.creator("1.3.6.1.4.1.8005.100.100.5",
                    seqacwrap));
        }

        KeyUsage keyUsage = new KeyUsage(KeyUsage.digitalSignature
                | KeyUsage.keyEncipherment | KeyUsage.dataEncipherment);
        extensions.put("2.5.29.15", ExtensionData.creator("2.5.29.15",
                keyUsage.toASN1Primitive()));

        X509Credential proxy = myCreateCredential(
                cred.getUserChain(),
                cred.getUserKey(),
                bits,
                lifetime,
                delegType,
                gtVersion,
                extensions,
                policyType);

        return proxy;
    }

    private static X509Credential myCreateCredential(
            X509Certificate[] certs,
            PrivateKey privateKey,
            int bits,
            int lifetime,
            DelegationType delegationMode,
            CertificateType gtVersion,
            HashMap extensions,
            String policyType) {
        KeyPairGenerator keys = null;

        try {
            keys = KeyPairGenerator.getInstance("RSA", "BC");
        } catch (NoSuchAlgorithmException e) {
            throw new VOMSException(e.getMessage(), e.getCause());
        } catch (NoSuchProviderException e) {
            throw new VOMSException(e.getMessage(), e.getCause());
        }

        keys.initialize(bits);
        KeyPair pair = keys.genKeyPair();

        X509Certificate proxy = myCreateProxyCertificate(
                certs[0],
                privateKey,
                pair.getPublic(),
                lifetime,
                delegationMode,
                gtVersion,
                extensions,
                policyType);


        X509Certificate[] newCerts = new X509Certificate[certs.length + 1];
        newCerts[0] = proxy;
        System.arraycopy(certs, 0, newCerts, 1, certs.length);

        X509Credential credential = new X509Credential(pair.getPrivate(), newCerts);

        return credential;
    }

    private static X509Certificate myCreateProxyCertificate(
            X509Certificate cert,
            PrivateKey issuerKey,
            PublicKey publicKey,
            int lifetime,
            DelegationType delegationMode,
            CertificateType gtVersion,
            HashMap extensions,
            String policyType) {

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();

        String cnValue = null;
        ProxyPolicy policy = null;
        BigInteger serialNum = null;

        switch (delegationMode) {
            case LIMITED:
                cnValue = "limited proxy";
                break;
            case FULL:
                cnValue = "proxy";
                break;
            default:
                break;
        }


        switch (gtVersion) {
            case GSI_2_PROXY:
                policy = new ProxyPolicy(ProxyPolicy.IMPERSONATION);
                serialNum = cert.getSerialNumber();
                break;
            case GSI_2_LIMITED_PROXY:
                policy = new ProxyPolicy(ProxyPolicy.LIMITED);
                serialNum = cert.getSerialNumber();
                break;
            case GSI_3_IMPERSONATION_PROXY:
            case GSI_3_INDEPENDENT_PROXY:
            case GSI_3_LIMITED_PROXY:
            case GSI_3_RESTRICTED_PROXY:
            case GSI_4_IMPERSONATION_PROXY:
            case GSI_4_INDEPENDENT_PROXY:
            case GSI_4_LIMITED_PROXY:
            case GSI_4_RESTRICTED_PROXY:
                Random rand = new Random();
                long number = Math.abs(rand.nextLong());
                cnValue = String.valueOf(number);
                serialNum = new BigInteger(String.valueOf(number));

                ExtensionData data = (ExtensionData) extensions.get(PROXY_CERT_INFO_V3_OID);
                if (data == null) {
                    if (policyType == null) {

                        switch (gtVersion) {
                            case GSI_3_LIMITED_PROXY:
                            case GSI_4_LIMITED_PROXY:
                                policy = new ProxyPolicy(ProxyPolicy.LIMITED);
                                break;
                            case GSI_3_IMPERSONATION_PROXY:
                            case GSI_4_IMPERSONATION_PROXY:
                                policy = new ProxyPolicy(ProxyPolicy.IMPERSONATION);
                                break;
                            case GSI_3_INDEPENDENT_PROXY:
                            case GSI_4_INDEPENDENT_PROXY:
                                policy = new ProxyPolicy(ProxyPolicy.INDEPENDENT);
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid proxyType " + gtVersion);

                        }
                    } else {
                        try {
                            policy = new ProxyPolicy(new ASN1ObjectIdentifier(policyType));
                        } catch (IllegalArgumentException e) {
                            throw new VOMSException("OID required as policyType");
                        }
                    }

                    if(ProxyCertificateUtil.isGsi3Proxy(gtVersion)) {
                            extensions.put(PROXY_CERT_INFO_V3_OID,
                                    ExtensionData.creator(PROXY_CERT_INFO_V3_OID,
                                    new MyProxyCertInfo(policy, gtVersion).toASN1Primitive()));
                    } else if(ProxyCertificateUtil.isGsi4Proxy(gtVersion)) {
                            extensions.put(PROXY_CERT_INFO_V4_OID,
                                    ExtensionData.creator(PROXY_CERT_INFO_V4_OID, true,
                                    new MyProxyCertInfo(policy, gtVersion).toASN1Primitive()));
                    }

                }
        }

        ExtensionData[] exts = (ExtensionData[]) extensions.values().toArray(new ExtensionData[]{});
        for (int i = 0; i < exts.length; i++) {
            certGen.addExtension(exts[i].getOID(), exts[i].getCritical(), exts[i].getObj());
        }

        //X509Name issuerDN = (X509Name) cert.getIssuerDN(); //getSubjectDN();
        //X509NameHelper issuer = new X509NameHelper(issuerDN);
        X509Name subjectDN = (X509Name)cert.getSubjectDN();

        X509NameHelper subject = new X509NameHelper(subjectDN);
        subject.add(RFC4519Style.cn, cnValue);

        certGen.setSubjectDN(subject.getAsName());
        certGen.setIssuerDN(subjectDN);

        certGen.setSerialNumber(serialNum);
        certGen.setPublicKey(publicKey);
        certGen.setSignatureAlgorithm(cert.getSigAlgName());

        GregorianCalendar notBefore = new GregorianCalendar(TimeZone.getTimeZone("GMT"));

        /* Allow for a five minute clock skew here. */
        notBefore.add(Calendar.MINUTE, -5);
        certGen.setNotBefore(notBefore.getTime());

        /* If hours = 0, then cert lifetime is set to user cert */
        /*if (lifetime <= 0) {
            certGen.setNotAfter(cert.getNotAfter());
        } else {*/
        GregorianCalendar notAfter = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        notAfter.add(Calendar.SECOND, lifetime);
        certGen.setNotAfter(notAfter.getTime());
      //  }

        /*new X509v3CertificateBuilder(
                subjectDN,
                serialNum,
                notBefore.getTime(),
                notAfter.getTime(),
                subject.getAsName(),
                publicKey
        )*/

        try {
            return certGen.generate(issuerKey);
        } catch (SignatureException e) {
            throw new VOMSException(e);
        } catch (InvalidKeyException e) {
            throw new VOMSException(e);
        } catch (CertificateEncodingException e) {
            throw new VOMSException(e);
        } catch (IllegalStateException e) {
            throw new VOMSException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new VOMSException(e);
        }

    }

    /**
     * This method is write a globus proxy to an output stream.
     *
     * @param cred
     * @param os
     */
    public static void saveProxy(X509Credential cred, OutputStream os) {
        try {
            cred.save(os);
        } catch (IOException e) {
            throw new VOMSException("Error saving generated proxy: " + e.getMessage(), e);
        }  catch (CertificateEncodingException e) {
            throw new VOMSException("Error saving generated proxy: " + e.getMessage(), e);
        }
    }

    /**
     * This method saves a globus proxy to a file.
     *
     * @param cred
     * @param filename
     * @throws FileNotFoundException
     */
    public static void saveProxy(X509Credential cred, String filename)
            throws FileNotFoundException {

        saveProxy(cred, new FileOutputStream(filename));
    }
}
