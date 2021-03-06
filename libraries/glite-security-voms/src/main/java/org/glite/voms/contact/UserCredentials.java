/*********************************************************************
 *
 * Authors: 
 *      Andrea Ceccanti - andrea.ceccanti@cnaf.infn.it 
 *          
 * Copyright (c) 2006 INFN-CNAF on behalf of the EGEE project.
 * 
 * For license conditions see LICENSE
 *
 * Parts of this code may be based upon or even include verbatim pieces,
 * originally written by other people, in which case the original header
 * follows.
 *
 *********************************************************************/
package org.glite.voms.contact;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.glite.voms.PKIUtils;
import org.globus.gsi.CredentialException;
import org.globus.gsi.X509Credential;
import org.globus.gsi.bc.BouncyCastleOpenSSLKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

/**
 * This class implements parsing and handling of X509 user credentials 
 * in PEM or PKCS12 format.
 * 
 * @author Andrea Ceccanti
 * @author Vincenzo Ciaschini
 *
 */
public class UserCredentials {

    static{
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private X509Certificate userCert;
    private X509Certificate[] userChain;

    private PrivateKey userKey;

    
    /**
     * 
     *  This method returs the user certificate loaded in this {@link UserCredentials}.
     * 
     * @return the X509 user certificate.
     */
    public X509Certificate getUserCertificate() {

        return userCert;

    }
    /**
     * 
     *  This method returs the user certificate chain loaded in this {@link UserCredentials}.
     * 
     * @return the X509 user certificate.
     */
    public X509Certificate[] getUserChain() {

        return userChain;

    }

    
    /**
     *  This method returs the user credential openssl private key.
     *  
     * @return the user credentials private key.
     */
    public PrivateKey getUserKey() {

        return userKey;

    }
    
    
    /**
     * 
     * This method is used to load and parse an X509 certificate in PEM format.
     * 
     * @param userCertFile the file object referring to the X509 certificate.
     */
    private void loadCert(File userCertFile){
        
        try {
            userChain = PKIUtils.loadCertificates(userCertFile);
            userCert = userChain[0];
        } catch ( CertificateException e ) {
            throw new VOMSException( e );
        }
        
    }
    
    
    /**
     * This method is used to load and decrypt a user private key in PEM format. 
     *
     * @param userKeyFile the file object that points to the PEM key.
     * @param password the password needed to decrypt the key.
     */
    private void loadKey(File userKeyFile, final String password){
        
        try {
            BouncyCastleOpenSSLKey bcKey = new BouncyCastleOpenSSLKey(userKeyFile.getAbsolutePath());
            bcKey.decrypt(password);

            /*userKey = bcKey.getPrivateKey();

                    CertificateLoadUtil.loadPrivateKey(
                    userKeyFile.getAbsolutePath(),
                    new PasswordFinder() {

                public char[] getPassword() {
                    return password.toCharArray();
                }
            }); */
            
            //userKey = new BouncyCastleOpenSSLKey( new FileInputStream(userKeyFile) );

        } catch ( IOException e ) {
            throw new VOMSException( e );
        } catch ( GeneralSecurityException e ) {
            throw new VOMSException( e );
        }
        
    }
    
    private void loadCredentials(File userCertFile, File userKeyFile, String keyPassword){
        
        loadCert( userCertFile );
        loadKey( userKeyFile, keyPassword );
     
    }
    
    private void loadPKCS12Credentials(File pkcs12File, String keyPassword){
        
        try {
            KeyStore ks = KeyStore.getInstance( "PKCS12", "BC" );
            ks.load( new FileInputStream(pkcs12File), keyPassword.toCharArray() );
            Enumeration aliases = ks.aliases();
            
            String alias = null;
            while(aliases.hasMoreElements()) {
                alias = (String) aliases.nextElement();
                if(ks.isKeyEntry(alias)) break;
            }

            if (alias == null)
                throw new VOMSException("No aliases found inside pkcs12 certificate!");
            
            userCert=(X509Certificate) ks.getCertificate( alias );
            userKey = (PrivateKey) ks.getKey(alias, keyPassword.toCharArray());
            //new BouncyCastleOpenSSLKey((PrivateKey)ks.getKey( alias, keyPassword.toCharArray()));
            //userChain = (X509Certificate[]) ks.getCertificateChain( alias);
            userChain = new X509Certificate[1];
		    userChain[0] = userCert;
        } catch ( Exception e ) {
            throw new VOMSException(e);
        }    
        
    }

    private UserCredentials(X509Credential credentials) throws CredentialException {
        userChain = credentials.getCertificateChain();
        userKey   = credentials.getPrivateKey();
        userCert  = userChain[0];
    }

    private UserCredentials( String keyPassword ) {
        String x509UserCert = System.getProperty( "X509_USER_CERT", null );
        String x509UserKey = System.getProperty( "X509_USER_KEY", null );
        String x509UserKeyPassword = System.getProperty(
                "X509_USER_KEY_PASSWORD", null );
        
        String pkcs12UserCert = System.getProperty( "PKCS12_USER_CERT", null );
        String pkcs12UserKeyPassword = System.getProperty( "PKCS12_USER_KEY_PASSWORD", null );

        

        if ( x509UserCert != null && x509UserKey != null ){
            try{
                loadCredentials(new File( x509UserCert ), new File( x509UserKey ), (x509UserKeyPassword != null)? x509UserKeyPassword: keyPassword);
                return;
            }catch (VOMSException e) {
		        throw e;
            }
        }
        
        /*log.debug( "Looking for pem certificates in "+System.getProperty( "user.home" )+File.separator+".globus" );
        
        File globusCert = new File (System.getProperty( "user.home" )+File.separator+".globus"+File.separator+"usercert.pem");
        File globusKey = new File (System.getProperty( "user.home" )+File.separator+".globus"+File.separator+"userkey.pem");
        
        try{
        
            loadCredentials( globusCert, globusKey, (x509UserKeyPassword != null)? x509UserKeyPassword: keyPassword);
            log.debug( "Credentials loaded succesfully." );
            return;
        
        }catch (VOMSException e) {
            log.debug ("Error parsing credentials:"+e.getMessage());
            if (log.isDebugEnabled())
                log.debug(e.getMessage(),e);
        }*/
        
        // PKCS12 credentials support
        if (pkcs12UserCert!=null){
            File pkcs12File = null;
            
            try {
		// resolve bug load pkcs12UserCert and not default value
                //pkcs12File = new File(System.getProperty( "user.home" )+File.separator+".globus" +File.separator+"usercert.p12");
                pkcs12File = new File(pkcs12UserCert);
                loadPKCS12Credentials( pkcs12File, (pkcs12UserKeyPassword != null)? pkcs12UserKeyPassword: keyPassword);
                return;
                
            }catch(VOMSException e){
		        throw e;
            }
            
        }
        
        /*log.debug( "Looking for pkcs12 certificate in "+ System.getProperty( "user.home" )+File.separator+".globus"+File.separator+"usercert.p12");
        
        File pkcs12File = null;
        
        try {
            
            pkcs12File = new File(System.getProperty( "user.home" )+File.separator+".globus" +File.separator+"usercert.p12");
            loadPKCS12Credentials( pkcs12File, (pkcs12UserKeyPassword != null)? pkcs12UserKeyPassword: keyPassword);
            log.debug( "Credentials loaded succesfully." );
            return;
            
        }catch(VOMSException e){
            log.debug ("Error parsing credentials from "+pkcs12File+":"+e.getMessage());
            if (log.isDebugEnabled())
                log.debug(e.getMessage(),e);
            
        }*/
        
        throw new VOMSException("No user credentials found!");
    }
    
    private UserCredentials(String userCertFile, String userKeyFile, String keyPassword){
        
        loadCredentials( new File(userCertFile), new File(userKeyFile), keyPassword);
    }
    
    //sreynaud
    private UserCredentials(File pkcs12UserCert, String pkcs12KeyPassword) {
        loadPKCS12Credentials( pkcs12UserCert, pkcs12KeyPassword);
    }

    //sreynaud
    public static UserCredentials instance(File pkcs12UserCert, String pkcs12KeyPassword){
        return new UserCredentials(pkcs12UserCert, pkcs12KeyPassword);
    }
    
    /**
     * Static instance constructor for a {@link UserCredentials}.
     * This method should be used with credentials whose private key is not encrypted.
     * 
     * The current implementation looks for user credentials in the following places (in sequence):
     * 
     * <ul>
     * <li> If the <code>X509_USER_CERT</code> and <code>X509_USER_KEY</code> system
     * properties are set, their values are used to load the user credentials
     * </li>
     * 
     * <li>If the <code>PKCS12_USER_CERT</code> system property is set, its value is used to 
     * load the user credentials.
     * </li>
     * 
     * <li>The content of the <code>.globus</code> directory in the user's home is searched for a PEM certificate (in the 
     * <code>usercert.pem</code> and <code>userkey.pem</code> files).
     * </li>
     * 
     *  <li>The content of the .globus directory in the user's home is searched for a PKC12 certificate (in the 
     * <code>usercert.p12</code>  file).
     * </li>
     * </ul>
     *  
     * @return the loaded user credentials.
     * 
     * @throws  VOMSException 
     *          if there is an error loading the user credentials. 
     */
    public static UserCredentials instance(){
        
        return new UserCredentials((String)null);
    }
    
    /**
     * Static instance constructor for a {@link UserCredentials}.
     * For more info on the user credentials load procedure, see {@link #instance()}.
     * 
     * @param keyPassword, the password that is to be used to decrypt the user private key. 
     * @return the loaded user credentials.
     * 
     * @throws  VOMSException 
     *          if there is an error loading the user credentials.
     */
    public static UserCredentials instance(String keyPassword){
        
        return new UserCredentials(keyPassword);
    }
      
    

    /**
     *   Static instance constructor for a {@link UserCredentials}.
     *
     * This methods allows a user to bypass the default credentials search procedure (highlighted {@link #instance() here}),
     * by specifying the path to a PEM X509 user cert and private key.
     * 
     * @param userCertFile, the path to the PEM X509 user certificate.
     * @param userKeyFile, the path to the PEM X509 private key.
     * @param keyPassword, the password that is to be used to decrypt the user private key.
     * @return the loaded user credentials.
     * 
     * @throws  VOMSException 
     *          if there is an error loading the user credentials.
     * 
     */
    public static UserCredentials instance(String userCertFile, String userKeyFile, String keyPassword){
        
        return new UserCredentials(userCertFile, userKeyFile, keyPassword);
    }
    
    /**
     *   Static instance constructor for a {@link UserCredentials}.
     *
     * This methods allows a user to bypass the default credentials search procedure (highlighted {@link #instance() here}),
     * by specifying the path to a PEM X509 user cert and private key.
     * 
     * @param userCertFile, the path to the PEM X509 user certificate.
     * @param userKeyFile, the path to the PEM X509 private key.
     * @return the loaded user credentials.
     * 
     * @throws  VOMSException 
     *          if there is an error loading the user credentials.
     * 
     */
    public static UserCredentials instance(String userCertFile, String userKeyFile){
        return UserCredentials.instance(userCertFile, userKeyFile, null);
    }

    /**
     *   Static instance constructor for a {@link UserCredentials}.
     *
     * This methods allows a user to bypass the default credentials search procedure (highlighted {@link #instance() here}),
     * by specifying the path to a PEM X509 user cert and private key.
     * 
     * @param credentials, the GlobusCredentials object containing the user's own proxy
     * @return the loaded user credentials.
     * 
     * @throws  VOMSException 
     *          if there is an error loading the user credentials.
     * 
     */
    public static UserCredentials instance(X509Credential credentials) throws CredentialException {
        return new UserCredentials(credentials);
    }
    
}
