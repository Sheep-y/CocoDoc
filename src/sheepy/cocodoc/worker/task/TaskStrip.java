package sheepy.cocodoc.worker.task;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import sheepy.cocodoc.CocoRunError;
import sheepy.util.text.Text;

public class TaskStrip extends Task {

   @Override public Action getAction () { return Action.STRIP; }

   @Override protected Predicate<List<String>> validParam() { return nonEmpty; }
   @Override protected String invalidParamMessage() { return "strip() task should have xpath parameters."; }

   @Override protected void run () {
      if ( ! hasParams() ) {
         log( Level.INFO, "Skipping strip(), no parameter" );
         return;
      }
      log( Level.FINER, "Stripping xml" );

      if ( ! getBlock().hasData() ) {
         log( Level.INFO, "Skipping strip(), no content" );
         return;
      }

      XPath path = XPathFactory.newInstance().newXPath();
      Document doc = null;
      boolean hasDeclaration = false;
      int count = 0;

      for ( String param : getParams() ) {

         // Compile the expression first - if it goes wrong, saves us lots of work.
         XPathExpression exp;
         try {
            exp = path.compile( param );
         } catch (XPathExpressionException ex) {
            throwOrWarn( new CocoRunError( "Cannot parse \"" + param + "\" as XPath", ex ) );
            continue;
         }

         try {
            // Parse document on first run.  The document will be reused in subsequence params.
            if ( doc == null ) {
               DocumentBuilder docFac = DocumentBuilderFactory.newInstance().newDocumentBuilder();
               try { // Try detect present of doctype or xml declarations
                  String txt = getBlock().getText().toString();
                  hasDeclaration = txt.startsWith( "<!DOCTYPE" ) || txt.startsWith( "<?xml" );
               } catch( CocoRunError ignored ){}
               doc = docFac.parse( new ByteArrayInputStream( getBlock().getBinary() ) );
               docFac = null;
            }

            NodeList list = (NodeList) exp.evaluate( doc, XPathConstants.NODESET );
            if ( list == null || list.getLength() <= 0 ) continue;

            // Remove nodes from document
            log( Level.FINEST, "Strip found {0} {1}", list.getLength(), param );
            for ( int i = list.getLength()-1 ; i >= 0 ; i-- ) {
               Node target = list.item( i );
               if ( target instanceof Attr ) {
                  Element parent = ( (Attr) target ).getOwnerElement();
                  if ( parent != null ) {
                     parent.removeAttribute( target.getNodeName() );
                     ++count;
                  }
               } else {
                  Node parent = target.getParentNode();
                  if ( parent != null ) {
                     parent.removeChild( target );
                     ++count;
                  }
               }
            }

         } catch ( ParserConfigurationException | SAXException | IOException | XPathExpressionException ex ) {
            throwOrWarn( new CocoRunError( "Cannot parse content as XML", ex ) );
         }
      }

      // If anything were deleted, resave the DOM
      if ( count > 0 ) try {
         log( Level.FINEST, "Rebuilding stripped document" );
         DOMSource source = new DOMSource( doc );
         ByteArrayOutputStream buf = new ByteArrayOutputStream();
         StreamResult result = new StreamResult( buf );
         TransformerFactory.newInstance().newTransformer().transform( source, result );
         if ( ! hasDeclaration ) { // Transformer rebuild may introduce xml declaration that is originally missing.
            getBlock().setText( new String( buf.toByteArray(), Text.UTF8 ).replaceFirst( "^<\\?xml[^>]+>", "" ) );
         } else {
            getBlock().setBinary( buf.toByteArray() );
         }
      } catch ( TransformerException ex ) {
         throwOrWarn( new CocoRunError( "Cannot rebuild doucment", ex ) );
      }

      log( Level.FINEST, "Stripped {0} document nodes", count );
   }

}