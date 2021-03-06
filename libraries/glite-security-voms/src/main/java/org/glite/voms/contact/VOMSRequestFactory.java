package org.glite.voms.contact;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Iterator;

/**
 *
 * This class builds VOMS XML requests starting from {@link org.glite.voms.contact.VOMSRequestOptions} objects.
 *
 * @author Andrea Ceccanti
 *
 */
public class VOMSRequestFactory {

    private String orderString;
    private String targetString;
    private long lifetime = 0;

    protected DocumentBuilder docBuilder;


    public VOMSRequestFactory(){

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments( true );
        factory.setNamespaceAware( false );
        factory.setValidating( false );

        try {
            docBuilder = factory.newDocumentBuilder();
        } catch ( ParserConfigurationException e ) {
            throw new VOMSException(e);
        }
    }

    public long getLifetime() {
        return lifetime;
    }

    public void setLifetime( long lifetime ) {
        this.lifetime = lifetime;
    }

    public String getOrderString() {
        return orderString;
    }




    public void setOrderString( String orderString ) {
        this.orderString = orderString;
    }

    public String getTargetString() {
        return targetString;
    }




    public void setTargetString( String targetString ) {
        this.targetString = targetString;
    }

    private void setOptionsForRequest(VOMSRequestFragment fragment){
        if (orderString != null && orderString != "")
            fragment.buildOrderElement( orderString );

        if (targetString != null && targetString != "")
            fragment.buildTargetsElement( targetString );


        fragment.buildLifetime( lifetime );
    }

    private void loadOptions(VOMSRequestOptions options){

        lifetime = options.getLifetime();
        setOrderString( options.getOrdering() );
        setTargetString( options.getTargetsAsString() );

    }

    public Document buildRequest(VOMSRequestOptions options){

        loadOptions( options );

        Document request = docBuilder.newDocument();
        VOMSRequestFragment frag = new VOMSRequestFragment(request);

        if (options.getRequestedFQANs().isEmpty()){
            if (options.getVoName() == null)
                throw new VOMSException("No vo name specified for AC retrieval.");

            String voName = options.getVoName();

            if (!voName.startsWith( "/"))
                voName = "/"+voName;

            frag.groupCommand( voName );
            setOptionsForRequest( frag );

            request.appendChild( frag.getFragment() );
            return request;
        }

        Iterator fqanIter = options.getRequestedFQANs().iterator();

        while (fqanIter.hasNext()){

            String FQAN = (String)fqanIter.next();

            if (PathNamingScheme.isGroup( FQAN )){

                frag.groupCommand( FQAN );

            }else if (PathNamingScheme.isRole( FQAN )){

                frag.roleCommand( PathNamingScheme.getRoleName( FQAN ));

            }else if (PathNamingScheme.isQualifiedRole( FQAN )){

                frag.mappingCommand( PathNamingScheme.getGroupName( FQAN ), PathNamingScheme.getRoleName( FQAN ));
            }
        }

        setOptionsForRequest( frag );

        request.appendChild( frag.getFragment() );
        return request;
    }





}

class VOMSRequestFragment{

    private Document doc;

    DocumentFragment fragment;
    Element root;
    Element command;
    Element order;
    Element targets;
    Element lifetime;

    public VOMSRequestFragment(Document doc){

        this.doc = doc;

        fragment = doc.createDocumentFragment();
        buildRootElement();
    }

    protected void buildRootElement(){

        root = doc.createElement( "voms" );
        fragment.appendChild( root );

    }

    private void appendTextChild(Element e, String text){

        e.appendChild( doc.createTextNode( text ) );
    }


    private String buildCompatibleOrderString(String s){

        String[] FQANs = s.split(",");

        if (FQANs.length == 0)
            return "";


        for (int i=0; i < FQANs.length; i++){
            if (PathNamingScheme.isQualifiedRole( FQANs[i] ))
                FQANs[i] = PathNamingScheme.toOldQualifiedRoleSyntax( FQANs[i] );
        }

        return StringUtils.join( FQANs, ",");



    }
    void buildCommandElement(String cmdString){
        command = doc.createElement( "command");
        appendTextChild( command, cmdString);
        root.appendChild( command );
    }

    void buildOrderElement(String orderString){
        order = doc.createElement( "order" );

        // Temporary compatibility hack
        appendTextChild( order,buildCompatibleOrderString( orderString ));
        root.appendChild( order );
    }

    void buildTargetsElement(String targetString){
        targets = doc.createElement( "targets" );
        appendTextChild( targets, targetString);
        root.appendChild( targets );
    }

    void buildLifetime(long lifetime){
        buildLifetime( Long.toString( lifetime ) );
    }

    void buildLifetime(String lifetimeString){
        lifetime = doc.createElement( "lifetime" );
        appendTextChild( lifetime, lifetimeString);
        root.appendChild( lifetime );
    }

    public DocumentFragment getFragment() {
        return fragment;
    }


    public void groupCommand(String groupName){
        buildCommandElement( "G"+groupName );
    }

    public void roleCommand(String roleName){
        buildCommandElement( "R"+roleName );
    }

    public void mappingCommand(String groupName, String roleName){
        buildCommandElement( "B"+groupName+":"+roleName );

    }

    public void allCommand(){
        buildCommandElement( "A" );
    }

}