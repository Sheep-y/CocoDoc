package sheepy.cocodoc.worker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import sheepy.cocodoc.CocoMonitor;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.worker.directive.Directive;
import sheepy.cocodoc.worker.parser.Parser;
import sheepy.cocodoc.worker.parser.coco.ParserCoco;
import sheepy.cocodoc.worker.task.Task;
import sheepy.util.concurrent.AbstractFuture;

/**
 * A document processing block.
 */
public class Block extends AbstractFuture<Block> {
   static final Logger log = Logger.getLogger(Block.class.getName() );
   static {
      log.setLevel( Level.ALL );
   }

   private final Block parent;
   private final Directive directive;
   private File basePath;
   private Task outputTarget;

   private String name = "";
   private BlockStats stats = new BlockStats(this);

   public Block ( Block parent, Directive directive ) {
      this.parent = parent;
      this.directive = directive;
      if ( parent != null ) {
         basePath = parent.basePath;
         stats.cloneParentStat( parent.stats() );
      }
      directive.setBlock( this ); // Do throw NPE if null
      if ( directive.getContent() != null ) {
         setText( directive.getContent() );
         directive.setContent( null );
      }
   }

   @Override protected Block implRun () {
      if ( hasMonitor() ) getMonitor().start();

      for ( Task task : getTasks() ) task.init();
      for ( Task task : getTasks() ) {
         if ( Thread.currentThread().isInterrupted() ) return this;
         task.process();
      }

      if ( hasData() ) {
         if ( hasText() && getText().indexOf( "<?coco-postprocess " ) >= 0 ) {
            log.log( Level.FINE, "Post processing {0}", this );
            Parser postprocessor = new ParserCoco( true );
            postprocessor.start( this );
            setText( postprocessor.get() );
         }

         if ( getOutputTarget() != null ) {
            String fname = getOutputTarget().getParam( 0 );
            if ( ! fname.equals( "NUL" ) && ! fname.equals( "/dev/null" ) ) {
               File f = new File( getBasePath(), fname );
               byte[] data = getBinary();
               log.log( Level.INFO, "Writing {1} bytes to {0}.", new Object[]{ f, data.length } );
               try ( FileOutputStream out = new FileOutputStream( f, false ) ) {
                  out.write( data );
               } catch ( IOException ex ) {
                  if ( getOutputTarget().isThrowError() ) throw new CocoRunError( ex );
               }
            }
            setText( null );
         } else {
            if ( getParent() == null )
               System.out.println( getText() );
         }
      }

      if ( hasMonitor() ) getMonitor().done();
      return this;
   }

   /**************************************************************************************************/

   public Block getParent() {
      return this.parent;
   }

   public Block getRoot() {
      Block pos = this;
      while ( pos.parent != null )
         pos = pos.parent;
      return pos;
   }

   public Directive getDirective () { return directive; }
   public List<Task> getTasks ()    { return directive.getTasks(); }
   public boolean hasMonitor ()     { return directive.getMonitor() != null; }
   public CocoMonitor getMonitor () { return directive.getMonitor(); }

   public Block setName( CharSequence name ) {
      if ( name == null || name.length() <= 0 ) return this;
      if ( this.name.isEmpty() )
         this.name = name.toString();
      else
         this.name += ',' + name.toString();
      if ( hasMonitor() )
         getMonitor().setName( this.name );
      return this;
   }

   public BlockStats stats() {
      return stats;
   }

   /**************************************************************************************************/

   // There are three output modes:
   //  1. Initial (no data): both null
   //  2. Text mode  : textResult is non-null, binaryResult is null. call toBinary() to exit text mode.
   //  3. Binary mode: textResult is null, binaryResult is non-null. call toText()   to exit binary mode.
   private ByteArrayOutputStream binaryResult;
   private StringBuilder textResult;

   private List<Charset> toBinaryCharset;
   private List<Charset> toTextCharset;
   private Charset currentCharset;

   public boolean hasData()   { return hasText() || hasBinary(); }
   public boolean hasText()   { return textResult   != null && textResult.length() > 0; }
   public boolean hasBinary() { return binaryResult != null && binaryResult.size() > 0; }

   public byte[] getBinary() {
      toBinary( null );
      return hasBinary() ? binaryResult.toByteArray() : new byte[0];
   }

