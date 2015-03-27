/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sheepy.cocodoc.worker.parser.coco;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.directive.Directive;
import sheepy.cocodoc.worker.error.CocoParseError;
import sheepy.cocodoc.worker.error.CocoRunError;
import sheepy.cocodoc.worker.parser.Parser;
import sheepy.cocodoc.worker.parser.coco.XmlSelector.PosElementAttr;
import sheepy.cocodoc.worker.task.Task;
import sheepy.cocodoc.worker.task.TaskFile;
import sheepy.cocodoc.worker.util.CocoUtils;
import static sheepy.cocodoc.worker.util.CocoUtils.tagPool;
import sheepy.util.Text;
import sheepy.util.collection.NullData;

public class ParserCoco extends Parser {
   private String startTag = "<\\?coco(?:-(\\w*))?"; // Dynamic; first group must be the directive
   private String endTag = "\\?>"; // Dynamic; set on creation.
   private Matcher startMatcher;
   private Matcher endMatcher;

   public ParserCoco () {}

   public ParserCoco ( String startTag, String endTag ) {
      this.setTags( startTag, endTag );
   }

   public ParserCoco ( Parser parent ) {
      super( parent );
      ParserCoco p = (ParserCoco) parent;
      this.startTag = p.startTag;
      this.endTag = p.endTag;
   }

   @Override protected StringBuilder implParse ( Block context, String text ) {
      parseDocument( context, text );
      log.log( Level.FINE, "Parsed {0} coco tags.", tagCount );
      if ( resultStack == null ) return null; // No tag found
      return composeResult();
   }

   @Override public ParserCoco clone() {
      return new ParserCoco( this );
   }

   /**************************************************************************************************************/
   // General directive and task parsing

   private static final String paramRegx = "\"([^\"]|\"\")*\"|'([^']|'')*'|[^,()]+"; // double quoted | single quoted | plain parameter
   private static final String attrRegx = ("(\\w+)\\s* ( \\(\\s* (?:" + paramRegx + ") (?:\\s*,\\s*(?:"+paramRegx+") )* \\s* \\))?").replaceAll( " +", "" );
   private static final String firstParamRegx = "\"([^\"]|\"\")*\"|'([^']|'')*'|[^\r\n\t ,()]+"; // differs from paramRegx in that this stop at space
                                       //   directive     \(          first param      (      ,        more params   )*       \)
   // Only live during parsing.
   private Matcher attributeMatcher;
   private Matcher parameterMatcher;
   private Matcher firstParameterMatcher;
   private int tagCount;

   /**
    * Match coco tags from given context and text.
    *
    * @param context Context of current text; used for recursive parsing.
    * @param text Text to be parsed.
    * @return Parsed result which is a mix of String and Directive.
    */
   private void parseDocument ( Block context, String text ) {
      tagCount = 0;
      Matcher start = startMatcher = CocoUtils.tagPool.get( startTag );
      Matcher end   = endMatcher   = CocoUtils.tagPool.get( endTag   );

      while ( start.reset( text ).find() ) {
         if ( shouldStop() ) return;
         if ( ! end.reset( text ).find( start.end() ) ) break;
         String tag = text.substring( start.start(), end.start() );

         try {
            Directive dir = parseDirective( tag, start );
            if ( dir != null ) {
               ++tagCount;
               addToResult( text.subSequence( 0, start.start() ) );
               log.log( Level.FINE, "Found coco tag {0}", dir );
               switch ( dir.getAction() ) {
                  case START:
                     dir.setContent( text.substring( end.end() ) ); // Pass content to directive
                     addToResult( dir.start( context ) );
                     text = dir.getContent().toString(); // Get remaining (unparsed) content
                     if ( text == null ) throw new CocoParseError( "Coco:start without Coco:end" );
                     break;
                  case END:
                     if ( context.getParent() == null ) throw new CocoParseError( "Coco:end without Coco:start" );
                     context.getDirective().setContent( text.substring( end.end() ) ); // Parse unparsed content back to upper level.
                     return; // Terminate
                  default:
                     addToResult( dir.start( context ) );
                     text = text.substring( end.end() );
               }
            }
         } catch ( CocoParseError ex ) {
            log.log( Level.WARNING, "Cannot parse coco tag {1}: {0}", new Object[]{ ex, tag } );
            if ( text == null ) return;
            addToResult( text.subSequence( 0, end.end() ) );
            text = text.substring( end.end() );
         }
         if ( text.isEmpty() ) break;
      }
      if ( tagCount <= 0 ) resultStack = null;
      else if ( ! text.isEmpty() ) addToResult( text );
   }

