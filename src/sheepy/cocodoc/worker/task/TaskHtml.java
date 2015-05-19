package sheepy.cocodoc.worker.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import sheepy.cocodoc.worker.parser.ParserHtml;
import sheepy.cocodoc.worker.parser.coco.XmlNode;
import sheepy.util.text.Escape;
import sheepy.util.collection.CollectionPredicate;
import sheepy.util.text.I18n;

public class TaskHtml extends Task {
   @Override public Action getAction () { return Action.HTML; }

   private static final Predicate<List<String>> validate = CollectionPredicate.<List<String>>size( 0, 2 );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "html() task should have zero to two parameters."; }

   @Override protected void run () {
      if ( ! hasParams() ) {
         parse();
      } else {
         if ( ! isPostProcess() ) {
            setPostProcess( "HTML structure" );
            return;
         }
         switch ( getParam( 0 ).toLowerCase() ) {
            case "toc":
               outputTOC();
               break;

            case "index":
               outputIndex( getParam( 1 ) );
               break;

            case "glossary":
               outputGlossary( getParam( 1 ) );
               break;
         }
      }
   }

   private void parse () {
      getBlock().setName( "Parse HTML" );
      log( Level.FINER, "Parsing HTML tags" );
      ParserHtml p = new ParserHtml();
      p.start( getBlock() );
      log( Level.FINEST, "Parsed HTML. Found {0} headers, {1} indice, {2} glossary terms.", p.getHeaderCount(), p.getIndexCount(), p.getGlossaryCount() );
   }

   private int count;

   /*******************************************************************************************************************/
   // Table of Content

   private void outputTOC () {
      getBlock().setName( "Table of Content" );
      log( Level.FINER, "Generating Table of Content" );
      count = 0;
      ParserHtml.Header title = (ParserHtml.Header) getBlock().stats().getVar( ParserHtml.VAR_HEADER );
      StringBuilder buf = new StringBuilder();
      buf.append( "<ol class='h0'>" );
      if ( title != null )
         recurTOC( title.children, buf );
      buf.append( "\n</ol>" );
      getBlock().getText().append( buf );
      log( Level.FINEST, "Generated Table of Content with {0} items.", count );
   }

   private void recurTOC( List<ParserHtml.Header> list, StringBuilder buf ) {
      if ( list == null ) return;
      for ( ParserHtml.Header h : list ) {
         ++count;
         String content = h.tag.getXml().toString();
         content = content.replaceAll( "<a[^>]+>|</a>", "" ); // Removes <a>
         content = content.substring( 3, content.length()-5 ); // Removes <h1 ... </h1>
         buf.append( "\n<li class='h" ).append( h.level ).append( "'>" ); // And replace by <li ... </li>
         buf.append( "<a href=\"#" ).append( Escape.xml( h.id ) ).append( '"' ).append( content ).append( "</a>" );
         if ( h.children != null && ! h.children.isEmpty() ) {
            buf.append( "\n<ol class='h" ).append( h.level ).append( "'>" );
            recurTOC( h.children, buf );
            buf.append( "\n</ol>" );
         }
         buf.append( "\n</li>" );
      }
   }

   /*******************************************************************************************************************/
   // Index

   private void outputIndex ( String name ) {
      getBlock().setName( "Index" );
      Map<String,List<XmlNode>> data = getNodeMap( ParserHtml.VAR_INDEX( name ) );
      log( Level.FINER, "Generating index {0} of {1} entities", name, data.size() );

      StringBuilder result = new StringBuilder().append( "<dl>" );
      data.keySet().stream().sorted( I18n.comparator() ).sequential().forEach( e -> {
         result.append( "<dt>" ).append( Escape.xml( e ) ).append( "</dt>" );
         for ( XmlNode dfn : data.get( e ) )
            result.append( "<dd>" ).append( dfn.getXml() ).append( "</dd>" );
      });
      result.append( "</dl>" );
      getBlock().getText().append( result );
   }

   public Map<String,List<XmlNode>> getNodeMap( String name ) {
      Map<String,List<XmlNode>> data = (Map<String,List<XmlNode>>) getBlock().stats().getVar( name );
      if ( data == null )
         data = new HashMap<>(0);
      return data;
   }

   /*******************************************************************************************************************/
   // Glossary

   private void outputGlossary ( String name ) {
      getBlock().setName( "Glossary" );
      Map<String,List<XmlNode>> data = getNodeMap( ParserHtml.VAR_GLOSSARY( name ) );
      log( Level.FINER, "Generating glossary {0} of {1} entries", name, data.size() );

      StringBuilder result = new StringBuilder().append( "<dl>" );
      data.keySet().stream().sorted( I18n.comparator() ).sequential().forEach( e -> {
         result.append( "<dt>" ).append( Escape.xml( e ) ).append( "</dt>" );
         for ( XmlNode dfn : data.get( e ) )
            result.append( "<dd>" ).append( dfn.getXml() ).append( "</dd>" );
      });
      result.append( "</dl>" );
      getBlock().getText().append( result );
   }
}