   public StringBuilder getText() {
      toText( null );
      if ( ! hasText() ) textResult = new StringBuilder();
      return textResult;
   }

   public Block appendBinary( byte[] data ) {
      try {
         toBinary( null );
         if ( ! hasBinary() ) setBinary( data );
         else binaryResult.write( data );
         return this;
      } catch ( IOException ex ) {
         throw new CocoRunError( ex );
      }
   }

   public void setBinary( byte[] data ) {
      if ( data == null ) {
         setText( null );
      } else try {
         if ( textResult   != null ) textResult.setLength( 0 );
         binaryResult = new ByteArrayOutputStream( data.length );
         binaryResult.write( data );
      } catch ( IOException ex ) {
         throw new CocoRunError( ex );
      }
   }

   public void setBinary( ByteArrayOutputStream data ) {
      if ( data == null ) {
         setText( null );
      } else {
         if ( data == binaryResult ) return;
         if ( textResult != null ) textResult.setLength( 0 );
         binaryResult = data;
      }
   }

   public void setText( CharSequence text ) {
      if ( text == null ) {
         binaryResult = null;
         textResult = null;
      } else {
         if ( text == textResult ) return;
         if ( binaryResult != null ) binaryResult.reset();
         if ( text instanceof StringBuilder ) {
            textResult = (StringBuilder) text;
         } else {
            textResult = new StringBuilder( text );
         }
      }
   }

   public void toBinary ( List<Charset> encoding ) {
      if ( encoding != null ) toBinaryCharset = new ArrayList<>( encoding );
      if ( hasBinary() || ! hasText() ) return;
      if ( toBinaryCharset == null || toBinaryCharset.isEmpty() ) toBinaryCharset = Collections.singletonList( Task.UTF8 );

      IOException error = null;
      int textLen = textResult.length();
      for ( Charset charset : toBinaryCharset ) try {
         setBinary( CharsetUtils.encode( textResult, CharsetUtils.strictEncoder( charset ) ) );
         currentCharset = charset;
         log.log( Level.FINER, "Converted {1} {0} characters to {2} bytes.", new Object[]{ charset, textLen, binaryResult.size() } );
         return;
      } catch ( IOException ex ) {
         error = ex;
      }
      String[] list = toBinaryCharset.stream().map( Charset::name ).toArray( String[]::new );
      throw new CocoRunError( "Cannot encode text in " + String.join( ", ", list ) , error );
   }

   public void toText ( List<Charset> encoding ) {
      if ( encoding != null ) toTextCharset = new ArrayList<>( encoding );
      if ( hasText() || ! hasBinary() ) return;
      if ( toTextCharset == null || toTextCharset.isEmpty() ) toTextCharset = Arrays.asList( Task.UTF8, Task.UTF16 );

      byte[] buf = binaryResult.toByteArray();
      IOException error = null;
      for ( Charset charset : toTextCharset ) try {
         setText(CharsetUtils.decode(buf, CharsetUtils.strictDecoder( charset ) ) );
         currentCharset = charset;
         log.log( Level.FINER, "Converted {2} bytes to {1} {0} characters.", new Object[]{ charset, textResult.length(), buf.length } );
         return;
      } catch ( CharacterCodingException ex ) {
         error = ex;
      }
      String[] list = toTextCharset.stream().map( Charset::name ).toArray( String[]::new );
      throw new CocoRunError( "Cannot decode text in " + String.join( ", ", list ) , error );
   }

   /** Return last used binary / text encoding. */
   public Charset getCurrentCharset() {
      return currentCharset;
   }

   /************************************************************************************************/

   public File getBasePath() { return basePath; }
   public Block setBasePath( File basePath ) { this.basePath = basePath; return this; }

   public Task getOutputTarget() { return outputTarget; }
   public void setOutputTarget( Task output ) {
      log.log( Level.FINE, "Set output target to {0}", output == null ? null : output.getParam( 0 ) );
      this.outputTarget = output;
   }

   private Parser parser;
   public Parser getParser () { return parser; }
   public void setParser ( Parser currentParser ) { this.parser = currentParser; }

   @Override public String toString() {
      if ( hasText() )
         return "Block " + name + " (" + textResult.length()+ " characters)";
      else if ( hasBinary() )
         return "Block " + name + " (" + binaryResult.size() + " bytes)";
      else
         return "Block " + name + " (no data)";
   }
}