   private Directive parseDirective ( String tag, Matcher start ) {
      String action = start.group( 1 );
      String txt = tag.substring( start.group().length() ).trim();
      List<Task> tasks = null;

      log.log( Level.FINER, "Parsing coco directive: {0} {1}", new Object[]{ action, txt } );
      if ( ! txt.isEmpty() ) {
         tasks = new ArrayList<>();
         String[] defaultCheck = checkDefaultParameter( txt );
         if ( defaultCheck != null ) {
            log.log( Level.FINEST, "Matched parameter {1} for default task {0}", new Object[]{ action, defaultCheck[0] } );
            tasks.add( new TaskFile().addParam( defaultCheck[0] ) );
            txt = defaultCheck[1];
         }
         if ( ! txt.isEmpty() ) { // Match remaining parameters
            Matcher attr = attributeMatcher;
            if ( attr == null ) attr = attributeMatcher = CocoUtils.tagPool.get( attrRegx );
            while ( ! txt.isEmpty() ) {
               if ( ! attr.reset( txt ).find() || attr.start() != 0 )
                  throw new CocoParseError( "Cannot parse tasks " + txt );

               tasks.add( parseTask( attr.group(1), attr.group(2) ) );

               txt = txt.substring( attr.end() ).trim();
            }
         }
      }
      return Directive.create( action, tasks );
   }

   private Task parseTask ( String taskname, String txt ) {
      List<String> params = new ArrayList<>(8);
      Matcher para = parameterMatcher;
      if ( para == null ) para = parameterMatcher = CocoUtils.tagPool.get( paramRegx );
      log.log( Level.FINEST, "Parsing coco task: {0} {1}", new Object[]{ taskname, txt } );
      if ( txt != null ) {
         txt = Text.unquote( txt, '(', ')' ).trim();
         while ( ! txt.isEmpty() ) {
            if ( shouldStop() ) return null;
            if ( ! para.reset( txt ).find() || para.start() != 0 )
               throw new UnsupportedOperationException( "Bug: parameterMatcher failed to match attributeMatcher result: " + txt );

            params.add( Task.unquote( para.group() ) );

            txt = txt.substring( para.end() ).trim();
            if ( txt.startsWith( "," ) ) txt = txt.substring( 1 ).trim();
         }
      }
      return Task.create( taskname, NullData.nullIfEmpty( params ) );
   }

   /**
    * Try to match the first task as a default parameter.
    *
    * @param txt Text to match.
    * @return null or [ default parameter, remaining text ]
    */
   private String[] checkDefaultParameter ( String txt ) {
      Matcher para = firstParameterMatcher;
      if ( para == null ) para = firstParameterMatcher = CocoUtils.tagPool.get( firstParamRegx );
      if ( ! para.reset( txt ).find() || para.start() != 0 ) return null;
      // Found a candidate. Test whether it should be parsed as parameter or task.
      String defaultParam = para.group();
      String remaining = txt.substring( para.end() ).trim();
      boolean isParam = Task.isQuoted( defaultParam );
      if ( ! isParam ) try {
         Task.create( defaultParam ); // Not quoted, not followed by bracket, so try parse as task.
      } catch ( CocoParseError ex ) {
         isParam = true;              // Failed to parse as task means it is a parameter not a task.
      }
      if ( isParam && remaining.startsWith( "(" ) ) return null; // Unless it is followed by a bracket
      if ( isParam ) return new String[]{ Task.unquote( defaultParam ), remaining };
      return null;
   }

   /************************************************************************************************************/
   // Position parsing

   private static final String positionAttrRegx = "([^\\W-]+)(?:\\s*([$|*~^]?=)\\s*(\"(?:[^\"]|\"\")*\"|'(?:[^']|'')*'|[^]]+)(\\s*i)?)?";
                                              // [ (   tag  )       ( $ or ^ =)    ( "      value     "|'   value    '| val )(    i)?   ]
   private static final String positionRegx =
      "(\\S+)\\s+of|" + // attr of - group 1
      "(the|all|[1-9]\\d*(?:st|nd|th))?\\s*" + // (the|1st|2nd|all)? - group 2
      "(line|<?([\\w:-]+)>?(?:\\s*\\[" + positionAttrRegx + "\\])*)\\s*" + // (line|<(tag|any)>[attr]*) - group 3,4 (plus 5,6,7,8 in attr)
      "(before|after)|" + // group 9
      "this";
   Matcher positionMatcher;
   Matcher positionAttrMatcher;


   public XmlSelector parseSelector ( String text ) {
      text = text.trim();
      XmlSelector result = null;
      if ( text.startsWith( "before " ) ) {
         result = new XmlSelector.PosBefore();
         text = text.substring( 7 ).trim();
      } else if ( text.startsWith( "after " ) ) {
         result = new XmlSelector.PosAfter();
         text = text.substring( 6 ).trim();
      } else if ( text.startsWith( "replace " ) ) {
         text = text.substring( 8 ).trim();
      }

      Matcher m = positionMatcher;
      if ( m == null ) m = positionMatcher = tagPool.get( positionRegx );
      while ( ! text.isEmpty() ) {
         if ( ! m.reset(text).find() ) throw new CocoParseError( "Cannot parse coco location parameter: " + text );
         result = createPosition( result, m );
         text = text.substring( m.end() ).trim();
      }
      tagPool.recycle( positionRegx, m );
      return result;
   }

   private XmlSelector createPosition ( XmlSelector parent, Matcher m ) {
      String text = m.group().trim();
      if ( text.equals( "this" ) ) return new XmlSelector.PosThis().setNext( parent );
      if ( text.endsWith( "of" ) ) return new XmlSelector.PosAttr( text.substring( 0, text.length()-3 ).trim() ).setNext( parent );
      String count = m.group( 2 );
      String tag = m.group( 3 );
      String direction = m.group( 9 );
      List<PosElementAttr> attrs = null;
      if ( ! tag.equals( "line" ) ) {
         tag = m.group( 4 );
         text = m.group( 3 ).replaceAll( "^<?\\w+>?", "" );
         if ( ! text.isEmpty() ) {
            attrs = new ArrayList<>();
            Matcher m2 = positionMatcher;
            if ( positionAttrMatcher == null ) m2 = positionAttrMatcher = tagPool.get( positionAttrRegx );
            do {
               if ( text.charAt( 0 ) == '[' ) text = text.substring( 1 ).trim();
               if ( ! m2.reset( text ).find() || m2.start() > 0 ) throw new CocoParseError( "Invalid attribute selector: " + text );

               String value = m2.group( 3 );
               if ( value != null ) value = Task.unquote( value );
               attrs.add( new PosElementAttr( m2.group( 1 ), m2.group( 2 ), value, m2.group( 4 ) != null ) );

               text = text.substring( m2.end() ).trim();
               if ( text.charAt( 0 ) == ']' ) text = text.substring( 1 ).trim();
            } while ( ! text.isEmpty() );
         }
      }
      return new XmlSelector.PosElement( count, tag, attrs, direction ).setNext( parent );
   }

   /************************************************************************************************************/
   // Results

   private int resultPosition;
   private List<Context> resultStack;
   private StringBuilder resultText;

   private class Context {
      Directive dir;
      IntRange range;

      private Context(Directive dir) {
         this.dir = dir;
         this.range = new IntRange( resultPosition );
      }
   }

   private void addToResult ( CharSequence text ) {
      if ( text.length() <= 0 ) return;
      if ( resultText == null ) resultText = new StringBuilder( 4096 );
      resultText.append( text );
      resultPosition += text.length();
   }

   private void addToResult ( Directive dir ) {
      if ( resultStack == null ) resultStack = new ArrayList<>();
      resultStack.add( new Context( dir ) );
   }

   private StringBuilder composeResult () {
      try {
         for ( Iterator<Context> i = resultStack.iterator() ; i.hasNext() ; ) {
            if ( shouldStop() ) throw new InterruptedException();
            final Context e = i.next();
            Task positionTask = null;
            XmlSelector cursor = null;

            // delete() task
            for ( Task task : e.dir.getTasks() ) try {
               if ( task.getAction() == Task.Action.DELETE && task.hasParams() ) {
                  for ( String param : task.getParams() ) {
                     final XmlSelector delSelector = parseSelector( param );
                     for ( IntRange delete : delSelector.locate( resultText, e.range ) ) {
                        System.out.println( "Delete: " + delSelector + ": " + delete + " " + delete.showInText( resultText ) );
                        if ( ! delete.isValid() || delete.length() <= 0 ) continue;
                        for ( Context c : resultStack ) c.range.shiftDeleted( delete, true );
                        resultText.delete( delete.start, delete.end );
                     }
                  }
               } else if ( task.getAction() == Task.Action.POSITION && task.hasParams() ) {
                  positionTask = task;
                  cursor = parseSelector( task.getParam( 0 ) );
               }
            } catch ( CocoParseError | CocoRunError ex ) {
               task.throwOrWarn( ex );
            }
            i.remove();

            // position() task
            if ( e.dir.getAction() == Directive.Action.OUTPUT ) continue;
            final Block block = e.dir.get();
            if ( block != null && block.hasData() && e.range.isValid() ) try {
               final CharSequence txt = block.getText();
               List<IntRange> position;
               if ( cursor != null ) position = cursor.locate( resultText, e.range );
               else position = Collections.singletonList( e.range );
               for ( IntRange range : position ) {
                  System.out.println( "Moves to: " + cursor + ": " + position + " " + range.showInText( resultText ) );
                  if ( range.length() > 0 ) {
                     resultText.delete( range.start, range.end );
                     for ( Context c : resultStack ) c.range.shiftDeleted( range, false );
                  }
                  resultText.insert( range.start, txt );
                  for ( Context c : resultStack ) c.range.shiftInserted( range.start, txt.length() );
               }
            } catch ( CocoRunError ex ) {
               positionTask.throwOrWarn( ex );
            }

         }
      } catch ( InterruptedException ex ) {
         Thread.currentThread().interrupt();
         return null;
      }
      return resultText;
   }

   /************************************************************************************************************/
   // Tag and matcher management

   public void reset () {
      close();
      resultPosition = 0;
      resultStack = null;
   }

   @Override public void close () {
      if ( startMatcher != null ) CocoUtils.tagPool.recycle( startTag, startMatcher );
      if (   endMatcher != null ) CocoUtils.tagPool.recycle( endTag, endMatcher );
      if ( attributeMatcher != null ) CocoUtils.tagPool.recycle( attrRegx, attributeMatcher );
      if ( parameterMatcher != null ) CocoUtils.tagPool.recycle( paramRegx, parameterMatcher );
      if ( firstParameterMatcher != null ) CocoUtils.tagPool.recycle( firstParamRegx, firstParameterMatcher );
      if ( positionMatcher  != null ) CocoUtils.tagPool.recycle( positionRegx, positionMatcher );
      if ( positionAttrMatcher   != null ) CocoUtils.tagPool.recycle( positionAttrRegx, positionAttrMatcher );
      startMatcher = null;
      endMatcher   = null;
      attributeMatcher = null;
      parameterMatcher = null;
      firstParameterMatcher = null;
      positionMatcher  = null;
      positionAttrMatcher   = null;
   }

   public String getStartTag () { return startTag; }
   public void setStartTag(String start_tag) { assert( start_tag != null ); this.startTag = start_tag; close(); }

   public String getEndTag () { return endTag; }
   public void setEndTag(String end_tag) { assert( end_tag != null ); this.endTag = end_tag; close(); }

   public void setTags ( String start, String end ) {
      if ( start != null ) setStartTag( start );
      if ( end != null ) setEndTag( end );
   }

   @Override public String toString() {
      return "ParserCoco(" + tagCount + " tages found: " + startTag + " ... " + endTag + ")";
   }
}